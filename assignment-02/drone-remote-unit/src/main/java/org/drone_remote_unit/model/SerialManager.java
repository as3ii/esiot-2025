package org.drone_remote_unit.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.drone_remote_unit.controller.SimpleLogger;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

/**
 * Manages the serial interface, its configuration and the message passing.
 */
public class SerialManager extends AbstractCommManager {

    private static final String CLASS_NAME = SerialManager.class.getName();
    private static final Logger LOGGER = SimpleLogger.getLogger(CLASS_NAME);
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
    private volatile SerialPort serialPort;

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
        serialPort = new SerialPort(port);
        serialPort.openPort();
        serialPort.setParams(baudRate, dataBits, stopBits, parity);
        serialPort.setEventsMask(SerialPort.MASK_RXCHAR);

        // Wait for arduino to restart, ignoring the first messages
        try {
            Thread.sleep(SLEEP_MS);
        } catch (final InterruptedException e) {
            // This should never happen, in theory
            assert true;
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
                            // LOGGER.logp(
                            //     Level.FINE,
                            //     CLASS_NAME,
                            //     "SerialPortEventListener",
                            //     "Enqueued: " + String.valueOf(convert(rxBuffer.toArray(Byte[]::new)))
                            // );
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


    /**
     * Send the given command.
     *
     * @param tx the {@code TXCommand} to send
     * @return {@code CompletableFuture} containing the response as {@code Result}
     */
    @Override
    public CompletableFuture<Result<?>> sendCommand(final TXCommand tx) {
        final CompletableFuture<Result<?>> future = new CompletableFuture<>();
        final RXCommand rx = RXCommand.expectedFrom(tx);
        futurePut(rx, future);

        // Apply timeout of 1 second
        future.orTimeout(1, TimeUnit.SECONDS).exceptionally(e -> {
            futureRemove(rx);
            return new Result.Err<>("Timeout");
        });

        threadPool.execute(() -> {
            synchronized (serialPort) {
                if (serialPort != null) {
                    try {
                        LOGGER.logp(Level.FINE, CLASS_NAME, "sendCommand", "Sending Q:" + tx.getInt());
                        serialPort.writeBytes(
                            new byte[]{'Q', ':', tx.getValue(), '\n'}
                        );
                    } catch (final SerialPortException e) {
                        LOGGER.logp(Level.SEVERE, CLASS_NAME, "sendCommand", e.getMessage(), e);
                        future.completeExceptionally(e);
                        futureRemove(rx);
                    }
                } else {
                    future.complete(new Result.Err<>("Serial port not connected"));
                    futureRemove(rx);
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
    public boolean isConnected() {
        return serialPort != null && serialPort.isOpened();
    }

    /**
     * Remove the event listener and close the port.
     *
     * @throws SerialPortException if exception occurred
     */
    @Override
    public void close() throws SerialPortException {
        if (serialPort != null) {
            serialPort.closePort();
            serialPort = null;
        }
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
