# code-log 架构总览

## 概述

结构化日志 + 审计日志 + 链路上下文。三件事一口完成。

## 架构图

```
请求进入
    │
    ▼
LogContext.init(userId, userName, tenantId)
    │  ├─ 生成 traceId (16位 UUID)
    │  ├─ ThreadLocal 存 userId/userName/tenantId/traceId
    │  └─ SLF4J MDC 写入 traceId/userId/tenantId
    │
    ├─ 业务代码 → StructuredLogger.info("操作成功")
    │              └─ SLF4J 自动注入 MDC 变量 → 日志含 traceId
    │
    ├─ 写操作 → AuditLogger.record(event)
    │              └─ AuditWriter.write() → AuditLogRepository → audit_log 表
    │
    └─ 请求结束 → LogContext.clear()
                   └─ ThreadLocal.remove() + MDC.clear()
```

## 三件事

| 功能 | 入口 | 核心类 | 存储 |
|---|---|---|---|
| 链路上下文 | `LogContext.init()` | ThreadLocal + MDC | 内存 |
| 结构化日志 | `StructuredLogger.of()` | SLF4J | 日志文件 |
| 审计日志 | `AuditLogger.record()` | AuditWriter | `audit_log` 表 |

## 审计日志表

```sql
CREATE TABLE audit_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     VARCHAR(64)  NOT NULL,
    user_name   VARCHAR(128),
    tenant_id   VARCHAR(64),
    action      VARCHAR(32)  NOT NULL,   -- INSERT / UPDATE / DELETE / EXPORT / LOGIN
    table_name  VARCHAR(128),
    row_id      BIGINT,
    before_data TEXT,                     -- 变更前 JSON
    after_data  TEXT,                     -- 变更后 JSON
    remark      TEXT,
    trace_id    VARCHAR(64),
    created_at  DATETIME NOT NULL,
    INDEX idx_log_user_id (user_id),
    INDEX idx_log_action (action),
    INDEX idx_log_created_at (created_at)
);
```

## 目录结构

```
code-log/src/main/java/com/pgaot/log/
├── api/
│   ├── LogContext.java          # traceId/userId/tenantId 上下文
│   └── AuditLogger.java         # 审计日志入口
├── core/
│   ├── StructuredLogger.java    # SLF4J + LogContext 自动注入
│   └── AuditWriter.java         # 委托 code-sql 持久化
├── annotation/
│   └── Auditable.java           # @Auditable 方法级注解
├── common/
│   ├── code/IResultCode.java     # 错误码接口
│   ├── code/ErrorCode.java      # 40_xxx_xxx
│   ├── constants/Messages.java  # 提示信息
│   └── model/AuditEvent.java    # 审计事件模型（Lombok Builder）
└── exception/LogException.java   # 2 个静态工厂
```

## 核心类速查

| 类 | 层级 | 职责 |
|---|---|---|
| `LogContext` | api | init/clear，ThreadLocal + MDC 双写 |
| `AuditLogger` | api | 构建 AuditEvent → 调 AuditWriter |
| `StructuredLogger` | core | SLF4J 封装，自动从 LogContext 取上下文 |
| `AuditWriter` | core | AuditEvent → AuditLogEntity → AuditLogRepository |
| `Auditable` | annotation | 标记方法需自动审计 |
| `AuditEvent` | model | userId/action/tableName/beforeData/afterData/traceId |

## 依赖

| 依赖 | 版本 | 用途 |
|---|---|---|
| code-sql | 1.0.0 | JPA 持久化 (AuditLogRepository) |
| slf4j-api | 2.0.17 | 日志门面 + MDC |
| lombok | 1.18.46 | 编译期代码生成（provided） |
| JUnit Jupiter | 5.11.4 | 单元测试（test） |
