# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

PGAOT Java 二方包 Monorepo — 父 POM 统一版本管理，子模块独立发布。

```
PGAOT_JAVA_PACKAGE/
├── pom.xml           # Parent POM — dependencyManagement + pluginManagement
├── code-auth/        # Authentication (JWT + Redis + strategy + API tokens)
├── code-sql/         # SQL engine (Druid firewall + JPA + multi-datasource)
├── code-datasheet/   # Multi-tenant datasheet (prefix isolation + sharing + Jackson)
├── code-log/         # Structured logging + audit log + trace context
├── doc/              # Developer documentation
├── install-local.sh  # Install jars to ~/.m2 for local development
└── .github/workflows/
    └── maven-publish.yml  # Tag-driven: test → ErrorCode → publish
```

**Dependency chain**: `code-sql` ← `code-auth`, `code-sql` ← `code-datasheet`, `code-sql` ← `code-log`. All versions managed in root `pom.xml` via `<dependencyManagement>`. Child POMs declare only `<artifactId>`, no `<version>`.

## Build & Test

Each project is self-contained. Tests auto-load `.env` from the module root directory — no manual `export` needed.

**Local development**: When modifying a downstream dependency (e.g., `code-sql`), use `install-local.sh` to install it to `~/.m2` so upstream modules pick up the change without publishing to GitHub Packages:

```bash
./install-local.sh code-sql    # install single module to ~/.m2
./install-local.sh             # install all modules in dependency order
```

```bash
# Run all tests (unit + integration with .env)
cd code-sql && mvn test
cd code-auth && mvn test
cd code-datasheet && mvn test

# Run single test class
mvn test -Dtest=PageQueryTest

# Run only integration tests
mvn test -Dgroups=integration
```

Run ErrorCode dedup check (mandatory before release):

```bash
mvn compile && java -cp target/classes com.pgaot.<module>.common.code.ErrorCode
```

## Error Code Allocation (CRITICAL)

Each module owns a 100,000-range block. Adding a module requires registering its range in `doc/GLOBAL.md` BEFORE coding.

| Range        | Module         |
| ------------ | -------------- |
| `10_xxx_xxx` | code-auth      |
| `20_xxx_xxx` | code-sql       |
| `30_xxx_xxx` | code-datasheet |

Within a module, sub-ranges are allocated by the middle 3 digits (e.g., `20_001` = connection, `20_002` = SQL execution). Every `ErrorCode` enum must have a `main()` method that checks for duplicates.

## Package Conventions

```
com.pgaot.<module>.<layer>
```

| Layer              | Visibility                | Purpose                                     |
| ------------------ | ------------------------- | ------------------------------------------- |
| `api`              | public                    | User-facing entry points                    |
| `api/model`        | public                    | Return value DTOs                           |
| `core`             | package-private preferred | Internal engine                             |
| `common/code`      | public                    | `IResultCode` + `ErrorCode`                 |
| `common/config`    | public                    | Configuration classes                       |
| `common/constants` | public                    | `Messages` + numeric constants              |
| `exception`        | public                    | Exception class with static factory methods |

## Exception Pattern

Each module has exactly one exception class. Error codes are encapsulated in static factory methods; callers must not use `new Exception(ErrorCode.XXX)`.

```java
// Correct
throw LoginException.tokenKicked();
throw SqlException.wallBlocked(e.getMessage());

// Wrong
throw new LoginException(ErrorCode.TOKEN_KICKED, "xxx");
```

All user-facing hardcoded strings belong in `Messages.java`. Numeric constants (defaults, limits, timeouts) go in `XxxConstants.java` with inner class grouping (e.g., `SqlConstants.Pool.DEFAULT_MAX_ACTIVE`). Environment variable keys are centralized: `AuthConstants.Env.*` for code-auth, `EnvConfig.*` constants for code-sql. Do not hardcode `"CODE_AUTH_XXX"` or `"CODE_SQL_XXX"` strings in business logic.

## CI/CD

**Tag-driven**: push a tag → test that module → publish. One workflow (`.github/workflows/maven-publish.yml`):

