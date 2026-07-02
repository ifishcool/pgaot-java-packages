package com.pgaot.datasheet;

import com.pgaot.datasheet.api.DatasheetEngine;
import com.pgaot.datasheet.common.model.*;

import java.util.List;
import java.util.Map;

/**
 * 导入导出 + 软删除演示.
 */
public class ImportExportTest {

    static int pass = 0, fail = 0;

    public static void main(String[] args) {
        DatasheetEngine engine = DatasheetEngine.fromEnv("DATA");

        System.out.println("==========================================");
        System.out.println("  code-datasheet 导入导出 + 软删除测试");
        System.out.println("==========================================\n");

        String user = "demo_io";
        String tableId;

        // 清理
        for (TableInfo old : engine.tables().list(user))
            try { engine.tables().drop(user, old.getId()); } catch (Exception ignored) {}
        // 物理清除之前软删除的
        for (TableInfo old : engine.tables().list(user))
            try { engine.tables().purge(user, old.getId()); } catch (Exception ignored) {}

        // ════════════════════════
        // 阶段一: 建表 + 初始数据
        // ════════════════════════
        pause();
        System.out.println("  === 阶段一: 建表 + SQL 插入 ===\n");

        print("1. 建表 [product(STRING), price(NUMBER), qty(NUMBER)]");
        TableInfo t = engine.tables().create(user, "inventory", "库存表", null, List.of(
                new ColumnInfo("product", ColumnType.STRING, true),
                new ColumnInfo("price",   ColumnType.DECIMAL, false),
                new ColumnInfo("qty",     ColumnType.DECIMAL, false)
        ));
        tableId = t.getId();

        print("2. SQL 插入 3 条");
        engine.data().sql(user, "INSERT INTO inventory (product,price,qty) VALUES ('笔记本',5999,10)");
        engine.data().sql(user, "INSERT INTO inventory (product,price,qty) VALUES ('显示器',1999,5)");
        engine.data().sql(user, "INSERT INTO inventory (product,price,qty) VALUES ('键盘',299,20)");
        List<Map<String, Object>> r2 = engine.data().sql(user, "SELECT * FROM inventory");
        check(r2.size() == 3, "应 3 行");
        printRows(r2);

        // ════════════════════════
        // 阶段二: 导出
        // ════════════════════════
        pause();
        System.out.println("  === 阶段二: 导出 ===\n");

        print("3. 导出 CSV");
        String csv = engine.data().exportCsv(user, tableId, null, null);
        check(csv.contains("笔记本"), "CSV 缺数据");
        System.out.println("    " + csv.replace("\n", "\n    "));

        print("4. 导出 JSON");
        String json = engine.data().exportJson(user, tableId, null, null);
        check(json.contains("笔记本"), "JSON 缺数据");
        System.out.println("    " + json.replace("\n", "\n    "));

        // ════════════════════════
        // 阶段三: 导入 CSV
        // ════════════════════════
        pause();
        System.out.println("  === 阶段三: 导入 CSV ===\n");

        engine.tables().drop(user, tableId);
        engine.tables().restore(user, tableId);
        engine.tables().truncate(user, tableId);

        print("5. 导入 CSV 数据");
        String csvData = "product,price,qty\n鼠标,99,30\n耳机,499,15\n网线,15,100";
        int n5 = engine.data().importCsv(user, tableId, csvData);
        check(n5 == 3, "导入 CSV 应 3 行: " + n5);
        List<Map<String, Object>> r5 = engine.data().sql(user, "SELECT * FROM inventory");
        check(r5.size() == 3, "导入后应 3 行");
        printRows(r5);

        // ════════════════════════
        // 阶段四: 导入 JSON
        // ════════════════════════
        pause();
        System.out.println("  === 阶段四: 导入 JSON ===\n");

        engine.tables().truncate(user, tableId);

        print("6. 导入 JSON 数据");
        String jsonData = "[{\"product\":\"U盘\",\"price\":59,\"qty\":50},{\"product\":\"硬盘\",\"price\":399,\"qty\":8}]";
        int n6 = engine.data().importJson(user, tableId, jsonData);
        check(n6 == 2, "导入 JSON 应 2 行: " + n6);
        List<Map<String, Object>> r6 = engine.data().sql(user, "SELECT * FROM inventory");
        check(r6.size() == 2, "导入后应 2 行");
        printRows(r6);

        // ════════════════════════
        // 阶段五: 先导出再导入（数据迁移）
        // ════════════════════════
        pause();
        System.out.println("  === 阶段五: 导出 JSON → 导入（数据迁移）===\n");

        print("7. 导出当前数据为 JSON");
        String exportJson = engine.data().exportJson(user, tableId, null, null);

        engine.tables().truncate(user, tableId);

        print("8. 清空后重新导入");
        int n8 = engine.data().importJson(user, tableId, exportJson);
        check(n8 == 2, "重导入应 2 行");
        List<Map<String, Object>> r8 = engine.data().sql(user, "SELECT * FROM inventory");
        check(r8.size() == 2, "迁移后应 2 行");
        printRows(r8);

        // ════════════════════════
        // 阶段六: 软删除 + 恢复
        // ════════════════════════
        pause();
        System.out.println("  === 阶段六: 软删除 + 恢复 ===\n");

        print("9. 软删除 inventory");
        engine.tables().drop(user, tableId);

        print("10. 查表 — 已删除应不存在");
        List<TableInfo> list10 = engine.tables().list(user);
        check(list10.stream().noneMatch(ti -> ti.getId().equals(tableId)), "已删除仍有");

        print("11. 恢复 inventory");
        engine.tables().restore(user, tableId);
        List<TableInfo> list11 = engine.tables().list(user);
        check(list11.stream().anyMatch(ti -> ti.getId().equals(tableId)), "恢复后应有");
        List<Map<String, Object>> r11 = engine.data().sql(user, "SELECT * FROM inventory");
        check(r11.size() == 2, "恢复后数据应还在");
        printRows(r11);

        // ════════════════════════
        // 阶段七: 物理删除
        // ════════════════════════
        pause();
        System.out.println("  === 阶段七: 物理删除 ===\n");

        print("12. 物理删除 inventory（不可恢复）");
        engine.tables().purge(user, tableId);
        TableInfo gone = engine.tables().get(tableId);
        check(gone == null, "物理删除后应 null");

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
            System.out.println("    │ " + row);
    }

    static void pause() {
        System.out.print("    按回车继续...");
        try { System.in.read(); } catch (Exception ignored) {}
        System.out.println();
    }
}
