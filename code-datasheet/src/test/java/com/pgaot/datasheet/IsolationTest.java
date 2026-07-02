package com.pgaot.datasheet;

import com.pgaot.datasheet.api.DatasheetEngine;
import com.pgaot.datasheet.common.model.*;

import java.util.List;
import java.util.Map;

public class IsolationTest {

    static int pass = 0, fail = 0;

    public static void main(String[] args) {
        DatasheetEngine engine = DatasheetEngine.fromEnv();

        System.out.println("==========================================");
        System.out.println("  code-datasheet 隔离测试");
        System.out.println("==========================================\n");
        System.out.println("  隔离: 表前缀 (userId_tableName) + owner 校验\n");

        String userA = "tenant_a";
        String userB = "tenant_b";
        String tableId;

        for (String u : List.of(userA, userB)) {
            for (TableInfo old : engine.tables().list(u)) {
                try { engine.tables().drop(u, old.getId()); } catch (Exception ignored) {}
            }
        }

        pause();
        System.out.println("  === 阶段一：建表 + 批量插入 ===\n");

        print("1. userA 建表 [name(STRING), score(NUMBER), city(STRING)]");
        TableInfo t = engine.tables().create(userA, "scores", "成绩表", null, List.of(
                new ColumnInfo("name",  ColumnType.STRING, true),
                new ColumnInfo("score", ColumnType.NUMBER, false),
                new ColumnInfo("city",  ColumnType.STRING, false)
        ));
        tableId = t.getId();
        check(t != null, "建表失败");
        System.out.println("    物理表: tenant_a_scores");

        pause();
        print("2. userA 批量插入 5 条");
        engine.data().insert(userA, tableId, List.of(
                Map.of("name", "张三", "score", 95, "city", "北京"),
                Map.of("name", "李四", "score", 87, "city", "上海"),
                Map.of("name", "王五", "score", 73, "city", "北京"),
                Map.of("name", "赵六", "score", 88, "city", "广州"),
                Map.of("name", "孙七", "score", 91, "city", "上海")
        ));
        check(true, "插入 5 行");

        pause();
        System.out.println("  === 阶段二：复杂 SQL ===\n");

        print("3. 聚合: 按城市统计平均分");
        List<Map<String, Object>> r3 = engine.data().sql(userA,
                "SELECT city, AVG(score) AS avg_score, COUNT(*) AS cnt FROM scores GROUP BY city ORDER BY avg_score DESC");
        check(r3.size() >= 2, "聚合异常");
        printRows(r3);

        pause();
        print("4. 子查询: 高于平均分的人");
        List<Map<String, Object>> r4 = engine.data().sql(userA,
                "SELECT name, score FROM scores WHERE score > (SELECT AVG(score) FROM scores) ORDER BY score DESC");
        check(r4.size() >= 1, "子查询异常");
        printRows(r4);

        pause();
        print("5. WHERE IN");
        List<Map<String, Object>> r5 = engine.data().sql(userA,
                "SELECT * FROM scores WHERE city IN ('北京', '广州') ORDER BY score DESC");
        check(r5.size() >= 2, "IN 查询异常");
        printRows(r5);

        pause();
        System.out.println("  === 阶段三：复杂 SQL 深度测试 ===\n");

        print("6a. 多层嵌套子查询(3层)");
        List<Map<String, Object>> rd1 = engine.data().sql(userA,
                "SELECT name, score FROM scores WHERE score > ("
                + "SELECT AVG(score) FROM scores WHERE score > ("
                + "SELECT MIN(score) FROM scores)) ORDER BY score DESC");
        check(rd1.size() >= 2, "3层子查询失败");
        printRows(rd1);

        pause();
        print("6b. EXISTS 子查询");
        List<Map<String, Object>> rd2 = engine.data().sql(userA,
                "SELECT name, score FROM scores s1 WHERE EXISTS ("
                + "SELECT 1 FROM scores s2 WHERE s2.city = s1.city AND s2.id != s1.id)");
        check(rd2.size() >= 1, "EXISTS 子查询失败");
        printRows(rd2);

        pause();
        print("6c. UNION ALL");
        List<Map<String, Object>> rd3 = engine.data().sql(userA,
                "SELECT name, score FROM scores WHERE score > 90 "
                + "UNION ALL SELECT name, score FROM scores WHERE score < 80 ORDER BY score DESC");
        check(rd3.size() >= 3, "UNION ALL 失败");
        printRows(rd3);

        pause();
        print("6d. CASE WHEN + 聚合");
        List<Map<String, Object>> rd4 = engine.data().sql(userA,
                "SELECT CASE WHEN score >= 90 THEN '优秀' WHEN score >= 80 THEN '良好' ELSE '及格' END AS grade, "
                + "COUNT(*) AS cnt FROM scores GROUP BY grade ORDER BY cnt DESC");
        check(rd4.size() >= 2, "CASE WHEN 失败");
        printRows(rd4);

        pause();
        print("6e. HAVING 过滤");
        List<Map<String, Object>> rd5 = engine.data().sql(userA,
                "SELECT city, AVG(score) AS avg_score, COUNT(*) AS cnt "
                + "FROM scores GROUP BY city HAVING avg_score > 85 ORDER BY avg_score DESC");
        check(rd5.size() >= 1, "HAVING 失败");
        printRows(rd5);

        pause();
        print("6f. LIKE + BETWEEN + ORDER BY 多条件");
        List<Map<String, Object>> rd6 = engine.data().sql(userA,
                "SELECT * FROM scores WHERE name LIKE '张%' OR score BETWEEN 85 AND 95 "
                + "ORDER BY score DESC, name ASC");
        check(rd6.size() >= 2, "多条件查询失败");
        printRows(rd6);

        pause();
        print("6g. 自连接 (SELF JOIN)");
        List<Map<String, Object>> rd7 = engine.data().sql(userA,
                "SELECT a.name AS n1, b.name AS n2, a.city "
                + "FROM scores a JOIN scores b ON a.city = b.city AND a.id < b.id");
        check(rd7.size() >= 1, "自连接失败");
        printRows(rd7);

        pause();
        print("6h. 数学函数 + 别名 + 排序");
        List<Map<String, Object>> rd8 = engine.data().sql(userA,
                "SELECT name, score, ROUND(score * 1.1, 1) AS bonus, "
                + "FLOOR(score / 10) * 10 AS score_range FROM scores ORDER BY bonus DESC");
        check(rd8.size() >= 3, "数学函数失败");
        printRows(rd8);

        pause();
        print("6i. COALESCE 处理空值");
        engine.data().insert(userA, tableId, Map.of("name", "NULL测试", "score", 50));
        List<Map<String, Object>> rd9 = engine.data().sql(userA,
                "SELECT name, COALESCE(city, '未知') AS city_label, score FROM scores ORDER BY id");
        check(rd9.size() >= 6, "COALESCE 失败");

        pause();
        print("6j. 复杂 JOIN + 聚合 + 排序");
        List<Map<String, Object>> rd10 = engine.data().sql(userA,
                "SELECT s.city, COUNT(*) AS n, MAX(s.score) AS top, MIN(s.score) AS bottom "
                + "FROM scores s WHERE s.city IS NOT NULL GROUP BY s.city "
                + "HAVING n >= 2 ORDER BY top DESC");
        check(rd10.size() >= 1, "复杂聚合失败");
        printRows(rd10);

        pause();
        System.out.println("  === 阶段四：SQL 边界测试（防绕过）===\n");

        print("6. 别名查询: FROM scores s");
        List<Map<String, Object>> r6a = engine.data().sql(userA,
                "SELECT s.name, s.score FROM scores s WHERE s.score > 80");
        check(r6a.size() >= 2, "别名查询失败");

        pause();
        print("7. 反引号表名: `scores`");
        List<Map<String, Object>> r6b = engine.data().sql(userA,
                "SELECT * FROM `scores` WHERE score > 80");
        check(r6b.size() >= 2, "反引号表名失败");

        pause();
        print("8. 字符串中嵌入表名不应被误判: WHERE name = 'scores'");
        List<Map<String, Object>> r6c = engine.data().sql(userA,
                "SELECT * FROM scores WHERE name = 'scores'");
        check(r6c.isEmpty(), "'scores' 字符串不应匹配");

        pause();
        print("9. userA 建第二张表 [course(STRING)] 测试跨表");
        TableInfo t2 = engine.tables().create(userA, "courses", "课程", null, List.of(
                new ColumnInfo("course", ColumnType.STRING, true)
        ));
        engine.data().insert(userA, t2.getId(), Map.of("course", "数学"));
        engine.data().insert(userA, t2.getId(), Map.of("course", "英语"));

        List<Map<String, Object>> r6e = engine.data().sql(userA,
                "SELECT * FROM scores, courses LIMIT 4");
        check(r6e.size() >= 1, "跨表查询失败");

        List<Map<String, Object>> r6f = engine.data().sql(userA,
                "SELECT name, course FROM scores, courses WHERE name IS NOT NULL AND course IS NOT NULL LIMIT 4");
        check(r6f.size() >= 1, "跨表JOIN失败");

        engine.tables().drop(userA, t2.getId());

        pause();
        System.out.println("  === 阶段五：SQL 注入/高危操作拦截 ===\n");

        print("11. DROP TABLE 注入");
        try {
            engine.data().sql(userA, "DROP TABLE scores");
            check(false, "DROP TABLE 应被拒绝");
        } catch (Exception e) {
            check(true, "拦截: " + e.getMessage().lines().findFirst().orElse(""));
        }

        pause();
        print("12. ALTER TABLE 注入");
        try {
            engine.data().sql(userA, "ALTER TABLE scores ADD COLUMN hack VARCHAR(100)");
            check(false, "ALTER TABLE 应被拒绝");
        } catch (Exception e) {
            check(true, "拦截: " + e.getMessage().lines().findFirst().orElse(""));
        }

        pause();
        print("13. TRUNCATE 注入");
        try {
            engine.data().sql(userA, "TRUNCATE TABLE scores");
            check(false, "TRUNCATE 应被拒绝");
        } catch (Exception e) {
            check(true, "拦截: " + e.getMessage().lines().findFirst().orElse(""));
        }

        pause();
        print("14. CREATE TABLE 注入");
        try {
            engine.data().sql(userA, "CREATE TABLE backdoor (id INT)");
            check(false, "CREATE TABLE 应被拒绝");
        } catch (Exception e) {
            check(true, "拦截: " + e.getMessage().lines().findFirst().orElse(""));
        }

        pause();
        print("15. 多语句注入: SELECT; DROP TABLE");
        try {
            engine.data().sql(userA, "SELECT * FROM scores; DROP TABLE scores");
            check(false, "多语句注入应被拒绝");
        } catch (Exception e) {
            check(true, "拦截: " + e.getMessage().lines().findFirst().orElse(""));
        }

        pause();
        System.out.println("  === 阶段六：跨租户 SQL 隔离 ===\n");

        print("16. userB 建表 [val(STRING)]");
        TableInfo tb = engine.tables().create(userB, "secrets", "机密", null, List.of(
                new ColumnInfo("val", ColumnType.STRING, true)
        ));
        engine.data().insert(userB, tb.getId(), Map.of("val", "secret_data"));

        pause();
        print("17. userA 尝试跨租户查询: JOIN scores 和 B 的 secrets");
        try {
            engine.data().sql(userA,
                    "SELECT s.name, t.val FROM scores s, secrets t LIMIT 1");
            check(false, "跨租户查询应被拒绝");
        } catch (Exception e) {
            check(e.getMessage().contains("无权"), "拦截: " + e.getMessage().lines().findFirst().orElse(""));
        }

        pause();
        print("18. userA 尝试子查询窃取: WHERE name IN (SELECT val FROM secrets)");
        try {
            engine.data().sql(userA,
                    "SELECT * FROM scores WHERE name IN (SELECT val FROM secrets)");
            check(false, "子查询窃取应被拒绝");
        } catch (Exception e) {
            check(e.getMessage().contains("无权"), "拦截: " + e.getMessage().lines().findFirst().orElse(""));
        }

        engine.tables().drop(userB, tb.getId());

        pause();
        System.out.println("  === 阶段七：跨表写入 + 只读拦截 ===\n");

        print("19. userA 建表 [logs(STRING)]");
        TableInfo tLog = engine.tables().create(userA, "logs", "日志", null, List.of(
                new ColumnInfo("msg", ColumnType.STRING, true)
        ));

        print("20. 设 logs 为 READ_ONLY");
        engine.tables().setMode(userA, tLog.getId(), TableMode.READ_ONLY);

        pause();
        print("21. 复杂 SQL 同时写两张表: INSERT INTO scores...INSERT INTO logs");
        try {
            engine.data().sql(userA,
                    "INSERT INTO scores (name, score) VALUES ('test', 50)");
            engine.data().sql(userA,
                    "INSERT INTO logs (msg) VALUES ('should fail')");
            check(false, "logs 只读，INSERT 应被拒绝");
        } catch (Exception e) {
            check(e.getMessage().contains("只读"), "拦截: " + e.getMessage().lines().findFirst().orElse(""));
        }

        pause();
        print("22. 验证: scores 的 INSERT 已提交（独立调用无事务），logs 被拦截");
        List<Map<String, Object>> r22 = engine.data().sql(userA,
                "SELECT * FROM scores WHERE name = 'test'");
        check(!r22.isEmpty(), "scores 应有 test 数据（独立调用无事务，拦截前已提交）");

        engine.tables().setMode(userA, tLog.getId(), TableMode.WRITE_ONLY);

        pause();
        print("23. 单 SQL 同时查 scores(READ_WRITE) 和 logs(WRITE_ONLY)");
        try {
            engine.data().sql(userA,
                    "SELECT s.name, l.msg FROM scores s, logs l LIMIT 1");
            check(false, "WRITE_ONLY 不应允许 SELECT");
        } catch (Exception e) {
            check(e.getMessage().contains("只写"), "拦截: " + e.getMessage().lines().findFirst().orElse(""));
        }

        engine.tables().drop(userA, tLog.getId());

        pause();
        System.out.println("  === 阶段八：字段变更 ===\n");

        print("11. userA 加列 [passed(BOOLEAN)]");
        engine.tables().addColumn(userA, tableId, new ColumnInfo("passed", ColumnType.BOOLEAN, false));
        check(engine.tables().get(tableId).getColumns().size() == 4, "加列失败");

        pause();
        print("12. 更新: score>=90 设为 passed=true");
        engine.data().update(userA, tableId, "score >= 90", Map.of("passed", true));
        List<Map<String, Object>> r7 = engine.data().sql(userA,
                "SELECT name, score, passed FROM scores WHERE passed = true");
        check(r7.size() >= 2, "更新异常");
        printRows(r7);

        pause();
        print("13. 重命名列: passed → is_passed");
        engine.tables().renameColumn(userA, tableId, "passed", "is_passed");
        List<Map<String, Object>> r8 = engine.data().sql(userA,
                "SELECT name, score, is_passed FROM scores WHERE is_passed = true");
        check(r8.size() >= 2, "重命名后查询异常");
        printRows(r8);

        pause();
        print("14. 删列 [city]");
        engine.tables().dropColumn(userA, tableId, "city");
        check(engine.tables().get(tableId).getColumns().size() == 3, "删列失败");

        pause();
        System.out.println("  === 阶段九：模式控制 ===\n");

        print("15. 设置表为 READ_ONLY 模式");
        engine.tables().setMode(userA, tableId, TableMode.READ_ONLY);
        check(true, "设只读成功");

        pause();
        print("16. SELECT — 只读模式仍可查询");
        List<Map<String, Object>> r16 = engine.data().sql(userA,
                "SELECT * FROM scores WHERE is_passed = true");
        check(r16.size() >= 2, "只读模式下查询失败");

        pause();
        print("17. INSERT — 只读模式应被拒绝");
        try {
            engine.data().insert(userA, tableId, Map.of("name", "test", "score", 0));
            check(false, "只读不应允许 INSERT");
        } catch (Exception e) {
            check(e.getMessage().contains("只读"), "拦截: " + e.getMessage());
        }

        pause();
        print("18. DELETE — 只读模式应被拒绝");
        try {
            engine.data().sql(userA, "DELETE FROM scores WHERE name = 'test'");
            check(false, "只读不应允许 DELETE");
        } catch (Exception e) {
            check(e.getMessage().contains("只读"), "拦截: " + e.getMessage());
        }

        pause();
        print("19. 恢复 READ_WRITE 模式");
        engine.tables().setMode(userA, tableId, TableMode.READ_WRITE);
        check(true, "恢复成功");

        pause();
        System.out.println("  === 阶段十：跨租户隔离 ===\n");

        print("20. userB 尝试查询 userA 的表");
        try {
            engine.data().sql(userB, "SELECT * FROM scores");
            check(false, "应被拒绝");
        } catch (Exception e) {
            check(true, "拒绝: " + e.getMessage().lines().findFirst().orElse(""));
        }

        pause();
        print("21. userB 尝试删表 — 非 owner");
        try {
            engine.tables().drop(userB, tableId);
            check(false, "非 owner 不应删表");
        } catch (Exception e) {
            check(e.getMessage().contains("创建者"), "拦截: " + e.getMessage());
        }

        pause();
        System.out.println("  === 阶段十一：owner 清场 ===\n");

        print("22. userA 删表");
        engine.tables().drop(userA, tableId);
        check(engine.tables().get(tableId) == null, "删表失败");

        System.out.println("\n==========================================");
        System.out.println("  总计: " + (pass + fail) + " | PASS: " + pass + " | FAIL: " + fail);
        System.out.println("==========================================");
        if (fail > 0) System.exit(1);
    }

    static void print(String msg) { System.out.print("  " + msg + " ... "); }

    static void check(boolean ok, String detail) {
        if (ok) { System.out.println("PASS"); pass++; }
        else    { System.out.println("FAIL — " + detail); fail++; }
    }

    static void printRows(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) { System.out.println("    (空)"); return; }
        System.out.println("    ┌" + "─".repeat(50));
        for (Map<String, Object> row : rows)
            System.out.println("    │ " + row.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .reduce((a, b) -> a + ", " + b).orElse(""));
        System.out.println("    └" + "─".repeat(50) + " 共 " + rows.size() + " 行");
    }

    static void pause() {
        System.out.print("    按回车继续...");
        try { System.in.read(); } catch (Exception ignored) {}
        System.out.println();
    }
}
