# Changelog

## v1.0.0 (2026-07-02)

首个正式版本。

### 功能

- 数据表 CRUD（TableApi：create/drop/rename/truncate/addColumn/dropColumn/renameColumn/list/get）
- 数据操作（DataApi：insert/update/delete/sql/exportCsv/exportJson）
- 表前缀隔离（userId_tableName，不同租户同表名不冲突）
- 模式控制（TableMode：READ_ONLY/WRITE_ONLY/READ_WRITE）
- Druid AST 表名提取 + 权限校验（归属 + 模式）
- 用户 SQL readWrite 防火墙保护（禁止 DDL/DELETE）
- 内部 SQL 管理员连接（insert/update/delete/export）
- CSV/JSON 导出（最多 50000 行）
- MySQL 原生类型校验（insert/update 直接走 MySQL，不做应用层校验）
- INFORMATION_SCHEMA 实时查列信息（无需 ds_column 元数据表）
- ds_table 元数据自动建表（首次使用自动 CREATE TABLE IF NOT EXISTS）
- IResultCode 错误码接口 + ErrorCode 分段枚举（30_xxx_xxx），含 main() 去重校验
- DatasheetException 统一异常 + 7 个静态工厂方法
- 43+ 项 SQL 安全测试（正常/模式/跨租户/注入/提权/边界/跨表）
- 22+ 步集成测试（建表/SQL/字段变更/模式控制/跨租户隔离）
- 完整 Javadoc 注释

### 依赖

| 依赖 | 版本 | 说明 |
|---|---|---|
| code-sql | 1.0.0 | SQL 执行引擎 |
| lombok | 1.18.36 | 编译期代码生成（provided） |
