# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

PGAOT Java 二方包 Monorepo — each directory under root is an independent Maven project. There is no parent POM, no shared dependency versions. Projects share only LICENSE, .gitignore, and CI workflow.

```
PGAOT_JAVA_PACKAGE/
├── code-auth/       # Authentication framework (JWT + Redis + strategy pattern)
├── code-sql/        # SQL engine (Druid firewall + JdbcTemplate + JPA)
├── code-datasheet/  # Multi-tenant datasheet platform (MySQL GRANT + Druid AST)
├── doc/             # Developer documentation (24 Markdown files)
└── .github/workflows/maven-publish.yml
```

## Build & Test

Each project is self-contained. Set environment variables before running integration tests.

```bash
# code-sql
cd code-sql
export $(cat .env | xargs)
mvn compile test-compile
mvn exec:java -Dexec.mainClass="com.pgaot.sql.NewApiTest" -Dexec.classpathScope="test"

# code-auth
cd code-auth
export $(cat .env | xargs)
mvn compile test-compile
mvn exec:java -Dexec.mainClass="com.pgaot.account.auth.JwtUtilTest" -Dexec.classpathScope="test"

# code-datasheet
cd code-datasheet
export $(cat .env | xargs)
mvn compile test-compile
mvn exec:java -Dexec.mainClass="com.pgaot.datasheet.DatasheetDemoTest" -Dexec.classpathScope="test"
```

Run ErrorCode dedup check (mandatory before release):
```bash
mvn compile && java -cp target/classes com.pgaot.<module>.common.code.ErrorCode
```

## Error Code Allocation (CRITICAL)

Each module owns a 100,000-range block. Adding a module requires registering its range in `doc/GLOBAL.md` BEFORE coding.

| Range | Module |
|-------|--------|
| `10_xxx_xxx` | code-auth |
| `20_xxx_xxx` | code-sql |
| `30_xxx_xxx` | code-datasheet |

Within a module, sub-ranges are allocated by the middle 3 digits (e.g., `20_001` = connection, `20_002` = SQL execution). Every `ErrorCode` enum must have a `main()` method that checks for duplicates.

## Package Conventions

```
com.pgaot.<module>.<layer>
```

| Layer | Visibility | Purpose |
|-------|-----------|---------|
| `api` | public | User-facing entry points |
| `api/model` | public | Return value DTOs |
| `core` | package-private preferred | Internal engine |
| `common/code` | public | `IResultCode` + `ErrorCode` |
| `common/config` | public | Configuration classes |
| `common/constants` | public | `Messages` + numeric constants |
| `exception` | public | Exception class with static factory methods |

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

Publishing is triggered by git tags with module prefix:
```bash
git tag code-sql/v1.0.1    # Publishes only code-sql
git tag code-auth/v1.0.2   # Publishes only code-auth
git push --tags
```

The workflow parses the tag to determine `$MODULE`, then runs `mvn -f $MODULE/pom.xml deploy`. Requires `GH_PACKAGES_TOKEN` in GitHub Actions secrets.

## code-auth Key Architecture

- **Entry point**: `LoginEntry` (3 static methods: login/validate/logout). `login()` catches all exceptions and returns `LoginResult` — callers check `isSuccess()`, no try-catch needed.
- **Engine**: `LoginService` holds `StrategyRegistry` + `JwtUtil` + `TokenStore`. Singleton via `YuntowerAuthFactory.fromEnv()`.
- **Single-device login**: Each login generates a unique `jti` stored in Redis (`login:token:{userId}`). `validate()` compares JWT jti against Redis jti. If Redis is down, `getJti()` returns null and validation passes (fail-open).
- **Strategy pattern**: `LoginStrategy` interface (1 method: `authenticate(params) -> UserInfo`). `StrategyRegistry` (ConcurrentHashMap) for thread-safe lookup. New login methods only need to implement the interface and register.
- **Environment variables**: `YUNTOWER_APP_ID`, `YUNTOWER_APP_SECRET`, `CODE_AUTH_JWT_SECRET` (min 32 chars), `CODE_AUTH_REDIS_URI`. Optional: `CODE_AUTH_TOKEN_TTL` (default 604800).

## code-sql Key Architecture

- **Entry point**: `SqlTemplate` — sql(), page(), batch(), unsafe(), raw().
- **Firewall**: Alibaba Druid WallFilter injected at the DataSource proxy filter layer during `SqlTemplate` construction. Two presets: `selectOnly()` (SELECT only, no DDL/writes/dangerous functions) and `readWrite()` (allows CRUD, blocks DDL). WallFilter exceptions contain the keyword "wall" in their message — `SqlTemplate` checks this to distinguish firewall blocks from other failures.
- **Pagination**: `TemplateExecutor.page()` wraps original SQL in `SELECT COUNT(*) FROM (...) _t` for count, then appends `LIMIT/OFFSET`. `PageQuery` validates page >= 1, size in 1-1000.
- **Multi-datasource**: Environment variables use `_NAME` suffix convention. `EnvConfig.createDataSource("MAIN")` reads `CODE_SQL_URL_MAIN`. Constants (`URL`, `USER`, `PASS`) are public in `EnvConfig` so `JpaTemplate` can reference them.
- **JPA mode**: Bypasses Druid — uses direct Hibernate connection. No WallFilter protection. Suitable for internal/admin use.
- **Environment variables**: `CODE_SQL_URL`, `CODE_SQL_USER`, `CODE_SQL_PASS` (required). Pool params optional with defaults: `CODE_SQL_POOL_INITIAL`(5), `CODE_SQL_POOL_MIN_IDLE`(5), `CODE_SQL_POOL_MAX_ACTIVE`(20), `CODE_SQL_POOL_MAX_WAIT`(60000).

## code-datasheet Key Architecture

- **Entry point**: `DatasheetEngine` exposes three sub-APIs: `tenants()`, `tables()`, `data()`.
- **Isolation**: MySQL native GRANT — each tenant gets a MySQL user, `CREATE TABLE` auto-executes `GRANT` on the physical table. Physical table naming: `{userId}_{tableName}`. No application-level permission checking needed.
- **SQL rewriting**: `SqlExecutor` uses Alibaba Druid's SQL parser (`SQLUtils.parseStatements()`) to build an AST, then replaces table name nodes precisely — no regex, won't touch column names or string literals.
- **Tenant management**: `TenantManager` manages MySQL users via `CREATE USER`/`GRANT`/`REVOKE`/`DROP USER`. `delete()` drops all tenant tables first, then the user.
- **Metadata**: `ds_table` + `ds_column` tables track table definitions. No permission or share tables (MySQL handles that).
- **Dependency**: Only depends on `code-sql` (which provides Druid + JdbcTemplate). No Spring, no HTTP layer.

## Documentation

`doc/README.md` is the entry point. Each feature is documented in its own file (function/flow/design/implementation). When adding a new module or feature, update both the module README and the corresponding doc file(s).

## Dependencies

- **code-auth**: yuntower-account-java-sdk 1.0.0, jjwt 0.12.6, lettuce-core 6.4.1
- **code-sql**: druid 1.2.23, spring-jdbc 6.2.7, hibernate-core 6.6.4, mysql-connector-j 9.7.0, lombok 1.18.36 (provided)
- **code-datasheet**: code-sql 1.0.0, lombok 1.18.36 (provided)
