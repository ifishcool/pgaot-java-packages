package com.pgaot.sql;

import com.pgaot.sql.api.SqlTemplate;
import com.pgaot.sql.api.SqlTemplateConfig;

import java.util.List;
import java.util.Map;

public class ComplexSqlTest {

    public static void main(String[] args) {
        SqlTemplate db = new SqlTemplate(SqlTemplateConfig.fromEnv());

        System.out.println("=== JOIN 查询 ===");
        q(db, "SELECT u.name, w.qty, w.salary FROM t_user u " +
                "JOIN t_work_record w ON u.id = w.worker_id LIMIT 5");
        pause();

        System.out.println("=== 聚合统计 ===");
        q(db, "SELECT worker_id, COUNT(*) AS cnt, SUM(salary) AS total " +
                "FROM t_work_record GROUP BY worker_id HAVING cnt > 2 LIMIT 5");
        pause();

        System.out.println("=== 子查询 ===");
        q(db, "SELECT * FROM t_user WHERE id IN (" +
                "SELECT worker_id FROM t_work_record WHERE salary > 100) LIMIT 5");
        pause();

        System.out.println("=== 排序分页 ===");
        q(db, "SELECT * FROM t_user ORDER BY id DESC LIMIT 3 OFFSET 0");
        pause();

        System.out.println("=== 条件更新 ===");
        int n = db.sql("UPDATE t_user SET age = age + 1 WHERE id = 1");
        System.out.println("更新行数: " + n);
        q(db, "SELECT id, name, age FROM t_user WHERE id = 1");

        System.out.println("\n完成");
    }

    @SuppressWarnings("unchecked")
    static void q(SqlTemplate db, String sql) {
        ((List<Map<String, Object>>) db.sql(sql)).forEach(r -> System.out.println("  " + r));
    }

    static void pause() {
        System.out.print("按回车...");
        try { System.in.read(); } catch (Exception ignored) {}
        System.out.println();
    }
}
