# 更新日志

## [1.0.0] — 2026-07-01

### 新增

#### 核心 API
- `SqlTemplate` 通用 SQL 执行器，自动识别 SELECT/INSERT/UPDATE/DELETE 并返回对应类型
- `SqlTemplateConfig` 安全配置类，支持 `selectOnly()` 只读模式、`readWrite()` 读写模式
- `JpaTemplate` JPA 操作模板，封装 Hibernate CRUD（findAll/save/update/delete/query）

#### 执行引擎
- `TemplateExecutor` 基于 Spring JdbcTemplate 封装，按 SQL 前缀路由到 query/update/execute
- `RawExecutor` 原生 JDBC 执行器，支持存储过程和手动事务 `transaction()`

#### SQL 防火墙（Alibaba Druid WallFilter）
- `selectOnly()` 模式：仅允许 SELECT，禁止 INSERT/UPDATE/DELETE/DDL，拦截 sleep/benchmark/load_file/INTO OUTFILE 等危险函数
- `readWrite()` 模式：允许增删改查，禁止 DDL 和危险函数
- 拦截 50+ 种注入与绕过手法：注释绕过、大小写变形、多空格、反引号、制表符、换行符、括号、多语句注入、堆叠注入、UNION 注入、报错注入、时间盲注、双查询注入、PREPARE 注入、ORDER/LIMIT 注入、XPATH 注入、INTO 变量注入等

#### 批量操作
- `SqlTemplate.batch()` 支持批量 INSERT/UPDATE/DELETE，适合数据导入和批量记工

#### 多数据源
- `EnvConfig` 通过环境变量 `CODE_SQL_URL_{NAME}` 支持命名数据源
- 默认数据源 `fromEnv()` + 命名数据源 `fromEnv("NAME")`

#### 异常体系
- `SqlException` 统一运行时异常，携带错误码
- `ErrorCode` 错误码枚举（SQL_EXECUTION_FAILED / SQL_BLOCKED_BY_WALL / CONNECTION_FAILED）

#### 安全测试
- `FilterDemoTest` 109 项安全测试用例，覆盖复杂查询、增删改操作、DDL 攻击、注入攻击、绕过手法、边界合规语法
- `ComplexSqlTest` JOIN/子查询/聚合/分页等复杂 SQL 功能验证
- `JpaDemoTest` JPA CRUD 基础演示

### 技术栈

- Java 21
- Alibaba Druid 1.2.23（连接池 + SQL 防火墙）
- Spring JdbcTemplate 6.2.7
- Hibernate 6.6.4（JPA）
- MySQL Connector/J 9.7.0
