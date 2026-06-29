package org.cus.controller;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.cus.model.LocalState;
import org.cus.model.Reading;
import org.cus.model.Result;
import org.cus.model.SerialManager;
import org.cus.model.TXCommand;
import org.cus.model.TmsState;
import org.cus.model.WcsState;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Main CUS controller.
 */
public final class CusController implements AutoCloseable {

    private static final String CLASS_NAME = CusController.class.getName();
    private static final Logger LOGGER = SimpleLogger.getLogger(CLASS_NAME);

    private static final int VALVE_OPEN = 100;
    private static final int VALVE_HALF_OPEN = 50;
    private static final int VALVE_CLOSED = 0;

    private static final String POLLER_GET_OPENING_M = "poller/asyncGetOpening";
    private static final String POLLER_GET_WCSSTATE_M = "poller/asyncGetWcsState";
    private static final String SET_OPENING_M = "setOpening";
    private static final String SET_CONNECTED_M = "setConnected";
    private static final String SET_UNCONNECTED_M = "setUnconnected";
    private static final String OK = "Ok";

    private final SerialManager serialManager;
    private final float warningLevel;
    private final float criticalLevel;
    private final long warningMs;
    private final long tmsTimeoutMs;
    private final long dedupSerialTxMs;
    private final int schedulerPeriodMs; // In milliseconds
    private final int historyLength;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final List<Reading> history = new ArrayList<>();

    // Volatile, have synchronization inside callbacks
    private volatile TmsState tmsState = TmsState.CONNECTED; // Updated with the correct value at runtime
    private volatile WcsState wcsState = WcsState.AUTOMATIC;
    private volatile LocalState localState = LocalState.AUTOMATIC;
    private volatile int valveOpening = VALVE_CLOSED;
    private long lastLevelTimestamp = System.currentTimeMillis();
    private long lastSentSetConnected;
    private long lastSentSetUnconnected;
    private long lastSentSetOpening;
    private long aboveWarningLevelSince = -1;
    private long aboveCriticalLevelSince = -1;
    private long lastSeq = -1;

