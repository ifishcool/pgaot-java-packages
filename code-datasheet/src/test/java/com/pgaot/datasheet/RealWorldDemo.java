package com.pgaot.datasheet;

import com.pgaot.datasheet.api.DatasheetEngine;
import com.pgaot.datasheet.common.model.*;

import java.util.List;
import java.util.Map;

/**
 * 真实场景模拟 — 小型销售团队日常使用.
 *
 * <p>角色:
 * <ul>
 *   <li>alice: 团队主管，建表、共享、月底锁表</li>
 *   <li>bob: 销售，录入客户信息和订单</li>
 *   <li>charlie: 财务，只能查订单金额</li>
 * </ul>
 */
public class RealWorldDemo {

    static int pass = 0, fail = 0;

    public static void main(String[] args) {
        DatasheetEngine engine = DatasheetEngine.fromEnv("DATA");

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║     code-datasheet 真实场景模拟                   ║");
        System.out.println("║     小型销售团队 — 从建表到月底结账               ║");
        System.out.println("╚══════════════════════════════════════════════════╝\n");

        String alice = "alice_real";
        String bob   = "bob_real";
        String charlie = "charlie_real";
        String custId, orderId, productId;

        // 清理
        for (String u : List.of(alice, bob, charlie))
            for (TableInfo old : engine.tables().list(u))
                try { engine.tables().drop(u, old.getId()); } catch (Exception ignored) {}

        // ═══════════════════════════════════════════════
        // 第一天: 主管建表
        // ═══════════════════════════════════════════════
        pause();
        section("第一天：主管 Alice 搭建数据表");

        step("1. Alice 创建客户表 [name, phone, city, level(INT)]");
        TableInfo ci = engine.tables().create(alice, "customers", "客户表", "公司客户信息", List.of(
                new ColumnInfo("name",  ColumnType.STRING, true),
                new ColumnInfo("phone",  ColumnType.STRING, false),
                new ColumnInfo("city",  ColumnType.STRING, false),
                new ColumnInfo("level", ColumnType.INT, false)
        ));
        custId = ci.getId();
        check(ci != null, "建客户表");
        System.out.println("    表: alice_real_customers, 4 列");

        step("2. Alice 创建产品表 [name, price(DECIMAL), stock(INT)]");
        TableInfo pi = engine.tables().create(alice, "products", "产品表", "在售产品", List.of(
                new ColumnInfo("name",  ColumnType.STRING, true),
                new ColumnInfo("price", ColumnType.DECIMAL, false),
                new ColumnInfo("stock", ColumnType.INT, false)
        ));
        productId = pi.getId();
        check(pi != null, "建产品表");

        step("3. Alice 创建订单表 [customer_id, product_id, qty, amount, order_date]");
        TableInfo oi = engine.tables().create(alice, "orders", "订单表", "销售订单", List.of(
                new ColumnInfo("customer_id", ColumnType.INT, true),
                new ColumnInfo("product_id",  ColumnType.INT, true),
                new ColumnInfo("qty",         ColumnType.INT, false),
                new ColumnInfo("amount",      ColumnType.DECIMAL, false),
                new ColumnInfo("order_date", ColumnType.DATE, false)
        ));
        orderId = oi.getId();
        check(oi != null, "建订单表");

        // ═══════════════════════════════════════════════
        // 第二天: 录入基础数据
        // ═══════════════════════════════════════════════
        pause();
        section("第二天：录入基础数据");

        step("4. 录入 4 个客户");
        engine.data().sql(alice,
                "INSERT INTO customers (name,phone,city,level) VALUES ('张三','1390001','北京',3)");
        engine.data().sql(alice,
                "INSERT INTO customers (name,phone,city,level) VALUES ('李四','1390002','上海',5)");
        engine.data().sql(alice,
                "INSERT INTO customers (name,phone,city,level) VALUES ('王五','1390003','广州',2)");
        engine.data().sql(alice,
                "INSERT INTO customers (name,phone,city,level) VALUES ('赵六','1390004','深圳',4)");
        List<Map<String, Object>> r4 = engine.data().sql(alice, "SELECT * FROM customers");
        check(r4.size() == 4, "客户数: " + r4.size());
        printRows(r4);

        step("5. 录入 3 个产品");
        engine.data().sql(alice,
                "INSERT INTO products (name,price,stock) VALUES ('笔记本',5999,50)");
        engine.data().sql(alice,
                "INSERT INTO products (name,price,stock) VALUES ('显示器',1999,30)");
        engine.data().sql(alice,
                "INSERT INTO products (name,price,stock) VALUES ('键盘',299,100)");
        List<Map<String, Object>> r5 = engine.data().sql(alice, "SELECT * FROM products");
        check(r5.size() == 3, "产品数");
        printRows(r5);

        step("6. 录入 3 条订单");
        engine.data().sql(alice,
                "INSERT INTO orders (customer_id,product_id,qty,amount,order_date) VALUES (1,1,2,11998,'2026-07-01')");
        engine.data().sql(alice,
                "INSERT INTO orders (customer_id,product_id,qty,amount,order_date) VALUES (2,2,1,1999,'2026-07-01')");
        engine.data().sql(alice,
                "INSERT INTO orders (customer_id,product_id,qty,amount,order_date) VALUES (3,3,5,1495,'2026-07-02')");
        List<Map<String, Object>> r6 = engine.data().sql(alice, "SELECT * FROM orders");
        check(r6.size() == 3, "订单数");
        printRows(r6);

        // ═══════════════════════════════════════════════
        // 第三天: 共享给团队
        // ═══════════════════════════════════════════════
        pause();
        section("第三天：团队协作");

        step("7. Alice 共享客户表给 Bob（全部权限），Bob 可以录入新客户");
        engine.shares().share(alice, custId, bob, SharePermission.ALL);

        step("8. Alice 共享订单表给 Bob（全部权限）");
        engine.shares().share(alice, orderId, bob, SharePermission.ALL);

        step("9. Alice 共享产品表给 Bob（只读），Bob 不能改价格");
        engine.shares().share(alice, productId, bob, SharePermission.SELECT_ONLY);

        step("10. Alice 共享订单表给 Charlie（只读），Charlie 只能看金额");
        engine.shares().share(alice, orderId, charlie, SharePermission.SELECT_ONLY);

        step("11. Bob 查看自己能访问的表");
        var bobTables = engine.tables().listWithSource(bob);
        System.out.println("    Bob 的表:");
        for (var tws : bobTables)
            System.out.println("      " + tws.getSource() + " " + tws.getTableInfo().getName()
                    + (tws.getFromUser() != null ? " from " + tws.getFromUser() : ""));

        step("12. Bob 录入新客户");
        engine.data().sql(bob, "INSERT INTO customers (name,phone,city,level) VALUES ('孙七','1390005','北京',1)");
        List<Map<String, Object>> r12 = engine.data().sql(bob,
                "SELECT * FROM customers WHERE name='孙七'");
        check(r12.size() == 1, "Bob 录入客户");

        step("13. Bob 尝试改产品价格 — 只读拒绝");
        try {
            engine.data().sql(bob, "UPDATE products SET price=999 WHERE name='键盘'");
            check(false, "只读不应允许 UPDATE");
        } catch (Exception e) {
            check(true, "拒绝");
        }

        // ═══════════════════════════════════════════════
        // 日常运营: 查询统计
        // ═══════════════════════════════════════════════
        pause();
        section("日常运营：数据查询与分析");

        step("14. Bob 查询今天订单总额");
        List<Map<String, Object>> r14 = engine.data().sql(bob,
                "SELECT SUM(amount) AS total FROM orders WHERE order_date='2026-07-01'");
        System.out.println("    7/1 订单总额: " + r14.get(0).get("total"));
        check(r14.size() == 1, "查询失败");

        step("15. Alice 查看各城市客户数");
        List<Map<String, Object>> r15 = engine.data().sql(alice,
                "SELECT city, COUNT(*) AS cnt FROM customers GROUP BY city ORDER BY cnt DESC");
        printRows(r15);
        check(r15.size() >= 2, "聚合查询失败");

        step("15b. Alice 三表 JOIN 查订单详情（客户名 + 产品名 + 金额）");
        List<Map<String, Object>> r15b = engine.data().sql(alice,
                "SELECT c.name AS customer, p.name AS product, o.amount, o.order_date "
                + "FROM orders o, customers c, products p "
                + "WHERE o.customer_id=c.id AND o.product_id=p.id ORDER BY o.order_date");
        printRows(r15b);
        check(r15b.size() >= 3, "三表 JOIN 失败");

        step("16. Charlie 查账: 每日订单统计");
        List<Map<String, Object>> r16 = engine.data().sql(charlie,
                "SELECT order_date, COUNT(*) AS cnt, SUM(amount) AS total FROM orders GROUP BY order_date");
        printRows(r16);
        check(r16.size() >= 2, "聚合查询失败");

        step("17. Charlie 尝试跨表查客户名 — customers 未共享被拦截");
        try {
            engine.data().sql(charlie, "SELECT o.id, c.name FROM orders o, customers c WHERE o.customer_id=c.id");
            check(false, "跨表应被拒绝");
        } catch (Exception e) {
            check(true, "拦截");
        }

        // ═══════════════════════════════════════════════
        // 月底: 锁表 + 导出报表
        // ═══════════════════════════════════════════════
        pause();
        section("月底：锁表 + 导出报表");

        step("18. Alice 设订单表为只读（月底封账）");
        engine.tables().setMode(alice, orderId, TableMode.READ_ONLY);

        step("19. Bob 尝试追加订单 — 只读拒绝");
        try {
            engine.data().sql(bob,
                    "INSERT INTO orders (customer_id,product_id,qty,amount,order_date) VALUES (4,2,1,1999,'2026-07-03')");
            check(false, "只读表不应允许 INSERT");
        } catch (Exception e) {
            check(true, "拒绝");
        }

        step("20. 导出订单为 CSV");
        String csv = engine.data().exportCsv(alice, orderId, null, null);
        check(csv.contains("amount") && csv.split("\n").length >= 2, "CSV 导出");
        System.out.println("    CSV 首行: " + csv.split("\n")[0]);

        step("21. 导出客户为 JSON");
        String json = engine.data().exportJson(alice, custId, null, null);
        check(json.contains("name"), "JSON 导出");
        System.out.println("    JSON 预览: " + json.substring(0, Math.min(80, json.length())) + "...");

        // ═══════════════════════════════════════════════
        // 次月: 恢复、清理
        // ═══════════════════════════════════════════════
        pause();
        section("次月：恢复操作 + 清理");

        step("22. Alice 恢复订单表");
        engine.tables().setMode(alice, orderId, TableMode.ALL);

        step("23. Bob 追加新订单");
        engine.data().sql(bob,
                "INSERT INTO orders (customer_id,product_id,qty,amount,order_date) VALUES (4,2,1,1999,'2026-07-03')");
        List<Map<String, Object>> r23 = engine.data().sql(bob,
                "SELECT COUNT(*) AS cnt FROM orders");
        System.out.println("    当前订单总数: " + r23.get(0).get("cnt"));

        step("24. Alice 删除不再需要的产品表");
        engine.tables().drop(alice, productId);
        check(engine.tables().get(productId) == null, "删除失败");
        System.out.println("    产品表已删除");

        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║  模拟完成                                          ║");
        System.out.println("║  覆盖: 建表→录入→共享→JOIN→锁表→导出→恢复        ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println("\n  总计: " + (pass + fail) + " | PASS: " + pass + " | FAIL: " + fail);
        if (fail > 0) System.exit(1);
    }

    static void section(String title) {
        System.out.println("\n  ┌─ " + title);
    }

    static void step(String msg) {
        System.out.print("  │ " + msg + " ... ");
    }

    static void check(boolean ok, String detail) {
        if (ok) { System.out.println("OK"); pass++; }
        else    { System.out.println("FAIL — " + detail); fail++; }
    }

    static void printRows(List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows)
            System.out.println("      " + row);
    }

    static void pause() {
        System.out.print("\n      按回车继续...");
        try { System.in.read(); } catch (Exception ignored) {}
        System.out.println();
    }
}
