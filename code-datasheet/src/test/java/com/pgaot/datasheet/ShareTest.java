package com.pgaot.datasheet;

import com.pgaot.datasheet.api.DatasheetEngine;
import com.pgaot.datasheet.common.model.*;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("resource")
class ShareTest {

    private static DatasheetEngine engine;
    private static final String OWNER = "alice";
    private static final String GUEST = "bob";
    private static String tableId;

    @BeforeAll
    static void requireDb() {
        assumeTrue(EnvLoader.hasDb(), "跳过：需要数据库");
        engine = DatasheetEngine.fromEnv();
    }

    @BeforeAll
    static void cleanUp() {
        for (String u : List.of(OWNER, GUEST))
            for (TableInfo old : engine.tables().list(u))
                try { engine.tables().drop(u, old.getId()); } catch (Exception ignored) {}
    }

    @Test @Order(1)
    void ownerCreatesTable() {
        TableInfo t = engine.tables().create(OWNER, "scores", "成绩表", null, List.of(
                new ColumnInfo("name", ColumnType.STRING, true),
                new ColumnInfo("score", ColumnType.DECIMAL, false)));
        assertNotNull(t);
        tableId = t.getId();
    }

    @Test @Order(2)
    void ownerInsertsData() {
        engine.data().sql(OWNER, "INSERT INTO scores (name, score) VALUES ('张三', 95)");
        engine.data().sql(OWNER, "INSERT INTO scores (name, score) VALUES ('李四', 87)");
        engine.data().sql(OWNER, "INSERT INTO scores (name, score) VALUES ('王五', 73)");
        var rows = engine.data().<List<?>>sql(OWNER, "SELECT * FROM scores");
        assertEquals(3, rows.size());
    }

    @Test @Order(3)
    void guestCannotAccessWithoutShare() {
        assertThrows(Exception.class, () -> engine.data().sql(GUEST, "SELECT * FROM scores"));
        assertThrows(Exception.class, () ->
                engine.data().sql(GUEST, "INSERT INTO scores (name,score) VALUES ('hack',0)"));
    }

    @Test @Order(4) void ownerSharesSelectOnly() {
        engine.shares().share(OWNER, tableId, GUEST, SharePermission.SELECT_ONLY);
        assertEquals(1, engine.shares().listSent(OWNER).size());
        assertEquals(1, engine.shares().listReceived(GUEST).size());
    }

    @Test @Order(5) void guestCanSelectWithShare() {
        engine.data().sql(GUEST, "SELECT * FROM scores");
    }

    @Test @Order(6) void guestInsertUpdateDeleteShouldFailWithSelectOnly() {
        assertThrows(Exception.class, () ->
                engine.data().sql(GUEST, "INSERT INTO scores (name,score) VALUES ('hack',0)"));
        assertThrows(Exception.class, () ->
                engine.data().sql(GUEST, "UPDATE scores SET score = 0 WHERE name = '张三'"));
        assertThrows(Exception.class, () ->
                engine.data().sql(GUEST, "DELETE FROM scores WHERE name = '张三'"));
    }

    @Test @Order(7) void ownerUpgradesToAllPermissions() {
        engine.shares().share(OWNER, tableId, GUEST, SharePermission.ALL);
        engine.data().sql(GUEST, "INSERT INTO scores (name,score) VALUES ('guest1',88)");
    }

    @Test @Order(8) void guestCanUpdateAndDeleteWithAll() {
        engine.data().sql(GUEST, "UPDATE scores SET score = 99 WHERE name = 'guest1'");
        engine.data().sql(GUEST, "DELETE FROM scores WHERE name = 'guest1'");
    }

    @Test @Order(9) void customPermissionsWork() {
        engine.shares().share(OWNER, tableId, GUEST,
                new SharePermission(true, true, false, false));
        // SELECT + INSERT ok
        engine.data().sql(GUEST, "SELECT * FROM scores");
        engine.data().sql(GUEST, "INSERT INTO scores (name,score) VALUES ('guest2',50)");
        // UPDATE + DELETE should fail
        assertThrows(Exception.class, () ->
                engine.data().sql(GUEST, "UPDATE scores SET score = 0"));
        assertThrows(Exception.class, () ->
                engine.data().sql(GUEST, "DELETE FROM scores WHERE name = 'guest2'"));
    }

    @Test @Order(10) void firewallBlocksDDLFromGuest() {
        assertThrows(Exception.class, () -> engine.data().sql(GUEST, "DROP TABLE scores"));
        assertThrows(Exception.class, () -> engine.data().sql(GUEST, "ALTER TABLE scores ADD COLUMN hack VARCHAR(100)"));
        assertThrows(Exception.class, () -> engine.data().sql(GUEST, "TRUNCATE TABLE scores"));
        assertThrows(Exception.class, () -> engine.data().sql(GUEST, "CREATE TABLE backdoor (id INT)"));
    }

    @Test @Order(11) void firewallBlocksInjectionFromGuest() {
        assertThrows(Exception.class, () ->
                engine.data().sql(GUEST, "SELECT * FROM scores; DROP TABLE scores"));
    }

    @Test @Order(12) void unshareBlocksGuest() {
        engine.shares().unshare(OWNER, tableId, GUEST);
        assertThrows(Exception.class, () -> engine.data().sql(GUEST, "SELECT * FROM scores"));
    }

    @Test @Order(13) void ownerDropsTable() {
        engine.tables().drop(OWNER, tableId);
        assertNull(engine.tables().get(tableId));
    }

    @AfterAll static void close() { if (engine != null) engine.close(); }
}