    /**
     * Controller constructor.
     *
     * @param serial initialized serial manager
     * @param config controller configuration
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "CusController needs write access to SerialManager"
    )
    public CusController(final SerialManager serial, final Config config) {
        this.serialManager = serial;
        this.warningLevel = config.warningLevel();
        this.criticalLevel = config.criticalLevel();
        this.warningMs = config.warningMs();
        this.tmsTimeoutMs = config.tmsTimeoutMs();
        this.dedupSerialTxMs = config.dedupSerialTxMs();
        this.schedulerPeriodMs = config.schedulerPeriodMs();
        this.historyLength = config.historyLength();

        serial.setResyncFunction(this::resyncWcs);

        scheduler.scheduleWithFixedDelay(this::poller,
            schedulerPeriodMs,
            schedulerPeriodMs,
            TimeUnit.MILLISECONDS);
    }

    private synchronized void poller() {
        final long now = System.currentTimeMillis();
        if (tmsState == TmsState.CONNECTED && lastLevelTimestamp > 0 && (now - lastLevelTimestamp) > tmsTimeoutMs) {
            LOGGER.log(Level.WARNING, "TMS timeout, entering Unconnected state");
                setUnconnected();
        }

        serialManager.sendCommand(TXCommand.GET_OPENING, null).thenAccept(response -> {
            switch (response) {
                case Result.Ok<?> ok when ok.value() instanceof Integer i -> {
                    valveOpening = i;
                }
                case Result.Ok<?> ok -> {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, POLLER_GET_OPENING_M,
                        "Unexpected Ok value: " + ok.value() + "(" + ok.value().getClass().getSimpleName() + ")");
                }
                case Result.Err(String e) -> {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, POLLER_GET_OPENING_M, e);
                }
            }
        }).exceptionally(e -> {
            LOGGER.logp(Level.WARNING, CLASS_NAME, POLLER_GET_OPENING_M, e.getMessage(), e);
            return null;
        });

        serialManager.sendCommand(TXCommand.GET_STATE, null).thenAccept(response -> {
            switch (response) {
                case Result.Ok<?> ok when ok.value() instanceof WcsState s -> {
                    wcsState = s;
                }
                case Result.Ok<?> ok -> {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, POLLER_GET_WCSSTATE_M,
                        "Unexpected Ok value: " + ok.value() + "(" + ok.value().getClass().getSimpleName() + ")");
                }
                case Result.Err(String e) -> {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, POLLER_GET_WCSSTATE_M, e);
                }
            }
        }).exceptionally(e -> {
            LOGGER.logp(Level.WARNING, CLASS_NAME, POLLER_GET_WCSSTATE_M, e.getMessage(), e);
            return null;
        });

    }

    private void addToHistory(final float level) {
        history.add(new Reading(level, System.currentTimeMillis()));
        if (history.size() > historyLength) {
            history.removeFirst();
        }
    }

    synchronized void resyncWcs() {
        LOGGER.log(Level.INFO, "Resyncing WCS");
        if (tmsState == TmsState.CONNECTED) {
            tmsState = TmsState.UNCONNECTED; // Elude the checks in setConnected()
            setConnected();
        } else {
            tmsState = TmsState.CONNECTED; // Elude the checks in setUnconnected()
            setUnconnected();
        }
        if (localState == LocalState.MANUAL) {
            setOpening(valveOpening);
        } // else: synced at next call of levelConsumer
    }

    /**
     * Consumer for MQTT parsed messages.
     *
     * @param seq sequence number, provided by the sender
     * @param level the parsed level
     */
    public synchronized void levelConsumer(final long seq, final float level) {
        // If Unconnected, restore to Connected
        if (tmsState == TmsState.UNCONNECTED) {
            setConnected();
            lastSeq = -1; // Handle eventual TMS reset
        }

        // Deduplicate messages
        if (seq <= lastSeq) {
            LOGGER.log(Level.INFO, "Discarded an old message");
            return;
        }
        lastSeq = seq;

        // Check timings and level (level: low number = high real level)
        final long now = System.currentTimeMillis();
        lastLevelTimestamp = now;
        if (level <= criticalLevel && aboveCriticalLevelSince < 0) {
            aboveWarningLevelSince = -1;
            aboveCriticalLevelSince = now;
        } else if (level > criticalLevel && level <= warningLevel && aboveWarningLevelSince < 0) {
            aboveWarningLevelSince = aboveCriticalLevelSince > 0 ? now - warningMs : now;
            aboveCriticalLevelSince = -1;
        } else if (level > warningLevel) {
            aboveCriticalLevelSince = -1;
            aboveWarningLevelSince = -1;
        }
        // If needed open valve
        if (wcsState == WcsState.AUTOMATIC && localState == LocalState.AUTOMATIC) {
            // Send command only once
            if (aboveCriticalLevelSince > 0 && valveOpening != VALVE_OPEN) {
                setOpening(VALVE_OPEN);
            } else if (aboveWarningLevelSince > 0 && (now - aboveWarningLevelSince) >= warningMs
                    && valveOpening != VALVE_HALF_OPEN) {
                setOpening(VALVE_HALF_OPEN);
            } else if (aboveCriticalLevelSince < 0 && aboveWarningLevelSince < 0
                    && valveOpening != VALVE_CLOSED) {
                setOpening(VALVE_CLOSED);
            }
        }
        addToHistory(level);
    }

    /**
     * Get {@code WcsState} from the remote system.
     *
     * @return last valid {@code WcsState}
     */
    public synchronized WcsState getWcsState() {
        return wcsState;
    }

    /**
     * Get {@code TmsState} from the remote system.
     *
     * @return last valid {@code TmsState}
     */
    public synchronized TmsState getTmsState() {
        return tmsState;
    }

    /**
     * Get {@code LocalState} of the system.
     *
     * @return last valid {@code LocalState}
     */
    public synchronized LocalState getLocalState() {
        return localState;
    }

    /**
     * Set {@code LocalState} of the system.
     *
     * @param state the state to set
     */
    public synchronized void setLocalState(final LocalState state) {
        localState = state;
    }

