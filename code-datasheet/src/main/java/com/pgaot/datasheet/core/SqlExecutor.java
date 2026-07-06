package com.pgaot.datasheet.core;

import com.pgaot.datasheet.metadata.MetadataStore;
import com.pgaot.sql.api.SqlTemplate;

/**
 * SQL 执行器编排器：解析表名 → 校验权限/模式 → AST 重写表名 → 执行.
 */
public class SqlExecutor {

    private final SqlTemplate sql;
    private final SqlTableExtractor extractor;
    private final SqlPermissionChecker permissionChecker;
    private final SqlAstRewriter rewriter;

    public SqlExecutor(MetadataStore store, SqlTemplate sql) {
        this.sql = sql;
        this.extractor = new SqlTableExtractor();
        this.permissionChecker = new SqlPermissionChecker(store);
        this.rewriter = new SqlAstRewriter();
    }

    public Object execute(String userId, String rawSql) {
        SqlParsedQuery parsed = extractor.parse(rawSql);
        SqlPermissionChecker.PermissionResult permissionResult = permissionChecker.validate(userId, parsed);
        String rewrittenSql = rewriter.rewrite(parsed, permissionResult.tableMapByLowerName());
        return sql.sql(rewrittenSql);
    }
}
