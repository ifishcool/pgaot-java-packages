package com.pgaot.sql.jpa.repository;

import com.pgaot.sql.api.JpaTemplate;
import com.pgaot.sql.jpa.entity.DsTableEntity;

import java.util.List;

/** ds_table 仓储 */
public class TableRepository {

    private final JpaTemplate jpa;

    public TableRepository(JpaTemplate jpa) { this.jpa = jpa; }

    public DsTableEntity save(DsTableEntity t) { jpa.save(t); return t; }
    public DsTableEntity findById(Long id) { return jpa.findById(DsTableEntity.class, id); }

    public DsTableEntity findByName(String ownerId, String name) {
        List<DsTableEntity> list = jpa.query(
                "FROM DsTableEntity WHERE ownerId = ?1 AND name = ?2",
                DsTableEntity.class, ownerId, name);
        return list.isEmpty() ? null : list.getFirst();
    }

    public List<DsTableEntity> listByOwner(String ownerId) {
        return jpa.query("FROM DsTableEntity WHERE ownerId = ?1",
                DsTableEntity.class, ownerId);
    }

    public void update(Long id, String name, String title) {
        DsTableEntity t = findById(id);
        if (t != null) { t.setName(name); t.setTitle(title); jpa.update(t); }
    }

    public void setMode(Long id, String mode) {
        DsTableEntity t = findById(id);
        if (t != null) { t.setMode(mode); jpa.update(t); }
    }

    public void delete(Long id) { jpa.delete(DsTableEntity.class, id); }
}
