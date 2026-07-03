package com.pgaot.datasheet.metadata;

import com.pgaot.datasheet.metadata.entity.ShareEntity;
import com.pgaot.datasheet.metadata.entity.TableEntity;
import com.pgaot.sql.api.SqlTemplate;

import java.util.*;
/** 元数据存储 — ds_table + ds_share */
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
                "mode VARCHAR(16) NOT NULL DEFAULT 'ALL', " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (id), " +
                "UNIQUE KEY uk_owner_table (owner_id, name))");
        sql.sql("CREATE TABLE IF NOT EXISTS ds_share (" +
                "id BIGINT NOT NULL AUTO_INCREMENT, " +
                "table_id BIGINT NOT NULL, " +
                "from_user VARCHAR(64) NOT NULL, " +
                "to_user VARCHAR(64) NOT NULL, " +
                "can_select BOOLEAN NOT NULL DEFAULT TRUE, " +
                "can_insert BOOLEAN NOT NULL DEFAULT FALSE, " +
                "can_update BOOLEAN NOT NULL DEFAULT FALSE, " +
                "can_delete BOOLEAN NOT NULL DEFAULT FALSE, " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (id), " +
                "UNIQUE KEY uk_share (table_id, from_user, to_user))");
    }

    // ===== ds_share =====

    public void upsertShare(Long tableId, String fromUser, String toUser,
                            boolean cs, boolean ci, boolean cu, boolean cd) {
        sql.sql("INSERT INTO ds_share (table_id, from_user, to_user, can_select, can_insert, can_update, can_delete) "
                + "VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
                + "can_select=?, can_insert=?, can_update=?, can_delete=?",
                tableId, fromUser, toUser, cs, ci, cu, cd, cs, ci, cu, cd);
    }

    public void deleteShare(Long tableId, String fromUser, String toUser) {
        sql.sql("DELETE FROM ds_share WHERE table_id=? AND from_user=? AND to_user=?", tableId, fromUser, toUser);
    }

    public ShareEntity getShare(Long tableId, String toUser) {
        List<Map<String, Object>> rows = sql.sql(
                "SELECT * FROM ds_share WHERE table_id=? AND to_user=?", tableId, toUser);
        if (rows.isEmpty()) return null;
        Map<String, Object> r = rows.get(0);
        ShareEntity s = new ShareEntity();
        s.setTableId(((Number) r.get("table_id")).longValue());
        s.setFromUser((String) r.get("from_user"));
        s.setToUser((String) r.get("to_user"));
        s.setCanSelect((Boolean) r.get("can_select"));
        s.setCanInsert((Boolean) r.get("can_insert"));
        s.setCanUpdate((Boolean) r.get("can_update"));
        s.setCanDelete((Boolean) r.get("can_delete"));
        return s;
    }

    public List<ShareEntity> getSharesByTable(Long tableId) {
        return sql.<List<Map<String, Object>>>sql("SELECT * FROM ds_share WHERE table_id=?", tableId)
                .stream().map(r -> {
                    ShareEntity s = new ShareEntity();
                    s.setTableId(((Number) r.get("table_id")).longValue());
                    s.setFromUser((String) r.get("from_user"));
                    s.setToUser((String) r.get("to_user"));
                    s.setCanSelect((Boolean) r.get("can_select"));
                    s.setCanInsert((Boolean) r.get("can_insert"));
                    s.setCanUpdate((Boolean) r.get("can_update"));
                    s.setCanDelete((Boolean) r.get("can_delete"));
                    return s;
                }).collect(Collectors.toList());
    }

    /** 共享给我的表 ID 列表 */
    public List<Long> getSharedTableIds(String userId) {
        return sql.<List<Map<String, Object>>>sql(
                "SELECT DISTINCT table_id FROM ds_share WHERE to_user=?", userId)
                .stream().map(r -> ((Number) r.get("table_id")).longValue())
                .collect(Collectors.toList());
    }

    // ===== ds_table =====

    public TableEntity insertTable(TableEntity t) {
        sql.sql("INSERT INTO ds_table (name, title, owner_id, description, mode) VALUES (?,?,?,?,?)",
                t.getName(), t.getTitle(), t.getOwnerId(), t.getDescription(),
                t.getMode() != null ? t.getMode() : "ALL");
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

    /** 该用户可见的所有表（自己创建的 + 共享给我的） */
    public List<TableEntity> listByUser(String userId) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Map<String, Object> r : sql.<List<Map<String, Object>>>sql(
                "SELECT id FROM ds_table WHERE owner_id=?", userId))
            ids.add(((Number) r.get("id")).longValue());
        ids.addAll(getSharedTableIds(userId));

        List<TableEntity> result = new ArrayList<>();
        for (Long id : ids) {
            TableEntity t = getTable(id);
            if (t != null) result.add(t);
        }
        return result;
    }

    /** 删除表元数据 */
    public void dropTable(Long id) {
        sql.sql("DELETE FROM ds_share WHERE table_id=?", id);
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
