package com.pgaot.web.controller.audit;

import com.pgaot.sql.api.JpaTemplate;
import com.pgaot.sql.jpa.entity.AuditLogEntity;
import com.pgaot.sql.jpa.repository.AuditLogRepository;
import com.pgaot.web.common.ApiResponse;
import com.pgaot.web.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "审计日志")
@RestController
@RequestMapping("/api/audit")
public class AuditController extends BaseController {

    private volatile AuditLogRepository repo;

    private AuditLogRepository repo() {
        if (repo == null) {
            synchronized (this) {
                if (repo == null) {
                    repo = new AuditLogRepository(
                            JpaTemplate.fromEnv("", true, AuditLogEntity.class));
                }
            }
        }
        return repo;
    }

    @Operation(summary = "按用户查询审计日志")
    @GetMapping("/user/{userId}")
    public ApiResponse<List<AuditLogEntity>> listByUser(@PathVariable("userId") String userId,
                                                         @RequestParam(name = "limit", defaultValue = "100") int limit) {
        return ApiResponse.ok(repo().listByUser(userId, limit));
    }

    @Operation(summary = "按表查询审计日志")
    @GetMapping("/table/{tableName}")
    public ApiResponse<List<AuditLogEntity>> listByTable(@PathVariable("tableName") String tableName,
                                                          @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return ApiResponse.ok(repo().listByTable(tableName, limit));
    }

    @Operation(summary = "按 traceId 查询审计日志")
    @GetMapping("/trace/{traceId}")
    public ApiResponse<List<AuditLogEntity>> listByTraceId(@PathVariable("traceId") String traceId) {
        return ApiResponse.ok(repo().listByTraceId(traceId));
    }
}