```bash
git tag code-sql/v1.0.1       # test + publish code-sql only
git tag code-auth/v1.0.2      # install code-sql → test + publish code-auth
git tag code-datasheet/v1.0.3 # install code-sql → test + publish code-datasheet
git tag code-log/v1.0.0       # test + publish code-log
git push --tags
```

Pipeline per module: `compile → unit+integration tests (165 total) → ErrorCode dedup → deploy`

Requires 8 GitHub Secrets: `GH_PACKAGES_TOKEN`, `YUNTOWER_APP_ID`, `YUNTOWER_APP_SECRET`, `CODE_AUTH_JWT_SECRET`, `CODE_AUTH_REDIS_URI`, `CODE_SQL_URL`, `CODE_SQL_USER`, `CODE_SQL_PASS`.

## code-auth Key Architecture

- **Entry point**: `LoginEntry` (3 static methods + `tokens()`). `login()` catches all exceptions and returns `LoginResult` — callers check `isSuccess()`, no try-catch needed.
- **Engine**: `LoginService` holds `StrategyRegistry` + `JwtUtil` + `TokenStore` + `UserRepository`. `LoginEntry` defaults to lazy provider (`YuntowerAuthFactory.fromEnv()`), and supports runtime injection via `configure(...)` / `configureProviders(...)` / `resetDefaults()`. Login syncs userId/nickname/avatar/email to `pgaot_user` table via JPA.
- **Single-device login**: Each login generates a unique `jti` stored in Redis (`login:token:{userId}`). `validate()` compares JWT jti against Redis jti. If Redis is down, `getJti()` returns null and validation passes (fail-open).
- **Strategy pattern**: `LoginStrategy` interface (1 method: `authenticate(params) -> UserInfo`). `StrategyRegistry` (ConcurrentHashMap). `UserInfo` carries userId/nickname/avatar/email/extra.
- **API Token**: `pat_` prefix tokens via `ApiTokenManager`. SHA-256 hashed, scope-based permission (`datasheet:data`, `*:*:*`). Features: create (with optional expiry), validate (scope + expiry check, `lastUsed` update), revoke (ownership check), list. Stored in `api_token` table via code-sql JPA (`TokenRepository`).
- **Environment variables**: `YUNTOWER_APP_ID`, `YUNTOWER_APP_SECRET`, `CODE_AUTH_JWT_SECRET` (min 32 chars), `CODE_AUTH_REDIS_URI`. Optional: `CODE_AUTH_TOKEN_TTL` (default 604800).

## code-sql Key Architecture

- **Entry point**: `SqlTemplate` — sql(), page(), batch(), unsafe(), raw(). Implements `AutoCloseable`.
- **Firewall**: Three presets: `selectOnly()`, `readWrite()`, `readWriteDelete()`. WallFilter is applied at DataSource creation via `EnvConfig.createDataSource(name, wallConfig, maxActive)` — SqlTemplate does NOT mutate the DataSource. `SqlTemplateConfig` creates DataSource lazily (on first `getDataSource()`).
- **Pagination**: `TemplateExecutor.page()` wraps SQL in `SELECT COUNT(*) FROM (...) _t`, then appends `LIMIT/OFFSET`. `PageQuery` validates page >= 1, size in 1-1000.
- **Multi-datasource**: `_NAME` suffix convention. `EnvConfig.createDataSource("MAIN")` reads `CODE_SQL_URL_MAIN`. Constants public so `JpaTemplate` can reference them. Overloads for WallConfig and defaultMaxActive.
- **JPA mode**: Bypasses Druid — uses direct Hibernate. No WallFilter. `autoDdl` controlled by `CODE_SQL_AUTO_DDL` env (default false).
- **Transactions**: `RawExecutor.transaction(Runnable)` and `transactionCall(Callable<T>)` with explicit rollback on failure.
- **Environment variables**: `CODE_SQL_URL/USER/PASS` (required). Pool: `CODE_SQL_POOL_INITIAL`(5), `CODE_SQL_POOL_MIN_IDLE`(5), `CODE_SQL_POOL_MAX_ACTIVE`(20), `CODE_SQL_POOL_MAX_WAIT`(60000). `CODE_SQL_AUTO_DDL`(false).

## code-datasheet Key Architecture

