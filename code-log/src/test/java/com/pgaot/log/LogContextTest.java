package com.pgaot.log;

import com.pgaot.log.api.LogContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogContextTest {

    @Test
    void shouldInitAndProvideContext() {
        LogContext.init("alice", "Alice", "tenant-1");
        assertTrue(LogContext.isInitialized());
        assertNotNull(LogContext.getTraceId());
        assertEquals(16, LogContext.getTraceId().length());
        assertEquals("alice", LogContext.getUserId());
        assertEquals("Alice", LogContext.getUserName());
        assertEquals("tenant-1", LogContext.getTenantId());
        LogContext.clear();
    }

    @Test
    void shouldClearContext() {
        LogContext.init("alice", "Alice", "tenant-1");
        LogContext.clear();
        assertFalse(LogContext.isInitialized());
        assertNull(LogContext.getUserId());
    }

    @Test
    void shouldInitTraceOnly() {
        LogContext.initTrace();
        assertNotNull(LogContext.getTraceId());
        assertNull(LogContext.getUserId());
        LogContext.clear();
    }

    @Test
    void shouldNotBeInitializedByDefault() {
        assertFalse(LogContext.isInitialized());
    }
}
