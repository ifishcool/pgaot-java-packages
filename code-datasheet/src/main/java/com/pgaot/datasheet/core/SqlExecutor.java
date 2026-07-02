package com.pgaot.datasheet.core;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.util.JdbcConstants;
import com.pgaot.datasheet.common.constants.Messages;
import com.pgaot.datasheet.exception.DatasheetException;
import com.pgaot.datasheet.metadata.MetadataStore;
import com.pgaot.datasheet.metadata.entity.TableEntity;
import com.pgaot.sql.api.SqlTemplate;

import java.util.*;

/**
 * SQL 执行器 — Druid AST 提取表名 → 权限校验 → 表名替换 → 执行.
 *
 * <p>目标表（INSERT/UPDATE/DELETE 的主表）和源表（SELECT FROM/JOIN/子查询）分开校验模式.
 * <p>注入/DDL 由 code-sql readWrite 防火墙兜底.
 */
public class SqlExecutor {

    private final MetadataStore store;
    private final SqlTemplate sql;

    public SqlExecutor(MetadataStore store, SqlTemplate sql) {
        this.store = store;
        this.sql = sql;
    }

    public Object execute(String userId, String rawSql) {
        String upper = rawSql.trim().toUpperCase();
        String targetOp;
        if (upper.startsWith("SELECT")) targetOp = "SELECT";
        else if (upper.startsWith("INSERT")) targetOp = "INSERT";
        else if (upper.startsWith("UPDATE")) targetOp = "UPDATE";
        else if (upper.startsWith("DELETE")) targetOp = "DELETE";
        else throw DatasheetException.sqlOperationDenied(upper.split("\\s")[0]);

        boolean isDelete = upper.startsWith("DELETE");
        return sql.sql(validateAndRewrite(userId, rawSql, targetOp, isDelete));
    }

    private String validateAndRewrite(String userId, String rawSql, String targetOp, boolean isDelete) {
        Map<String, TableEntity> owned = new HashMap<>();
        for (TableEntity t : store.listByUser(userId))
            owned.put(t.getName().toLowerCase(), t);
        if (owned.isEmpty()) throw DatasheetException.tableNotFound(rawSql);

        // 提取表名: 目标表(写操作校验) + 源表(读操作校验)
        Set<String> allNames = new LinkedHashSet<>();
        Set<String> targetNames = new LinkedHashSet<>();
        try {
            List<SQLStatement> stmts = SQLUtils.parseStatements(rawSql, JdbcConstants.MYSQL);
            for (SQLStatement stmt : stmts)
                collectTables(stmt, allNames, targetNames);
        } catch (Exception e) {
            throw DatasheetException.sqlOperationDenied("parse: " + e.getMessage());
        }

        // 权限 + 模式校验
        for (String name : allNames) {
            TableEntity t = owned.get(name.toLowerCase());
            if (t == null) throw DatasheetException.tableNotFound(name + Messages.TABLE_NO_ACCESS);

            boolean isTarget = targetNames.contains(name);
            String opForCheck = isTarget ? targetOp : "SELECT"; // 源表按 SELECT 校验
            boolean deleteCheck = isTarget && isDelete;

            String mode = t.getMode() != null ? t.getMode() : "READ_WRITE";
            switch (mode) {
                case "READ_ONLY":
                    if (!"SELECT".equals(opForCheck))
                        throw DatasheetException.sqlOperationDenied(String.format(Messages.MODE_READ_ONLY, name));
                    break;
                case "WRITE_ONLY":
                    if ("SELECT".equals(opForCheck))
                        throw DatasheetException.sqlOperationDenied(String.format(Messages.MODE_WRITE_ONLY, name));
                    if (deleteCheck)
                        throw DatasheetException.sqlOperationDenied(String.format(Messages.MODE_DELETE_BLOCKED, name));
                    break;
            }
        }

        // 表名替换
        String rewritten = rawSql;
        for (String name : allNames)
            rewritten = rewritten.replaceAll("(?i)\\b" + name + "\\b",
                    TableManager.physicalName(userId, name));
        return rewritten;
    }

