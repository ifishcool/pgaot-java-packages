package com.pgaot.datasheet.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.pgaot.datasheet.common.constants.DatasheetConstants;
import com.pgaot.datasheet.common.constants.Messages;
import com.pgaot.datasheet.exception.DatasheetException;
import com.pgaot.datasheet.metadata.MetadataStore;
import com.pgaot.datasheet.metadata.entity.ShareEntity;
import com.pgaot.datasheet.metadata.entity.TableEntity;
import com.pgaot.sql.api.SqlTemplate;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ExportManager {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final CsvMapper CSV = CsvMapper.builder().build();

    private final MetadataStore store;
    private final SqlTemplate sql;

    public ExportManager(MetadataStore store, SqlTemplate sql) {
        this.store = store;
        this.sql = sql;
    }

    public String exportCsv(String userId, Long tableId, List<String> columns, String where) {
        List<Map<String, Object>> rows = query(userId, tableId, columns, where);
        final List<String> cols = (columns != null && !columns.isEmpty())
                ? columns : new ArrayList<>(rows.isEmpty() ? List.of() : rows.get(0).keySet());

        try {
            CsvSchema.Builder schemaBuilder = CsvSchema.builder();
            for (String col : cols) schemaBuilder.addColumn(col);
            return CSV.writer(schemaBuilder.setUseHeader(true).build())
                    .writeValueAsString(rows.stream()
                            .map(row -> cols.stream()
                                    .map(c -> row.getOrDefault(c, ""))
                                    .collect(Collectors.toList()))
                            .collect(Collectors.toList()));
        } catch (IOException e) {
            throw DatasheetException.rowValidationFailed("CSV export: " + e.getMessage());
        }
    }

    public String exportJson(String userId, Long tableId, List<String> columns, String where) {
        List<Map<String, Object>> rows = query(userId, tableId, columns, where);
        if (rows.size() > DatasheetConstants.MAX_EXPORT_ROWS)
            throw DatasheetException.rowValidationFailed("max export rows: " + DatasheetConstants.MAX_EXPORT_ROWS);

        if (columns != null && !columns.isEmpty()) {
            rows = rows.stream().map(row -> {
                Map<String, Object> f = new LinkedHashMap<>();
                for (String c : columns) f.put(c, row.get(c));
                return f;
            }).collect(Collectors.toList());
        }

        try {
            return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(rows);
        } catch (IOException e) {
            throw DatasheetException.rowValidationFailed("JSON export: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> query(String userId, Long tableId, List<String> cols, String where) {
        TableEntity table = store.getTable(tableId);
        if (table == null) throw DatasheetException.tableNotFound(String.valueOf(tableId));

        if (!table.getOwnerId().equals(userId)) {
            ShareEntity s = store.getShare(tableId, userId);
            if (s == null || !s.isCanSelect())
                throw DatasheetException.exportPermissionDenied();
        }
        String mode = table.getMode() != null ? table.getMode() : "ALL";
        if ("WRITE_ONLY".equals(mode))
            throw DatasheetException.sqlOperationDenied(
                    String.format(Messages.MODE_WRITE_ONLY, table.getName()));

        String physical = TableManager.physicalName(table.getOwnerId(), table.getName());
        String c = (cols != null && !cols.isEmpty()) ? String.join(", ", cols) : "*";
        String fullSql = "SELECT " + c + " FROM " + physical;
        if (where != null && !where.isBlank()) fullSql += " WHERE " + where;
        List<Map<String, Object>> rows = sql.sql(fullSql);
        if (rows.size() > DatasheetConstants.MAX_EXPORT_ROWS)
            throw DatasheetException.rowValidationFailed("max export rows: " + DatasheetConstants.MAX_EXPORT_ROWS);
        return rows;
    }

    // ===== 导入 =====

    /** 解析 CSV 为 Map 列表 */
    public List<Map<String, Object>> parseCsv(String csv) {
        try {
            CsvSchema schema = CsvSchema.emptySchema().withHeader();
            var it = CSV.readerFor(new TypeReference<Map<String, String>>() {})
                    .with(schema).readValues(csv);
            try (it) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (it.hasNext()) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> raw = (Map<String, String>) it.next();
                    rows.add(new LinkedHashMap<>(raw));
                }
                if (rows.isEmpty()) throw DatasheetException.rowValidationFailed("CSV 至少需要表头+1行数据");
                return rows;
            }
        } catch (IOException e) {
            throw DatasheetException.rowValidationFailed("CSV parse: " + e.getMessage());
        }
    }

    /** 解析 JSON 数组为 Map 列表 */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> parseJson(String json) {
        try {
            List<Map<String, Object>> rows = JSON.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {});
            if (rows.isEmpty()) throw DatasheetException.rowValidationFailed("JSON 数组不能为空");
            return rows;
        } catch (IOException e) {
            throw DatasheetException.rowValidationFailed("JSON parse: " + e.getMessage());
        }
    }
}
