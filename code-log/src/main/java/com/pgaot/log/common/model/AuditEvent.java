package com.pgaot.log.common.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditEvent {
    private String userId;
    private String userName;
    private String tenantId;
    private String action;
    private String tableName;
    private Long rowId;
    private String beforeData;
    private String afterData;
    private String remark;
    private String traceId;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
