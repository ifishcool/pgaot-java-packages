package com.pgaot.sql.core.executor;

import com.alibaba.druid.pool.DruidDataSource;
import com.pgaot.sql.support.PageQuery;
import com.pgaot.sql.support.PageResponse;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/** JdbcTemplate 执行器 */
public class TemplateExecutor {

    private final JdbcTemplate jdbc;

    public TemplateExecutor(DruidDataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public Object execute(String sql, Object... params) {
        String s = sql.trim().toUpperCase();
        if (s.startsWith("SELECT")) return query(sql, params);
        if (s.startsWith("INSERT") || s.startsWith("UPDATE") || s.startsWith("DELETE"))
            return update(sql, params);
        jdbc.execute(sql);
        return null;
    }

    public Object unsafe(String sql, Object... params) {
        return execute(sql, params);
    }

    public void batch(String sql, List<Object[]> batch) {
        jdbc.batchUpdate(sql, batch);
    }

    /** 分页查询 — 自动拼 COUNT + LIMIT */
    public PageResponse<Map<String, Object>> page(String sql, PageQuery pq, Object... params) {
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") _t";
        long total;
        if (params.length > 0) {
            total = jdbc.queryForObject(countSql, Long.class, params);
        } else {
            total = jdbc.queryForObject(countSql, Long.class);
        }

        String pageSql = sql + " LIMIT " + pq.getSize() + " OFFSET " + pq.getOffset();
        List<Map<String, Object>> rows;
        if (params.length > 0) {
            rows = jdbc.queryForList(pageSql, params);
        } else {
            rows = jdbc.queryForList(pageSql);
        }

        return PageResponse.of(rows, total, pq.getPage(), pq.getSize());
    }

    private List<Map<String, Object>> query(String sql, Object... params) {
        return params.length > 0 ? jdbc.queryForList(sql, params) : jdbc.queryForList(sql);
    }

    private int update(String sql, Object... params) {
        return params.length > 0 ? jdbc.update(sql, params) : jdbc.update(sql);
    }
}
