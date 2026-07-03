package com.pgaot.sql.core.executor;

import com.pgaot.sql.exception.SqlException;

import com.alibaba.druid.pool.DruidDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/** Raw JDBC 执行器 — 存储过程/大事务 */
public class RawExecutor {

    private final DruidDataSource dataSource;

    public RawExecutor(DruidDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Object execute(String sql, Object... params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            if (ps.execute()) return toResultList(ps.getResultSet());
            return ps.getUpdateCount();
        } catch (SQLException e) {
            throw SqlException.executionFailed(e.getMessage());
        }
    }

    public void transaction(Runnable task) {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            task.run();
            conn.commit();
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw SqlException.executionFailed(e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /** 支持返回值的带事务执行 */
    public <T> T transactionCall(Callable<T> task) {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            T result = task.call();
            conn.commit();
            return result;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw SqlException.executionFailed(e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    private List<Map<String, Object>> toResultList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= meta.getColumnCount(); i++)
                row.put(meta.getColumnName(i), rs.getObject(i));
            list.add(row);
        }
        return list;
    }
}
