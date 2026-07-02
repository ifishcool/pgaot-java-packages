package com.pgaot.datasheet;

import com.pgaot.datasheet.api.DatasheetEngine;
import com.pgaot.datasheet.common.model.*;

import java.util.List;
import java.util.Map;

/**
 * SQL 安全测试 — 全部通过 engine.data().sql() 执行.
 *
 * <p>覆盖: 正常操作 / 模式违反 / 跨租户攻击 / SQL 注入 / 提权 / 边界
 */
public class SqlSecurityTest {

    static int pass = 0, fail = 0;

    public static void main(String[] args) {
        DatasheetEngine engine = DatasheetEngine.fromEnv();

        System.out.println("==========================================");
        System.out.println("  code-datasheet SQL 安全测试");
        System.out.println("==========================================\n");

        String A = "alice", B = "bob";

        // 清理
        for (String u : List.of(A, B))
            for (TableInfo old : engine.tables().list(u))
                try { engine.tables().drop(u, old.getId()); } catch (Exception ignored) {}

        // 建表
        String rwId, roId, woId, bobId;
        rwId = engine.tables().create(A, "rw_table", "读写表", null, List.of(
                new ColumnInfo("data", ColumnType.STRING, true),
                new ColumnInfo("val",  ColumnType.NUMBER, false))).getId();
        roId = engine.tables().create(A, "ro_table", "只读表", null, List.of(
                new ColumnInfo("data", ColumnType.STRING, true))).getId();
        woId = engine.tables().create(A, "wo_table", "只写表", null, List.of(
                new ColumnInfo("data", ColumnType.STRING, true))).getId();
        bobId = engine.tables().create(B, "bob_table", "Bob表", null, List.of(
                new ColumnInfo("data", ColumnType.STRING, true))).getId();

        // 先插入初始数据，再设模式
        engine.data().insert(A, rwId, Map.of("data", "hello", "val", 100));
        engine.data().insert(A, rwId, Map.of("data", "world", "val", 200));
        engine.data().insert(A, roId, Map.of("data", "readonly_data"));
        engine.data().insert(A, woId, Map.of("data", "writeonly_data"));
        engine.data().insert(B, bobId, Map.of("data", "bob_secret"));

        engine.tables().setMode(A, roId, TableMode.READ_ONLY);
        engine.tables().setMode(A, woId, TableMode.WRITE_ONLY);

        int total = 0;

        // ════════════════════════
        // 类别 1: 正常操作
        // ════════════════════════
        pause();
        System.out.println("  === 1. 正常操作（应全部通过）===\n");

        total += test(engine, "SELECT 读写表",          A, "SELECT * FROM rw_table", true);
        total += test(engine, "INSERT 读写表",          A, "INSERT INTO rw_table (data, val) VALUES ('ok', 1)", true);
        total += test(engine, "UPDATE 读写表",          A, "UPDATE rw_table SET val = 999 WHERE data = 'ok'", true);
        total += test(engine, "SELECT 只读表",          A, "SELECT * FROM ro_table", true);
        total += test(engine, "INSERT 只写表",          A, "INSERT INTO wo_table (data) VALUES ('ok')", true);
        total += test(engine, "UPDATE 只写表",          A, "UPDATE wo_table SET data = 'updated' WHERE data = 'ok'", true);
        total += test(engine, "SELECT 聚合",            A, "SELECT COUNT(*) AS n, SUM(val) AS s FROM rw_table", true);
        total += test(engine, "JOIN 自己的两张表",     A, "SELECT r.data, w.data FROM rw_table r, ro_table w LIMIT 1", true);
        total += test(engine, "子查询",                A, "SELECT * FROM rw_table WHERE val > (SELECT AVG(val) FROM rw_table)", true);
        total += test(engine, "UNION",                 A, "SELECT data FROM rw_table UNION SELECT data FROM ro_table", true);

        // ════════════════════════
        // 类别 2: 模式违反
        // ════════════════════════
        pause();
        System.out.println("  === 2. 模式违反（应全部拦截）===\n");

        total += test(engine, "INSERT 只读表",        A, "INSERT INTO ro_table (data) VALUES ('fail')", false);
        total += test(engine, "UPDATE 只读表",        A, "UPDATE ro_table SET data = 'hack'", false);
        total += test(engine, "DELETE 只读表",        A, "DELETE FROM ro_table", false);
        total += test(engine, "SELECT 只写表",        A, "SELECT * FROM wo_table", false);
        total += test(engine, "DELETE 只写表",        A, "DELETE FROM wo_table", false);
        total += test(engine, "JOIN 读写+只写 SELECT", A, "SELECT r.data, w.data FROM rw_table r, wo_table w LIMIT 1", false);

        // ════════════════════════
        // 类别 3: 跨租户攻击
        // ════════════════════════
        pause();
        System.out.println("  === 3. 跨租户攻击（应全部拦截）===\n");

        total += test(engine, "A 查 B 的表",          A, "SELECT * FROM bob_table", false);
        total += test(engine, "A INSERT B 的表",       A, "INSERT INTO bob_table (data) VALUES ('hack')", false);
        total += test(engine, "A JOIN B 的表",         A, "SELECT * FROM rw_table, bob_table LIMIT 1", false);
        total += test(engine, "A 子查询窃取 B",       A, "SELECT * FROM rw_table WHERE data IN (SELECT data FROM bob_table)", false);
        total += test(engine, "A UNION B 的表",       A, "SELECT data FROM rw_table UNION SELECT data FROM bob_table", false);
        total += test(engine, "B 查 A 的只读表",      B, "SELECT * FROM ro_table", false);

        // ════════════════════════
        // 类别 4: SQL 注入攻击
        // ════════════════════════
        pause();
        System.out.println("  === 4. SQL 注入攻击（应全部拦截）===\n");

        total += test(engine, "DROP TABLE",           A, "DROP TABLE rw_table", false);
        total += test(engine, "ALTER TABLE",          A, "ALTER TABLE rw_table ADD COLUMN hack VARCHAR(100)", false);
        total += test(engine, "TRUNCATE TABLE",       A, "TRUNCATE TABLE rw_table", false);
        total += test(engine, "CREATE TABLE",         A, "CREATE TABLE backdoor (id INT)", false);
        total += test(engine, "多语句注入",          A, "SELECT * FROM rw_table; DROP TABLE rw_table", false);
        total += test(engine, "多语句 INSERT+DDL",    A, "INSERT INTO rw_table (data,val) VALUES ('x',0); DROP TABLE rw_table", false);
        total += test(engine, "RENAME TABLE",         A, "RENAME TABLE rw_table TO hacked", false);
        total += test(engine, "DROP DATABASE",        A, "DROP DATABASE javatest", false);
        total += test(engine, "SET 变量注入",         A, "SET @a = 1", false);

        // ════════════════════════
        // 类别 5: 提权尝试
        // ════════════════════════
        pause();
        System.out.println("  === 5. 提权尝试（应全部拦截）===\n");

        total += test(engine, "SELECT mysql.user",    A, "SELECT * FROM mysql.user LIMIT 1", false);
        total += test(engine, "访问 ds_table",        A, "SELECT * FROM ds_table", false);
        total += test(engine, "修改元数据 ds_table",  A, "INSERT INTO ds_table (name,owner_id,description) VALUES ('hack','A','')", false);
        total += test(engine, "GRANT 自己权限",      A, "GRANT ALL ON *.* TO 'alice'@'%'", false);
        total += test(engine, "REVOKE 别人权限",     A, "REVOKE ALL ON *.* FROM 'root'@'%'", false);

        // ════════════════════════
        // 类别 6: 边界/绕过
        // ════════════════════════
        pause();
        System.out.println("  === 6. 边界/绕过（应全部拦截）===\n");

        total += test(engine, "注释绕过 DROP",        A, "DR/**/OP TABLE rw_table", false);
        total += test(engine, "大小写绕过",          A, "dRoP tAbLe rw_table", false);
        total += test(engine, "反引号访问系统表",    A, "SELECT * FROM `mysql`.`user` LIMIT 1", false);
        total += test(engine, "十六进制表名",        A, "SELECT * FROM 0x72775f7461626c65", false);

        // ════════════════════════
        // 类别 7: 跨表+跨模式复杂操作
        // ════════════════════════
        pause();
        System.out.println("  === 7. 跨表+跨模式复杂操作 ===\n");

        total += test(engine, "INSERT SELECT from只读→读写",  A,
                "INSERT INTO rw_table (data,val) SELECT data,1 FROM ro_table", true);
        total += test(engine, "INSERT SELECT from读写→只读",  A,
                "INSERT INTO ro_table (data) SELECT data FROM rw_table", false);
        total += test(engine, "UPDATE 子查询读只读表",         A,
                "UPDATE rw_table SET val=0 WHERE data IN (SELECT data FROM ro_table)", true);
        total += test(engine, "UPDATE 子查询读只写表",         A,
                "UPDATE rw_table SET val=0 WHERE data IN (SELECT data FROM wo_table)", false);
        total += test(engine, "INSERT 子查询跨租户",           A,
                "INSERT INTO rw_table (data,val) SELECT data,1 FROM bob_table", false);
        total += test(engine, "UPDATE JOIN 只写表",            A,
                "UPDATE rw_table r JOIN wo_table w SET r.val=0", false);
        total += test(engine, "SELECT JOIN 三表(读写+只读+只写)", A,
                "SELECT r.data FROM rw_table r, ro_table o, wo_table w LIMIT 1", false);
        total += test(engine, "SELECT rw + 子查询 ro(通过) + 子查询 wo(拦截)", A,
                "SELECT * FROM rw_table WHERE data IN (SELECT data FROM ro_table) AND val > (SELECT COUNT(*) FROM wo_table)", false);
        total += test(engine, "INSERT 三表混合: 读写OK+只读FAIL", A,
                "INSERT INTO ro_table (data) SELECT r.data FROM rw_table r, wo_table w LIMIT 1", false);

        // ════════════════════════
        // 类别 8: 嵌套/聚合/别名 边界
        // ════════════════════════
        pause();
        System.out.println("  === 8. 嵌套/聚合/别名 边界 ===\n");

        total += test(engine, "双层 INSERT SELECT",          A,
                "INSERT INTO rw_table (data,val) SELECT data,COUNT(*) FROM ro_table GROUP BY data", true);
        total += test(engine, "三层嵌套子查询",               A,
                "SELECT * FROM rw_table WHERE val > (SELECT AVG(val) FROM rw_table WHERE val > (SELECT MIN(val) FROM rw_table))", true);
        total += test(engine, "EXISTS + 子查询",             A,
                "SELECT * FROM rw_table r WHERE EXISTS (SELECT 1 FROM ro_table o WHERE o.data = r.data)", true);
        total += test(engine, "NOT EXISTS 跨租户",           A,
                "SELECT * FROM rw_table r WHERE NOT EXISTS (SELECT 1 FROM bob_table b WHERE b.data = r.data)", false);
        total += test(engine, "HAVING 聚合过滤",             A,
                "SELECT data,COUNT(*) AS n FROM rw_table GROUP BY data HAVING n > 0", true);
        total += test(engine, "ORDER BY 子查询",             A,
                "SELECT * FROM rw_table ORDER BY (SELECT COUNT(*) FROM ro_table) DESC", true);
        total += test(engine, "别名遮蔽: 别名=真实表名",       A,
                "SELECT * FROM rw_table scores WHERE scores.val > 0", true);
        total += test(engine, "COUNT(*) + DISTINCT + JOIN",  A,
                "SELECT COUNT(DISTINCT r.data) FROM rw_table r, ro_table o WHERE r.data = o.data", true);

        // ════════════════════════
        // 类别 9: 写操作边界
        // ════════════════════════
        pause();
        System.out.println("  === 9. 写操作边界 ===\n");

        total += test(engine, "INSERT DEFAULT VALUES",        A,
                "INSERT INTO rw_table (data,val) VALUES ('default',0)", true);
        total += test(engine, "INSERT 表达式计算",            A,
                "INSERT INTO rw_table (data,val) VALUES ('calc', (SELECT COUNT(*) FROM ro_table))", true);
        total += test(engine, "UPDATE SET=子查询",            A,
                "UPDATE rw_table SET val = (SELECT COUNT(*) FROM ro_table) WHERE data='hello'", true);
        total += test(engine, "UPDATE SET=子查询只写表",       A,
                "UPDATE rw_table SET val = (SELECT COUNT(*) FROM wo_table) WHERE data='hello'", false);
        total += test(engine, "UPDATE LIMIT",                 A,
                "UPDATE rw_table SET val=0 ORDER BY val DESC LIMIT 1", true);
        total += test(engine, "INSERT SELECT 有多列",         A,
                "INSERT INTO rw_table (data,val) SELECT data,1 FROM ro_table LIMIT 1", true);

        // ════════════════════════
        // 类别 10: 极端边界
        // ════════════════════════
        pause();
        System.out.println("  === 10. 极端边界 ===\n");

        total += test(engine, "SELECT from DUAL",             A,
                "SELECT 1+1 AS result", true);
        total += test(engine, "SELECT 常量 + 函数",            A,
                "SELECT NOW(), DATABASE(), VERSION()", true);
        total += test(engine, "空字符串表名",                  A,
                "SELECT * FROM \"\"", false);
        total += test(engine, "块注释 /* comment */",         A,
                "SELECT * FROM rw_table /* comment */", true);
        total += test(engine, "分号结尾 SQL",                 A,
                "SELECT * FROM rw_table;", true);

        // 清理
        for (String u : List.of(A, B))
            for (TableInfo old : engine.tables().list(u))
                try { engine.tables().drop(u, old.getId()); } catch (Exception ignored) {}

        System.out.println("\n==========================================");
        System.out.println("  总计: " + total + " | PASS: " + pass + " | FAIL: " + fail);
        System.out.println("==========================================");
        if (fail > 0) System.exit(1);
    }

    static int test(DatasheetEngine engine, String name, String userId, String sql, boolean shouldPass) {
        System.out.printf("  %-35s → ", name);
        try {
            Object r = engine.data().sql(userId, sql);
            if (shouldPass) { System.out.println("PASS"); pass++; }
            else { System.out.println("FAIL — 应拦截但通过: " + r); fail++; }
        } catch (Exception e) {
            String msg = e.getMessage().lines().findFirst().orElse("");
            if (!shouldPass) { System.out.println("PASS (拦截)"); pass++; }
            else { System.out.println("FAIL — " + msg); fail++; }
        }
        return 1;
    }

    static void pause() {
        System.out.print("    按回车继续...");
        try { System.in.read(); } catch (Exception ignored) {}
        System.out.println();
    }
}
