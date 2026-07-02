package com.pgaot.datasheet.api;

import com.pgaot.datasheet.core.ExportManager;
import com.pgaot.datasheet.core.RowManager;
import com.pgaot.datasheet.core.SqlExecutor;

import java.util.List;
import java.util.Map;

/**
 * 数据操作 API — 增删改查 + 导出.
 *
 * <p>insert/update/delete 走管理员连接，SQL 由代码生成，类型自动校验.
 * sql() 走 readWrite 防火墙连接，仅允许 SELECT/INSERT/UPDATE，禁止 DDL/DELETE.
 *
 * <h3>隔离检查</h3>
 * 每条 SQL 执行前 Druid AST 提取表名 → 逐表校验归属 + 模式.
 */
public class DataApi {

    private final RowManager rowManager;
    private final SqlExecutor sqlExecutor;
    private final ExportManager exportManager;

    DataApi(RowManager rowManager, SqlExecutor sqlExecutor, ExportManager exportManager) {
        this.rowManager = rowManager;
        this.sqlExecutor = sqlExecutor;
        this.exportManager = exportManager;
    }

    /** 插入单行，列类型自动校验 */
    public int insert(String userId, String tableId, Map<String, Object> row) {
        return rowManager.insert(userId, parseId(tableId), row);
    }

    /** 批量插入（最多 1000 行），列类型自动校验 */
    public int insert(String userId, String tableId, List<Map<String, Object>> rows) {
        return rowManager.insert(userId, parseId(tableId), rows);
    }

    /** 按条件删除行 */
    public int delete(String userId, String tableId, String whereClause) {
        return rowManager.delete(userId, parseId(tableId), whereClause);
    }

    /** 按条件更新行 */
    public int update(String userId, String tableId, String whereClause, Map<String, Object> values) {
        return rowManager.update(userId, parseId(tableId), whereClause, values);
    }

    /**
     * 执行用户 SQL — 仅允许 SELECT/INSERT/UPDATE.
     *
     * @param <T> SELECT → List&lt;Map&lt;String,Object&gt;&gt;, INSERT/UPDATE → Integer
     * @param sqlText 用户原始 SQL，内部会自动将逻辑表名替换为物理表名
     * @throws DatasheetException 表无权访问 / 模式限制 / DDL 被防火墙拦截
     */
    @SuppressWarnings("unchecked")
    public <T> T sql(String userId, String sqlText) {
        return (T) sqlExecutor.execute(userId, sqlText);
    }

    /** 导出 CSV 字符串 */
    public String exportCsv(String userId, String tableId, List<String> columns, String whereClause) {
        return exportManager.exportCsv(userId, parseId(tableId), columns, whereClause);
    }

    /** 导出 JSON 字符串 */
    public String exportJson(String userId, String tableId, List<String> columns, String whereClause) {
        return exportManager.exportJson(userId, parseId(tableId), columns, whereClause);
    }

    private long parseId(String id) {
        try { return Long.parseLong(id); } catch (NumberFormatException e) { return 0L; }
    }
}
