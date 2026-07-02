package com.pgaot.datasheet.core;

import com.pgaot.datasheet.common.constants.DatasheetConstants;
import com.pgaot.datasheet.exception.DatasheetException;
import com.pgaot.datasheet.metadata.MetadataStore;
import com.pgaot.datasheet.metadata.entity.TableEntity;
/** 导入导出 */
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

    // ===== 导入 =====

    /** 解析 CSV 为 Map 列表 */
    public List<Map<String, Object>> parseCsv(String csv) {
        String[] lines = csv.trim().split("\n");
        if (lines.length < 2) throw DatasheetException.rowValidationFailed("CSV 至少需要表头+1行数据");
        String[] headers = lines[0].split(",");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String[] vals = lines[i].split(",", -1);
            Map<String, Object> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.length; j++)
                row.put(headers[j].trim(), j < vals.length ? unquote(vals[j].trim()) : null);
            rows.add(row);
        }
        return rows;
    }

    /** 解析 JSON 数组为 Map 列表 */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> parseJson(String json) {
        // 简单 JSON 解析，假设格式 [{"k":"v"},...]
        List<Map<String, Object>> rows = new ArrayList<>();
        json = json.trim();
        if (!json.startsWith("[")) throw DatasheetException.rowValidationFailed("JSON 需为数组格式");
        String content = json.substring(1, json.length() - 1).trim();
        if (content.isEmpty()) return rows;

        for (String obj : splitJsonObjects(content)) {
            Map<String, Object> row = new LinkedHashMap<>();
            obj = obj.trim();
            if (obj.startsWith("{")) obj = obj.substring(1);
            if (obj.endsWith("}")) obj = obj.substring(0, obj.length() - 1);
            for (String pair : splitJsonPairs(obj)) {
                int colon = pair.indexOf(':');
                if (colon < 0) continue;
                String key = unquote(pair.substring(0, colon).trim());
                String val = pair.substring(colon + 1).trim();
                row.put(key, parseJsonValue(val));
            }
            rows.add(row);
        }
        return rows;
    }

    private List<String> splitJsonObjects(String s) {
        List<String> result = new ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') depth--;
            if (depth == 0 && c == ',') { result.add(s.substring(start, i)); start = i + 1; }
        }
        result.add(s.substring(start));
        return result;
    }

    private List<String> splitJsonPairs(String s) {
        List<String> result = new ArrayList<>();
        int depth = 0, start = 0;
        boolean inStr = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) inStr = !inStr;
            if (!inStr) {
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') depth--;
                else if (depth == 0 && c == ',') { result.add(s.substring(start, i)); start = i + 1; }
            }
        }
        result.add(s.substring(start));
        return result;
    }

    private Object parseJsonValue(String v) {
        if (v == null || v.isEmpty() || "null".equals(v)) return null;
        if ("true".equals(v)) return true;
        if ("false".equals(v)) return false;
        if (v.startsWith("\"")) return unquote(v);
        try { return v.contains(".") ? Double.valueOf(v) : Long.valueOf(v); }
        catch (NumberFormatException e) { return unquote(v); }
    }

    private String unquote(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\""))
            return s.substring(1, s.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        return s;
    }

    private String toJson(Object v) {
        if (v == null) return "null";
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        return "\"" + v.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
