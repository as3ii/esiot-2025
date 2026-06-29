package org.cus.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cus.controller.SimpleLogger;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

/**
 * Manages the serial interface, its configuration and the message passing.
 */
public class SerialManager extends AbstractCommManager {

    private static final String CLASS_NAME = SerialManager.class.getName();
    private static final Logger LOGGER = SimpleLogger.getLogger(CLASS_NAME);
    private static final String SEND_COMMAND_M = "sendCommand";
    private static final int SLEEP_MS = 3000;

    private static final Set<Integer> BAUD_RATES = Set.of(
            SerialPort.BAUDRATE_9600,
            SerialPort.BAUDRATE_19200,
            SerialPort.BAUDRATE_38400,
            SerialPort.BAUDRATE_57600,
            SerialPort.BAUDRATE_115200);
    private static final Set<Integer> DATA_BITS = Set.of(
            SerialPort.DATABITS_5,
            SerialPort.DATABITS_6,
            SerialPort.DATABITS_7,
            SerialPort.DATABITS_8);
    private static final Map<String, Integer> STOP_BITS = Map.ofEntries(
            Map.entry("1", SerialPort.STOPBITS_1),
            Map.entry("1.5", SerialPort.STOPBITS_1_5),
            Map.entry("2", SerialPort.STOPBITS_2));
    private static final Map<String, Integer> PARITY = Map.ofEntries(
            Map.entry("None", SerialPort.PARITY_NONE),
            Map.entry("Odd", SerialPort.PARITY_ODD),
            Map.entry("Even", SerialPort.PARITY_EVEN),
            Map.entry("Mark", SerialPort.PARITY_MARK),
            Map.entry("Space", SerialPort.PARITY_SPACE));
    private static Set<String> ports; // Initialized on first call of `getPorts()`

    private final List<Byte> rxBuffer = new ArrayList<>();
    private final ExecutorService threadPool = Executors.newVirtualThreadPerTaskExecutor();
    private final String port;
    private final int baudRate;
    private final int dataBits;
    private final int stopBits;
    private final int parity;
    private volatile SerialPort serialPort;
    private boolean reconnectionPending;
    private Runnable resyncFunction;

    /**
     * @param port name of the serial port
     * @param baudRate baud rate
     * @param dataBits data bits per byte
     * @param stopBits stop bits settings
     * @param parity parity settings
     * @throws SerialPortException if exception occurred
     */
    public SerialManager(final String port, final int baudRate, final int dataBits,
            final int stopBits, final int parity) throws SerialPortException {
        super();

        this.port = port;
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;

        connect();
    }

    @SuppressFBWarnings(
        value = "SWL_SLEEP_WITH_LOCK_HELD",
        justification = "serialPort need to be locked while connecting"
    )
    private synchronized void connect() throws SerialPortException {
        serialPort = new SerialPort(port);
        serialPort.openPort();
        serialPort.setParams(baudRate, dataBits, stopBits, parity);
        serialPort.setEventsMask(SerialPort.MASK_RXCHAR);

        // Wait for arduino to restart, ignoring the first messages
        try {
            Thread.sleep(SLEEP_MS);
        } catch (final InterruptedException e) {
            // This should never happen, in theory
            serialPort.closePort();
            serialPort = null;
            Thread.currentThread().interrupt();
            return;
        }

        serialPort.purgePort(SerialPort.PURGE_RXCLEAR);
        serialPort.addEventListener(event -> {
            // No need to synchronize this, this is the only reader
            if (event.isRXCHAR()) {
                try {
                    final byte[] msg = serialPort.readBytes(event.getEventValue());
                    for (final byte b : msg) {
                        // System.out.println((char)b);
                        if (b == '\r') {
                            continue; // Ignore carriage return
                        }
                        if ((char) b == '\n') {
                            // End of a message reached, enqueuing it and cleaning rxBuffer
                            rxEnqueue(rxBuffer.toArray(Byte[]::new));
                            rxBuffer.clear();
                        } else {
                            rxBuffer.add(b);
                        }
                    }
                } catch (final InterruptedException | SerialPortException e) {
                    LOGGER.logp(Level.SEVERE, CLASS_NAME, "SerialPortEventListener", e.getMessage(),
                            e);
                }
            }
        });
    }

    private synchronized void reconnect() {
        if (isConnected()) {
            reconnectionPending = false;
            return;
        }
        if (serialPort != null) {
            try {
                serialPort.closePort();
            } catch (final SerialPortException e) {
                // Log and ignore
                LOGGER.logp(Level.SEVERE, CLASS_NAME, "reconnect", "Failed closing serial port", e);
            }
        }
        rxBuffer.clear();
        clearQueues();
        try {
            connect();
            if (resyncFunction != null) {
                resyncFunction.run();
            }
        } catch (final SerialPortException e) {
            serialPort = null;
        }
        reconnectionPending = false;
    }

