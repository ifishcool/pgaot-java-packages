package com.pgaot.sql;

import com.pgaot.sql.exception.SqlException;
import com.pgaot.sql.support.PageQuery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PageQueryTest {

    @Test
    void shouldAcceptValidPage() {
        PageQuery pq = new PageQuery(1, 10);
        assertEquals(1, pq.getPage());
        assertEquals(10, pq.getSize());
        assertEquals(0, pq.getOffset());
    }

    @Test
    void shouldCalculateOffset() {
        assertEquals(20, new PageQuery(3, 10).getOffset());
        assertEquals(45, new PageQuery(4, 15).getOffset());
    }

    @Test
    void shouldRejectZeroPage() {
        assertThrows(SqlException.class, () -> new PageQuery(0, 10));
    }

    @Test
    void shouldRejectNegativeSize() {
        assertThrows(SqlException.class, () -> new PageQuery(1, -1));
    }

    @Test
    void shouldRejectOversizedPage() {
        assertThrows(SqlException.class, () -> new PageQuery(1, 1001));
    }

    @Test
    void shouldAcceptMaxSize() {
        assertDoesNotThrow(() -> new PageQuery(1, 1000));
    }
}
