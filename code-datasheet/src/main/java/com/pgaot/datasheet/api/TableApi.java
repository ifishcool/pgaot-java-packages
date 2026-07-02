package com.pgaot.datasheet.api;

import com.pgaot.datasheet.common.model.*;
import com.pgaot.datasheet.core.TableManager;
import com.pgaot.datasheet.exception.DatasheetException;
import com.pgaot.datasheet.metadata.MetadataStore;
import com.pgaot.datasheet.metadata.entity.ColumnEntity;
import com.pgaot.datasheet.metadata.entity.TableEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 表管理 API — 建表、删表、修改表结构、模式控制.
 *
 * <p>所有表结构操作需 owner 身份，非 owner 抛 {@link DatasheetException#notOwner()}.
 *
 * <h3>列类型映射</h3>
 * STRING→VARCHAR(512), NUMBER→DECIMAL(20,4), DATE→DATETIME, BOOLEAN→TINYINT(1)
 */
public class TableApi {

    private final TableManager tableManager;
    private final MetadataStore store;

    TableApi(TableManager tableManager, MetadataStore store) {
        this.tableManager = tableManager;
        this.store = store;
    }

    /**
     * 创建数据表.
     *
     * @param ownerId     创建者，物理表名 = ownerId_name
     * @param name        逻辑表名（用户可见）
     * @param title       显示名称（可选）
     * @param description 描述（可选）
     * @param columns     列定义，至少一列
     * @return 创建后的表信息（含 id）
     * @throws DatasheetException TABLE_NAME_DUPLICATE 表名重复
     */
    public TableInfo create(String ownerId, String name, String title, String description, List<ColumnInfo> columns) {
        return toTableInfo(tableManager.createTable(ownerId, name, title, description, columns));
    }

    /** 删除表（含元数据），需 owner */
    public void drop(String ownerId, String tableId) {
        checkOwner(ownerId, tableId);
        tableManager.dropTable(ownerId, parseId(tableId));
    }

    /** 重命名表，需 owner */
    public void rename(String ownerId, String tableId, String newName) {
        checkOwner(ownerId, tableId);
        tableManager.renameTable(ownerId, parseId(tableId), newName);
    }

    /** 清空表（保留结构），需 owner */
    public void truncate(String ownerId, String tableId) {
        checkOwner(ownerId, tableId);
        tableManager.truncate(ownerId, parseId(tableId));
    }

    /** 增加列，需 owner */
    public void addColumn(String ownerId, String tableId, ColumnInfo column) {
        checkOwner(ownerId, tableId);
        tableManager.addColumn(ownerId, parseId(tableId), column);
    }

    /** 删除列（必填列不可删），需 owner */
    public void dropColumn(String ownerId, String tableId, String columnName) {
        checkOwner(ownerId, tableId);
        tableManager.dropColumn(ownerId, parseId(tableId), columnName);
    }

    /** 重命名列，需 owner */
    public void renameColumn(String ownerId, String tableId, String oldName, String newName) {
        checkOwner(ownerId, tableId);
        tableManager.renameColumn(ownerId, parseId(tableId), oldName, newName);
    }

    /** 查看该租户有权限的所有表 */
    public List<TableInfo> list(String userId) {
        return store.listByUser(userId).stream().map(this::toTableInfo).collect(Collectors.toList());
    }

    /** 查看单表结构（含列定义） */
    public TableInfo get(String tableId) {
        TableEntity t = store.getTable(parseId(tableId));
        return t == null ? null : toTableInfo(t);
    }

    /**
     * 设置表模式.
     *
     * @param mode READ_ONLY / WRITE_ONLY / READ_WRITE
     */
    public void setMode(String ownerId, String tableId, TableMode mode) {
        checkOwner(ownerId, tableId);
        store.setMode(parseId(tableId), mode.name());
    }

    // === helpers ===

    private TableInfo toTableInfo(TableEntity t) {
        TableInfo info = new TableInfo();
        info.setId(String.valueOf(t.getId()));
        info.setName(t.getName()); info.setTitle(t.getTitle());
        info.setOwnerId(t.getOwnerId()); info.setDescription(t.getDescription());
        List<ColumnEntity> cols = store.getColumns(t.getId());
        info.setColumns(cols.stream().map(c -> new ColumnInfo(c.getName(),
                ColumnType.valueOf(c.getType()), c.isRequired())).collect(Collectors.toList()));
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
