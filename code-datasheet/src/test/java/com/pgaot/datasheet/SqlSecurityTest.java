package com.pgaot.datasheet;

import com.pgaot.datasheet.api.DatasheetEngine;
import com.pgaot.datasheet.common.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
class SqlSecurityTest {

    private static DatasheetEngine engine;
    private static final String A = "alice", B = "bob";

    @BeforeAll
    static void setup() {
        assumeTrue(EnvLoader.hasDb(), "跳过：需要数据库");
        engine = DatasheetEngine.fromEnv();
        for (String u : List.of(A, B))
            for (TableInfo old : engine.tables().list(u))
                try { engine.tables().drop(u, old.getId()); } catch (Exception ignored) {}

        var rw = engine.tables().create(A, "rw_table", "读写表", null, List.of(
                new ColumnInfo("data", ColumnType.STRING, true),
                new ColumnInfo("val", ColumnType.DECIMAL, false)));
        var ro = engine.tables().create(A, "ro_table", "只读表", null, List.of(
                new ColumnInfo("data", ColumnType.STRING, true)));
        var wo = engine.tables().create(A, "wo_table", "只写表", null, List.of(
                new ColumnInfo("data", ColumnType.STRING, true)));
        engine.tables().create(B, "bob_table", "Bob表", null, List.of(
                new ColumnInfo("data", ColumnType.STRING, true)));

        engine.data().insert(A, rw.getId(), Map.of("data", "hello", "val", 100));
        engine.data().insert(A, rw.getId(), Map.of("data", "world", "val", 200));
        engine.data().insert(A, ro.getId(), Map.of("data", "readonly_data"));
        engine.data().insert(A, wo.getId(), Map.of("data", "writeonly_data"));
        engine.data().insert(B, engine.tables().list(B).getFirst().getId(), Map.of("data", "bob_secret"));
        engine.tables().setMode(A, ro.getId(), TableMode.READ_ONLY);
        engine.tables().setMode(A, wo.getId(), TableMode.WRITE_ONLY);
    }

    static Stream<Arguments> shouldPass() {
        return Stream.of(
            Arguments.of("SELECT 读写表", A, "SELECT * FROM rw_table"),
            Arguments.of("INSERT 读写表", A, "INSERT INTO rw_table (data, val) VALUES ('ok', 1)"),
            Arguments.of("UPDATE 读写表", A, "UPDATE rw_table SET val = 999 WHERE data = 'ok'"),
            Arguments.of("SELECT 只读表", A, "SELECT * FROM ro_table"),
            Arguments.of("INSERT 只写表", A, "INSERT INTO wo_table (data) VALUES ('ok')"),
            Arguments.of("SELECT 聚合", A, "SELECT COUNT(*) AS n, SUM(val) AS s FROM rw_table"),
            Arguments.of("INSERT SELECT 跨模式", A, "INSERT INTO rw_table (data,val) SELECT data,1 FROM ro_table"),
            Arguments.of("双层 INSERT SELECT", A, "INSERT INTO rw_table (data,val) SELECT data,COUNT(*) FROM ro_table GROUP BY data"),
            Arguments.of("三层嵌套子查询", A, "SELECT * FROM rw_table WHERE val > (SELECT AVG(val) FROM rw_table WHERE val > (SELECT MIN(val) FROM rw_table))"),
            Arguments.of("EXISTS + 子查询", A, "SELECT * FROM rw_table r WHERE EXISTS (SELECT 1 FROM ro_table o WHERE o.data = r.data)"),
            Arguments.of("SELECT DUAL", A, "SELECT 1+1 AS result"),
            Arguments.of("SELECT 常量函数", A, "SELECT NOW(), DATABASE(), VERSION()"),
            Arguments.of("INSERT DEFAULT", A, "INSERT INTO rw_table (data,val) VALUES ('default',0)"),
            Arguments.of("UPDATE LIMIT", A, "UPDATE rw_table SET val=0 ORDER BY val DESC LIMIT 1")
        );
    }

    static Stream<Arguments> shouldBlock() {
        return Stream.of(
            Arguments.of("INSERT 只读表", A, "INSERT INTO ro_table (data) VALUES ('fail')"),
            Arguments.of("SELECT 只写表", A, "SELECT * FROM wo_table"),
            Arguments.of("DELETE 只写表", A, "DELETE FROM wo_table"),
            Arguments.of("A 查 B 的表", A, "SELECT * FROM bob_table"),
            Arguments.of("A JOIN B 的表", A, "SELECT * FROM rw_table, bob_table LIMIT 1"),
            Arguments.of("A 子查询窃取 B", A, "SELECT * FROM rw_table WHERE data IN (SELECT data FROM bob_table)"),
            Arguments.of("DROP TABLE", A, "DROP TABLE rw_table"),
            Arguments.of("ALTER TABLE", A, "ALTER TABLE rw_table ADD COLUMN hack VARCHAR(100)"),
            Arguments.of("TRUNCATE TABLE", A, "TRUNCATE TABLE rw_table"),
            Arguments.of("CREATE TABLE", A, "CREATE TABLE backdoor (id INT)"),
            Arguments.of("多语句注入", A, "SELECT * FROM rw_table; DROP TABLE rw_table"),
            Arguments.of("SELECT mysql.user", A, "SELECT * FROM mysql.user LIMIT 1"),
            Arguments.of("GRANT 提权", A, "GRANT ALL ON *.* TO 'alice'@'%'"),
            Arguments.of("注释绕过 DROP", A, "DR/**/OP TABLE rw_table"),
            Arguments.of("大小写绕过", A, "dRoP tAbLe rw_table"),
            Arguments.of("NOT EXISTS 跨租户", A, "SELECT * FROM rw_table r WHERE NOT EXISTS (SELECT 1 FROM bob_table b WHERE b.data = r.data)")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("shouldPass")
    void allowedOperations(String name, String userId, String sql) {
        assertDoesNotThrow(() -> engine.data().sql(userId, sql), name + " 应通过");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("shouldBlock")
    void blockedOperations(String name, String userId, String sql) {
        assertThrows(Exception.class, () -> engine.data().sql(userId, sql), name + " 应拦截");
    }

    @AfterAll
    static void cleanup() {
        if (engine != null)
            for (String u : List.of(A, B))
                for (TableInfo old : engine.tables().list(u))
                    try { engine.tables().drop(u, old.getId()); } catch (Exception ignored) {}
    }
}
