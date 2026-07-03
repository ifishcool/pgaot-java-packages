package com.pgaot.datasheet.metadata;

import com.pgaot.datasheet.exception.DatasheetException;
import com.pgaot.datasheet.metadata.entity.ShareEntity;
import com.pgaot.datasheet.metadata.entity.TableEntity;
import com.pgaot.sql.api.JpaTemplate;
import com.pgaot.sql.api.SqlTemplate;
import com.pgaot.sql.jpa.entity.DsTableEntity;
import com.pgaot.sql.jpa.repository.ShareRepository;
import com.pgaot.sql.jpa.repository.TableRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 元数据存储 — 委托 code-sql 的 JPA Repository 管理 ds_table / ds_share.
 */
public class MetadataStore {

    private final SqlTemplate sql;          // 仅用于 INFORMATION_SCHEMA 查询
    private final TableRepository tableRepo;
    private final ShareRepository shareRepo;

    public MetadataStore(SqlTemplate sql, JpaTemplate metaJpa) {
        this.sql = sql;
        this.tableRepo = new TableRepository(metaJpa);
        this.shareRepo = new ShareRepository(metaJpa);
    }

    // ===== ds_table (委托 TableRepository) =====

    public TableEntity insertTable(TableEntity t) {
        DsTableEntity e = new DsTableEntity();
        e.setName(t.getName()); e.setTitle(t.getTitle());
        e.setOwnerId(t.getOwnerId()); e.setDescription(t.getDescription());
        e.setMode(t.getMode() != null ? t.getMode() : "ALL");
        tableRepo.save(e);
        t.setId(e.getId());
        return t;
    }

    public TableEntity getTable(Long id) {
        DsTableEntity e = tableRepo.findById(id);
        return e == null ? null : toTableEntity(e);
    }

    public TableEntity getTableByName(String ownerId, String name) {
        DsTableEntity e = tableRepo.findByName(ownerId, name);
        return e == null ? null : toTableEntity(e);
    }

    public List<TableEntity> listByUser(String userId) {
        Set<Long> ids = new LinkedHashSet<>();
        for (DsTableEntity e : tableRepo.listByOwner(userId))
            ids.add(e.getId());
        ids.addAll(shareRepo.getSharedTableIds(userId));

        List<TableEntity> result = new ArrayList<>();
        for (Long id : ids) {
            DsTableEntity e = tableRepo.findById(id);
            if (e != null) result.add(toTableEntity(e));
        }
        return result;
    }

    public void dropTable(Long id) {
        shareRepo.deleteByTable(id);
        tableRepo.delete(id);
    }

    public void updateTable(Long id, String name, String title) {
        tableRepo.update(id, name, title);
    }

    public void setMode(Long id, String mode) {
        tableRepo.setMode(id, mode);
    }

    // ===== ds_share (委托 ShareRepository) =====

    public void upsertShare(Long tableId, String from, String to,
                            boolean cs, boolean ci, boolean cu, boolean cd) {
        shareRepo.upsert(tableId, from, to, cs, ci, cu, cd);
    }

    public void deleteShare(Long tableId, String from, String to) {
        shareRepo.delete(tableId, from, to);
    }

    public ShareEntity getShare(Long tableId, String userId) {
        var s = shareRepo.get(tableId, userId);
        return s == null ? null : toShareEntity(s);
    }

    public List<ShareEntity> getSharesByTable(Long tableId) {
        return shareRepo.listByTable(tableId).stream()
                .map(this::toShareEntity).collect(Collectors.toList());
    }

    public List<Long> getSharedTableIds(String userId) {
        return shareRepo.getSharedTableIds(userId);
    }

    // ===== INFORMATION_SCHEMA（保留原生 SQL） =====

    public List<Map<String, Object>> getColumns(String physicalTable) {
        try {
            return sql.sql("SELECT COLUMN_NAME AS name, DATA_TYPE AS type, "
                    + "IS_NULLABLE AS nullable FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? "
                    + "AND COLUMN_NAME != 'id' ORDER BY ORDINAL_POSITION", physicalTable);
        } catch (Exception e) {
            throw DatasheetException.sqlOperationDenied("metadata query: " + e.getMessage());
        }
    }

    private TableEntity toTableEntity(DsTableEntity e) {
        TableEntity t = new TableEntity();
        t.setId(e.getId()); t.setName(e.getName()); t.setTitle(e.getTitle());
        t.setOwnerId(e.getOwnerId()); t.setDescription(e.getDescription());
        t.setMode(e.getMode());
        return t;
    }

    private ShareEntity toShareEntity(com.pgaot.sql.jpa.entity.DsShareEntity s) {
        ShareEntity se = new ShareEntity();
        se.setTableId(s.getTableId()); se.setFromUser(s.getFromUser());
        se.setToUser(s.getToUser());
        se.setCanSelect(s.isCanSelect()); se.setCanInsert(s.isCanInsert());
        se.setCanUpdate(s.isCanUpdate()); se.setCanDelete(s.isCanDelete());
        return se;
    }
}
