package com.pgaot.datasheet;

import com.pgaot.datasheet.api.DatasheetEngine;
import com.pgaot.datasheet.common.model.*;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IsolationTest {

    private static DatasheetEngine engine;
    private static final String USER_A = "tenant_a";
    private static final String USER_B = "tenant_b";
    private static String tableId;

    @BeforeAll
    static void requireDb() {
        assumeTrue(EnvLoader.hasDb(), "跳过：需要数据库");
        engine = DatasheetEngine.fromEnv("DATA");
        for (String u : List.of(USER_A, USER_B))
            for (TableInfo old : engine.tables().list(u))
                try { engine.tables().drop(u, old.getId()); } catch (Exception ignored) {}
    }

    @Test @Order(1)
    void userACreatesTable() {
        TableInfo t = engine.tables().create(USER_A, "scores", "成绩表", null, List.of(
                new ColumnInfo("name", ColumnType.STRING, true),
                new ColumnInfo("score", ColumnType.DECIMAL, false),
                new ColumnInfo("city", ColumnType.STRING, false)));
        assertNotNull(t);
        tableId = t.getId();
    }

    @Test @Order(2)
    void userABulkInserts() {
        engine.data().insert(USER_A, tableId, List.of(
                Map.of("name", "张三", "score", 95, "city", "北京"),
                Map.of("name", "李四", "score", 87, "city", "上海"),
                Map.of("name", "王五", "score", 73, "city", "北京"),
                Map.of("name", "赵六", "score", 88, "city", "广州"),
                Map.of("name", "孙七", "score", 91, "city", "上海")));
        var rows = engine.data().<List<Map<String, Object>>>sql(USER_A, "SELECT * FROM scores");
        assertEquals(5, rows.size());
    }

    @Test @Order(3) void aggregateByCity() {
        var r = engine.data().<List<?>>sql(USER_A,
                "SELECT city, AVG(score) AS avg_score, COUNT(*) AS cnt FROM scores GROUP BY city ORDER BY avg_score DESC");
        assertTrue(r.size() >= 2);
    }

    @Test @Order(4) void subquery() {
        var r = engine.data().<List<?>>sql(USER_A,
                "SELECT * FROM scores WHERE score > (SELECT AVG(score) FROM scores)");
        assertTrue(r.size() >= 1);
    }

    @Test @Order(5) void nestedSubquery() {
        var r = engine.data().<List<?>>sql(USER_A,
                "SELECT * FROM scores WHERE score > (SELECT AVG(score) FROM scores WHERE score > (SELECT MIN(score) FROM scores))");
        assertTrue(r.size() >= 2);
    }

    @Test @Order(6) void existsSubquery() {
        var r = engine.data().<List<?>>sql(USER_A,
                "SELECT * FROM scores s1 WHERE EXISTS (SELECT 1 FROM scores s2 WHERE s2.city = s1.city AND s2.name != s1.name)");
        assertTrue(r.size() >= 1);
    }

    @Test @Order(7) void unionAll() {
        var r = engine.data().<List<?>>sql(USER_A,
                "SELECT name, score FROM scores WHERE score > 90 UNION ALL SELECT name, score FROM scores WHERE city = '上海'");
        assertTrue(r.size() >= 3);
    }

    @Test @Order(8) void caseWhen() {
        var r = engine.data().<List<?>>sql(USER_A,
                "SELECT name, CASE WHEN score >= 90 THEN 'A' WHEN score >= 80 THEN 'B' ELSE 'C' END AS grade FROM scores ORDER BY score DESC");
        assertTrue(r.size() >= 2);
    }

    @Test @Order(9) void havingFilter() {
        var r = engine.data().<List<?>>sql(USER_A,
                "SELECT city, COUNT(*) AS cnt FROM scores GROUP BY city HAVING cnt >= 1");
        assertTrue(r.size() >= 1);
    }

    @Test @Order(10)
    void userBCannotAccess() {
        assertThrows(Exception.class, () ->
                engine.data().sql(USER_B, "SELECT * FROM scores"));
    }

    @Test @Order(11) void crossTenantInsertBlocked() {
        assertThrows(Exception.class, () ->
                engine.data().sql(USER_B, "INSERT INTO scores (name,score,city) VALUES ('hack',0,'')"));
    }

    @Test @Order(12) void joinBlocked() {
        assertThrows(Exception.class, () ->
                engine.data().sql(USER_B, "SELECT * FROM scores, (SELECT 1) AS t LIMIT 1"));
    }

    @Test @Order(13) void updateCellAndDeleteRow() {
        engine.data().updateCell(USER_A, tableId, 1, "score", 100);
        engine.data().deleteRow(USER_A, tableId, 1);
        var r = engine.data().<List<?>>sql(USER_A, "SELECT * FROM scores WHERE id = 1");
        assertTrue(r.isEmpty());
    }

    @AfterAll
    static void cleanup() {
        if (engine != null)
            for (String u : List.of(USER_A, USER_B))
                for (TableInfo old : engine.tables().list(u))
                    try { engine.tables().drop(u, old.getId()); } catch (Exception ignored) {}
    }
}
