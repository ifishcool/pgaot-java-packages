package com.pgaot.datasheet;

import com.pgaot.datasheet.api.DatasheetEngine;
import com.pgaot.datasheet.common.model.*;

import java.util.List;
import java.util.Map;

/**
 * 共享功能测试 — 全部数据操作通过 engine.data().sql() 走 readWrite 防火墙.
 */
public class ShareTest {

    static int pass = 0, fail = 0;

    public static void main(String[] args) {
        DatasheetEngine engine = DatasheetEngine.fromEnv();

        System.out.println("==========================================");
        System.out.println("  code-datasheet 共享功能测试 (全sql)");
        System.out.println("==========================================\n");

        String owner = "alice";
        String guest = "bob";
        String tableId;

        for (String u : List.of(owner, guest))
            for (TableInfo old : engine.tables().list(u))
                try { engine.tables().drop(u, old.getId()); } catch (Exception ignored) {}

        // ════════════════════════
        // 阶段一: 建表 + 插入
        // ════════════════════════
        pause();
        System.out.println("  === 阶段一: owner 建表并插入数据 ===\n");

        print("1. owner 建表 [name(STRING), score(NUMBER)]");
        TableInfo t = engine.tables().create(owner, "scores", "成绩表", null, List.of(
                new ColumnInfo("name",  ColumnType.STRING, true),
                new ColumnInfo("score", ColumnType.DECIMAL, false)
        ));
        tableId = t.getId();
        check(t != null, "建表失败");

        print("2. owner 用 sql INSERT 3 条");
        engine.data().sql(owner, "INSERT INTO scores (name, score) VALUES ('张三', 95)");
        engine.data().sql(owner, "INSERT INTO scores (name, score) VALUES ('李四', 87)");
        engine.data().sql(owner, "INSERT INTO scores (name, score) VALUES ('王五', 73)");
        List<Map<String, Object>> r2 = engine.data().sql(owner, "SELECT * FROM scores");
        check(r2.size() == 3, "应插入 3 行: " + r2.size());
        printRows(r2);

        // ════════════════════════
        // 阶段二: 无共享时 guest 无法访问
        // ════════════════════════
        pause();
        System.out.println("  === 阶段二: 无共享 → 全部拒绝 ===\n");

        print("3. guest SQL SELECT — 未共享应拒绝");
        try {
            engine.data().sql(guest, "SELECT * FROM scores");
            check(false, "未共享不应查到");
        } catch (Exception e) {
            check(true, "拒绝: " + e.getMessage().lines().findFirst().orElse(""));
        }

        print("4. guest SQL INSERT — 未共享应拒绝");
        try {
            engine.data().sql(guest, "INSERT INTO scores (name,score) VALUES ('hack',0)");
            check(false, "未共享不应插入");
        } catch (Exception e) {
            check(true, "拒绝");
        }

        // ════════════════════════
        // 阶段三: 只读共享
        // ════════════════════════
        pause();
        System.out.println("  === 阶段三: 只读共享 (SELECT only) ===\n");

        print("5. owner 共享给 guest — 仅查询");
        engine.shares().share(owner, tableId, guest, SharePermission.SELECT_ONLY);

        print("6. owner 查看共享列表");
        var sent = engine.shares().listSent(owner);
        check(sent.size() == 1, "应有共享记录");
        System.out.println("    共享给: " + sent.get(0).getToUser()
                + " 表: " + sent.get(0).getTableName()
                + " 权限: S=" + sent.get(0).getPermission().isCanSelect());

        print("7. guest 查看收到共享");
        var received = engine.shares().listReceived(guest);
        check(received.size() == 1, "应有收到共享");
        System.out.println("    来自: " + received.get(0).getFromUser()
                + " 表: " + received.get(0).getTableName());

        print("8a. guest 查看自己所有表（含来源）");
        var all = engine.tables().listWithSource(guest);
        for (var tws : all) {
            System.out.println("    │ " + tws.getSource() + " " + tws.getTableInfo().getName()
                    + (tws.getFromUser() != null ? " from " + tws.getFromUser() : "")
                    + " S=" + (tws.getPermission() != null && tws.getPermission().isCanSelect()));
        }
        check(all.size() == 1, "应有 1 个表（共享来的）");

        print("8b. guest SQL SELECT — 有权限应成功");
        List<Map<String, Object>> r8 = engine.data().sql(guest, "SELECT * FROM scores");
        check(r8.size() == 3, "应查到 3 行: " + r8.size());
        printRows(r8);

        print("9. guest SQL INSERT — 无权限应拒绝");
        try {
            engine.data().sql(guest, "INSERT INTO scores (name,score) VALUES ('hack',0)");
            check(false, "无 INSERT 权限");
        } catch (Exception e) {
            check(true, "拒绝");
        }

        print("10. guest SQL UPDATE — 无权限应拒绝");
        try {
            engine.data().sql(guest, "UPDATE scores SET score = 0 WHERE name = '张三'");
            check(false, "无 UPDATE 权限");
        } catch (Exception e) {
            check(true, "拒绝");
        }

        print("11. guest SQL DELETE — 无权限应拒绝");
        try {
            engine.data().sql(guest, "DELETE FROM scores WHERE name = '张三'");
            check(false, "无 DELETE 权限");
        } catch (Exception e) {
            check(true, "拒绝");
        }

        // ════════════════════════
        // 阶段四: 全权限
        // ════════════════════════
        pause();
        System.out.println("  === 阶段四: 全权限 (ALL) ===\n");

        print("12. owner 升级共享 → ALL");
        engine.shares().share(owner, tableId, guest, SharePermission.ALL);

        print("13. guest SQL INSERT");
        engine.data().sql(guest, "INSERT INTO scores (name,score) VALUES ('guest1',88)");
        List<Map<String, Object>> r13 = engine.data().sql(guest,
                "SELECT * FROM scores WHERE name = 'guest1'");
        check(r13.size() == 1, "INSERT 失败");
        printRows(r13);

        print("14. guest SQL UPDATE");
        engine.data().sql(guest, "UPDATE scores SET score = 99 WHERE name = 'guest1'");
        List<Map<String, Object>> r14 = engine.data().sql(guest,
                "SELECT * FROM scores WHERE name = 'guest1'");
        check(((Number) r14.get(0).get("score")).doubleValue() == 99.0, "UPDATE 失败");
        printRows(r14);

        print("15. guest SQL DELETE");
        engine.data().sql(guest, "DELETE FROM scores WHERE name = 'guest1'");
        List<Map<String, Object>> r15 = engine.data().sql(guest,
                "SELECT * FROM scores WHERE name = 'guest1'");
        check(r15.isEmpty(), "DELETE 失败");

        // ════════════════════════
        // 阶段五: 自定义权限
        // ════════════════════════
        pause();
        System.out.println("  === 阶段五: 自定义 (SELECT+INSERT, 无 UPDATE/DELETE) ===\n");

        print("16. owner 设自定义权限");
        engine.shares().share(owner, tableId, guest,
                new SharePermission(true, true, false, false));

        print("17. guest SQL SELECT");
        List<Map<String, Object>> r17 = engine.data().sql(guest, "SELECT * FROM scores");
        check(r17.size() >= 3, "SELECT 失败");

        print("18. guest SQL INSERT");
        engine.data().sql(guest, "INSERT INTO scores (name,score) VALUES ('guest2',50)");
        List<Map<String, Object>> r18 = engine.data().sql(guest,
                "SELECT * FROM scores WHERE name = 'guest2'");
        check(!r18.isEmpty(), "INSERT 失败");

        print("19. guest SQL UPDATE — 无权限应拒绝");
        try {
            engine.data().sql(guest, "UPDATE scores SET score = 0");
            check(false, "UPDATE 应拒绝");
        } catch (Exception e) {
            check(true, "拒绝");
        }

        print("20. guest SQL DELETE — 无权限应拒绝");
        try {
            engine.data().sql(guest, "DELETE FROM scores WHERE name = 'guest2'");
            check(false, "DELETE 应拒绝");
        } catch (Exception e) {
            check(true, "拒绝");
        }

        // ════════════════════════
        // 阶段六: 高危 SQL 拦截（共享用户也受防火墙保护）
        // ════════════════════════
        pause();
        System.out.println("  === 阶段六: 高危 SQL 拦截 ===\n");

        print("21. guest SQL DROP TABLE — 防火墙");
        try {
            engine.data().sql(guest, "DROP TABLE scores");
            check(false, "DROP TABLE 应拦截");
        } catch (Exception e) {
            check(true, "拦截: " + e.getMessage().lines().findFirst().orElse(""));
        }

        print("22. guest SQL ALTER TABLE — 防火墙");
        try {
            engine.data().sql(guest, "ALTER TABLE scores ADD COLUMN hack VARCHAR(100)");
            check(false, "ALTER TABLE 应拦截");
        } catch (Exception e) {
            check(true, "拦截");
        }

        print("23. guest SQL TRUNCATE — 防火墙");
        try {
            engine.data().sql(guest, "TRUNCATE TABLE scores");
            check(false, "TRUNCATE 应拦截");
        } catch (Exception e) {
            check(true, "拦截");
        }

        print("24. guest SQL CREATE TABLE — 防火墙");
        try {
            engine.data().sql(guest, "CREATE TABLE backdoor (id INT)");
            check(false, "CREATE TABLE 应拦截");
        } catch (Exception e) {
            check(true, "拦截");
        }

        print("25. guest SQL 多语句注入");
        try {
            engine.data().sql(guest, "SELECT * FROM scores; DROP TABLE scores");
            check(false, "多语句注入应拦截");
        } catch (Exception e) {
            check(true, "拦截");
        }

        print("26. guest SQL 子查询窃取 owner 其他表");
        engine.tables().create(owner, "secrets", "机密表", null, List.of(
                new ColumnInfo("secret", ColumnType.STRING, true)));
        engine.data().sql(owner, "INSERT INTO secrets (secret) VALUES ('top_secret')");
        try {
            engine.data().sql(guest, "SELECT * FROM scores WHERE name IN (SELECT secret FROM secrets)");
            check(false, "跨表窃取应拦截（secrets 未共享）");
        } catch (Exception e) {
            check(true, "拦截");
        }
        engine.tables().drop(owner,
                engine.tables().list(owner).stream()
                        .filter(ti -> "secrets".equals(ti.getName()))
                        .findFirst().get().getId());

        print("27. guest SQL INSERT 到不存在表 — 表名欺骗");
        try {
            engine.data().sql(guest, "INSERT INTO scores_backup (name,score) SELECT name,score FROM scores");
            check(false, "scores_backup 不存在应拦截");
        } catch (Exception e) {
            check(true, "拦截");
        }

        // ════════════════════════
        // 阶段七: 取消共享
        // ════════════════════════
        pause();
        System.out.println("  === 阶段六: 取消共享 ===\n");

        print("28. owner 取消共享");
        engine.shares().unshare(owner, tableId, guest);

        print("29. guest 再次查询 — 应拒绝");
        try {
            engine.data().sql(guest, "SELECT * FROM scores");
            check(false, "取消后不应查到");
        } catch (Exception e) {
            check(true, "拒绝");
        }

        print("30. owner 删表");
        engine.tables().drop(owner, tableId);
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
        for (Map<String, Object> row : rows)
            System.out.println("    │ " + row.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .reduce((a, b) -> a + ", " + b).orElse(""));
    }

    static void pause() {
        System.out.print("    按回车继续...");
        try { System.in.read(); } catch (Exception ignored) {}
        System.out.println();
    }
}