    /**
     * Get the reading history.
     *
     * @return List of the last recorded water level and receive timestamp.
     */
    public synchronized List<Reading> getLevelHistory() {
        return List.copyOf(history);
    }

    /**
     * Get current valve opening.
     *
     * @return last valid valve opening
     */
    public synchronized int getOpening() {
        return valveOpening;
    }

    /**
     * Set the valve opening. Do nothing if WCS is in manual mode.
     *
     * @param opening the valve opening in range [0..100]
     */
    public synchronized void setOpening(final int opening) {
        final long now = System.currentTimeMillis();
        if ((now - lastSentSetOpening) < dedupSerialTxMs || wcsState == WcsState.MANUAL) {
            return;
        }
        lastSentSetOpening = now;

        serialManager.sendCommand(TXCommand.SET_OPENING, String.valueOf(opening).getBytes(StandardCharsets.US_ASCII))
        .thenAccept(response -> {
            switch (response) {
                case Result.Ok(Object t) -> { // NOPMD(UnusedLocalVariable)
                    LOGGER.logp(Level.FINE, CLASS_NAME, SET_OPENING_M, OK);
                }
                case Result.Err(String e) -> {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, SET_OPENING_M, e);
                }
            }
        }).exceptionally(e -> {
            LOGGER.logp(Level.WARNING, CLASS_NAME, SET_OPENING_M, e.getMessage(), e);
            return null;
        });
    }

    /**
     * Tell WCS that TMS is connected.
     */
    public synchronized void setConnected() {
        final long now = System.currentTimeMillis();
        if ((now - lastSentSetConnected) < dedupSerialTxMs || tmsState == TmsState.CONNECTED) {
            return;
        }
        lastSentSetConnected = now;

        serialManager.sendCommand(TXCommand.SET_CONNECTED, null).thenAccept(response -> {
            switch (response) {
                case Result.Ok(Object t) -> { // NOPMD(UnusedLocalVariable)
                    tmsState = TmsState.CONNECTED;
                    LOGGER.logp(Level.FINE, CLASS_NAME, SET_CONNECTED_M, OK);
                }
                case Result.Err(String e) -> {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, SET_CONNECTED_M, e);
                }
            }
        }).exceptionally(e -> {
            LOGGER.logp(Level.WARNING, CLASS_NAME, SET_CONNECTED_M, e.getMessage(), e);
            return null;
        });
    }

    /**
     * Tell WCS that TMS is unconnected.
     */
    public synchronized void setUnconnected() {
        final long now = System.currentTimeMillis();
        if ((now - lastSentSetUnconnected) < dedupSerialTxMs || tmsState == TmsState.UNCONNECTED) {
            return;
        }
        lastSentSetUnconnected = now;

        serialManager.sendCommand(TXCommand.SET_UNCONNECTED, null).thenAccept(response -> {
            switch (response) {
                case Result.Ok(Object t) -> { // NOPMD(UnusedLocalVariable)
                    tmsState = TmsState.UNCONNECTED;
                    LOGGER.logp(Level.FINE, CLASS_NAME, SET_UNCONNECTED_M, OK);
                }
                case Result.Err(String e) -> {
                    LOGGER.logp(Level.WARNING, CLASS_NAME, SET_UNCONNECTED_M, e);
                }
            }
        }).exceptionally(e -> {
            LOGGER.logp(Level.WARNING, CLASS_NAME, SET_UNCONNECTED_M, e.getMessage(), e);
            return null;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        scheduler.shutdownNow();
    }

    /**
     * Configuration parameters for CusController.
     *
     * @param warningLevel value of the warning level
     * @param criticalLevel value of the critical level
     * @param warningMs milliseconds to wait before triggering the warning action
     * @param tmsTimeoutMs TMS timeout in milliseconds
     * @param dedupSerialTxMs milliseconds to wait before sending again the same command
     * @param schedulerPeriodMs scheduler period in milliseconds
     * @param historyLength history length
     */
    public record Config(
        float warningLevel,
        float criticalLevel,
        long warningMs,
        long tmsTimeoutMs,
        long dedupSerialTxMs,
        int schedulerPeriodMs,
        int historyLength
    ) { }
}
