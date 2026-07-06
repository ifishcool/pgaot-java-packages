package com.pgaot.sql;

import com.pgaot.sql.api.SqlTemplate;
import com.pgaot.sql.api.SqlTemplateConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
class FilterDemoTest {

    record Case(String label, String sql, boolean expectBlock, String category) {}

    private static SqlTemplate safe;

    @BeforeAll
    static void requireDb() {
        assumeTrue(EnvLoader.hasDb(), "跳过：需要数据库");
        safe = new SqlTemplate(SqlTemplateConfig.fromEnv().readWrite());
    }

    @AfterAll
    static void cleanup() {
        if (safe != null) {
            try { safe.unsafe("DELETE FROM t_user WHERE name LIKE 'test%' OR name IN ('A','B','C','xx')"); }
            catch (Exception ignored) {}
            safe.close();
        }
    }

    static Stream<Case> cases() {
        return Stream.of(
            // 复杂查询 — 应通过
            new Case("多表JOIN", "SELECT u.name, w.qty, w.salary FROM t_user u LEFT JOIN t_work_record w ON u.id = w.worker_id WHERE w.salary > 100 ORDER BY w.qty DESC", false, "查询"),
            new Case("GROUP BY", "SELECT worker_id, COUNT(*) cnt, SUM(salary) total FROM t_work_record GROUP BY worker_id HAVING cnt >= 2", false, "查询"),
            new Case("嵌套子查询", "SELECT * FROM t_user WHERE id IN (SELECT worker_id FROM t_work_record WHERE salary > (SELECT AVG(salary) FROM t_work_record))", false, "查询"),
            new Case("CASE WHEN", "SELECT name, CASE WHEN age < 25 THEN '青年' WHEN age < 35 THEN '中年' ELSE '其他' END AS age_group FROM t_user", false, "查询"),
            new Case("UNION ALL", "SELECT name, age FROM t_user WHERE age > 25 UNION ALL SELECT name, age FROM t_user WHERE age < 25", false, "查询"),
            new Case("EXISTS", "SELECT * FROM t_user u WHERE EXISTS (SELECT 1 FROM t_work_record w WHERE w.worker_id = u.id AND w.salary > 200)", false, "查询"),
            new Case("日期函数", "SELECT * FROM t_user WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)", false, "查询"),
            new Case("字符串函数", "SELECT CONCAT(name, '-', age) AS info FROM t_user WHERE name IS NOT NULL", false, "查询"),
            new Case("INSERT单行", "INSERT INTO t_user (name, age) VALUES ('test_insert', 18)", false, "DML"),
            new Case("INSERT多行", "INSERT INTO t_user (name, age) VALUES ('A',20),('B',25),('C',30)", false, "DML"),
            new Case("UPDATE单行", "UPDATE t_user SET age = 99 WHERE id = 1", false, "DML"),
            // DELETE — 应拦截（readWrite 模式禁止 DELETE）
            new Case("DELETE单行", "DELETE FROM t_user WHERE id = 99999", true, "DML"),
            // DDL — 全部拦截
            new Case("DROP TABLE", "DROP TABLE t_user", true, "DDL"),
            new Case("DROP TABLE IF EXISTS", "DROP TABLE IF EXISTS t_user", true, "DDL"),
            new Case("TRUNCATE", "TRUNCATE TABLE t_user", true, "DDL"),
            new Case("ALTER ADD", "ALTER TABLE t_user ADD COLUMN backdoor VARCHAR(100)", true, "DDL"),
            new Case("ALTER DROP", "ALTER TABLE t_user DROP COLUMN name", true, "DDL"),
            new Case("RENAME TABLE", "RENAME TABLE t_user TO t_hacked", true, "DDL"),
            new Case("CREATE TABLE", "CREATE TABLE t_backdoor (id INT)", true, "DDL"),
            // 注入攻击 — 全部拦截
            new Case("多语句", "SELECT * FROM t_user; DROP TABLE t_user", true, "注入"),
            new Case("UNION注入", "SELECT * FROM t_user WHERE id = 1 UNION SELECT * FROM mysql.user", true, "注入"),
            new Case("万能密码", "SELECT * FROM t_user WHERE name = '' OR '1'='1'", true, "注入"),
            new Case("EXTRACTVALUE", "SELECT extractvalue(1, concat(0x7e, database()))", true, "注入"),
            new Case("延时SLEEP", "SELECT * FROM t_user WHERE id = 1 AND sleep(5)", true, "注入"),
            new Case("延时BENCHMARK", "SELECT * FROM t_user WHERE id = 1 AND benchmark(1000000, md5('x'))", true, "注入"),
            new Case("写OUTFILE", "SELECT * FROM t_user INTO OUTFILE '/tmp/hack.txt'", true, "注入"),
            // 绕过 — 全部拦截
            new Case("注释绕过", "DR/**/OP TABLE t_user", true, "绕过"),
            new Case("版本注释", "/*!50000 DROP*/ TABLE t_user", true, "绕过"),
            new Case("大小写绕过", "dRoP tAbLe t_user", true, "绕过"),
            new Case("多空格绕过", "DROP     TABLE      t_user", true, "绕过"),
            new Case("反引号绕过", "DROP TABLE `t_user`", true, "绕过"),
            new Case("换行绕过", "DROP\nTABLE\nt_user", true, "绕过")
        );
    }

    @ParameterizedTest(name = "[{0}] {1}")
    @MethodSource("cases")
    void firewallCheck(Case c) {
        if (c.expectBlock) {
            assertThrows(Exception.class, () -> safe.sql(c.sql),
                    c.label + " (" + c.category + ") 应被拦截");
        } else {
            assertDoesNotThrow(() -> {
                Object r = safe.sql(c.sql);
                if (r instanceof List<?> l) assertTrue(l.size() >= 0);
            }, c.label + " (" + c.category + ") 不应被拦截");
        }
    }
}
