package com.pgaot.datasheet.core;

import com.alibaba.druid.sql.ast.SQLStatement;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class SqlParsedQuery {

    private final String rawSql;
    private final List<SQLStatement> statements;
    private final Set<String> allNames;
    private final Set<String> targetNames;
    private final String targetOperation;
    private final boolean deleteOperation;

    SqlParsedQuery(String rawSql,
                   List<SQLStatement> statements,
                   Set<String> allNames,
                   Set<String> targetNames,
                   String targetOperation,
                   boolean deleteOperation) {
        this.rawSql = rawSql;
        this.statements = List.copyOf(statements);
        this.allNames = Set.copyOf(new LinkedHashSet<>(allNames));
        this.targetNames = Set.copyOf(new LinkedHashSet<>(targetNames));
        this.targetOperation = targetOperation;
        this.deleteOperation = deleteOperation;
    }

    String rawSql() { return rawSql; }

    List<SQLStatement> statements() { return statements; }

    Set<String> allNames() { return allNames; }

    Set<String> targetNames() { return targetNames; }

    String targetOperation() { return targetOperation; }

    boolean deleteOperation() { return deleteOperation; }
}
