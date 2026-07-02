package com.pgaot.datasheet.api;

import com.pgaot.datasheet.common.config.DatasheetConfig;
import com.pgaot.datasheet.core.*;
import com.pgaot.datasheet.metadata.MetadataStore;
import com.pgaot.sql.api.SqlTemplate;

public class DatasheetEngine {

    private final TableApi tableApi;
    private final DataApi dataApi;
    private final ShareApi shareApi;

    public DatasheetEngine(DatasheetConfig config) {
        SqlTemplate adminSql = config.adminSql();
        MetadataStore store = new MetadataStore(adminSql);

        TableManager tableManager = new TableManager(store, adminSql);
        RowManager rowManager = new RowManager(store, adminSql);
        SqlExecutor sqlExecutor = new SqlExecutor(store, config.readWriteSql());
        ExportManager exportManager = new ExportManager(store, adminSql);

        this.tableApi = new TableApi(tableManager, store);
        this.dataApi = new DataApi(rowManager, sqlExecutor, exportManager);
        this.shareApi = new ShareApi(store);
    }

    public static DatasheetEngine fromEnv() { return new DatasheetEngine(new DatasheetConfig()); }

    public TableApi tables() { return tableApi; }
    public DataApi data() { return dataApi; }
    public ShareApi shares() { return shareApi; }
}
