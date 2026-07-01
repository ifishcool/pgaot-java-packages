package com.pgaot.sql.api;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.wall.WallFilter;
import com.pgaot.sql.common.constants.Messages;
import com.pgaot.sql.core.executor.RawExecutor;
import com.pgaot.sql.core.executor.TemplateExecutor;
import com.pgaot.sql.exception.SqlException;
import com.pgaot.sql.support.PageQuery;
import com.pgaot.sql.support.PageResponse;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 通用 SQL 执行器
 *
 * <pre>{@code
 * SqlTemplate db = new SqlTemplate(SqlTemplateConfig.fromEnv());
 *
 * // 查询 → List<Map>
 * List<Map<String, Object>> rows = db.sql("SELECT * FROM t_user WHERE id = ?", 1);
 *
 * // 增删改 → 影响行数 int
 * int n = db.sql("UPDATE t_user SET name = ? WHERE id = ?", "张三", 1);
 *
 * // 绕过安全
 * db.unsafe("TRUNCATE t_log");
 *
 * // 批量
 * db.batch("INSERT INTO t_log (msg) VALUES (?)", batch);
 * }</pre>
 */
public class SqlTemplate {

    private final TemplateExecutor template;
    private final RawExecutor raw;

    /** 默认配置（WallFilter 全开放） */
    public SqlTemplate(DataSource dataSource) {
        this(new SqlTemplateConfig(dataSource));
    }

    /** 自定义安全配置 */
    public SqlTemplate(SqlTemplateConfig config) {
        DruidDataSource druid = toDruid(config.getDataSource());
        WallFilter wall = new WallFilter();
        wall.setConfig(config.getWallConfig());

        List<com.alibaba.druid.filter.Filter> filters = new ArrayList<>(druid.getProxyFilters());
        filters.add(wall);
        druid.setProxyFilters(filters);

        this.template = new TemplateExecutor(druid);
        this.raw = new RawExecutor(druid);
    }

    /**
     * SQL 执行
     *
     * @param sql    SQL 语句，用 ? 占位符防止注入
     * @param params 参数值，按顺序匹配 ?
     * @return SELECT → List&lt;Map&gt; / INSERT/UPDATE/DELETE → int
     * @throws SqlException SQL 被防火墙拦截或执行失败
     */
    @SuppressWarnings("unchecked")
    public <T> T sql(String sql, Object... params) {
        requireNonBlank(sql);
        try {
            return (T) template.execute(sql, params);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains(Messages.WALL_KEYWORD)) {
                throw SqlException.wallBlocked(e.getMessage());
            }
            throw SqlException.executionFailed(e.getMessage());
        }
    }

    /** 绕过安全检查 — 管理员专用 */
    @SuppressWarnings("unchecked")
    public <T> T unsafe(String sql, Object... params) {
        requireNonBlank(sql);
        try {
            return (T) template.unsafe(sql, params);
        } catch (Exception e) {
            throw SqlException.executionFailed(e.getMessage());
        }
    }

    /** 批量操作 */
    public void batch(String sql, List<Object[]> batch) {
        requireNonBlank(sql);
        if (batch == null || batch.isEmpty()) {
            throw SqlException.executionFailed(Messages.BATCH_EMPTY);
        }
        try {
            template.batch(sql, batch);
        } catch (Exception e) {
            throw SqlException.executionFailed(e.getMessage());
        }
    }

    /**
     * 分页查询
     *
     * @param sql SELECT 语句（不含 LIMIT）
     * @param pq  分页参数（页码从 1 开始）
     * @param params SQL 参数
     * @return 分页结果
     */
    public PageResponse<Map<String, Object>> page(String sql, PageQuery pq, Object... params) {
        requireNonBlank(sql);
        try {
            return template.page(sql, pq, params);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains(Messages.WALL_KEYWORD)) {
                throw SqlException.wallBlocked(e.getMessage());
            }
            throw SqlException.executionFailed(e.getMessage());
        }
    }

    /** 获取 Raw JDBC 执行器 */
    public RawExecutor raw() { return raw; }

    private DruidDataSource toDruid(DataSource ds) {
        if (ds instanceof DruidDataSource d) return d;
        throw SqlException.connectionFailed(Messages.DATASOURCE_NOT_DRUID);
    }

    private static void requireNonBlank(String sql) {
        if (sql == null || sql.isBlank()) {
            throw SqlException.executionFailed(Messages.SQL_BLANK);
        }
    }
}
