package com.pgaot.sql.core.executor;

import com.pgaot.sql.exception.SqlException;

import com.alibaba.druid.pool.DruidDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            task.run();
            conn.commit();
        } catch (Exception e) {
            throw SqlException.executionFailed(e.getMessage());
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
