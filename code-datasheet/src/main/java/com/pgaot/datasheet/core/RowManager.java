package com.pgaot.datasheet.core;

import com.pgaot.datasheet.common.constants.DatasheetConstants;
import com.pgaot.datasheet.common.constants.Messages;
import com.pgaot.datasheet.exception.DatasheetException;
import com.pgaot.datasheet.metadata.MetadataStore;
import com.pgaot.datasheet.metadata.entity.ShareEntity;
import com.pgaot.datasheet.metadata.entity.TableEntity;
import com.pgaot.sql.api.SqlTemplate;

import java.util.*;

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
        if (rows.isEmpty()) return 0;
        if (rows.size() > DatasheetConstants.MAX_INSERT_ROWS)
            throw DatasheetException.rowValidationFailed("max rows: " + DatasheetConstants.MAX_INSERT_ROWS);

        TableEntity table = store.getTable(tableId);
        checkMode(table, false);
        checkAccess(userId, table, false, false);
        String physical = TableManager.physicalName(table.getOwnerId(), table.getName());

        // 从第一行取列名
        Set<String> colNames = new LinkedHashSet<>(rows.get(0).keySet());
        for (Map<String, Object> r : rows) colNames.addAll(r.keySet());
        List<String> names = new ArrayList<>(colNames);

        String placeholders = "?" + ", ?".repeat(names.size() - 1);
        String sqlText = "INSERT INTO " + physical + " (" + String.join(", ", names) + ") VALUES (" + placeholders + ")";

        List<Object[]> batch = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            List<Object> values = new ArrayList<>();
            for (String name : names) values.add(row.getOrDefault(name, null));
            batch.add(values.toArray());
        }
        sql.batch(sqlText, batch);
        return rows.size();
    }

    public int delete(String userId, Long tableId, String whereClause) {
        TableEntity table = store.getTable(tableId);
        checkMode(table, true);
        checkAccess(userId, table, true, false);
        String physical = TableManager.physicalName(table.getOwnerId(), table.getName());
        return sql.sql("DELETE FROM " + physical + " WHERE " + whereClause);
    }

    public int update(String userId, Long tableId, String whereClause, Map<String, Object> values) {
        TableEntity table = store.getTable(tableId);
        checkMode(table, false);
        checkAccess(userId, table, false, false);
        String physical = TableManager.physicalName(table.getOwnerId(), table.getName());

        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (Map.Entry<String, Object> e : values.entrySet()) {
            sets.add(e.getKey() + " = ?");
            params.add(e.getValue());
        }
        if (sets.isEmpty()) return 0;
        return sql.sql("UPDATE " + physical + " SET " + String.join(", ", sets) + " WHERE " + whereClause,
                params.toArray());
    }

    private void checkAccess(String userId, TableEntity table, boolean isDelete, boolean isInsert) {
        if (table.getOwnerId().equals(userId)) return;
        ShareEntity s = store.getShare(table.getId(), userId);
        if (s == null) throw DatasheetException.tableNotFound(table.getName() + Messages.TABLE_NO_ACCESS);
        boolean ok = isDelete ? s.isCanDelete() : isInsert ? s.isCanInsert() : s.isCanUpdate();
        if (!ok) throw DatasheetException.sqlOperationDenied("共享权限不足: " + table.getName());
    }

    private void checkMode(TableEntity table, boolean isDelete) {
        String mode = table.getMode() != null ? table.getMode() : "READ_WRITE";
        if ("READ_ONLY".equals(mode))
            throw DatasheetException.sqlOperationDenied(String.format(Messages.MODE_READ_ONLY, table.getName()));
        if (isDelete && "WRITE_ONLY".equals(mode))
            throw DatasheetException.sqlOperationDenied(String.format(Messages.MODE_DELETE_BLOCKED, table.getName()));
    }
}