    /**
     * Set resync function, to call after reconnection.
     *
     * @param r function to call
     */
    public synchronized void setResyncFunction(final Runnable r) {
        resyncFunction = r;
    }

    /**
     * Send the given command.
     *
     * @param tx the {@code TXCommand} to send
     * @param value optional/nullable value to send
     * @return {@code CompletableFuture} containing the response as {@code Result}
     */
    @Override
    public CompletableFuture<Result<?>> sendCommand(final TXCommand tx, @Nullable final byte[] value) {
        final CompletableFuture<Result<?>> future = new CompletableFuture<>();
        final RXCommand rx = RXCommand.expectedFrom(tx);
        futurePut(rx, future);

        // Apply timeout of 1 second
        future.orTimeout(1, TimeUnit.SECONDS).exceptionally(e -> {
            futureRemove(rx, future);
            return new Result.Err<>("Timeout");
        });

        threadPool.execute(() -> {
            synchronized (this) {
                if (!isConnected()) {
                    // Handle serial reconnection
                    if (!reconnectionPending) {
                        reconnectionPending = true;
                        threadPool.execute(this::reconnect);
                    }
                    future.complete(new Result.Err<>("Serial port not connected"));
                    futureRemove(rx, future);
                    return;
                }
                if (future.isDone()) {
                    return;
                }
                try {
                    if (value != null) {
                        LOGGER.logp(Level.FINE, CLASS_NAME, SEND_COMMAND_M,
                            "Sending Q:" + tx.getInt() + "|" + Arrays.toString(value));
                        final byte[] start = {'Q', ':', tx.getValue(), '|'};
                        final byte[] result = new byte[start.length + value.length + 1];
                        System.arraycopy(start, 0, result, 0, start.length);
                        System.arraycopy(value, 0, result, start.length, value.length);
                        result[start.length + value.length] = '\n';
                        serialPort.writeBytes(result);
                    } else {
                        LOGGER.logp(Level.FINE, CLASS_NAME, SEND_COMMAND_M, "Sending Q:" + tx.getInt());
                        serialPort.writeBytes(
                            new byte[]{'Q', ':', tx.getValue(), '\n'}
                        );
                    }
                } catch (final SerialPortException e) {
                    LOGGER.logp(Level.SEVERE, CLASS_NAME, SEND_COMMAND_M, e.getMessage(), e);
                    future.completeExceptionally(e);
                    futureRemove(rx, future);
                    try {
                        serialPort.closePort();
                    } catch (final SerialPortException ignored) {
                        // Do nothing
                    }
                    serialPort = null;
                }
            }
        });

        return future;
    }

    /**
     * Check if the serial port is connected.
     *
     * @return true if the connection is established
     */
    public synchronized boolean isConnected() {
        return serialPort != null && serialPort.isOpened();
    }

    /**
     * Remove the event listener and close the port.
     *
     * @throws SerialPortException if exception occurred
     */
    @Override
    public synchronized void close() throws SerialPortException {
        if (serialPort != null) {
            serialPort.closePort();
            serialPort = null;
        }
        threadPool.close();
    }

    /**
     * Refresh the available serial ports.
     */
    public static void refreshPorts() {
        ports = Set.of(SerialPortList.getPortNames());
    }

    /**
     * Get the available ports.
     *
     * @return Set of available ports, can be empty
     */
    public static Set<String> getPorts() {
        if (ports == null) {
            refreshPorts();
        }
        return Set.copyOf(ports);
    }

    /**
     * Get common baud rates.
     *
     * @return Set of common baud rates
     */
    public static Set<Integer> getBaudRates() {
        return BAUD_RATES;
    }

    /**
     * Get supported number of data bits per byte.
     *
     * @return Set of supported number of data bits
     */
    public static Set<Integer> getDataBits() {
        return DATA_BITS;
    }

    /**
     * Get supported stop bits.
     *
     * @return Map of supported stop bits: key is the human-readable representation, value is the
     *         value to be used for SerialManager()
     */
    public static Map<String, Integer> getStopBits() {
        return Map.copyOf(STOP_BITS);
    }

    /**
     * Get supported stop bits.
     *
     * @return Map of supported parity settings: key is the human-readable representation, value is
     *         the value to be used for SerialManager()
     */
    public static Map<String, Integer> getParity() {
        return Map.copyOf(PARITY);
    }
}
