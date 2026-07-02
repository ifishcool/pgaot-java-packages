package com.pgaot.datasheet.metadata;

import com.pgaot.datasheet.metadata.entity.*;
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
        try { sql.sql("ALTER TABLE ds_table ADD COLUMN mode VARCHAR(16) NOT NULL DEFAULT 'READ_WRITE'"); } catch (Exception ignored) {}
        sql.sql("CREATE TABLE IF NOT EXISTS ds_column (" +
                "id BIGINT NOT NULL AUTO_INCREMENT, " +
                "table_id BIGINT NOT NULL, " +
                "name VARCHAR(64) NOT NULL, " +
                "type VARCHAR(32) NOT NULL, " +

                "required BOOLEAN NOT NULL DEFAULT FALSE, " +
                "sort_order INT NOT NULL DEFAULT 0, " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (id), " +
                "UNIQUE KEY uk_table_column (table_id, name))");
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
        sql.sql("DELETE FROM ds_column WHERE table_id=?", id);
        sql.sql("DELETE FROM ds_table WHERE id=?", id);
    }

    public void updateTable(Long id, String name, String title) {
        sql.sql("UPDATE ds_table SET name=?, title=? WHERE id=?", name, title, id);
    }

    public void setMode(Long id, String mode) {
        sql.sql("UPDATE ds_table SET mode=? WHERE id=?", mode, id);
    }

    // ===== ds_column =====

    public void insertColumn(ColumnEntity c) {
        sql.sql("INSERT INTO ds_column (table_id, name, type, required, sort_order) VALUES (?,?,?,?,?)",
                c.getTableId(), c.getName(), c.getType(), c.isRequired(), c.getSortOrder());
    }

    public List<ColumnEntity> getColumns(Long tableId) {
        return sql.<List<Map<String, Object>>>sql(
                "SELECT * FROM ds_column WHERE table_id=? ORDER BY sort_order", tableId)
                .stream().map(this::mapToColumn).collect(Collectors.toList());
    }

    public ColumnEntity getColumn(Long tableId, String name) {
        List<Map<String, Object>> rows = sql.sql(
                "SELECT * FROM ds_column WHERE table_id=? AND name=?", tableId, name);
        return rows.isEmpty() ? null : mapToColumn(rows.get(0));
    }

    public void dropColumn(Long tableId, String name) {
        sql.sql("DELETE FROM ds_column WHERE table_id=? AND name=?", tableId, name);
    }

    public void renameColumn(Long tableId, String oldName, String newName) {
        sql.sql("UPDATE ds_column SET name=? WHERE table_id=? AND name=?", newName, tableId, oldName);
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

    private ColumnEntity mapToColumn(Map<String, Object> row) {
        ColumnEntity c = new ColumnEntity();
        c.setId(((Number) row.get("id")).longValue());
        c.setTableId(((Number) row.get("table_id")).longValue());
        c.setName((String) row.get("name"));
        c.setType((String) row.get("type"));
        c.setRequired((Boolean) row.get("required"));
        c.setSortOrder(((Number) row.get("sort_order")).intValue());
        return c;
    }
}
