package org.drone_remote_unit.model;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.drone_remote_unit.controller.SimpleLogger;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Abstract class that implements basic functions for a communication manager.
 */
public abstract class AbstractCommManager implements AutoCloseable {

    private static final String CLASS_NAME = AbstractCommManager.class.getName();
    private static final Logger LOGGER = SimpleLogger.getLogger(CLASS_NAME);
    private static final String RECEIVED = "Received ";

    private final BlockingQueue<Byte[]> rxQueue = new ArrayBlockingQueue<>(100);
    private final Map<RXCommand, CompletableFuture<Result<?>>> pendingRequests =
            new ConcurrentHashMap<>();
    private final ExecutorService threadPool = Executors.newVirtualThreadPerTaskExecutor();
    @Nullable private volatile IllegalArgumentException lastException;

    AbstractCommManager() {
        threadPool.execute(this::messageProcessor);
    }

    private void messageProcessor() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                final Byte[] rawMsg = rxQueue.take();
                messageParser(rawMsg);
            } catch (final InterruptedException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                Thread.currentThread().interrupt();
                break;
            } catch (final IllegalArgumentException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                lastException = e;
            }
        }
    }

    /**
     * Return the last IllegalArgumentException thrown by the messageProcessor.
     * Useful for testing
     *
     * @return null or {@code IllegalArgumentException}
     */
    @CheckForNull
    final IllegalArgumentException getLastProcessorException() {
        return lastException;
    }

    final void resetLastProcessorException() {
        lastException = null;
    }

    final void rxEnqueue(final Byte[] msg) throws InterruptedException {
        rxQueue.put(msg);
    }

    final void futurePut(final RXCommand rx, final CompletableFuture<Result<?>> future) {
        pendingRequests.put(rx, future);
    }

    final void futureRemove(final RXCommand rx) {
        pendingRequests.remove(rx);
    }

    private void messageParser(final Byte[] rawMsg) throws InterruptedException {
        // Format: "R:RXCommand|optional values"
        //         "D:debug text"
        if (rawMsg.length < 3) {
            LOGGER.log(
                Level.WARNING,
                "Invalid message " + Arrays.toString(rawMsg)
            );
            return;
        }
        // Copy and cast character from the third to the end included
        final byte[] msg = new byte[rawMsg.length - 2];
        for (int i = 2; i < rawMsg.length; i++) { // NOPMD(AvoidArrayLoops)
            msg[i - 2] = rawMsg[i];
        }
        final String tmpStr = new String(msg, StandardCharsets.US_ASCII);
        // Remove non-printable and non-ASCII characters
        final String str = tmpStr.replaceAll("[\\p{Cntrl}]", "?");

        switch (rawMsg[0]) {
            case 'D':
                // Handle debug messages
                LOGGER.log(Level.FINE, "RemDbg:" + str);
                return;
            case 'R':
                // Handle actual responses
                final RXCommand rx = RXCommand.fromByte(msg[0]);
                switch (rx) {
                    case INVALID:
                        LOGGER.log(Level.WARNING, RECEIVED + rx.toString());
                        return;
                    case STATE:
                        // Expected msg having 3 bytes: RXCommand + '|' + State
                        if (msg.length == 3) {
                            final var state = State.fromByte(msg[2]);
                            completeOk(rx, state);
                            return;
                        }
                        break;
                    case ACK_TAKE_OFF:
                    case ACK_LANDING:
                        LOGGER.log(Level.FINE, RECEIVED + rx.toString());
                        completeOk(rx, null);
                        return;
                    case TEMPERATURE:
                    case DISTANCE:
                        // Expected 3 or more digits + decimal separator
                        if (msg.length >= 4) {
                            // Ignore the first 2 characters "?|"
                            final float t = Float.parseFloat(str.substring(2));
                            LOGGER.log(Level.FINE, RECEIVED + rx.toString() + " - " + t);
                            completeOk(rx, t);
                            return;
                        }
                        break;
                }
                LOGGER.log(
                    Level.WARNING,
                    RECEIVED + rx.toString() + " followed by " + Arrays.toString(msg) + " (" + str + ")"
                );
                break;
            default:
                LOGGER.log(
                    Level.WARNING,
                    "Invalid command " + (char) rawMsg[0].byteValue()
                );
                break;
        }
    }

    private void completeOk(final RXCommand rx, @Nullable final Object data) {
        final CompletableFuture<Result<?>> req = pendingRequests.remove(rx);
        if (req != null) {
            req.complete(new Result.Ok<>(data));
        }
    }

    /**
     * Send the given command.
     *
     * @param tx the {@code TXCommand} to send
     * @return {@code CompletableFuture} containing the response as {@code Result}
     */
    public CompletableFuture<Result<?>> sendCommand(final TXCommand tx) {
        final CompletableFuture<Result<?>> future = new CompletableFuture<>();
        final RXCommand rx = RXCommand.expectedFrom(tx);
        futurePut(rx, future);

        // Apply timeout of 1 second
        future.orTimeout(1, TimeUnit.SECONDS).exceptionally(e -> {
            futureRemove(rx);
            return new Result.Err<>("Timeout");
        });

        LOGGER.log(Level.INFO, "Virtually sent value " + tx.getValue());

        return future;
    }
}
