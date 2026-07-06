package com.pgaot.sql.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_log_user_id", columnList = "user_id"),
    @Index(name = "idx_log_action", columnList = "action"),
    @Index(name = "idx_log_created_at", columnList = "created_at")
})
public class AuditLogEntity {

    @Setter @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Setter @Column(name = "user_id", nullable = false, length = 64) private String userId;
    @Setter @Column(name = "user_name", length = 128) private String userName;
    @Setter @Column(name = "tenant_id", length = 64) private String tenantId;
    @Setter @Column(nullable = false, length = 32) private String action;
    @Setter @Column(name = "table_name", length = 128) private String tableName;
    @Setter @Column(name = "row_id") private Long rowId;
    @Setter @Column(name = "before_data", columnDefinition = "TEXT") private String beforeData;
    @Setter @Column(name = "after_data", columnDefinition = "TEXT") private String afterData;
    @Setter @Column(columnDefinition = "TEXT") private String remark;
    @Setter @Column(name = "trace_id", length = 64) private String traceId;

    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
