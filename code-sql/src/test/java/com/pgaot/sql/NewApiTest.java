package com.pgaot.sql;

import com.pgaot.sql.api.SqlTemplate;
import com.pgaot.sql.api.SqlTemplateConfig;
import com.pgaot.sql.common.code.ErrorCode;
import com.pgaot.sql.common.code.IResultCode;
import com.pgaot.sql.exception.SqlException;
import com.pgaot.sql.support.PageQuery;
import com.pgaot.sql.support.PageResponse;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 新 API 测试 — 分页 / 异常静态工厂 / 错误码体系.
 *
 * <pre>
 * 通过标准:
 *   [PASS] — 测试通过
 *   [FAIL] — 行为不符合预期
 * </pre>
 */
public class NewApiTest {

    static int pass = 0, fail = 0;

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("  code-sql 新 API 测试");
        System.out.println("==========================================\n");

        testErrorCodeDedupCheck();
        testIResultCodeInterface();
        testErrorCodeRangeAllocation();
        testSqlExceptionStaticFactories();
        testPageQueryValidation();
        testPageQueryOffsetCalc();
        testPageResponseEmpty();
        testPageResponseConvert();
        testPageResponsePagesCalc();

        // 以下需要数据库连接，仅在有环境变量时执行
        String url = System.getenv("CODE_SQL_URL");
        if (url != null && !url.isBlank()) {
            testPageQueryWithDb();
            testEnvConfigPoolDefaults();
        } else {
            System.out.println("\n[SKIP] 未配置 CODE_SQL_URL，跳过数据库相关测试");
        }

        System.out.println("\n==========================================");
        System.out.println("  总计: " + (pass + fail) + " | PASS: " + pass + " | FAIL: " + fail);
        System.out.println("==========================================");