- **Entry point**: `DatasheetEngine` exposes three sub-APIs: `tables()`, `data()`, `shares()`. Multi-datasource: `fromEnv()` or `fromEnv("NAME")`.
- **Isolation**: Table prefix — physical table name = `{userId}_{tableName}`. Different tenants can use the same logical table name without conflict.
- **SQL execution**: `SqlExecutor` is an orchestrator: `SqlTableExtractor` (AST parse + target/source table extraction) → `SqlPermissionChecker` (ownership/share/mode checks) → `SqlAstRewriter` (AST node-level table rewrite) → `SqlTemplate.sql()` execution.
- **Table modes**: `READ_ONLY` (SELECT only), `WRITE_ONLY` (no SELECT, no DELETE), `READ_WRITE` (no DELETE), `ALL` (default, all operations). Mode checked in `SqlExecutor` (for sql()) and `RowManager` (for insert/update/delete).
- **Sharing**: `ShareApi` — fine-grained permissions (SELECT/INSERT/UPDATE/DELETE). Stored in `ds_share` table. Shared users access the table using the owner's userId prefix.
- **Firewall**: `readWriteDelete()` mode on user SQL — allows CRUD, blocks DDL (DROP/ALTER/TRUNCATE/CREATE). Admin connection has no restrictions.
- **Import/Export**: CSV/JSON via `DataApi` using Jackson `ObjectMapper` + `CsvMapper`. Export checks ownership/share/mode permissions. `parseCsv`/`parseJson` for bulk import.
- **Convenience methods**: `updateCell()` (single cell update) and `deleteRow()` (delete by id) on `DataApi`.
- **Metadata**: `ds_table` (auto-created) + `ds_share`. Column info from `INFORMATION_SCHEMA` at query time. Metadata query errors are surfaced as `DatasheetException`.
- **Connection model**: Two `DruidDataSource` instances — adminSql (no firewall, max 2 connections, DDL only) and readWriteSql (readWriteDelete firewall, user SQL). WallFilter applied at DS creation via `SqlTemplateConfig`, not mutated by SqlTemplate. JPA is a third Hibernate pool. `DatasheetEngine` implements `AutoCloseable` to release all resources.
- **Dependency**: `code-sql` 1.0.0, Jackson 2.22.0. No Spring, no HTTP layer.

## code-log Key Architecture

- **Entry point**: `LogContext` — ThreadLocal + SLF4J MDC 双写，init(userId, userName, tenantId) 生成 traceId，clear() 清理。
- **Structured logging**: `StructuredLogger.of(Class)` 提供 info/warn/error/debug，自动从 LogContext 注入 traceId/userId/tenantId 到 MDC。
- **Audit logging**: `AuditLogger.record(AuditEvent)` → `AuditWriter.write()` → `AuditLogRepository` (code-sql JPA) → `audit_log` 表。记录 who/when/what/before/after。
- **Annotation**: `@Auditable(action, tableName)` 方法级注解，标记需要审计的操作。
- **Audit table**: `audit_log` (code-sql) — userId/userName/tenantId/action/tableName/rowId/beforeData/afterData/remark/traceId/createdAt。索引: user_id, action, created_at。
- **Dependency**: `code-sql` 1.0.0, SLF4J 2.0.17. No Spring, no HTTP layer.

## Documentation

`doc/README.md` is the entry point. Each feature is documented in its own file (function/flow/design/implementation). When adding a new module or feature, update both the module README and the corresponding doc file(s).

## Dependencies

- **code-auth**: yuntower-account-java-sdk 1.0.0, jjwt 0.13.0, lettuce-core 7.6.0, code-sql 1.0.0, JUnit 5.11.4 (test)
- **code-sql**: druid 1.2.28, spring-jdbc 7.0.8, hibernate-core 6.6.4, mysql-connector-j 9.7.0, lombok 1.18.46 (provided), JUnit 5.11.4 (test)
- **code-datasheet**: code-sql 1.0.0, jackson-databind 2.22.0, jackson-dataformat-csv 2.22.0, lombok 1.18.46 (provided)
- **code-log**: code-sql 1.0.0, slf4j-api 2.0.17, lombok 1.18.46 (provided), JUnit 5.11.4 (test)
