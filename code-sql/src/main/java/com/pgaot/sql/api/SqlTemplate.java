package com.pgaot.sql.api;

import com.alibaba.druid.pool.DruidDataSource;
import com.pgaot.sql.common.constants.Messages;
import com.pgaot.sql.core.executor.RawExecutor;
import com.pgaot.sql.core.executor.TemplateExecutor;
import com.pgaot.sql.exception.SqlException;
import com.pgaot.sql.support.PageQuery;
import com.pgaot.sql.support.PageResponse;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * 通用 SQL 执行器.
 *
 * <p>不修改传入的 DataSource — WallFilter 应在创建 DataSource 时通过
 * {@link com.pgaot.sql.common.config.EnvConfig#createDataSource(String,
 * com.alibaba.druid.wall.WallConfig, int)} 附加。
 *
 * <pre>{@code
 * SqlTemplate db = new SqlTemplate(SqlTemplateConfig.fromEnv());
 * List<Map<String, Object>> rows = db.sql("SELECT * FROM t_user WHERE id = ?", 1);
 * }</pre>
 */
public class SqlTemplate implements AutoCloseable {

    private final DruidDataSource dataSource;
    private final TemplateExecutor template;
    private final RawExecutor raw;

    public SqlTemplate(DataSource dataSource) {
        this.dataSource = toDruid(dataSource);
        this.template = new TemplateExecutor(this.dataSource);
        this.raw = new RawExecutor(this.dataSource);
    }

    public SqlTemplate(SqlTemplateConfig config) {
        this(config.getDataSource());
    }

    /** 关闭连接池 */
    @Override
    public void close() { dataSource.close(); }

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
