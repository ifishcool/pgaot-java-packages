package com.pgaot.sql;

import com.pgaot.sql.api.SqlTemplate;
import com.pgaot.sql.api.SqlTemplateConfig;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
class ComplexSqlTest {

    private static SqlTemplate db;

    @BeforeAll
    static void requireDb() {
        assumeTrue(EnvLoader.hasDb(), "跳过：需要数据库");
        db = new SqlTemplate(SqlTemplateConfig.fromEnv());
    }

    @Test
    void joinQuery() {
        List<Map<String, Object>> rows = db.sql(
                "SELECT u.name, w.qty, w.salary FROM t_user u " +
                "JOIN t_work_record w ON u.id = w.worker_id LIMIT 5");
        assertNotNull(rows);
    }

    @Test
    void aggregateWithHaving() {
        List<Map<String, Object>> rows = db.sql(
                "SELECT worker_id, COUNT(*) AS cnt, SUM(salary) AS total " +
                "FROM t_work_record GROUP BY worker_id HAVING cnt > 2 LIMIT 5");
        assertNotNull(rows);
    }

    @Test
    void subQuery() {
        List<Map<String, Object>> rows = db.sql(
                "SELECT * FROM t_user WHERE id IN (" +
                "SELECT worker_id FROM t_work_record WHERE salary > 100) LIMIT 5");
        assertNotNull(rows);
    }

    @Test
    void orderByWithPagination() {
        List<Map<String, Object>> rows = db.sql(
                "SELECT * FROM t_user ORDER BY id DESC LIMIT 3 OFFSET 0");
        assertNotNull(rows);
    }

    @Test
    void conditionalUpdate() {
        int n = db.sql("UPDATE t_user SET age = age + 1 WHERE id = 1");
        assertTrue(n >= 0);
    }

    @AfterAll static void cleanup() { if (db != null) db.close(); }
}
