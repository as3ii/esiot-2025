package org.cus;

import org.cus.controller.ApiHandler;
import org.cus.controller.CusController;
import org.cus.controller.SimpleLogger;
import org.cus.model.MqttManager;
import org.cus.model.SerialManager;
import org.eclipse.paho.client.mqttv3.MqttException;
import jssc.SerialPort;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Application entry-point class.
 */
@Command(mixinStandardHelpOptions = true, version = "0.0.1")
public final class CUS implements Runnable {

    private static final String CLASS_NAME = CUS.class.getName();
    private static final Logger LOGGER = SimpleLogger.getLogger(CLASS_NAME);

    @Option(names = {"-b", "--baud-rate"}, description = "Serial baud rate",
        defaultValue = "${env:BAUD_RATE:-115200}")
    private int baudRate;
    @Option(names = {"-s", "--serial-port"}, description = "Serial port name/path",
        defaultValue = "${env:SERIAL_PORT:-/dev/ttyACM0}")
    private String serialPort;
    @Option(names = {"-r", "--broker"}, description = "MQTT broker address",
        defaultValue = "${env:BROKER_ADDRESS:-broker.mqtt-dashboard.com}")
    private String mqttBroker;
    @Option(names = {"-t", "--topic"}, description = "MQTT topic",
        defaultValue = "${env:TOPIC:-esiot-2025-ld}")
    private String mqttTopic;
    @Option(names = {"-i", "--http-ip"}, description = "IP to bind for HTTP server",
        defaultValue = "${env:HTTP_IP:-0.0.0.0}")
    private String httpIp;
    @Option(names = {"-p", "--port"}, description = "Port to bind for HTTP server",
        defaultValue = "${env:HTTP_PORT:-8080}")
    private int httpPort;
    @Option(names = {"-d", "--static-dir"}, description = "Path to static files to serve",
        defaultValue = "${env:STATIC_DIR:-../dbs}")
    private String staticDir;
    @Option(names = {"-v", "--verbose"}, description = "Enable verbose logging",
        defaultValue = "${env:VERBOSE:-false}")
    private boolean verbose;
    @Option(names = "--warning-level", description = "Warning water level threshold",
        defaultValue = "${env:WARNING_LEVEL:-50.0}")
    private float warningLevel;
    @Option(names = "--critical-level", description = "Critical water level threshold",
        defaultValue = "${env:CRITICAL_LEVEL:-80.0}")
    private float criticalLevel;
    @Option(names = "--warning-ms", description = "Time above warning level before opening valve (ms)",
        defaultValue = "${env:WARNING_MS:-5000}")
    private long warningMs;
    @Option(names = "--tms-timeout-ms", description = "TMS connection timeout (ms)",
        defaultValue = "${env:TMS_TIMEOUT_MS:-10000}")
    private long tmsTimeoutMs;
    @Option(names = "--dedup-ms", description = "Serial send dedup interval (ms)",
        defaultValue = "${env:DEDUP_MS:-500}")
    private long dedupSerialTxMs;
    @Option(names = "--poll-interval-ms", description = "Scheduler poll interval (ms)",
        defaultValue = "${env:POLL_INTERVAL_MS:-1000}")
    private int schedulerPeriodMs;
    @Option(names = "--history-length", description = "Max readings in history",
        defaultValue = "${env:HISTORY_LENGTH:-1000}")
    private int historyLength;

    private CUS() { }

    static {
        // https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Formatter.html
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%4$s: %1$tF %1$tT %2$s: %5$s%6$s%n");
    }

    /**
     * Application entry-point.
     *
     * @param args input parameters
     */
    public static void main(final String[] args) {
        System.exit(new CommandLine(new CUS()).execute(args));
    }

     @Override
     public void run() {
        SimpleLogger.setLevel(verbose ? Level.ALL : Level.INFO);

        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(mainThread::interrupt));

        final String clientId = "backend-esiot-2025-" + System.currentTimeMillis();
        if (staticDir == null || staticDir.isBlank() || !new File(staticDir).isDirectory()) {
            throw new IllegalArgumentException("Not a valid directory: " + staticDir);
        }
        final File dir = new File(staticDir);

        final var config = new CusController.Config(warningLevel, criticalLevel, warningMs,
                tmsTimeoutMs, dedupSerialTxMs, schedulerPeriodMs, historyLength);

        try (SerialManager serialManager = new SerialManager(serialPort, baudRate,
                SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
             CusController controller = new CusController(serialManager, config);
             MqttManager mqttManager = new MqttManager(mqttBroker, clientId, // NOPMD(UnusedLocalVariable)
                 mqttTopic, controller::levelConsumer);
             ApiHandler apiHandler = new ApiHandler(httpIp, httpPort, controller, dir); // NOPMD(UnusedLocalVariable)
        ) {
            Thread.currentThread().join();
        } catch (final MqttException | IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Runtime error", e);
        }
     }
}
