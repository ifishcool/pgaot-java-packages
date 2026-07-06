package com.pgaot.datasheet.core;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLExistsExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLInSubQueryExpr;
import com.alibaba.druid.sql.ast.expr.SQLQueryExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.util.JdbcConstants;
import com.pgaot.datasheet.metadata.entity.TableEntity;

import java.util.Map;

final class SqlAstRewriter {

    String rewrite(SqlParsedQuery parsed, Map<String, TableEntity> tableMapByLowerName) {
        for (SQLStatement stmt : parsed.statements()) {
            rewriteStatement(stmt, tableMapByLowerName);
        }
        return SQLUtils.toSQLString(parsed.statements(), JdbcConstants.MYSQL);
    }

    private void rewriteStatement(SQLStatement stmt, Map<String, TableEntity> tableMapByLowerName) {
        if (stmt instanceof SQLSelectStatement s) {
            rewriteQuery(s.getSelect().getQuery(), tableMapByLowerName);
        } else if (stmt instanceof SQLInsertStatement s) {
            rewriteTableSource(s.getTableSource(), tableMapByLowerName);
            if (s.getQuery() != null) rewriteQuery(s.getQuery().getQuery(), tableMapByLowerName);
            if (s.getValuesList() != null)
                for (SQLInsertStatement.ValuesClause vc : s.getValuesList())
                    for (SQLExpr val : vc.getValues())
                        rewriteExpr(val, tableMapByLowerName);
        } else if (stmt instanceof SQLUpdateStatement s) {
            rewriteTableSource(s.getTableSource(), tableMapByLowerName);
            if (s.getWhere() != null) rewriteExpr(s.getWhere(), tableMapByLowerName);
            if (s.getItems() != null)
                for (SQLUpdateSetItem item : s.getItems())
                    rewriteExpr(item.getValue(), tableMapByLowerName);
        } else if (stmt instanceof SQLDeleteStatement s) {
            rewriteTableSource(s.getTableSource(), tableMapByLowerName);
            if (s.getWhere() != null) rewriteExpr(s.getWhere(), tableMapByLowerName);
        }
    }

    private void rewriteQuery(SQLSelectQuery query, Map<String, TableEntity> tableMapByLowerName) {
        if (query instanceof SQLSelectQueryBlock block) {
            rewriteTableSource(block.getFrom(), tableMapByLowerName);
            if (block.getWhere() != null) rewriteExpr(block.getWhere(), tableMapByLowerName);
            for (SQLSelectItem item : block.getSelectList()) {
                rewriteExpr(item.getExpr(), tableMapByLowerName);
            }
            if (block.getOrderBy() != null)
                for (SQLSelectOrderByItem ob : block.getOrderBy().getItems())
                    rewriteExpr(ob.getExpr(), tableMapByLowerName);
        } else if (query instanceof SQLUnionQuery union) {
            rewriteQuery(union.getLeft(), tableMapByLowerName);
            rewriteQuery(union.getRight(), tableMapByLowerName);
        }
    }

    private void rewriteExpr(SQLExpr expr, Map<String, TableEntity> tableMapByLowerName) {
        if (expr == null) return;
        if (expr instanceof SQLInSubQueryExpr sq)
            rewriteQuery(sq.getSubQuery().getQuery(), tableMapByLowerName);
        else if (expr instanceof SQLQueryExpr qe)
            rewriteQuery(qe.getSubQuery().getQuery(), tableMapByLowerName);
        else if (expr instanceof SQLExistsExpr ex)
            rewriteQuery(ex.getSubQuery().getQuery(), tableMapByLowerName);
        else if (expr instanceof SQLBinaryOpExpr bin) {
            rewriteExpr(bin.getLeft(), tableMapByLowerName);
            rewriteExpr(bin.getRight(), tableMapByLowerName);
        }
    }

    private void rewriteTableSource(SQLTableSource src, Map<String, TableEntity> tableMapByLowerName) {
        if (src == null) return;
        if (src instanceof SQLExprTableSource exprSource) {
            String logicalName = SqlTableExtractor.normalizeName(exprSource.getExpr().toString());
            TableEntity ownerTable = tableMapByLowerName.get(logicalName.toLowerCase());
            if (ownerTable != null) {
                exprSource.setExpr(new SQLIdentifierExpr(
                        TableManager.physicalName(ownerTable.getOwnerId(), logicalName)));
            }
        } else if (src instanceof SQLJoinTableSource join) {
            rewriteTableSource(join.getLeft(), tableMapByLowerName);
            rewriteTableSource(join.getRight(), tableMapByLowerName);
            if (join.getCondition() != null) {
                rewriteExpr(join.getCondition(), tableMapByLowerName);
            }
        } else if (src instanceof SQLSubqueryTableSource sub) {
            rewriteQuery(sub.getSelect().getQuery(), tableMapByLowerName);
        }
    }
}
