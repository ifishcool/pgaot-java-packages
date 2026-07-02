package com.pgaot.datasheet.core;

import com.pgaot.datasheet.common.constants.DatasheetConstants;
import com.pgaot.datasheet.exception.DatasheetException;
import com.pgaot.datasheet.metadata.MetadataStore;
import com.pgaot.datasheet.metadata.entity.TableEntity;
import com.pgaot.sql.api.SqlTemplate;

import java.util.*;
import java.util.stream.Collectors;

public class ExportManager {

    private final MetadataStore store;
    private final SqlTemplate sql;

    public ExportManager(MetadataStore store, SqlTemplate sql) {
        this.store = store;
        this.sql = sql;
    }

    public String exportCsv(String userId, Long tableId, List<String> columns, String where) {
        List<Map<String, Object>> rows = query(userId, tableId, columns, where);
        if (columns == null || columns.isEmpty())
            columns = new ArrayList<>(rows.isEmpty() ? List.of() : rows.get(0).keySet());

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", columns)).append("\n");
        for (Map<String, Object> row : rows) {
            StringJoiner sj = new StringJoiner(",");
            for (String col : columns) {
                Object v = row.get(col);
                sj.add(v == null ? "" : "\"" + v.toString().replace("\"", "\"\"") + "\"");
            }
            sb.append(sj).append("\n");
        }
        return sb.toString();
    }

    public String exportJson(String userId, Long tableId, List<String> columns, String where) {
        List<Map<String, Object>> rows = query(userId, tableId, columns, where);
        if (rows.size() > DatasheetConstants.MAX_EXPORT_ROWS)
            throw DatasheetException.rowValidationFailed("max export rows: " + DatasheetConstants.MAX_EXPORT_ROWS);

        if (columns != null && !columns.isEmpty()) {
            List<String> cols = columns;
            rows = rows.stream().map(row -> {
                Map<String, Object> f = new LinkedHashMap<>();
                for (String c : cols) f.put(c, row.get(c));
                return f;
            }).collect(Collectors.toList());
        }

        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            sb.append("  {");
            StringJoiner sj = new StringJoiner(", ");
            for (Map.Entry<String, Object> e : row.entrySet())
                sj.add("\"" + e.getKey() + "\": " + toJson(e.getValue()));
            sb.append(sj).append("}");
            if (i < rows.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> query(String userId, Long tableId, List<String> cols, String where) {
        TableEntity table = store.getTable(tableId);
        String physical = TableManager.physicalName(table.getOwnerId(), table.getName());
        String c = (cols != null && !cols.isEmpty()) ? String.join(", ", cols) : "*";
        String fullSql = "SELECT " + c + " FROM " + physical;
        if (where != null && !where.isBlank()) fullSql += " WHERE " + where;
        List<Map<String, Object>> rows = (List<Map<String, Object>>) sql.sql(fullSql);
        if (rows.size() > DatasheetConstants.MAX_EXPORT_ROWS)
            throw DatasheetException.rowValidationFailed("max export rows: " + DatasheetConstants.MAX_EXPORT_ROWS);
        return rows;
    }

    private String toJson(Object v) {
        if (v == null) return "null";
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        return "\"" + v.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
