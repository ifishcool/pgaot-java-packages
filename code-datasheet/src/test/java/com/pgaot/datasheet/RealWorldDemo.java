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
@SuppressWarnings("resource")
class RealWorldDemo {

    private static DatasheetEngine engine;
    private static final String ALICE = "alice_real";
    private static final String BOB = "bob_real";
    private static final String CHARLIE = "charlie_real";
    private static String custId, orderId, productId;

    @BeforeAll
    static void setup() {
        assumeTrue(EnvLoader.hasDb(), "跳过：需要数据库");
        engine = DatasheetEngine.fromEnv();
            for (String u : List.of(ALICE, BOB, CHARLIE))
            for (TableInfo old : engine.tables().list(u))
                try { engine.tables().drop(u, old.getId()); } catch (Exception ignored) {}
    }

    @Test @Order(1) void aliceCreatesCustomersTable() {
        TableInfo t = engine.tables().create(ALICE, "customers", "客户表", "公司客户信息", List.of(
                new ColumnInfo("name", ColumnType.STRING, true),
                new ColumnInfo("phone", ColumnType.STRING, false),
                new ColumnInfo("city", ColumnType.STRING, false),
                new ColumnInfo("level", ColumnType.INT, false)));
        assertNotNull(t);
        custId = t.getId();
    }

    @Test @Order(2) void aliceCreatesProductsTable() {
        TableInfo t = engine.tables().create(ALICE, "products", "产品表", "在售产品", List.of(
                new ColumnInfo("name", ColumnType.STRING, true),
                new ColumnInfo("price", ColumnType.DECIMAL, false),
                new ColumnInfo("stock", ColumnType.INT, false)));
        assertNotNull(t);
        productId = t.getId();
    }

    @Test @Order(3) void aliceCreatesOrdersTable() {
        TableInfo t = engine.tables().create(ALICE, "orders", "订单表", "销售订单", List.of(
                new ColumnInfo("customer", ColumnType.STRING, true),
                new ColumnInfo("product", ColumnType.STRING, false),
                new ColumnInfo("amount", ColumnType.DECIMAL, false),
                new ColumnInfo("salesman", ColumnType.STRING, false)));
        assertNotNull(t);
        orderId = t.getId();
    }

    @Test @Order(4) void aliceSharesAllRightsToBob() {
        engine.shares().share(ALICE, custId, BOB, SharePermission.ALL);
        engine.shares().share(ALICE, productId, BOB, SharePermission.ALL);
        engine.shares().share(ALICE, orderId, BOB, SharePermission.ALL);
        assertEquals(3, engine.shares().listSent(ALICE).size());
    }

    @Test @Order(5) void aliceSharesOrderReadOnlyToCharlie() {
        engine.shares().share(ALICE, orderId, CHARLIE, SharePermission.SELECT_ONLY);
    }

    @Test @Order(6) void bobCanInsertCustomerAndOrder() {
        engine.data().sql(BOB, "INSERT INTO customers (name,phone,city,level) " +
                "VALUES ('XX公司','1380000','上海',3)");
        engine.data().sql(BOB, "INSERT INTO orders (customer,product,amount,salesman) " +
                "VALUES ('XX公司','产品A',5000,'bob')");
    }

    @Test @Order(7) void charlieCanOnlySelectOrders() {
        var r = engine.data().<List<?>>sql(CHARLIE, "SELECT * FROM orders");
        assertTrue(r.size() >= 1);
        assertThrows(Exception.class, () ->
                engine.data().sql(CHARLIE, "INSERT INTO orders (customer) VALUES ('x')"));
    }

    @Test @Order(8) void charlieCannotSeeCustomersOrProducts() {
        assertThrows(Exception.class, () ->
                engine.data().sql(CHARLIE, "SELECT * FROM customers"));
        assertThrows(Exception.class, () ->
                engine.data().sql(CHARLIE, "SELECT * FROM products"));
    }

    @AfterAll
    static void cleanup() {
        if (engine != null)
            for (String u : List.of(ALICE, BOB, CHARLIE))
                for (TableInfo old : engine.tables().list(u))
                    try { engine.tables().drop(u, old.getId()); } catch (Exception ignored) {}
        if (engine != null) engine.close();
    }
}
