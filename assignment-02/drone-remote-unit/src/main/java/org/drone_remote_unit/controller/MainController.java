package org.drone_remote_unit.controller;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.drone_remote_unit.model.RXCommand;
import org.drone_remote_unit.model.Result;
import org.drone_remote_unit.model.SerialManager;
import org.drone_remote_unit.model.State;
import org.drone_remote_unit.model.TXCommand;
import org.drone_remote_unit.view.IView;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jssc.SerialPortException;

/**
 * Implementation of the Controller interface.
 */
public final class MainController implements IController {

    private static final String CLASS_NAME = MainController.class.getName();
    private static final Logger LOGGER = SimpleLogger.getLogger(CLASS_NAME);

    private final IView view;
    private SerialManager serialManager;

    /**
     * MainController constructor.
     *
     * @param view the current window shown
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Controller needs write access to the view"
    )
    public MainController(final IView view) {
        this.view = view;
    }

    private void pushSerial(final TXCommand tx, @Nullable final Object data) {
        final String msg = tx.toString()
            + " -> "
            + RXCommand.expectedFrom(tx).toString()
            + " ("
            + (data != null ? data.toString() : "null")
            + ")\n";
        view.pushSerialLine(msg);
    }

    private void pushSerialErr(final TXCommand tx, final String err) {
        final String msg = tx.toString()
            + " -> "
            + RXCommand.INVALID.toString()
            + " ("
            + err
            + ")\n";
        view.pushSerialLine(msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initSerial(
        final String port, final int baudRate, final int dataBits,
        final int stopBits, final int parity
    ) throws SerialPortException {
        serialManager = new SerialManager(port, baudRate, dataBits, stopBits, parity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeSerial() throws SerialPortException {
        if (serialManager != null) {
            serialManager.close();
            serialManager = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConnected() {
        return serialManager != null && serialManager.isConnected();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queryState() {
        serialManager.sendCommand(TXCommand.GET_STATE).thenApply(
            r -> (Result<State>) r
        ).thenAccept(response -> {
            switch (response) {
                case Result.Ok(State s) -> {
                    view.setState(s);
                    pushSerial(TXCommand.GET_STATE, s);
                }
                case Result.Err(String e) -> {
                    view.showAlert(e, true);
                    pushSerialErr(TXCommand.GET_STATE, e);
                }
            }
        }).exceptionally(e -> {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "queryState", e.getMessage(), e);
            view.showAlert(e.getMessage(), true);
            return null;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestTakeOff() {
        serialManager.sendCommand(TXCommand.REQ_TAKE_OFF).thenAccept(response -> {
            switch (response) {
                case Result.Ok(Object t) -> { // NOPMD(UnusedLocalVariable)
                    view.showAlert(RXCommand.ACK_TAKE_OFF.toString(), false);
                    pushSerial(TXCommand.REQ_TAKE_OFF, null);
                }
                case Result.Err(String e) -> {
                    view.showAlert(e, true);
                    pushSerialErr(TXCommand.REQ_TAKE_OFF, e);
                }
            }
        }).exceptionally(e -> {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "requestTakeOff", e.getMessage(), e);
            view.showAlert(e.getMessage(), true);
            return null;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestLanding() {
        serialManager.sendCommand(TXCommand.REQ_LANDING).thenAccept(response -> {
            switch (response) {
                case Result.Ok(Object t) -> { // NOPMD(UnusedLocalVariable)
                    view.showAlert(RXCommand.ACK_LANDING.toString(), false);
                    pushSerial(TXCommand.REQ_LANDING, null);
                }
                case Result.Err(String e) -> {
                    view.showAlert(e, true);
                    pushSerialErr(TXCommand.REQ_LANDING, e);
                }
            }
        }).exceptionally(e -> {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "requestLanding", e.getMessage(), e);
            view.showAlert(e.getMessage(), true);
            return null;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queryTemperature() {
        serialManager.sendCommand(TXCommand.GET_TEMPERATURE).thenApply(
            r -> (Result<Float>) r
        ).thenAccept(response -> {
            switch (response) {
                case Result.Ok(Float t) -> {
                    view.setTemperature(t);
                    pushSerial(TXCommand.GET_TEMPERATURE, t);
                }
                case Result.Err(String e) -> {
                    view.showAlert(e, true);
                    pushSerialErr(TXCommand.GET_TEMPERATURE, e);
                }
            }
        }).exceptionally(e -> {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "queryTemperature", e.getMessage(), e);
            view.showAlert(e.getMessage(), true);
            return null;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queryDistance() {
        serialManager.sendCommand(TXCommand.GET_DISTANCE).thenApply(
            r -> (Result<Float>) r
        ).thenAccept(response -> {
            switch (response) {
                case Result.Ok(Float t) -> {
                    view.setDistance(t);
                    pushSerial(TXCommand.GET_DISTANCE, t);
                }
                case Result.Err(String e) -> {
                    view.showAlert(e, true);
                    pushSerialErr(TXCommand.GET_DISTANCE, e);
                }
            }
        }).exceptionally(e -> {
            LOGGER.logp(Level.WARNING, CLASS_NAME, "queryDistance", e.getMessage(), e);
            view.showAlert(e.toString(), true);
            return null;
        });
    }
}
