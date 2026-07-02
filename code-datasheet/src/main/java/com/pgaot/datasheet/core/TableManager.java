package com.pgaot.datasheet.core;
/** DDL 生成 */

import com.pgaot.datasheet.common.constants.Messages;
import com.pgaot.datasheet.common.model.ColumnInfo;
import com.pgaot.datasheet.common.model.ColumnType;
import com.pgaot.datasheet.exception.DatasheetException;
import com.pgaot.datasheet.metadata.MetadataStore;
import com.pgaot.datasheet.metadata.entity.TableEntity;
import com.pgaot.sql.api.SqlTemplate;

import java.util.List;
import java.util.Map;

public class TableManager {

    private final MetadataStore store;
    private final SqlTemplate sql;

    public TableManager(MetadataStore store, SqlTemplate sql) {
        this.store = store;
        this.sql = sql;
    }

    public static String physicalName(String userId, String tableName) {
        return userId + "_" + tableName;
    }

    public TableEntity createTable(String ownerId, String name, String title, String description, List<ColumnInfo> columns) {
        if (name == null || name.isBlank()) throw DatasheetException.rowValidationFailed(Messages.TABLE_EMPTY);
        if (columns == null || columns.isEmpty()) throw DatasheetException.rowValidationFailed(Messages.COLUMNS_EMPTY);
        if (store.getTableByName(ownerId, name) != null) throw DatasheetException.tableNameDuplicate(name);

        String physical = physicalName(ownerId, name);
        StringBuilder ddl = new StringBuilder("CREATE TABLE ").append(physical)
                .append(" (id BIGINT AUTO_INCREMENT PRIMARY KEY");
        for (ColumnInfo c : columns) {
            ddl.append(", ").append(c.getName()).append(" ").append(toSqlType(c.getType()));
            if (c.isRequired()) ddl.append(" NOT NULL");
        }
        ddl.append(")");
        sql.sql(ddl.toString());

        TableEntity t = new TableEntity();
        t.setName(name); t.setTitle(title); t.setOwnerId(ownerId); t.setDescription(description);
        return store.insertTable(t);
    }

    /** 软删除（标记删除，不 DROP 表） */
    public void dropTable(String ownerId, Long tableId) {
        store.softDelete(tableId);
    }

    /** 恢复软删除 */
    public void restoreTable(Long tableId) {
        store.restore(tableId);
    }

    /** 物理删除（DROP TABLE + 清元数据） */
    public void purgeTable(String ownerId, Long tableId) {
        TableEntity t = store.getTable(tableId);
        if (t != null) {
            String physical = physicalName(ownerId, t.getName());
            sql.sql("DROP TABLE IF EXISTS " + physical);
        }
        store.dropTable(tableId);
    }

    public void addColumn(String ownerId, Long tableId, ColumnInfo column) {
        TableEntity t = store.getTable(tableId);
        String physical = physicalName(ownerId, t.getName());
        sql.sql("ALTER TABLE " + physical + " ADD COLUMN " + column.getName() + " " + toSqlType(column.getType())
                + (column.isRequired() ? " NOT NULL" : ""));
    }

    public void dropColumn(String ownerId, Long tableId, String columnName) {
        TableEntity t = store.getTable(tableId);
        String physical = physicalName(ownerId, t.getName());
        sql.sql("ALTER TABLE " + physical + " DROP COLUMN " + columnName);
    }

    public void renameTable(String ownerId, Long tableId, String newName) {
        TableEntity t = store.getTable(tableId);
        String oldPhysical = physicalName(ownerId, t.getName());
        String newPhysical = physicalName(ownerId, newName);
        sql.sql("RENAME TABLE " + oldPhysical + " TO " + newPhysical);
        store.updateTable(tableId, newName, t.getTitle());
    }

    public void renameColumn(String ownerId, Long tableId, String oldName, String newName) {
        TableEntity t = store.getTable(tableId);
        String physical = physicalName(ownerId, t.getName());
        // 从 INFORMATION_SCHEMA 查类型
        List<Map<String, Object>> cols = sql.sql(
                "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?",
                physical, oldName);
        String type = cols.isEmpty() ? "VARCHAR(512)" : (String) cols.get(0).get("DATA_TYPE");
        sql.sql("ALTER TABLE " + physical + " CHANGE COLUMN " + oldName + " " + newName + " " + type);
    }

    public void truncate(String ownerId, Long tableId) {
        TableEntity t = store.getTable(tableId);
        String physical = physicalName(ownerId, t.getName());
        sql.sql("TRUNCATE TABLE " + physical);
    }

    private String toSqlType(ColumnType type) {
        return switch (type) {
            case STRING    -> "VARCHAR(512)";
            case TEXT      -> "TEXT";
            case INT       -> "INT";
            case BIGINT    -> "BIGINT";
            case TINYINT   -> "TINYINT";
            case DOUBLE    -> "DOUBLE";
            case DECIMAL   -> "DECIMAL(20,4)";
            case DATE      -> "DATE";
            case TIME      -> "TIME";
            case DATETIME  -> "DATETIME";
            case TIMESTAMP -> "TIMESTAMP";
            case BOOLEAN   -> "TINYINT(1)";
            case JSON      -> "JSON";
        };
    }
}
