# 测试规范

## 运行

```bash
# 全部测试（自动加载模块根目录的 .env）
cd <module>
mvn test

# 只跑某个测试类
mvn test -Dtest=PageQueryTest

# 只跑集成测试（需要数据库/Redis）
mvn test -Dgroups=integration
```

## 环境变量

测试启动时自动加载模块根目录的 `.env` 文件（通过 `EnvLoader` 写入 `System.properties`）。如果 `.env` 不存在也无需担心——集成测试会自动跳过。

可在 GitHub Actions Secrets 中配置生产环境变量，CI 自动跑全量集成测试。

## 测试类型

| 类型 | 框架 | 跳过条件 |
|---|---|---|
| 单元测试 | JUnit 5 | 永远执行 |
| 集成测试 | JUnit 5 `@Tag("integration")` | 无 `.env` 且无环境变量时自动跳过 |
| 手动测试 | JUnit 5 `@Disabled` | 永远跳过（需人工操作，如 OAuth 浏览器授权） |

## ErrorCode 去重校验

```bash
cd <module>
mvn compile && java -cp target/classes com.pgaot.<module>.common.code.ErrorCode
```

## 现有测试

### code-auth — 25 tests

| 测试类 | 用例 | 说明 |
|---|---|---|
| `JwtUtilUnitTest` | 3 | JWT 生成/校验/过期 |
| `TokenGeneratorTest` | 4 | pat_ 生成/SHA-256/格式化 |
| `ScopeTest` | 6 | 精确匹配/通配符/null 处理 |
| `ApiTokenTest` | 10 | API Token 创建/校验/吊销/列表 |
| `TokenStoreTest` | 2 | Redis TokenStore 读写 |
| `LoginTest` | `@Disabled` | OAuth 登录（需人工浏览器授权） |

### code-sql — 73 tests

| 测试类 | 用例 | 说明 |
|---|---|---|
| `PageQueryTest` | 6 | 分页参数校验/offset 计算 |
| `NewApiTest` | 27 | 错误码/SqlException/PageResponse/DB 分页 |
| `FilterDemoTest` | 32 | Druid WallFilter 拦截（DDL/注入/绕过） |
| `ComplexSqlTest` | 5 | JOIN/子查询/聚合/排序 |
| `JpaDemoTest` | 3 | JPA CRUD 流程 |

### code-datasheet — 62 tests

| 测试类 | 用例 | 说明 |
|---|---|---|
| `ShareTest` | 13 | 共享/权限/取消共享/DDL 拦截 |
| `IsolationTest` | 13 | 多租户隔离/复杂 SQL/跨租户拦截 |
| `ImportExportTest` | 6 | CSV/JSON 导入导出 |
| `SqlSecurityTest` | 16+16 | SQL 安全（正常放行 + 攻击拦截） |
| `CmsDemo` | 7 | CMS 场景（admin/editor/reviewer） |
| `RealWorldDemo` | 8 | 销售团队场景（alice/bob/charlie） |

### code-log — 5 tests

| 测试类 | 用例 | 说明 |
|---|---|---|
| `LogContextTest` | 4 | init/clear/initTrace/默认未初始化 |
| `AuditWriterTest` | 1 | 审计日志写入 + 验证持久化 |