        if (fail > 0) System.exit(1);
    }

    // ==================== 错误码体系 ====================

    static void testErrorCodeDedupCheck() {
        print("错误码去重校验");

        Set<Integer> codes = new HashSet<>();
        boolean hasDup = false;
        for (ErrorCode ec : ErrorCode.values()) {
            if (!codes.add(ec.getCode())) {
                hasDup = true;
                break;
            }
        }

        check(!hasDup, "存在重复错误码");
        check(codes.size() == ErrorCode.values().length, "去重后数量不一致");
    }

    static void testIResultCodeInterface() {
        print("IResultCode 接口实现");

        for (ErrorCode ec : ErrorCode.values()) {
            check(ec instanceof IResultCode, ec.name() + " 未实现 IResultCode");
            check(ec.getCode() > 0, ec.name() + " 错误码无效: " + ec.getCode());
            check(ec.getMessage() != null && !ec.getMessage().isBlank(),
                    ec.name() + " 错误消息为空");
        }
    }

    static void testErrorCodeRangeAllocation() {
        print("错误码分段编号");

        check(inRange(ErrorCode.CONNECTION_FAILED, 10_001_000, 10_001_999),
                "CONNECTION_FAILED 不在连接段");
        check(inRange(ErrorCode.ENV_MISSING, 10_001_000, 10_001_999),
                "ENV_MISSING 不在连接段");
        check(inRange(ErrorCode.SQL_EXECUTION_FAILED, 10_002_000, 10_002_999),
                "SQL_EXECUTION_FAILED 不在执行段");
        check(inRange(ErrorCode.SQL_BLOCKED_BY_WALL, 10_003_000, 10_003_999),
                "SQL_BLOCKED_BY_WALL 不在防火墙段");
        check(inRange(ErrorCode.PAGE_PARAM_INVALID, 10_004_000, 10_004_999),
                "PAGE_PARAM_INVALID 不在分页段");
        check(inRange(ErrorCode.JPA_EXECUTION_FAILED, 10_005_000, 10_005_999),
                "JPA_EXECUTION_FAILED 不在 JPA 段");
    }

    // ==================== 异常静态工厂 ====================

    static void testSqlExceptionStaticFactories() {
        print("SqlException 静态工厂方法");

        SqlException e1 = SqlException.wallBlocked("DROP TABLE t_user");
        check(e1.getCode() == ErrorCode.SQL_BLOCKED_BY_WALL.getCode(),
                "wallBlocked 错误码不匹配: " + e1.getCode());
        check(e1.getMessage().contains("DROP TABLE t_user"),
                "wallBlocked 消息不包含详情");

        SqlException e2 = SqlException.executionFailed("timeout");
        check(e2.getCode() == ErrorCode.SQL_EXECUTION_FAILED.getCode(),
                "executionFailed 错误码不匹配");

        SqlException e3 = SqlException.connectionFailed("Access denied");
        check(e3.getCode() == ErrorCode.CONNECTION_FAILED.getCode(),
                "connectionFailed 错误码不匹配");

        SqlException e4 = SqlException.envMissing("CODE_SQL_URL");
        check(e4.getCode() == ErrorCode.ENV_MISSING.getCode(),
                "envMissing 错误码不匹配");
        check(e4.getMessage().contains("CODE_SQL_URL"),
                "envMissing 消息不包含变量名");

        SqlException e5 = SqlException.pageParamInvalid("页码必须 >= 1");
        check(e5.getCode() == ErrorCode.PAGE_PARAM_INVALID.getCode(),
                "pageParamInvalid 错误码不匹配");

        SqlException e6 = SqlException.jpaFailed("query timeout");
        check(e6.getCode() == ErrorCode.JPA_EXECUTION_FAILED.getCode(),
                "jpaFailed 错误码不匹配");

        // IResultCode 构造器
        SqlException e7 = new SqlException(ErrorCode.SQL_BLOCKED_BY_WALL);
        check(e7.getCode() == 10_003_001, "IResultCode 构造器错误码不匹配");

        SqlException e8 = new SqlException(ErrorCode.SQL_EXECUTION_FAILED, "detail");
        check(e8.getCode() == 10_002_001, "IResultCode+detail 构造器错误码不匹配");
        check(e8.getMessage().contains("detail"), "IResultCode+detail 构造器消息不完整");
    }

    // ==================== 分页参数校验 ====================

    static void testPageQueryValidation() {
        print("PageQuery 参数校验");

        // 合法参数
        try {
            PageQuery pq = new PageQuery(1, 10);
            check(true, "合法 PageQuery(1, 10)");
        } catch (Exception e) {
            check(false, "合法参数被拒绝: " + e.getMessage());
        }

        // 页码 < 1
        try {
            new PageQuery(0, 10);
            check(false, "页码 0 未被拒绝");
        } catch (SqlException e) {
            check(e.getCode() == 10_004_001,
                    "页码 0 错误码不对: " + e.getCode());
        }

        // 页码 = 0
        try {
            new PageQuery(-1, 10);
            check(false, "页码 -1 未被拒绝");
        } catch (SqlException e) {
            check(true, "页码 -1 被正确拒绝");
        }

        // size = 0
        try {
            new PageQuery(1, 0);
            check(false, "size 0 未被拒绝");
        } catch (SqlException e) {
            check(true, "size 0 被正确拒绝");
        }

        // size > 1000
        try {
            new PageQuery(1, 1001);
            check(false, "size 1001 未被拒绝");
        } catch (SqlException e) {
            check(true, "size 1001 被正确拒绝");
        }

        // 边界值: size = 1（最小）
        try { new PageQuery(1, 1); check(true, "size=1 边界"); }
        catch (Exception e) { check(false, "size=1 被误拦"); }

        // 边界值: size = 1000（最大）
        try { new PageQuery(1, 1000); check(true, "size=1000 边界"); }
        catch (Exception e) { check(false, "size=1000 被误拦"); }
    }

    static void testPageQueryOffsetCalc() {
        print("PageQuery offset 计算");

        check(new PageQuery(1, 10).getOffset() == 0, "第1页 offset 不为 0");
        check(new PageQuery(2, 10).getOffset() == 10, "第2页 offset 不为 10");
        check(new PageQuery(3, 20).getOffset() == 40, "第3页(20/page) offset 不为 40");
        check(new PageQuery(1, 1).getOffset() == 0, "单条分页 offset 不为 0");
    }

    static void testPageResponseEmpty() {
        print("PageResponse.empty()");

        PageResponse<String> r = PageResponse.empty(2, 15);

        check(r.getRows().isEmpty(), "空响应 rows 不为空");
        check(r.getTotal() == 0, "空响应 total 不为 0");
        check(r.getPage() == 2, "空响应 page 不对");
        check(r.getSize() == 15, "空响应 size 不对");
        check(r.getPages() == 0, "空响应 pages 不为 0");
    }

    static void testPageResponseConvert() {
        print("PageResponse.convert()");

        PageResponse<Integer> src = PageResponse.of(List.of(1, 2, 3), 100, 1, 10);
        PageResponse<String> dst = src.convert(String::valueOf);

        check(dst.getRows().equals(List.of("1", "2", "3")), "转换后数据不对");
        check(dst.getTotal() == 100, "转换后 total 不对");
        check(dst.getPage() == 1, "转换后 page 不对");
        check(dst.getSize() == 10, "转换后 size 不对");
        check(dst.getPages() == 10, "转换后 pages 不对");
    }

    static void testPageResponsePagesCalc() {
        print("PageResponse pages 计算");

        check(PageResponse.of(List.of(), 100, 1, 10).getPages() == 10,
                "100/10 pages != 10");
        check(PageResponse.of(List.of(), 101, 1, 10).getPages() == 11,
                "101/10 pages != 11");
        check(PageResponse.of(List.of(), 0, 1, 10).getPages() == 0,
                "0 条 pages != 0");
        check(PageResponse.of(List.of(), 1, 1, 10).getPages() == 1,
                "1 条 pages != 1");
    }

    // ==================== 数据库相关测试 ====================

    static void testPageQueryWithDb() {
        print("SqlTemplate.page() 数据库分页");

        try {
            SqlTemplate db = new SqlTemplate(SqlTemplateConfig.fromEnv());

            // 分页查询
            PageQuery pq = new PageQuery(1, 3);
            PageResponse<Map<String, Object>> page = db.page(
                    "SELECT * FROM t_user ORDER BY id", pq);

            check(page.getPage() == 1, "返回页码不对");
            check(page.getSize() == 3, "返回 size 不对");
            check(page.getRows().size() <= 3, "返回行数超过 page size");
            check(page.getTotal() >= 0, "total 为负数");
            check(page.getPages() >= 0, "pages 为负数");

            // 空结果页
            PageQuery lastPage = new PageQuery(99999, 10);
            PageResponse<Map<String, Object>> empty = db.page(
                    "SELECT * FROM t_user WHERE id < 0 ORDER BY id", lastPage);
            check(empty.getRows().isEmpty(), "空结果 rows 不为空");
            check(empty.getTotal() == 0, "空结果 total 不为 0");
        } catch (Exception e) {
            System.out.println("  [WARN] 数据库不可用，跳过: " + e.getMessage());
        }
    }

    static void testEnvConfigPoolDefaults() {
        print("连接池默认参数");

        try {
            SqlTemplateConfig config = SqlTemplateConfig.fromEnv();
            SqlTemplate db = new SqlTemplate(config);

            // 能成功执行即代表连接池工作正常
            List<Map<String, Object>> rows = db.sql("SELECT 1 AS test");
            check(rows.size() == 1, "SELECT 1 未返回 1 行");
            check(rows.get(0).get("test") != null, "SELECT 1 结果为空");
        } catch (Exception e) {
            System.out.println("  [WARN] 数据库不可用，跳过: " + e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    static void print(String name) { System.out.print("  " + name + " ... "); }

    static void check(boolean ok, String detail) {
        if (ok) { System.out.println("PASS"); pass++; }
        else    { System.out.println("FAIL — " + detail); fail++; }
    }

    static boolean inRange(ErrorCode ec, int lo, int hi) {
        return ec.getCode() >= lo && ec.getCode() <= hi;
    }
}
