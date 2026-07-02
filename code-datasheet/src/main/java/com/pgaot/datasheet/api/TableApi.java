package com.pgaot.datasheet.api;

import com.pgaot.datasheet.common.model.*;
import com.pgaot.datasheet.core.TableManager;
import com.pgaot.datasheet.exception.DatasheetException;
import com.pgaot.datasheet.metadata.MetadataStore;
import com.pgaot.datasheet.metadata.entity.ShareEntity;
import com.pgaot.datasheet.metadata.entity.TableEntity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 表管理 API — 建表、删表、修改表结构、模式控制.
 */
public class TableApi {

    private final TableManager tableManager;
    private final MetadataStore store;

    TableApi(TableManager tableManager, MetadataStore store) {
        this.tableManager = tableManager;
        this.store = store;
    }

    /** 创建数据表 */
    public TableInfo create(String ownerId, String name, String title, String description, List<ColumnInfo> columns) {
        return toTableInfo(tableManager.createTable(ownerId, name, title, description, columns), ownerId, name);
    }

    public void drop(String ownerId, String tableId) {
        checkOwner(ownerId, tableId);
        tableManager.dropTable(ownerId, parseId(tableId));
    }

    public void rename(String ownerId, String tableId, String newName) {
        checkOwner(ownerId, tableId);
        tableManager.renameTable(ownerId, parseId(tableId), newName);
    }

    public void truncate(String ownerId, String tableId) {
        checkOwner(ownerId, tableId);
        tableManager.truncate(ownerId, parseId(tableId));
    }

    public void addColumn(String ownerId, String tableId, ColumnInfo column) {
        checkOwner(ownerId, tableId);
        tableManager.addColumn(ownerId, parseId(tableId), column);
    }

    public void dropColumn(String ownerId, String tableId, String columnName) {
        checkOwner(ownerId, tableId);
        tableManager.dropColumn(ownerId, parseId(tableId), columnName);
    }

    public void renameColumn(String ownerId, String tableId, String oldName, String newName) {
        checkOwner(ownerId, tableId);
        tableManager.renameColumn(ownerId, parseId(tableId), oldName, newName);
    }

    /** 恢复软删除 */
    public void restore(String ownerId, String tableId) {
        checkOwner(ownerId, tableId);
        tableManager.restoreTable(parseId(tableId));
    }

    /** 物理删除（不可恢复） */
    public void purge(String ownerId, String tableId) {
        checkOwner(ownerId, tableId);
        tableManager.purgeTable(ownerId, parseId(tableId));
    }

    /** 查看该租户有权限的所有表（不含已删除） */
    public List<TableInfo> list(String userId) {
        return store.listByUser(userId).stream()
                .map(t -> toTableInfo(t, userId, t.getName())).collect(Collectors.toList());
    }

    /** 查看单表结构（列信息从 INFORMATION_SCHEMA 实时读取） */
    public TableInfo get(String tableId) {
        TableEntity t = store.getTable(parseId(tableId));
        return t == null ? null : toTableInfo(t, t.getOwnerId(), t.getName());
    }

    public void setMode(String ownerId, String tableId, TableMode mode) {
        checkOwner(ownerId, tableId);
        store.setMode(parseId(tableId), mode.name());
    }

    /** 返回所有表 + 来源信息（自己的表 / 从谁共享来的） */
    public List<TableWithSource> listWithSource(String userId) {
        return store.listByUser(userId).stream().map(t -> {
            TableWithSource tws = new TableWithSource();
            tws.setTableInfo(toTableInfo(t, t.getOwnerId(), t.getName()));
            if (t.getOwnerId().equals(userId)) {
                tws.setSource("OWNED");
            } else {
                tws.setSource("SHARED");
                tws.setFromUser(t.getOwnerId());
                ShareEntity s = store.getShare(t.getId(), userId);
                if (s != null) tws.setPermission(new SharePermission(
                        s.isCanSelect(), s.isCanInsert(), s.isCanUpdate(), s.isCanDelete()));
            }
            return tws;
        }).collect(Collectors.toList());
    }

    /** 表 + 来源信息 */
    public static class TableWithSource {
        private TableInfo tableInfo;
        private String source;      // "OWNED" / "SHARED"
        private String fromUser;    // 共享自谁（source=SHARED 时有值）
        private SharePermission permission;
        public TableInfo getTableInfo() { return tableInfo; }
        public void setTableInfo(TableInfo v) { tableInfo = v; }
        public String getSource() { return source; }
        public void setSource(String v) { source = v; }
        public String getFromUser() { return fromUser; }
        public void setFromUser(String v) { fromUser = v; }
        public SharePermission getPermission() { return permission; }
        public void setPermission(SharePermission v) { permission = v; }
    }

    // === helpers ===

    private TableInfo toTableInfo(TableEntity t, String ownerId, String name) {
        TableInfo info = new TableInfo();
        info.setId(String.valueOf(t.getId()));
        info.setName(t.getName()); info.setTitle(t.getTitle());
        info.setOwnerId(t.getOwnerId()); info.setDescription(t.getDescription());
        info.setMode(t.getMode());
        // 从 INFORMATION_SCHEMA 实时查列
        String physical = TableManager.physicalName(ownerId, name);
        info.setColumns(store.getColumns(physical));
        return info;
    }

    private void checkOwner(String userId, String tableId) {
        TableEntity t = store.getTable(parseId(tableId));
        if (t == null) throw DatasheetException.tableNotFound(tableId);
        if (!t.getOwnerId().equals(userId)) throw DatasheetException.notOwner();
    }

    private long parseId(String id) {
        try { return Long.parseLong(id); } catch (NumberFormatException e) { return 0L; }
    }
}
