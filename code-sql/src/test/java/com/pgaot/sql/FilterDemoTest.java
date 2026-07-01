package com.pgaot.sql;

import com.pgaot.sql.api.SqlTemplate;
import com.pgaot.sql.api.SqlTemplateConfig;

import java.util.List;
import java.util.Map;

public class FilterDemoTest {

    record Test(String label, String sql, boolean block, String note) {}

    public static void main(String[] args) {
        SqlTemplate safe = new SqlTemplate(SqlTemplateConfig.fromEnv().readWrite());

        Test[] tests = {
            // ===== 复杂查询 =====
            new Test("多表 JOIN", "SELECT u.name, w.qty, w.salary FROM t_user u LEFT JOIN t_work_record w ON u.id = w.worker_id WHERE w.salary > 100 ORDER BY w.qty DESC", false, "✅"),
            new Test("GROUP BY", "SELECT worker_id, COUNT(*) cnt, SUM(salary) total, AVG(salary) avg_pay FROM t_work_record GROUP BY worker_id HAVING cnt >= 2 ORDER BY total DESC", false, "✅"),
            new Test("嵌套子查询", "SELECT * FROM t_user WHERE id IN (SELECT worker_id FROM t_work_record WHERE salary > (SELECT AVG(salary) FROM t_work_record))", false, "✅"),
            new Test("CASE WHEN", "SELECT name, CASE WHEN age < 25 THEN '青年' WHEN age < 35 THEN '中年' ELSE '其他' END AS age_group FROM t_user", false, "✅"),
            new Test("UNION ALL", "SELECT name, age FROM t_user WHERE age > 25 UNION ALL SELECT name, age FROM t_user WHERE age < 25", false, "✅"),
            new Test("EXISTS", "SELECT * FROM t_user u WHERE EXISTS (SELECT 1 FROM t_work_record w WHERE w.worker_id = u.id AND w.salary > 200)", false, "✅"),
            new Test("日期函数", "SELECT * FROM t_user WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)", false, "✅"),
            new Test("字符串函数", "SELECT CONCAT(name, '-', age) AS info, LENGTH(name) len FROM t_user WHERE name IS NOT NULL", false, "✅"),
            new Test("多条件组合", "SELECT * FROM t_user WHERE (age BETWEEN 20 AND 30 AND name LIKE '%张%') OR id IN (SELECT worker_id FROM t_work_record WHERE salary > 150)", false, "✅"),
            new Test("NULL判断", "SELECT * FROM t_user WHERE email IS NULL OR email = '' ORDER BY COALESCE(age, 0)", false, "✅"),
            new Test("自连接", "SELECT a.name AS worker, b.name AS teammate FROM t_user a JOIN t_user b ON a.age = b.age AND a.id != b.id", false, "✅"),
            new Test("数学运算", "SELECT name, age, salary, ROUND(salary * 1.1, 2) AS bonus FROM t_user u JOIN t_work_record w ON u.id = w.worker_id", false, "✅"),

            // ===== 增删改操作 =====
            new Test("INSERT 单行", "INSERT INTO t_user (name, age) VALUES ('test_insert', 18)", false, "✅"),
            new Test("INSERT 多行", "INSERT INTO t_user (name, age) VALUES ('A',20),('B',25),('C',30)", false, "✅"),
            new Test("INSERT SET", "INSERT INTO t_user SET name='test_set', age=22", false, "✅"),
            new Test("INSERT SELECT", "INSERT INTO t_user (name) SELECT name FROM t_user WHERE id=1", false, "✅"),
            new Test("UPDATE 单行", "UPDATE t_user SET age = 99 WHERE id = 1", false, "✅"),
            new Test("UPDATE 多条件", "UPDATE t_user SET age=age+1 WHERE age<25 AND email IS NULL", false, "✅"),
            new Test("UPDATE JOIN", "UPDATE t_user u JOIN t_work_record w ON u.id=w.worker_id SET u.age=99 WHERE w.salary>300", false, "✅"),
            new Test("DELETE 单行", "DELETE FROM t_user WHERE id = 99999", true, "禁止"),
            new Test("DELETE LIMIT", "DELETE FROM t_user WHERE name LIKE .test%. LIMIT 2", true, "禁止"),

            // ===== DDL 攻击 =====
            new Test("DROP TABLE", "DROP TABLE t_user", true, "高危"),
            new Test("DROP CASCADE", "DROP TABLE t_user CASCADE", true, "高危"),
            new Test("DROP IF EXISTS", "DROP TABLE IF EXISTS t_user", true, "高危"),
            new Test("DROP DATABASE", "DROP DATABASE javatest", true, "高危"),
            new Test("TRUNCATE", "TRUNCATE TABLE t_user", true, "高危"),
            new Test("ALTER ADD", "ALTER TABLE t_user ADD COLUMN backdoor VARCHAR(100)", true, "高危"),
            new Test("ALTER DROP", "ALTER TABLE t_user DROP COLUMN name", true, "高危"),
            new Test("ALTER MODIFY", "ALTER TABLE t_user MODIFY name TEXT", true, "高危"),
            new Test("ALTER RENAME COL", "ALTER TABLE t_user RENAME COLUMN name TO newname", true, "高危"),
            new Test("RENAME TABLE", "RENAME TABLE t_user TO t_hacked", true, "高危"),
            new Test("CREATE TABLE 1", "CREATE TABLE t_backdoor (id INT)", true, "高危"),
            new Test("CREATE TABLE AS", "CREATE TABLE t_backdoor AS SELECT * FROM t_user", true, "高危"),
            new Test("CREATE TEMP TABLE", "CREATE TEMPORARY TABLE t_tmp (id INT)", true, "高危"),
            new Test("DROP VIEW", "DROP VIEW IF EXISTS v_user", true, "高危"),
            new Test("REVOKE", "REVOKE ALL ON t_user FROM root", true, "高危"),
            new Test("GRANT", "GRANT ALL ON t_user TO root", true, "高危"),
            new Test("FLUSH", "FLUSH TABLES", true, "高危"),
            new Test("KILL", "KILL CONNECTION 1", true, "高危"),

            // ===== 绕过手法 =====
            new Test("注释绕过", "DR/**/OP TABLE t_user", true, "绕过"),
            new Test("版本注释绕过", "/*!50000 DROP*/ TABLE t_user", true, "绕过"),
            new Test("大小写绕过", "dRoP tAbLe t_user", true, "绕过"),
            new Test("多空格绕过", "DROP     TABLE      t_user", true, "绕过"),
            new Test("反引号绕过", "DROP TABLE `t_user`", true, "绕过"),
            new Test("换行绕过", "DROP\nTABLE\nt_user", true, "绕过"),
            new Test("制表符绕过", "DROP\tTABLE\tt_user", true, "绕过"),
            new Test("回车绕过", "DROP\rTABLE\rt_user", true, "绕过"),
            new Test("括号绕过", "DROP TABLE (t_user)", true, "绕过"),
            new Test("注释混合绕过", "DR/**/OP /*comment*/ TABLE /*x*/ t_user", true, "绕过"),

            // ===== 注入攻击 =====
            new Test("多语句注入", "SELECT * FROM t_user; DROP TABLE t_user", true, "注入"),
            new Test("三语句", "SELECT 1; DROP TABLE t_user; DROP TABLE t_work_record", true, "注入"),
            new Test("UNION查询", "SELECT * FROM t_user WHERE id = 1 UNION SELECT * FROM mysql.user", true, "注入"),
            new Test("UNION子查询", "SELECT * FROM t_user WHERE id = 1 UNION SELECT user(),database(),version()", true, "注入"),
            new Test("UNION NULL", "SELECT * FROM t_user WHERE id = 1 UNION SELECT NULL,NULL,NULL,NULL", true, "注入"),
            new Test("万能密码1", "SELECT * FROM t_user WHERE name = '' OR '1'='1'", true, "注入"),
            new Test("万能密码2", "SELECT * FROM t_user WHERE name = '' OR 1=1 --", true, "注入"),
            new Test("子查询窃取", "INSERT INTO t_user (name) SELECT concat(user,':',password) FROM mysql.user", true, "注入"),
            new Test("EXTRACTVALUE", "SELECT extractvalue(1, concat(0x7e, database()))", true, "注入"),
            new Test("UPDATEXML", "SELECT updatexml(1, concat(0x7e, database()), 1)", true, "注入"),
            new Test("延时SLEEP", "SELECT * FROM t_user WHERE id = 1 AND sleep(5)", true, "注入"),
            new Test("延时BENCHMARK", "SELECT * FROM t_user WHERE id = 1 AND benchmark(1000000, md5('x'))", true, "注入"),
            new Test("写文件", "SELECT * FROM t_user INTO OUTFILE '/tmp/hack.txt'", true, "注入"),
            new Test("写DUMPFILE", "SELECT '<?php eval($_GET[1])?>' INTO DUMPFILE '/tmp/shell.php'", true, "注入"),
            new Test("堆叠注入", "SELECT 1; SET @a = 1; DROP TABLE t_user", true, "注入"),
            new Test("PREPARE注入", "SET @s = CONCAT('DROP TABLE ', 't_user'); PREPARE stmt FROM @s; EXECUTE stmt", true, "注入"),
            new Test("ORDER注入", "SELECT * FROM t_user ORDER BY (SELECT 1 FROM (SELECT sleep(2))x)", true, "注入"),
            new Test("LIMIT注入", "SELECT * FROM t_user LIMIT 1 OFFSET (SELECT 1 FROM (SELECT sleep(2))x)", true, "注入"),
            new Test("时间盲注", "SELECT * FROM t_user WHERE id = 1 AND IF(ascii(substring(database(),1,1))>64, sleep(2), 0)", true, "注入"),
            new Test("双查询注入", "SELECT * FROM t_user WHERE id = 1 AND (SELECT 1 FROM (SELECT count(*),concat(version(),floor(rand(0)*2))x FROM information_schema.tables GROUP BY x)a)", true, "注入"),
            new Test("XPATH注入", "SELECT * FROM t_user WHERE id = 1 AND updatexml(1,make_set(3, '~', version()), 1)", true, "注入"),
            new Test("INTO变量注入", "SELECT * FROM t_user WHERE id = 1 INTO @a,@b,@c,@d,@e", true, "注入"),
            new Test("子查询报错", "SELECT * FROM t_user WHERE id = 1 AND (SELECT * FROM (SELECT COUNT(*),CONCAT((SELECT(@@version)),0x3a,FLOOR(RAND(0)*2))x FROM information_schema.tables GROUP BY x)a)", true, "注入"),
            new Test("EXP报错", "SELECT * FROM t_user WHERE id = 1 AND EXP(~(SELECT * FROM (SELECT user())a))", true, "注入"),
            new Test("GEOMETRY注入", "SELECT * FROM t_user WHERE id = 1 AND geometrycollection((SELECT * FROM(SELECT * FROM(SELECT user())a)b))", true, "注入"),
            new Test("POLYGON注入", "SELECT * FROM t_user WHERE id = 1 AND polygon((SELECT * FROM(SELECT * FROM(SELECT user())a)b))", true, "注入"),
            new Test("多行注释闭合", "SELECT * FROM t_user WHERE id = 1 /*!UNION*/ /*!SELECT*/ 1,2,3", true, "注入"),
            new Test("ORDER REGEXP注入", "SELECT * FROM t_user WHERE 1=1 ORDER BY (SELECT 1 REGEXP IF(SUBSTRING(version(),1,1)=5, '.*', 'xxx'))", true, "注入"),

            // ===== 合规语法 =====
            new Test("合规:LOAD_FILE", "SELECT LOAD_FILE('/etc/passwd')", false, "⚠️合规"),
            new Test("合规:IF函数", "SELECT IF(age>20, 'yes', 'no') FROM t_user", false, "⚠️合规"),
            new Test("合规:十六进制", "SELECT * FROM t_user WHERE name = 0x61646d696e", false, "⚠️合规"),
            new Test("合规:宽字节", "SELECT * FROM t_user WHERE name = 'admin%df'", false, "⚠️合规"),
        };

        int pass = 0, block = 0, legit = 0, wrong = 0;
        for (Test t : tests) {
            System.out.printf("  [%s] %-25s → ", t.block ? "拦截" : "通过", t.label);
            try {
                Object result = safe.sql(t.sql);
                if (t.block) { System.out.println("❌ (" + t.note + ")"); wrong++; }
                else if (t.note.startsWith("⚠️")) { System.out.println(t.note); legit++; }
                else { System.out.println("✅ " + preview(result)); pass++; }
            } catch (Exception e) {
                if (t.block) { System.out.println("✅ (" + t.note + ")"); block++; }
                else { System.out.println("❌ 误拦"); wrong++; }
            }
        }

        System.out.println("\n========================================");
        System.out.println("  总计:" + tests.length + " | 通过:" + pass + " | 拦截:" + block + " | 合规:" + legit + " | 异常:" + wrong);
        System.out.println("========================================");

        // 清理测试数据
        new SqlTemplate(SqlTemplateConfig.fromEnv())
                .unsafe("DELETE FROM t_user WHERE name LIKE 'test%' OR name IN ('A','B','C','xx')");
    }

    static String preview(Object result) {
        if (result instanceof List<?> l) return l.size() + "行";
        return String.valueOf(result);
    }
}
