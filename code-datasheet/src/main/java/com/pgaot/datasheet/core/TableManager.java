package com.pgaot.datasheet.core;

import com.pgaot.datasheet.common.constants.Messages;
import com.pgaot.datasheet.common.model.ColumnInfo;
import com.pgaot.datasheet.common.model.ColumnType;
import com.pgaot.datasheet.exception.DatasheetException;
import com.pgaot.datasheet.metadata.MetadataStore;
import com.pgaot.datasheet.metadata.entity.ColumnEntity;
import com.pgaot.datasheet.metadata.entity.TableEntity;
import com.pgaot.sql.api.SqlTemplate;

import java.util.List;

public class TableManager {

    private final MetadataStore store;
    private final SqlTemplate sql;

    public TableManager(MetadataStore store, SqlTemplate sql) {
        this.store = store;
        this.sql = sql;
    }

    // ===== 表名 =====

    /** 物理表名: userId_name */
    public static String physicalName(String userId, String tableName) {
        return userId + "_" + tableName;
    }

    // ===== DDL 操作 =====

    public TableEntity createTable(String ownerId, String name, String title, String description, List<ColumnInfo> columns) {
        if (name == null || name.isBlank()) throw DatasheetException.rowValidationFailed(Messages.TABLE_EMPTY);
        if (columns == null || columns.isEmpty()) throw DatasheetException.rowValidationFailed(Messages.COLUMNS_EMPTY);
        if (store.getTableByName(ownerId, name) != null) throw DatasheetException.tableNameDuplicate(name);

        String physical = physicalName(ownerId, name);

        StringBuilder ddl = new StringBuilder("CREATE TABLE ").append(physical)
                .append(" (id BIGINT AUTO_INCREMENT PRIMARY KEY");
        for (int i = 0; i < columns.size(); i++) {
            ColumnInfo c = columns.get(i);
            ddl.append(", ").append(c.getName()).append(" ").append(toSqlType(c.getType()));
            if (c.isRequired()) ddl.append(" NOT NULL");
        }
        ddl.append(")");
        sql.sql(ddl.toString());

        TableEntity t = new TableEntity();
        t.setName(name); t.setTitle(title); t.setOwnerId(ownerId); t.setDescription(description);
        t = store.insertTable(t);

        for (int i = 0; i < columns.size(); i++) {
            ColumnInfo c = columns.get(i);
            ColumnEntity ce = new ColumnEntity();
            ce.setTableId(t.getId()); ce.setName(c.getName());
            ce.setType(c.getType().name()); ce.setRequired(c.isRequired());
            ce.setSortOrder(i);
            store.insertColumn(ce);
        }
        return t;
    }

    public void dropTable(String ownerId, Long tableId) {
        TableEntity t = store.getTable(tableId);
        String physical = physicalName(ownerId, t.getName());
        sql.sql("DROP TABLE IF EXISTS " + physical);
        store.dropTable(tableId);
    }

    public void addColumn(String ownerId, Long tableId, ColumnInfo column) {
        TableEntity t = store.getTable(tableId);
        String physical = physicalName(ownerId, t.getName());
        sql.sql("ALTER TABLE " + physical + " ADD COLUMN " + column.getName() + " " + toSqlType(column.getType())
                + (column.isRequired() ? " NOT NULL" : ""));

        ColumnEntity ce = new ColumnEntity();
        ce.setTableId(tableId); ce.setName(column.getName());
        ce.setType(column.getType().name()); ce.setRequired(column.isRequired());
        ce.setSortOrder(store.getColumns(tableId).size());
        store.insertColumn(ce);
    }

    public void dropColumn(String ownerId, Long tableId, String columnName) {
        ColumnEntity col = store.getColumn(tableId, columnName);
        if (col == null) return;
        if (col.isRequired()) throw DatasheetException.columnRequired(columnName);

        TableEntity t = store.getTable(tableId);
        String physical = physicalName(ownerId, t.getName());
        sql.sql("ALTER TABLE " + physical + " DROP COLUMN " + columnName);
        store.dropColumn(tableId, columnName);
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
        ColumnEntity col = store.getColumn(tableId, oldName);
        String physical = physicalName(ownerId, t.getName());
        sql.sql("ALTER TABLE " + physical + " CHANGE COLUMN " + oldName + " " + newName + " " + toSqlTypeStr(col.getType()));
        store.renameColumn(tableId, oldName, newName);
    }

    public void truncate(String ownerId, Long tableId) {
        TableEntity t = store.getTable(tableId);
        String physical = physicalName(ownerId, t.getName());
        sql.sql("TRUNCATE TABLE " + physical);
    }

    private String toSqlType(ColumnType type) {
        return switch (type) {
            case STRING -> "VARCHAR(512)";
            case NUMBER -> "DECIMAL(20,4)";
            case DATE   -> "DATETIME";
            case BOOLEAN -> "TINYINT(1)";
        };
    }

    private String toSqlTypeStr(String typeName) {
        return toSqlType(ColumnType.valueOf(typeName));
    }
}
