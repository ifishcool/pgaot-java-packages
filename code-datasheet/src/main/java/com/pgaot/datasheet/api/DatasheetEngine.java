package com.pgaot.datasheet.api;

import com.pgaot.datasheet.common.config.DatasheetConfig;
import com.pgaot.datasheet.core.*;
import com.pgaot.datasheet.metadata.MetadataStore;
import com.pgaot.sql.api.SqlTemplate;

/**
 * 数据表引擎入口 — 所有操作的起点.
 *
 * <h3>隔离模型</h3>
 * 表前缀: 物理表名 = userId_tableName，不同租户同表名不冲突.
 * 模式控制: READ_ONLY(只读) / WRITE_ONLY(只写) / READ_WRITE(读写).
 * 权限: 数据操作校验表归属 + 模式; DDL 操作校验 owner.
 *
 * <h3>连接模型</h3>
 * 管理员连接: DDL/GRANT/内部 SQL（全开放）.
 * 用户 SQL 连接: readWrite 防火墙（禁止 DDL/DELETE）.
 *
 * <pre>{@code
 * DatasheetEngine engine = DatasheetEngine.fromEnv();
 *
 * // 建表
 * TableInfo t = engine.tables().create("tenant_a", "sales", "销售表", null,
 *     List.of(new ColumnInfo("product", ColumnType.STRING, true)));
 *
 * // 数据操作
 * engine.data().insert("tenant_a", t.getId(), Map.of("product", "A"));
 * List<Map<String, Object>> rows = engine.data().sql("tenant_a", "SELECT * FROM sales");
 *
 * // 导出
 * String csv = engine.data().exportCsv("tenant_a", t.getId(), null, null);
 * }</pre>
 */
public class DatasheetEngine {

    private final TableApi tableApi;
    private final DataApi dataApi;

    public DatasheetEngine(DatasheetConfig config) {
        SqlTemplate adminSql = config.adminSql();
        MetadataStore store = new MetadataStore(adminSql);

        TableManager tableManager = new TableManager(store, adminSql);
        RowManager rowManager = new RowManager(store, adminSql);
        SqlExecutor sqlExecutor = new SqlExecutor(store, config.readWriteSql());
        ExportManager exportManager = new ExportManager(store, adminSql);

        this.tableApi = new TableApi(tableManager, store);
        this.dataApi = new DataApi(rowManager, sqlExecutor, exportManager);
    }

    /** 从环境变量创建（CODE_SQL_URL/USER/PASS） */
    public static DatasheetEngine fromEnv() { return new DatasheetEngine(new DatasheetConfig()); }

    /** 表管理 API */
    public TableApi tables() { return tableApi; }

    /** 数据操作 API */
    public DataApi data() { return dataApi; }
}
