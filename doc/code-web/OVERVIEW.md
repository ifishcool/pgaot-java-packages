# code-web 架构总览

## 概述

REST API 层 — 统一响应体 + 全局异常 + Swagger 自动文档 + 注解驱动认证/审计。

## 架构图

```
请求
  │
  ▼
AuthAspect（AspectJ @Before）
  ├─ @RequiredAuth    → JWT 校验 → setAttribute(userId/nickname)
  └─ @RequiredScope   → API Key 校验
  │
  ▼
Controller（extends BaseController）
  │  getUserId() / getNickname() / getClientIp()
  │
  ├─ auth/AuthController    → login / validate / logout
  ├─ datasheet/TableController → CRUD 表
  ├─ DataController    → SQL / insert / update / delete / export
  └─ AuditController   → 查询审计日志
  │
  ▼
异常 → GlobalExceptionHandler → ApiResponse.fail(code, msg)
  │
  ▼
响应 → ApiResponse<T> {code, message, data, traceId}
```

## 核心设计

### 统一响应体

所有接口返回 `ApiResponse<T>`，成功 `ok(data)`，失败 `fail(code, msg)`。前端只需判断 `code == 0`。

### 注解驱动

| 注解 | 拦截器 | 效果 |
|---|---|---|
| `@RequiredAuth` | AuthInterceptor | JWT 校验，注入 userId |
| `@RequiredScope("s")` | AuthInterceptor | API Key + scope 校验 |
| `@Auditable(action, table)` | AuditableProxy | 审计日志自动记录 |

### 全局异常映射

```
LoginException     → 401 UNAUTHORIZED
AuthException      → 401 UNAUTHORIZED
SqlException       → 400 BAD_REQUEST
DatasheetException → 400 BAD_REQUEST
Exception          → 500 INTERNAL_SERVER_ERROR
```

## 目录结构

```
code-web/src/main/java/com/pgaot/web/
├── annotation/       @RequiredAuth / @RequiredScope / @Auditable
├── aspect/           AuthAspect（AOP 拦截）
├── proxy/            AuditableProxy（@Auditable 代理）
├── config/           SwaggerConfig（OpenAPI）
├── common/           ApiResponse + GlobalExceptionHandler
├── controller/       BaseController + auth/ datasheet/ audit/
├── param/auth/       LoginRequest（入参）
└── vo/               LoginVO（出参）
```

## 依赖

| 依赖 | 版本 | 用途 |
|---|---|---|
| code-auth | 1.0.0 | 登录认证 |
| code-datasheet | 1.0.0 | 数据表操作 |
| code-log | 1.0.0 | 审计日志 |
| spring-boot-starter-web | 3.4.1 | REST API |
| spring-boot-starter-aop | 3.4.1 | AspectJ AOP |
| knife4j-openapi3-jakarta | 4.5.0 | API 文档 (Knife4j) |
