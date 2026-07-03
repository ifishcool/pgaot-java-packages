package com.pgaot.datasheet.api;

import com.pgaot.datasheet.common.config.DatasheetConfig;
import com.pgaot.datasheet.core.*;
import com.pgaot.datasheet.metadata.MetadataStore;
import com.pgaot.sql.api.SqlTemplate;

/**
 * 数据表引擎入口.
 *
 * <pre>{@code
 * DatasheetEngine engine = DatasheetEngine.fromEnv();
 * engine.tables().create("alice", "sales", ...);
 * engine.data().sql("alice", "SELECT * FROM sales");
 * engine.shares().share("alice", tableId, "bob", SharePermission.ALL);
 * }</pre>
 */
public class DatasheetEngine implements AutoCloseable {

    private final DatasheetConfig config;
    private final TableApi tableApi;
    private final DataApi dataApi;
    private final ShareApi shareApi;

    public DatasheetEngine(DatasheetConfig config) {
        this.config = config;
        SqlTemplate adminSql = config.adminSql();
        MetadataStore store = new MetadataStore(adminSql, config.metaJpa());

        TableManager tableManager = new TableManager(store, adminSql);
        RowManager rowManager = new RowManager(store, adminSql);
        SqlExecutor sqlExecutor = new SqlExecutor(store, config.readWriteSql());
        ExportManager exportManager = new ExportManager(store, adminSql);

        this.tableApi = new TableApi(tableManager, store);
        this.dataApi = new DataApi(rowManager, sqlExecutor, exportManager);
        this.shareApi = new ShareApi(store);
    }

    /** 默认数据源（CODE_SQL_URL/USER/PASS） */
    public static DatasheetEngine fromEnv() { return new DatasheetEngine(new DatasheetConfig()); }

    /** 命名数据源（CODE_SQL_URL_{name}） */
    public static DatasheetEngine fromEnv(String name) { return new DatasheetEngine(new DatasheetConfig(name)); }

    public TableApi tables() { return tableApi; }
    public DataApi data() { return dataApi; }
    public ShareApi shares() { return shareApi; }

    /** 关闭所有连接池 */
    @Override
    public void close() { config.close(); }
}
