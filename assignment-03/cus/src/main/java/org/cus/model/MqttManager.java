package org.cus.model;

import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cus.controller.SimpleLogger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Manages the MQTT client.
 */
public class MqttManager implements AutoCloseable {

    private static final String CLASS_NAME = MqttManager.class.getName();
    private static final Logger LOGGER = SimpleLogger.getLogger(CLASS_NAME);
    /*
     * - 0: at most once (minimum)
     * - 1: at least once
     * - 2: exactly once (maximum)
     */
    private static final int QOS = 1;
    private static final int TIMEOUT = 30;

    private final MqttClient client;
    private final MqttConnectOptions connectionOptions;

    /**
     * MqttManager constructor.
     *
     * @param broker domain/IP of the MQTT broker (without starting "tcp://")
     * @param clientId client ID
     * @param topic topic to subscribe
     * @param levelConsumer consumer function that handles the level received
     * @throws MqttException for communication or connection errors
     */
    public MqttManager(final String broker, final String clientId, final String topic,
        final BiConsumer<Long, Float> levelConsumer
    ) throws MqttException {
        client = new MqttClient("tcp://" + broker, clientId, new MemoryPersistence());

        connectionOptions = new MqttConnectOptions();
        connectionOptions.setAutomaticReconnect(true);
        connectionOptions.setCleanSession(true); // Discard unsent messages
        connectionOptions.setConnectionTimeout(TIMEOUT); // The default

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(final Throwable cause) {
                LOGGER.log(Level.WARNING, "MQTT connection lost", cause);
            }

            @Override
            public void messageArrived(final String topic, final MqttMessage message) throws Exception {
                final String payload = new String(message.getPayload(), StandardCharsets.US_ASCII);
                LOGGER.log(Level.FINE, "MQTT received: " + payload);
                try {
                    final JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();
                    final float level = obj.get("level").getAsFloat();
                    final long seq = obj.get("seq").getAsLong();
                    levelConsumer.accept(seq, level);
                } catch (final JsonParseException | IllegalStateException e) {
                    LOGGER.log(Level.WARNING, "Failed to parse TMS message: " + payload, e);
                }
            }

            @Override
            public void deliveryComplete(final IMqttDeliveryToken token) {
                // Do nothing
            }
        });

        client.connect(connectionOptions);
        LOGGER.logp(Level.FINE, CLASS_NAME,
                   "MqttManager", "Connected MQTT client");
        client.subscribe(topic, QOS);
    }

    /**
     * Check if the MQTT client is connected.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return client.isConnected();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws MqttException {
        if (client.isConnected()) {
            LOGGER.logp(Level.FINE, CLASS_NAME,
                    "close", "Disconnecting and closing MQTT client");
            client.disconnect();
            client.close();
        }
    }
}
