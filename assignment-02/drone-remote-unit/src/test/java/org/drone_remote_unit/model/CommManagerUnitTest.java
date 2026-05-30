package org.drone_remote_unit.model;

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
            convert("D:Temp 29.01".getBytes(StandardCharsets.US_ASCII)),
            new Result.Err<>(TIMEOUT),
            "Test get state, response Err(Timeout), received debug text"
        ),
        new TestCase(
            TXCommand.GET_STATE,
            new Byte[] {82, 58, 1, 124, 1}, // Q:State|Idle
            new Result.Ok<>(State.IDLE),
            "Test get state, response Ok(Idle)"
        ),
        new TestCase(
            TXCommand.GET_TEMPERATURE,
            new Byte[] {82, 58, 8, 124, 51, 57, 46, 56, 52}, // Q:Temperature|39.48
            new Result.Ok<>(39.48),
            "Test get temperature, response Ok(39.48)"
        ),
        new TestCase(
            TXCommand.REQ_TAKE_OFF,
            new Byte[] {82, 58, 2}, // Q:AckTakeoff
            new Result.Ok<>(null),
            "Test request takeof, response ACK->Ok(null)"
        ),
        new TestCase(
            TXCommand.REQ_LANDING,
            new Byte[] {82, 58, 127}, // Q:Invalid
            new Result.Err<>(TIMEOUT),
            "Test request landing, response Invalid->Err(Timeout)"
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
                        manager.sendCommand(element.tx());

                    // Enqueue the response
                    manager.rxEnqueue(element.data());

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
                        assertInstanceOf(
                            element.res().getClass(),
                            resState,
                            "Expected type " + element.res().getClass().getSimpleName());
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
        Byte[] data,
        Result<?> res,
        String desc
    ) { }
}
