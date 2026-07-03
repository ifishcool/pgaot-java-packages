# Changelog

## v1.0.0 (2026-07-01)

首个正式版本。

### 功能

- 通用 SQL 执行（SqlTemplate，一条 API 自动识别 SELECT/INSERT/UPDATE/DELETE）
- 分页查询（TemplateExecutor.page()，自动拼 COUNT + LIMIT/OFFSET）
- 分页参数校验（PageQuery，页码 >= 1，每页 1~1000）
- 分页响应模型（PageResponse，rows/total/page/size/pages，支持 convert 类型转换）
- Druid SQL 防火墙（WallFilter 代理层注入，selectOnly/readWrite 双预设 + 自定义 WallConfig）
- 拦截 50+ 种注入与绕过手法（注释绕过、大小写、多空格、多语句、UNION、报错、盲注等）
- 批量操作（SqlTemplate.batch()，JdbcTemplate.batchUpdate）
- 原生 JDBC 执行器（RawExecutor，存储过程 + 手动事务）
- JPA 模式（JpaTemplate，Hibernate CRUD + autoDdl 自动建表）
- readWriteDelete 防火墙模式（允许增删改查，禁止 DDL）
- JPA 仓储层（UserRepository / TokenRepository）
- PGAOT 用户实体（pgaot_user，userId 主键）
- API Token 实体（api_token，含 user_id 索引）
- 多数据源（EnvConfig，环境变量 `CODE_SQL_URL_{NAME}` 命名后缀）
- 连接池可配（CODE_SQL_POOL_* 环境变量，含默认值）
- IResultCode 错误码接口 + ErrorCode 分段枚举（20_xxx_xxx），含 main() 去重校验
- SqlException 统一异常 + 6 个静态工厂方法
- 提示信息常量集中管理（Messages.java）
- 数值常量内部类分组（SqlConstants：Pool/Page/Batch）
- 环境变量 Key 集中管理（EnvConfig 公共常量）
- 参数校验（SQL 非空、batch 非空）
- 109 项安全测试（FilterDemoTest）
- 71 项 API 测试（NewApiTest）
- 完整 Javadoc 注释

### 依赖

| 依赖 | 版本 | 说明 |
|---|---|---|
| druid | 1.2.23 | 连接池 + SQL 防火墙 |
| spring-jdbc | 6.2.7 | JdbcTemplate |
| hibernate-core | 6.6.4 | JPA |
| mysql-connector-j | 9.7.0 | MySQL 驱动 |
| lombok | 1.18.36 | 编译期代码生成（provided） |
