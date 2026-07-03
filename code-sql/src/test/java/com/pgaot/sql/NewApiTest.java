package com.pgaot.sql;

import com.pgaot.sql.api.SqlTemplate;
import com.pgaot.sql.api.SqlTemplateConfig;
import com.pgaot.sql.common.code.ErrorCode;
import com.pgaot.sql.common.code.IResultCode;
import com.pgaot.sql.exception.SqlException;
import com.pgaot.sql.support.PageQuery;
import com.pgaot.sql.support.PageResponse;
import org.junit.jupiter.api.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NewApiTest {

    // ═══════════════════ ErrorCode ═══════════════════

    @Test
    void errorCodeNoDuplicates() {
        Set<Integer> codes = new HashSet<>();
        for (ErrorCode ec : ErrorCode.values())
            assertTrue(codes.add(ec.getCode()), "重复错误码: " + ec.getCode());
        assertEquals(ErrorCode.values().length, codes.size());
    }

    @Test
    void errorCodeImplementsIResultCode() {
        for (ErrorCode ec : ErrorCode.values()) {
            assertInstanceOf(IResultCode.class, ec);
            assertTrue(ec.getCode() > 0, ec.name() + " 错误码 <= 0");
            assertNotNull(ec.getMessage());
            assertFalse(ec.getMessage().isBlank(), ec.name() + " 消息为空");
        }
    }

    @Test
    void errorCodeRangeAllocation() {
        assertTrue(inRange(ErrorCode.CONNECTION_FAILED, 20_001_000, 20_001_999));
        assertTrue(inRange(ErrorCode.ENV_MISSING, 20_001_000, 20_001_999));
        assertTrue(inRange(ErrorCode.SQL_EXECUTION_FAILED, 20_002_000, 20_002_999));
        assertTrue(inRange(ErrorCode.SQL_BLOCKED_BY_WALL, 20_003_000, 20_003_999));
        assertTrue(inRange(ErrorCode.PAGE_PARAM_INVALID, 20_004_000, 20_004_999));
        assertTrue(inRange(ErrorCode.JPA_EXECUTION_FAILED, 20_005_000, 20_005_999));
    }

    // ═══════════════════ SqlException factories ═══════════════════

    @Test
    void wallBlockedException() {
        SqlException e = SqlException.wallBlocked("DROP TABLE t_user");
        assertEquals(ErrorCode.SQL_BLOCKED_BY_WALL.getCode(), e.getCode());
        assertTrue(e.getMessage().contains("DROP TABLE t_user"));
    }

    @Test
    void executionFailedException() {
        assertEquals(ErrorCode.SQL_EXECUTION_FAILED.getCode(),
                SqlException.executionFailed("timeout").getCode());
    }

    @Test
    void connectionFailedException() {
        assertEquals(ErrorCode.CONNECTION_FAILED.getCode(),
                SqlException.connectionFailed("Access denied").getCode());
    }

    @Test
    void envMissingException() {
        SqlException e = SqlException.envMissing("CODE_SQL_URL");
        assertEquals(ErrorCode.ENV_MISSING.getCode(), e.getCode());
        assertTrue(e.getMessage().contains("CODE_SQL_URL"));
    }

    @Test
    void pageParamInvalidException() {
        assertEquals(ErrorCode.PAGE_PARAM_INVALID.getCode(),
                SqlException.pageParamInvalid("页码必须 >= 1").getCode());
    }

    @Test
    void jpaFailedException() {
        assertEquals(ErrorCode.JPA_EXECUTION_FAILED.getCode(),
                SqlException.jpaFailed("query timeout").getCode());
    }

    @Test
    void iResultCodeConstructor() {
        SqlException e = new SqlException(ErrorCode.SQL_BLOCKED_BY_WALL);
        assertEquals(20_003_001, e.getCode());
    }

    @Test
    void iResultCodeWithDetailConstructor() {
        SqlException e = new SqlException(ErrorCode.SQL_EXECUTION_FAILED, "detail");
        assertEquals(20_002_001, e.getCode());
        assertTrue(e.getMessage().contains("detail"));
    }

    // ═══════════════════ PageQuery ═══════════════════

    @Test
    void validPageQuery() {
        PageQuery pq = assertDoesNotThrow(() -> new PageQuery(1, 10));
        assertEquals(1, pq.getPage());
        assertEquals(10, pq.getSize());
    }

    @Test
    void rejectPageZero() {
        SqlException e = assertThrows(SqlException.class, () -> new PageQuery(0, 10));
        assertEquals(20_004_001, e.getCode());
    }

    @Test
    void rejectPageNegative() {
        assertThrows(SqlException.class, () -> new PageQuery(-1, 10));
    }

    @Test
    void rejectSizeZero() {
        assertThrows(SqlException.class, () -> new PageQuery(1, 0));
    }

    @Test
    void rejectSizeOver1000() {
        assertThrows(SqlException.class, () -> new PageQuery(1, 1001));
    }

    @Test
    void acceptBoundarySize1() {
        assertDoesNotThrow(() -> new PageQuery(1, 1));
    }

    @Test
    void acceptBoundarySize1000() {
        assertDoesNotThrow(() -> new PageQuery(1, 1000));
    }

    // ═══════════════════ PageQuery offset ═══════════════════

    @Test
    void page1Offset0() { assertEquals(0, new PageQuery(1, 10).getOffset()); }
    @Test
    void page2Offset10() { assertEquals(10, new PageQuery(2, 10).getOffset()); }
    @Test
    void page3Size20Offset40() { assertEquals(40, new PageQuery(3, 20).getOffset()); }

    // ═══════════════════ PageResponse ═══════════════════

    @Test
    void emptyResponse() {
        PageResponse<String> r = PageResponse.empty(2, 15);
        assertTrue(r.getRows().isEmpty());
        assertEquals(0, r.getTotal());
        assertEquals(2, r.getPage());
        assertEquals(15, r.getSize());
        assertEquals(0, r.getPages());
    }

    @Test
    void responseConvert() {
        PageResponse<Integer> src = PageResponse.of(List.of(1, 2, 3), 100, 1, 10);
        PageResponse<String> dst = src.convert(String::valueOf);
        assertEquals(List.of("1", "2", "3"), dst.getRows());
        assertEquals(100, dst.getTotal());
        assertEquals(1, dst.getPage());
        assertEquals(10, dst.getPages());
    }

    @Test
    void pagesCalculation() {
        assertEquals(10, PageResponse.of(List.of(), 100, 1, 10).getPages());
        assertEquals(11, PageResponse.of(List.of(), 101, 1, 10).getPages());
        assertEquals(0, PageResponse.of(List.of(), 0, 1, 10).getPages());
        assertEquals(1, PageResponse.of(List.of(), 1, 1, 10).getPages());
    }

    // ═══════════ DB-dependent (skip without env) ═══════════

    @Nested
    @Tag("integration")
    class DatabaseTests {

        @BeforeEach
        void requireDb() {
            assumeTrue(EnvLoader.hasDb(), "跳过：需要数据库");
        }

        @Test
        void pageQueryWithDb() {
            SqlTemplate db = new SqlTemplate(SqlTemplateConfig.fromEnv());
            PageQuery pq = new PageQuery(1, 3);
            PageResponse<Map<String, Object>> page = db.page(
                    "SELECT * FROM t_user ORDER BY id", pq);
            assertEquals(1, page.getPage());
            assertEquals(3, page.getSize());
            assertTrue(page.getRows().size() <= 3);
            assertTrue(page.getTotal() >= 0);
        }

        @Test
        void pageQueryEmptyResult() {
            SqlTemplate db = new SqlTemplate(SqlTemplateConfig.fromEnv());
            PageQuery pq = new PageQuery(99999, 10);
            PageResponse<Map<String, Object>> empty = db.page(
                    "SELECT * FROM t_user WHERE id < 0 ORDER BY id", pq);
            assertTrue(empty.getRows().isEmpty());
            assertEquals(0, empty.getTotal());
        }

        @Test
        void select1ReturnsOneRow() {
            SqlTemplate db = new SqlTemplate(SqlTemplateConfig.fromEnv());
            List<Map<String, Object>> rows = db.sql("SELECT 1 AS test");
            assertEquals(1, rows.size());
            assertNotNull(rows.getFirst().get("test"));
        }
    }

    // ═══════════════════ helpers ═══════════════════

    private static boolean inRange(ErrorCode ec, int lo, int hi) {
        return ec.getCode() >= lo && ec.getCode() <= hi;
    }
}
