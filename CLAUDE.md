# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

PGAOT Java 二方包 Monorepo — each directory under root is an independent Maven project. There is no parent POM, no shared dependency versions. Projects share only LICENSE, .gitignore, and CI workflow.

```
PGAOT_JAVA_PACKAGE/
├── code-auth/       # Authentication (JWT + Redis + strategy + API tokens)
├── code-sql/        # SQL engine (Druid firewall + JPA + multi-datasource)
├── code-datasheet/  # Multi-tenant datasheet (prefix isolation + sharing + Jackson)
├── doc/             # Developer documentation
├── install-local.sh # Install jars to ~/.m2 for local development
└── .github/workflows/
    ├── ci.yml             # CI: compile + test + ErrorCode on push/PR
    └── maven-publish.yml  # Publish on tag push
```

**Dependency chain**: `code-sql` ← `code-auth`, `code-sql` ← `code-datasheet`. code-sql is the foundation; changes to it require `./install-local.sh code-sql` before building the other two.

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

Two GitHub Actions workflows:

**CI** (`.github/workflows/ci.yml`) — runs on every push/PR to main:
- Compiles all 3 modules in dependency order (code-sql → code-auth → code-datasheet)
- Runs JUnit 5 tests (160 test cases: code-auth 25, code-sql 73, code-datasheet 62)
- `@Tag("integration")` tests auto-skip without DB/Redis; CI with secrets runs full suite
- Runs ErrorCode dedup for all modules

**Publish** (`.github/workflows/maven-publish.yml`) — triggered by git tags:
```bash
git tag code-sql/v1.0.1    # Publishes only code-sql
git tag code-auth/v1.0.2   # Publishes only code-auth
git push --tags
```
Before deploy: install code-sql (if downstream), compile, run tests, ErrorCode dedup. Requires `GH_PACKAGES_TOKEN` + DB/Redis secrets in GitHub Actions.

## code-auth Key Architecture

- **Entry point**: `LoginEntry` (3 static methods + `tokens()`). `login()` catches all exceptions and returns `LoginResult` — callers check `isSuccess()`, no try-catch needed.
- **Engine**: `LoginService` holds `StrategyRegistry` + `JwtUtil` + `TokenStore` + `UserRepository`. Singleton via `YuntowerAuthFactory.fromEnv()`. Login syncs userId/nickname/avatar/email to `pgaot_user` table via JPA.
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
- **SQL execution**: `SqlExecutor` uses Druid's SQL parser to build an AST, extracts table names, validates ownership + mode, then replaces logical table names with physical names. Simple string replacement after AST validation.
- **Table modes**: `READ_ONLY` (SELECT only), `WRITE_ONLY` (no SELECT, no DELETE), `READ_WRITE` (no DELETE), `ALL` (default, all operations). Mode checked in `SqlExecutor` (for sql()) and `RowManager` (for insert/update/delete).
- **Sharing**: `ShareApi` — fine-grained permissions (SELECT/INSERT/UPDATE/DELETE). Stored in `ds_share` table. Shared users access the table using the owner's userId prefix.
- **Firewall**: `readWriteDelete()` mode on user SQL — allows CRUD, blocks DDL (DROP/ALTER/TRUNCATE/CREATE). Admin connection has no restrictions.
- **Import/Export**: CSV/JSON via `DataApi` using Jackson `ObjectMapper` + `CsvMapper`. Export checks ownership/share/mode permissions. `parseCsv`/`parseJson` for bulk import.
- **Convenience methods**: `updateCell()` (single cell update) and `deleteRow()` (delete by id) on `DataApi`.
- **Metadata**: `ds_table` (auto-created) + `ds_share`. Column info from `INFORMATION_SCHEMA` at query time. Metadata query errors are surfaced as `DatasheetException`.
- **Connection model**: Two `DruidDataSource` instances — adminSql (no firewall, max 2 connections, DDL only) and readWriteSql (readWriteDelete firewall, user SQL). WallFilter applied at DS creation via `SqlTemplateConfig`, not mutated by SqlTemplate. JPA is a third Hibernate pool. `DatasheetEngine` implements `AutoCloseable` to release all resources.
- **Dependency**: `code-sql` 1.0.0, Jackson 2.18.3 (databind + dataformat-csv). No Spring, no HTTP layer.

## Documentation

`doc/README.md` is the entry point. Each feature is documented in its own file (function/flow/design/implementation). When adding a new module or feature, update both the module README and the corresponding doc file(s).

## Dependencies

- **code-auth**: yuntower-account-java-sdk 1.0.0, jjwt 0.12.6, lettuce-core 6.4.1, code-sql 1.0.0, JUnit 5.11.4 (test)
- **code-sql**: druid 1.2.23, spring-jdbc 6.2.7, hibernate-core 6.6.4, mysql-connector-j 9.7.0, lombok 1.18.36 (provided), JUnit 5.11.4 (test)
- **code-datasheet**: code-sql 1.0.0, jackson-databind 2.18.3, jackson-dataformat-csv 2.18.3, lombok 1.18.36 (provided)
