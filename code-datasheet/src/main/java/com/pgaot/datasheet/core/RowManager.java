package com.pgaot.datasheet.core;

import com.pgaot.datasheet.common.constants.DatasheetConstants;
import com.pgaot.datasheet.common.constants.Messages;
import com.pgaot.datasheet.common.model.ColumnType;
import com.pgaot.datasheet.exception.DatasheetException;
import com.pgaot.datasheet.metadata.MetadataStore;
import com.pgaot.datasheet.metadata.entity.ColumnEntity;
import com.pgaot.datasheet.metadata.entity.TableEntity;
import com.pgaot.sql.api.SqlTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class RowManager {

    private final MetadataStore store;
    private final SqlTemplate sql;

    public RowManager(MetadataStore store, SqlTemplate sql) {
        this.store = store;
        this.sql = sql;
    }

    public int insert(String userId, Long tableId, Map<String, Object> row) {
        return insert(userId, tableId, List.of(row));
    }

    public int insert(String userId, Long tableId, List<Map<String, Object>> rows) {
        if (rows.size() > DatasheetConstants.MAX_INSERT_ROWS)
            throw DatasheetException.rowValidationFailed("max rows: " + DatasheetConstants.MAX_INSERT_ROWS);

        TableEntity table = store.getTable(tableId);
        checkMode(table, false);
        List<ColumnEntity> columns = store.getColumns(tableId);
        String physical = TableManager.physicalName(userId, table.getName());

        String names = columns.stream().map(ColumnEntity::getName).collect(Collectors.joining(", "));
        String sqlText = "INSERT INTO " + physical + " (" + names + ") VALUES ("
                + "?" + ", ?".repeat(columns.size() - 1) + ")";

        List<Object[]> batch = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            List<Object> values = new ArrayList<>();
            for (ColumnEntity col : columns) {
                validateAndCollect(col, row.get(col.getName()), values);
            }
            batch.add(values.toArray());
        }
        sql.batch(sqlText, batch);
        return rows.size();
    }

    public int delete(String userId, Long tableId, String whereClause) {
        TableEntity table = store.getTable(tableId);
        checkMode(table, true);
        String physical = TableManager.physicalName(userId, table.getName());
        return sql.sql("DELETE FROM " + physical + " WHERE " + whereClause);
    }

    public int update(String userId, Long tableId, String whereClause, Map<String, Object> values) {
        TableEntity table = store.getTable(tableId);
        checkMode(table, false);
        List<ColumnEntity> columns = store.getColumns(tableId);
        String physical = TableManager.physicalName(userId, table.getName());

        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (ColumnEntity col : columns) {
            if (!values.containsKey(col.getName())) continue;
            sets.add(col.getName() + " = ?");
            params.add(convertValue(col, values.get(col.getName())));
        }
        if (sets.isEmpty()) return 0;
        return sql.sql("UPDATE " + physical + " SET " + String.join(", ", sets) + " WHERE " + whereClause,
                params.toArray());
    }

    private void validateAndCollect(ColumnEntity col, Object raw, List<Object> target) {
        if (raw == null) {
            if (col.isRequired()) throw DatasheetException.rowValidationFailed(col.getName() + " is required");
            target.add(null);
            return;
        }
        target.add(convertValue(col, raw));
    }

    private Object convertValue(ColumnEntity col, Object raw) {
        try {
            return switch (ColumnType.valueOf(col.getType())) {
                case STRING  -> raw.toString();
                case NUMBER  -> Double.valueOf(raw.toString());
                case DATE    -> raw instanceof LocalDateTime ? raw
                        : raw instanceof LocalDate ? raw
                        : LocalDateTime.parse(raw.toString().replace(" ", "T"));
                case BOOLEAN -> raw instanceof Boolean ? raw : Boolean.valueOf(raw.toString());
            };
        } catch (DateTimeParseException | NumberFormatException e) {
            throw DatasheetException.rowValidationFailed(col.getName() + " type mismatch, expected " + col.getType());
        }
    }

    private void checkMode(TableEntity table, boolean isDelete) {
        String mode = table.getMode() != null ? table.getMode() : "READ_WRITE";
        if ("READ_ONLY".equals(mode))
            throw DatasheetException.sqlOperationDenied(String.format(Messages.MODE_READ_ONLY, table.getName()));
        if (isDelete && "WRITE_ONLY".equals(mode))
            throw DatasheetException.sqlOperationDenied(String.format(Messages.MODE_DELETE_BLOCKED, table.getName()));
    }
}
