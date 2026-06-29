package org.cus.model;

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
import org.cus.controller.SimpleLogger;
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

    final void clearQueues() {
        rxQueue.clear();
        pendingRequests.forEach((rx, f) -> {
            f.complete(new Result.Err<>("Request deleted"));
        });
        pendingRequests.clear();
    }

    final void futurePut(final RXCommand rx, final CompletableFuture<Result<?>> future) {
        pendingRequests.put(rx, future);
    }

    final void futureRemove(final RXCommand rx, final CompletableFuture<Result<?>> future) {
        pendingRequests.remove(rx, future);
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
                        // Expected msg having 3 bytes: RXCommand + '|' + WcsState
                        if (msg.length == 3) {
                            final var state = WcsState.fromByte(msg[2]);
                            LOGGER.log(Level.FINE, RECEIVED + rx.toString() + " - " + state.toString());
                            completeOk(rx, state);
                            return;
                        }
                        break;
                    case OPENING:
                        // Expected 3 or more digits
                        if (msg.length >= 3) {
                            // Ignore the first 2 characters "?|"
                            final int p = Integer.parseUnsignedInt(str.substring(2));
                            LOGGER.log(Level.FINE, RECEIVED + rx.toString() + " - " + p);
                            completeOk(rx, p);
                            return;
                        }
                        break;
                    case ACK_OPENING:
                    case ACK_UNCONNECTED:
                    case ACK_CONNECTED:
                        LOGGER.log(Level.FINE, RECEIVED + rx.toString());
                        completeOk(rx, null);
                        return;
                }
                LOGGER.log(
                    Level.WARNING,
                    RECEIVED + rx.toString() + " followed by "
                        + Arrays.toString(Arrays.copyOfRange(msg, 1, msg.length))
                        + " (" + str + ")"
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
     * @param value optional/nullable value to send
     * @return {@code CompletableFuture} containing the response as {@code Result}
     */
    public CompletableFuture<Result<?>> sendCommand(final TXCommand tx, @Nullable final byte[] value) {
        final CompletableFuture<Result<?>> future = new CompletableFuture<>();
        final RXCommand rx = RXCommand.expectedFrom(tx);
        futurePut(rx, future);

        // Apply timeout of 1 second
        future.orTimeout(1, TimeUnit.SECONDS).exceptionally(e -> {
            futureRemove(rx, future);
            return new Result.Err<>("Timeout");
        });

        LOGGER.log(Level.INFO,
                "Virtually sent value " + tx.getValue() + " with optional payload: " + (value != null
                        ? Arrays.toString(value).trim()
                        : "(null)"));

        return future;
    }
}
