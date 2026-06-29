package org.cus.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
// import java.util.logging.Level;
// import java.util.logging.Logger;
import java.util.stream.Stream;
// import org.drone_remote_unit.controller.SimpleLogger;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.MethodOrderer;
// import org.junit.jupiter.api.Order;
// import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CommManagerUnitTest {

    // private static final String CLASS_NAME = CommManagerUnitTest.class.getName();
    // private static final Logger LOGGER = SimpleLogger.getLogger(CLASS_NAME);
    private static final String TIMEOUT = "Timeout";

    private static final List<TestCase> PARAMS = List.of(
        new TestCase(
            TXCommand.GET_STATE,
            null,
            convert("D:Temp 29.01".getBytes(StandardCharsets.US_ASCII)),
            new Result.Err<>(TIMEOUT),
            "Test get state, response Err(Timeout), received debug text"
        ),
        new TestCase(
            TXCommand.GET_STATE,
            null,
            new Byte[] {'R', ':', RXCommand.STATE.getValue(), '|', WcsState.AUTOMATIC.getValue()},
            new Result.Ok<>(WcsState.AUTOMATIC),
            "Test get state, response Ok(Idle)"
        ),
        new TestCase(
            TXCommand.GET_OPENING,
            null,
            new Byte[] {'R', ':', RXCommand.OPENING.getValue(), '|', '3', '9'},
            new Result.Ok<>(39),
            "Test get opening, response Ok(39)"
        ),
        new TestCase(
            TXCommand.SET_OPENING,
            new byte[] {'1', '2'},
            new Byte[] {'R', ':', RXCommand.ACK_OPENING.getValue()},
            new Result.Ok<>(null),
            "Test set opening, response ACK->Ok(null)"
        ),
        new TestCase(
            TXCommand.SET_UNCONNECTED,
            null,
            new Byte[] {'R', ':', RXCommand.ACK_UNCONNECTED.getValue()},
            new Result.Ok<>(null),
            "Test set unconnected, response ACK->Ok(null)"
        ),
        new TestCase(
            TXCommand.SET_CONNECTED,
            null,
            new Byte[] {'R', ':', RXCommand.ACK_CONNECTED.getValue()}, // Q:AckConnected
            new Result.Ok<>(null),
            "Test set connected, response ACK->Ok(null)"
        ),
        new TestCase(
            TXCommand.SET_CONNECTED,
            null,
            new Byte[] {'R', ':', RXCommand.INVALID.getValue()}, // Q:Invalid
            new Result.Err<>(TIMEOUT),
            "Test set connected, response Invalid->Err(Timeout)"
        )
    );

    private final AbstractCommManager manager = new AbstractCommManager() {
        @Override
        public void close() throws Exception {
            // Do nothing
        }
    };

    private static Byte[] convert(final byte[] src) {
        final Byte[] res = new Byte[src.length];
        for (int i = 0; i < src.length; i++) { // NOPMD(AvoidArrayLoops)
            res[i] = src[i];
        }
        return res;
    }

    @TestFactory
    Stream<DynamicTest> testAll() {
        return PARAMS.stream()
            .map(element -> dynamicTest(element.desc(), () -> {
                    // Reset the last message processor exception, if necessary
                    manager.resetLastProcessorException();

                    // Build the future "sending" the command
                    final CompletableFuture<Result<?>> future =
                        manager.sendCommand(element.tx(), element.txData());

                    // Enqueue the response
                    manager.rxEnqueue(element.rxData());

                    // Here the message get parsed internally by manager

                    if (isTimeoutExpected(element.res())) {
                        // ExecutionException caused by TimeoutException is expected
                        final ExecutionException e = assertThrows(ExecutionException.class, future::get);
                        assertInstanceOf(
                            TimeoutException.class,
                            e.getCause(),
                            "Expected TimeoutException");
                    } else {
                        // Normal case, no exception expected here
                        final Result<?> resState = future.get();
                        if (element.res() instanceof Result.Ok<?> expectedOk) {
                            assertInstanceOf(
                                expectedOk.getClass(),
                                resState,
                                "Expected type " + element.res().getClass().getSimpleName());
                            assertEquals(expectedOk.value(), ((Result.Ok<?>) resState).value());
                        } else if (element.res() instanceof Result.Err<?> expectedErr) {
                            assertInstanceOf(
                                expectedErr.getClass(),
                                resState,
                                "Expected type " + element.res().getClass().getSimpleName());
                            assertEquals(expectedErr.error(), ((Result.Err<?>) resState).error());
                        }
                    }

                    // Check for IllegalArgumentException during message processing
                    assertNull(manager.getLastProcessorException());
                })
            );
    }

    private boolean isTimeoutExpected(final Result<?> result) {
        return result instanceof Result.Err err && TIMEOUT.equals(err.error());
    }

    record TestCase(
        TXCommand tx,
        byte[] txData, // nullable
        Byte[] rxData,
        Result<?> res,
        String desc
    ) { }
}
