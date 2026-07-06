package com.pgaot.datasheet.core;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLExistsExpr;
import com.alibaba.druid.sql.ast.expr.SQLInSubQueryExpr;
import com.alibaba.druid.sql.ast.expr.SQLQueryExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.util.JdbcConstants;
import com.pgaot.datasheet.exception.DatasheetException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class SqlTableExtractor {

    SqlParsedQuery parse(String rawSql) {
        String upper = rawSql.trim().toUpperCase();
        String targetOp;
        if (upper.startsWith("SELECT")) targetOp = "SELECT";
        else if (upper.startsWith("INSERT")) targetOp = "INSERT";
        else if (upper.startsWith("UPDATE")) targetOp = "UPDATE";
        else if (upper.startsWith("DELETE")) targetOp = "DELETE";
        else throw DatasheetException.sqlOperationDenied(upper.split("\\s")[0]);

        Set<String> allNames = new LinkedHashSet<>();
        Set<String> targetNames = new LinkedHashSet<>();
        List<SQLStatement> statements;
        try {
            statements = SQLUtils.parseStatements(rawSql, JdbcConstants.MYSQL);
            for (SQLStatement stmt : statements) {
                collectTables(stmt, allNames, targetNames);
            }
        } catch (Exception e) {
            throw DatasheetException.sqlOperationDenied("parse: " + e.getMessage());
        }

        return new SqlParsedQuery(rawSql, statements, allNames, targetNames,
                targetOp, "DELETE".equals(targetOp));
    }

    private void collectTables(SQLStatement stmt, Set<String> all, Set<String> target) {
        if (stmt instanceof SQLSelectStatement s) {
            collectFromQuery(s.getSelect().getQuery(), all);
        } else if (stmt instanceof SQLInsertStatement s) {
            addName(s.getTableSource(), all, target);
            if (s.getQuery() != null) collectFromQuery(s.getQuery().getQuery(), all);
            if (s.getValuesList() != null)
                for (SQLInsertStatement.ValuesClause vc : s.getValuesList())
                    for (SQLExpr val : vc.getValues())
                        collectFromExpr(val, all);
        } else if (stmt instanceof SQLUpdateStatement s) {
            addUpdateTarget(s.getTableSource(), all, target);
            if (s.getWhere() != null) collectFromExpr(s.getWhere(), all);
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
            for (SQLSelectItem item : block.getSelectList()) {
                collectFromExpr(item.getExpr(), all);
            }
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

    private void addUpdateTarget(SQLTableSource src, Set<String> all, Set<String> target) {
        if (src instanceof SQLExprTableSource expr) {
            addName(expr, all, target);
        } else if (src instanceof SQLJoinTableSource join) {
            addName(join.getLeft(), all, target);
            addName(join.getRight(), all, null);
        }
    }

    private void addDeleteTarget(SQLTableSource src, Set<String> all, Set<String> target) {
        addUpdateTarget(src, all, target);
    }

    private void addName(SQLTableSource src, Set<String> all, Set<String> target) {
        if (src == null) return;
        if (src instanceof SQLExprTableSource expr) {
            String name = normalizeName(expr.getExpr().toString());
            all.add(name);
            if (target != null) target.add(name);
        } else if (src instanceof SQLJoinTableSource join) {
            addName(join.getLeft(), all, target);
            addName(join.getRight(), all, target);
        } else if (src instanceof SQLSubqueryTableSource sub) {
            collectFromQuery(sub.getSelect().getQuery(), all);
        }
    }

    static String normalizeName(String rawName) {
        String name = rawName.replace("`", "");
        if (name.contains(".")) name = name.substring(name.lastIndexOf('.') + 1);
        return name;
    }
}
