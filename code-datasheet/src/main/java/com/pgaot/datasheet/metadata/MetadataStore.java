package com.pgaot.datasheet.metadata;

import com.pgaot.datasheet.metadata.entity.TableEntity;
import com.pgaot.sql.api.SqlTemplate;

import java.util.*;
import java.util.stream.Collectors;

public class MetadataStore {

    private final SqlTemplate sql;

    public MetadataStore(SqlTemplate sql) {
        this.sql = sql;
        initTables();
    }

    private void initTables() {
        sql.sql("CREATE TABLE IF NOT EXISTS ds_table (" +
                "id BIGINT NOT NULL AUTO_INCREMENT, " +
                "name VARCHAR(64) NOT NULL, " +
                "title VARCHAR(128) DEFAULT NULL, " +
                "owner_id VARCHAR(64) NOT NULL, " +
                "description TEXT DEFAULT NULL, " +
                "mode VARCHAR(16) NOT NULL DEFAULT 'READ_WRITE', " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (id), " +
                "UNIQUE KEY uk_owner_table (owner_id, name))");
    }

    // ===== ds_table =====

    public TableEntity insertTable(TableEntity t) {
        sql.sql("INSERT INTO ds_table (name, title, owner_id, description, mode) VALUES (?,?,?,?,?)",
                t.getName(), t.getTitle(), t.getOwnerId(), t.getDescription(),
                t.getMode() != null ? t.getMode() : "READ_WRITE");
        List<Map<String, Object>> rows = sql.sql(
                "SELECT * FROM ds_table WHERE owner_id=? AND name=? ORDER BY id DESC LIMIT 1",
                t.getOwnerId(), t.getName());
        t.setId(((Number) rows.get(0).get("id")).longValue());
        return t;
    }

    public TableEntity getTable(Long id) {
        List<Map<String, Object>> rows = sql.sql("SELECT * FROM ds_table WHERE id=?", id);
        return rows.isEmpty() ? null : mapToTable(rows.get(0));
    }

    public TableEntity getTableByName(String ownerId, String name) {
        List<Map<String, Object>> rows = sql.sql(
                "SELECT * FROM ds_table WHERE owner_id=? AND name=?", ownerId, name);
        return rows.isEmpty() ? null : mapToTable(rows.get(0));
    }

    public List<TableEntity> listByUser(String userId) {
        return sql.<List<Map<String, Object>>>sql(
                "SELECT * FROM ds_table WHERE owner_id=?", userId)
                .stream().map(this::mapToTable).collect(Collectors.toList());
    }

    public void dropTable(Long id) {
        sql.sql("DELETE FROM ds_table WHERE id=?", id);
    }

    public void updateTable(Long id, String name, String title) {
        sql.sql("UPDATE ds_table SET name=?, title=? WHERE id=?", name, title, id);
    }

    public void setMode(Long id, String mode) {
        sql.sql("UPDATE ds_table SET mode=? WHERE id=?", mode, id);
    }

    /** 从 INFORMATION_SCHEMA 查询列信息 */
    public List<Map<String, Object>> getColumns(String physicalTable) {
        try {
            return sql.sql("SELECT COLUMN_NAME AS name, DATA_TYPE AS type, "
                    + "IS_NULLABLE AS nullable FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? "
                    + "AND COLUMN_NAME != 'id' ORDER BY ORDINAL_POSITION", physicalTable);
        } catch (Exception e) {
            return List.of();
        }
    }

    // ===== helpers =====

    private TableEntity mapToTable(Map<String, Object> row) {
        TableEntity t = new TableEntity();
        t.setId(((Number) row.get("id")).longValue());
        t.setName((String) row.get("name"));
        t.setTitle((String) row.get("title"));
        t.setOwnerId((String) row.get("owner_id"));
        t.setDescription((String) row.get("description"));
        t.setMode((String) row.get("mode"));
        return t;
    }
}
