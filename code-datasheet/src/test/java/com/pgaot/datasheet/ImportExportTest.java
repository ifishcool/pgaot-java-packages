package com.pgaot.datasheet;

import com.pgaot.datasheet.api.DatasheetEngine;
import com.pgaot.datasheet.common.model.*;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ImportExportTest {

    private static DatasheetEngine engine;
    private static final String USER = "demo_io";
    private static String tableId;

    @BeforeAll
    static void requireDb() {
        assumeTrue(EnvLoader.hasDb(), "跳过：需要数据库");
        engine = DatasheetEngine.fromEnv("DATA");
        for (TableInfo old : engine.tables().list(USER))
            try { engine.tables().drop(USER, old.getId()); } catch (Exception ignored) {}
    }

    @Test @Order(1) void createTable() {
        TableInfo t = engine.tables().create(USER, "inventory", "库存表", null, List.of(
                new ColumnInfo("product", ColumnType.STRING, true),
                new ColumnInfo("price", ColumnType.DECIMAL, false),
                new ColumnInfo("qty", ColumnType.DECIMAL, false)));
        assertNotNull(t);
        tableId = t.getId();
    }

    @Test @Order(2) void insertData() {
        engine.data().sql(USER, "INSERT INTO inventory (product,price,qty) VALUES ('笔记本',5999,10)");
        engine.data().sql(USER, "INSERT INTO inventory (product,price,qty) VALUES ('显示器',1999,5)");
        engine.data().sql(USER, "INSERT INTO inventory (product,price,qty) VALUES ('键盘',299,20)");
        var rows = engine.data().<List<?>>sql(USER, "SELECT * FROM inventory");
        assertEquals(3, rows.size());
    }

    @Test @Order(3) void exportCsv() {
        String csv = engine.data().exportCsv(USER, tableId, null, null);
        assertTrue(csv.contains("笔记本"));
    }

    @Test @Order(4) void exportJson() {
        String json = engine.data().exportJson(USER, tableId, null, null);
        assertTrue(json.contains("笔记本"));
    }

    @Test @Order(5) void importCsv() {
        engine.tables().truncate(USER, tableId);
        int n = engine.data().importCsv(USER, tableId,
                "product,price,qty\n鼠标,99,30\n耳机,499,15\n网线,15,100");
        assertEquals(3, n);
    }

    @Test @Order(6) void importJson() {
        engine.tables().truncate(USER, tableId);
        int n = engine.data().importJson(USER, tableId,
                "[{\"product\":\"U盘\",\"price\":79,\"qty\":200}," +
                "{\"product\":\"硬盘\",\"price\":499,\"qty\":10}]");
        assertEquals(2, n);
    }

    @AfterAll
    static void cleanup() {
        if (engine != null && tableId != null)
            try { engine.tables().drop(USER, tableId); } catch (Exception ignored) {}
    }
}
