# PGAOT Code Log

[![JDK](https://img.shields.io/badge/JDK-21%2B-blue)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-GPL--3.0-green)](LICENSE)

结构化日志 + 审计日志 + 链路上下文（traceId/userId/tenantId）。

---

## 环境要求

- JDK 21+
- code-sql 1.0.0+

## 安装

```xml
<dependency>
    <groupId>com.pgaot</groupId>
    <artifactId>code-log</artifactId>
    <version>1.0.0</version>
</dependency>
```

运行时需要 SLF4J 实现（如 Logback）：

```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.5.18</version>
</dependency>
```

---

## 快速开始

```java
import com.pgaot.log.api.LogContext;
import com.pgaot.log.api.AuditLogger;
import com.pgaot.log.core.AuditWriter;
import com.pgaot.log.core.StructuredLogger;
import com.pgaot.log.common.model.AuditEvent;
import com.pgaot.sql.api.JpaTemplate;
import com.pgaot.sql.jpa.entity.AuditLogEntity;
import com.pgaot.sql.jpa.repository.AuditLogRepository;

// 1. 初始化 Writer（应用启动时一次）
JpaTemplate jpa = JpaTemplate.fromEnv("", true, AuditLogEntity.class);
AuditLogger.configure(new AuditWriter(new AuditLogRepository(jpa)));

// 2. 请求进来 → 初始化上下文
LogContext.init("alice", "Alice", "tenant-1");
// MDC: {traceId=a1b2c3d4, userId=alice, tenantId=tenant-1}

// 3. 结构化日志 → 自动带 traceId
StructuredLogger log = StructuredLogger.of(MyService.class);
log.info("用户 {} 修改了订单 {}", "alice", 123);
// 输出: [INFO] 用户 alice 修改了订单 123  [traceId=a1b2c3d4 userId=alice]

// 4. 审计日志 → 持久化到 audit_log 表
AuditLogger.log(AuditEvent.builder()
    .userId("alice")
    .userName("Alice")
    .action("UPDATE")
    .tableName("orders")
    .rowId(123L)
    .beforeData("{\"status\":\"pending\"}")
    .afterData("{\"status\":\"paid\"}")
    .build());

// 5. 请求结束 → 清理上下文
LogContext.clear();
```

---

## API 参考

### LogContext

| 方法 | 说明 |
|---|---|
| `init(userId, userName, tenantId)` | 初始化上下文，生成 traceId，写入 MDC |
| `initTrace()` | 仅设 traceId（非 HTTP 场景） |
| `clear()` | 清理 ThreadLocal + MDC |
| `getTraceId()` / `getUserId()` / `getUserName()` / `getTenantId()` | 读上下文 |

### AuditLogger

| 方法 | 说明 |
|---|---|
| `configure(AuditWriter)` | 注入 Writer（启动时调用） |
| `log(AuditEvent)` | 写入审计日志，自动补 traceId |

### StructuredLogger

| 方法 | 说明 |
|---|---|
| `of(Class)` | 创建 Logger，自动带 MDC 上下文 |
| `info/warn/error/debug(msg, args...)` | 常规日志 |

### AuditLogRepository

| 方法 | 说明 |
|---|---|
| `save(AuditLogEntity)` | 写入一条审计记录 |
| `listByUser(userId, limit)` | 查某用户最近操作 |
| `listByTable(tableName, limit)` | 查某张表最近变更 |
| `listByTraceId(traceId)` | 查一条链路所有操作 |

### AuditEvent（Builder 模式）

| 字段 | 说明 |
|---|---|
| `userId` | 操作人 ID（必填） |
| `userName` | 操作人昵称 |
| `tenantId` | 租户 ID |
| `action` | INSERT / UPDATE / DELETE / EXPORT / LOGIN |
| `tableName` | 目标表名 |
| `rowId` | 目标行 ID |
| `beforeData` | 变更前数据（JSON） |
| `afterData` | 变更后数据（JSON） |
| `remark` | 补充说明 |

---

## 数据库查询

```sql
-- 谁改了这行数据
SELECT user_id, user_name, before_data, after_data, created_at
FROM audit_log WHERE table_name = 'scores' AND row_id = 123;

-- 某用户今天干了什么
SELECT * FROM audit_log
WHERE user_id = 'alice' AND DATE(created_at) = CURDATE();

-- 按 traceId 追踪整条链路
SELECT * FROM audit_log WHERE trace_id = 'a1b2c3d4';

-- 谁删了数据（含删除前的内容）
SELECT user_id, table_name, row_id, before_data, created_at
FROM audit_log WHERE action = 'DELETE' ORDER BY created_at DESC;
```

---

## 集成 Servlet Filter

```java
public class LogFilter implements Filter {
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        try {
            LogContext.init(getUserId(req), getUserName(req), getTenantId(req));
            chain.doFilter(req, res);
        } finally {
            LogContext.clear();
        }
    }
}
```

## 项目结构

```
code-log/src/main/java/com/pgaot/log/
├── api/
│   ├── LogContext.java          # traceId/userId/tenantId 上下文
│   └── AuditLogger.java         # 审计日志入口
├── core/
│   ├── StructuredLogger.java    # SLF4J + MDC 封装
│   └── AuditWriter.java         # 委托 code-sql JPA 持久化
├── annotation/
│   └── Auditable.java           # 方法级审计注解
├── common/
│   ├── code/ErrorCode.java      # 40_xxx_xxx
│   ├── code/IResultCode.java
│   ├── constants/Messages.java
│   └── model/AuditEvent.java    # 审计事件（Lombok Builder）
└── exception/LogException.java  # 异常（2 个工厂方法）
```