    // === 表名提取: allNames=全部, targetNames=主表 ===

    private void collectTables(SQLStatement stmt, Set<String> all, Set<String> target) {
        if (stmt instanceof SQLSelectStatement s) {
            collectFromQuery(s.getSelect().getQuery(), all);
        } else if (stmt instanceof SQLInsertStatement s) {
            addName(s.getTableSource(), all, target);
            if (s.getQuery() != null) collectFromQuery(s.getQuery().getQuery(), all);
            // VALUES 子句中的子查询
            if (s.getValuesList() != null)
                for (SQLInsertStatement.ValuesClause vc : s.getValuesList())
                    for (SQLExpr val : vc.getValues())
                        collectFromExpr(val, all);
        } else if (stmt instanceof SQLUpdateStatement s) {
            // UPDATE: 主表是 target，JOIN 的表只收集不标 target
            addUpdateTarget(s.getTableSource(), all, target);
            if (s.getWhere() != null) collectFromExpr(s.getWhere(), all);
            // SET 子句中的子查询
            if (s.getItems() != null)
                for (SQLUpdateSetItem item : s.getItems())
                    collectFromExpr(item.getValue(), all);
        } else if (stmt instanceof SQLDeleteStatement s) {
            addDeleteTarget(s.getTableSource(), all, target);
            if (s.getWhere() != null) collectFromExpr(s.getWhere(), all);
        }
    }

    private void collectFromQuery(SQLSelectQuery query, Set<String> all) {
        if (query instanceof SQLSelectQueryBlock block) {
            addName(block.getFrom(), all, null);
            if (block.getWhere() != null) collectFromExpr(block.getWhere(), all);
            // SELECT 列表和 ORDER BY 中的子查询
            for (SQLSelectItem item : block.getSelectList())
                collectFromExpr(item.getExpr(), all);
            if (block.getOrderBy() != null)
                for (SQLSelectOrderByItem ob : block.getOrderBy().getItems())
                    collectFromExpr(ob.getExpr(), all);
        } else if (query instanceof SQLUnionQuery union) {
            collectFromQuery(union.getLeft(), all);
            collectFromQuery(union.getRight(), all);
        }
    }

    private void collectFromExpr(SQLExpr expr, Set<String> all) {
        if (expr == null) return;
        if (expr instanceof SQLInSubQueryExpr sq)
            collectFromQuery(sq.getSubQuery().getQuery(), all);
        else if (expr instanceof SQLQueryExpr qe)
            collectFromQuery(qe.getSubQuery().getQuery(), all);
        else if (expr instanceof SQLExistsExpr ex)
            collectFromQuery(ex.getSubQuery().getQuery(), all);
        else if (expr instanceof SQLBinaryOpExpr bin) {
            collectFromExpr(bin.getLeft(), all);
            collectFromExpr(bin.getRight(), all);
        }
    }

    /** UPDATE: 主表(左侧)是 target，JOIN 的表只收集不标 target */
    private void addUpdateTarget(SQLTableSource src, Set<String> all, Set<String> target) {
        if (src instanceof SQLExprTableSource expr) {
            addName(expr, all, target);
        } else if (src instanceof SQLJoinTableSource join) {
            addName(join.getLeft(), all, target);   // 主表=target
            addName(join.getRight(), all, null);    // JOIN表=仅all
        }
    }

    private void addDeleteTarget(SQLTableSource src, Set<String> all, Set<String> target) {
        addUpdateTarget(src, all, target);
    }

    private void addName(SQLTableSource src, Set<String> all, Set<String> target) {
        if (src == null) return;
        if (src instanceof SQLExprTableSource expr) {
            String name = expr.getExpr().toString().replace("`", "");
            if (name.contains(".")) name = name.substring(name.lastIndexOf('.') + 1);
            all.add(name);
            if (target != null) target.add(name);
        } else if (src instanceof SQLJoinTableSource join) {
            addName(join.getLeft(), all, target);
            addName(join.getRight(), all, target);
        } else if (src instanceof SQLSubqueryTableSource sub) {
            collectFromQuery(sub.getSelect().getQuery(), all);
        }
    }
}
