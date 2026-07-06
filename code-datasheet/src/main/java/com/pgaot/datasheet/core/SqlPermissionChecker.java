package com.pgaot.datasheet.core;

import com.pgaot.datasheet.common.constants.Messages;
import com.pgaot.datasheet.exception.DatasheetException;
import com.pgaot.datasheet.metadata.MetadataStore;
import com.pgaot.datasheet.metadata.entity.ShareEntity;
import com.pgaot.datasheet.metadata.entity.TableEntity;

import java.util.HashMap;
import java.util.Map;

final class SqlPermissionChecker {

    private final MetadataStore store;

    SqlPermissionChecker(MetadataStore store) {
        this.store = store;
    }

    PermissionResult validate(String userId, SqlParsedQuery parsed) {
        Map<String, TableEntity> owned = new HashMap<>();
        Map<String, ShareEntity> shared = new HashMap<>();
        for (TableEntity t : store.listByUser(userId)) {
            String key = t.getName().toLowerCase();
            if (t.getOwnerId().equals(userId)) owned.put(key, t);
            else {
                ShareEntity se = store.getShare(t.getId(), userId);
                if (se != null) shared.put(key, se);
            }
        }
        if (owned.isEmpty() && shared.isEmpty()) {
            throw DatasheetException.tableNotFound(parsed.rawSql());
        }

        Map<String, TableEntity> tableMapByLowerName = new HashMap<>();
        for (String name : parsed.allNames()) {
            String lowerName = name.toLowerCase();
            TableEntity ownerTable = owned.get(lowerName);
            ShareEntity share = shared.get(lowerName);
            if (ownerTable == null && share == null) {
                throw DatasheetException.tableNotFound(name + Messages.TABLE_NO_ACCESS);
            }

            boolean isTarget = parsed.targetNames().contains(name);
            String opForCheck = isTarget ? parsed.targetOperation() : "SELECT";
            boolean deleteCheck = isTarget && parsed.deleteOperation();

            if (share != null && ownerTable == null) {
                if (!checkSharedPerm(share, opForCheck)) {
                    throw DatasheetException.sqlOperationDenied("共享权限不足: " + name);
                }
                ownerTable = store.getTable(share.getTableId());
                if (ownerTable != null) {
                    checkMode(ownerTable, opForCheck, deleteCheck, name);
                }
            } else {
                checkMode(ownerTable, opForCheck, deleteCheck, name);
            }

            if (ownerTable != null) {
                tableMapByLowerName.put(lowerName, ownerTable);
            }
        }

        return new PermissionResult(tableMapByLowerName);
    }

    private void checkMode(TableEntity table, String op, boolean deleteCheck, String name) {
        if (table == null) return;
        String mode = table.getMode() != null ? table.getMode() : "ALL";
        switch (mode) {
            case "READ_ONLY":
                if (!"SELECT".equals(op)) {
                    throw DatasheetException.sqlOperationDenied(String.format(Messages.MODE_READ_ONLY, name));
                }
                break;
            case "WRITE_ONLY":
                if ("SELECT".equals(op)) {
                    throw DatasheetException.sqlOperationDenied(String.format(Messages.MODE_WRITE_ONLY, name));
                }
                if (deleteCheck) {
                    throw DatasheetException.sqlOperationDenied(String.format(Messages.MODE_DELETE_BLOCKED, name));
                }
                break;
        }
    }

    private boolean checkSharedPerm(ShareEntity share, String op) {
        return switch (op) {
            case "SELECT" -> share.isCanSelect();
            case "INSERT" -> share.isCanInsert();
            case "UPDATE" -> share.isCanUpdate();
            case "DELETE" -> share.isCanDelete();
            default -> false;
        };
    }

    record PermissionResult(Map<String, TableEntity> tableMapByLowerName) {}
}
