package com.pgaot.log;

import com.pgaot.log.api.LogContext;
import com.pgaot.log.core.ContextRunner;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ContextRunnerTest {

    @Test
    void shouldInitAndClearAroundRunnable() {
        ContextRunner.run("alice", "Alice", "t1", () -> {
            assertTrue(LogContext.isInitialized());
            assertEquals("alice", LogContext.getUserId());
        });
        assertFalse(LogContext.isInitialized());
    }

    @Test
    void shouldReturnValueFromCallable() {
        String result = ContextRunner.call("bob", "Bob", "t2", () -> "done");
        assertEquals("done", result);
        assertFalse(LogContext.isInitialized());
    }

    @Test
    void shouldClearOnException() {
        try {
            ContextRunner.run("alice", "Alice", "t1", () -> {
                throw new RuntimeException("fail");
            });
        } catch (RuntimeException ignored) {}
        assertFalse(LogContext.isInitialized(), "异常后仍应清理上下文");
    }
}
