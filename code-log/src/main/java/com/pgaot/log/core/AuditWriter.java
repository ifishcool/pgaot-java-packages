package com.pgaot.log.core;

import com.pgaot.log.common.model.AuditEvent;
import com.pgaot.log.exception.LogException;
import com.pgaot.sql.jpa.entity.AuditLogEntity;
import com.pgaot.sql.jpa.repository.AuditLogRepository;

/**
 * 审计日志写入器 — 将 AuditEvent 持久化到 audit_log 表（委托 code-sql JPA）.
 */
public class AuditWriter {

    private final AuditLogRepository repo;

    public AuditWriter(AuditLogRepository repo) { this.repo = repo; }

    public void write(AuditEvent event) {
        try {
            AuditLogEntity e = new AuditLogEntity();
            e.setUserId(event.getUserId());
            e.setUserName(event.getUserName());
            e.setTenantId(event.getTenantId());
            e.setAction(event.getAction());
            e.setTableName(event.getTableName());
            e.setRowId(event.getRowId());
            e.setBeforeData(event.getBeforeData());
            e.setAfterData(event.getAfterData());
            e.setRemark(event.getRemark());
            e.setTraceId(event.getTraceId());
            repo.save(e);
        } catch (Exception e) {
            throw LogException.auditWriteFailed(e.getMessage());
        }
    }
}
