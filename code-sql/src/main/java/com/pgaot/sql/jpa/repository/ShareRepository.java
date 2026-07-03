package com.pgaot.sql.jpa.repository;

import com.pgaot.sql.api.JpaTemplate;
import com.pgaot.sql.jpa.entity.DsShareEntity;

import java.util.List;

/** ds_share 仓储 */
public class ShareRepository {

    private final JpaTemplate jpa;

    public ShareRepository(JpaTemplate jpa) { this.jpa = jpa; }

    public void upsert(Long tableId, String from, String to,
                       boolean cs, boolean ci, boolean cu, boolean cd) {
        List<DsShareEntity> existing = jpa.query(
                "FROM DsShareEntity WHERE tableId = ?1 AND fromUser = ?2 AND toUser = ?3",
                DsShareEntity.class, tableId, from, to);
        DsShareEntity s;
        if (!existing.isEmpty()) {
            s = existing.getFirst();
        } else {
            s = new DsShareEntity();
            s.setTableId(tableId); s.setFromUser(from); s.setToUser(to);
        }
        s.setCanSelect(cs); s.setCanInsert(ci); s.setCanUpdate(cu); s.setCanDelete(cd);
        if (s.getId() != null) jpa.update(s); else jpa.save(s);
    }

    public void delete(Long tableId, String from, String to) {
        List<DsShareEntity> list = jpa.query(
                "FROM DsShareEntity WHERE tableId = ?1 AND fromUser = ?2 AND toUser = ?3",
                DsShareEntity.class, tableId, from, to);
        for (DsShareEntity s : list) jpa.delete(DsShareEntity.class, s.getId());
    }

    public DsShareEntity get(Long tableId, String toUser) {
        List<DsShareEntity> list = jpa.query(
                "FROM DsShareEntity WHERE tableId = ?1 AND toUser = ?2",
                DsShareEntity.class, tableId, toUser);
        return list.isEmpty() ? null : list.getFirst();
    }

    public List<DsShareEntity> listByTable(Long tableId) {
        return jpa.query("FROM DsShareEntity WHERE tableId = ?1",
                DsShareEntity.class, tableId);
    }

    public List<Long> getSharedTableIds(String userId) {
        return jpa.query("SELECT s.tableId FROM DsShareEntity s WHERE s.toUser = ?1",
                Long.class, userId);
    }

    public void deleteByTable(Long tableId) {
        for (DsShareEntity s : listByTable(tableId))
            jpa.delete(DsShareEntity.class, s.getId());
    }
}
