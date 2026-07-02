# SQL 防火墙

## 功能

基于 Alibaba Druid WallFilter，在数据库连接层拦截危险 SQL 和注入攻击。

## 工作原理

WallFilter 内置完整 SQL 语法解析器（MySQL 语法），能识别：语句类型、嵌套子查询、注释绕过（`/**/`、`/*!50000*/`）、变量/函数调用。拦截发生在 `DruidDataSource.getConnection()` 代理层，对 JdbcTemplate 和 RawExecutor 透明。

## 注入方式

```
SqlTemplate 构造时
    │
    ├─ 1. EnvConfig.createDataSource() → DruidDataSource
    ├─ 2. SqlTemplateConfig → 配置 WallConfig
    └─ 3. SqlTemplate 构造器:
         ├─ druid.getProxyFilters()
         ├─ filters.add(new WallFilter(config))
         └─ druid.setProxyFilters(filters)
```

## 预设模式

### selectOnly()

仅允许 SELECT，拦截所有写操作、DDL、危险函数。

```java
wallConfig.setSelectAllow(true);
// 以下全部设为 false:
wallConfig.setInsertAllow(false);
wallConfig.setUpdateAllow(false);
wallConfig.setDeleteAllow(false);
wallConfig.setDropTableAllow(false);
wallConfig.setAlterTableAllow(false);
wallConfig.setTruncateAllow(false);
wallConfig.setRenameTableAllow(false);
wallConfig.setCreateTableAllow(false);
wallConfig.setSelectIntoAllow(false);
wallConfig.setSetAllow(false);
wallConfig.setCallAllow(false);
wallConfig.setDescribeAllow(false);
wallConfig.setShowAllow(false);
wallConfig.setUseAllow(false);
wallConfig.setMergeAllow(false);
wallConfig.setFunctionCheck(true);
// 禁止危险函数
wallConfig.getDenyFunctions().addAll(List.of(
    "sleep", "benchmark", "load_file", "into outfile", "into dumpfile"));
```

### readWrite()

允许增删改查，拦截 DDL 和危险函数。

```java
// 放行: INSERT, UPDATE, SELECT（默认打开）
// 禁止: DELETE, DDL, 危险函数
wallConfig.setDeleteAllow(false);
wallConfig.setDropTableAllow(false);
wallConfig.setAlterTableAllow(false);
wallConfig.setTruncateAllow(false);
wallConfig.setRenameTableAllow(false);
wallConfig.setCreateTableAllow(false);
wallConfig.setFunctionCheck(true);
wallConfig.getDenyFunctions().addAll(List.of("sleep", "benchmark", "load_file"));
```

## 规则对比

| 规则 | selectOnly() | readWrite() |
|---|---|---|
| SELECT | 允许 | 允许 |
| INSERT | 禁止 | 允许 |
| UPDATE | 禁止 | 允许 |
| DELETE | 禁止 | 禁止 |
| DROP/ALTER/TRUNCATE/CREATE/RENAME | 禁止 | 禁止 |
| SET/MERGE/CALL | 禁止 | 允许 |
| DESCRIBE/SHOW/USE | 禁止 | 允许 |
| SELECT INTO | 禁止 | 允许 |
| sleep/benchmark/load_file | 禁止 | 禁止 |
| into outfile/dumpfile | 禁止 | N/A |

## 自定义防火墙

```java
// 在预设基础上微调
SqlTemplateConfig config = new SqlTemplateConfig(
    SqlTemplateConfig.fromEnv().getDataSource());
config.selectOnly();
config.getWallConfig().setSelectHavingAlwayTrueCheck(false); // 关 HAVING 检查
SqlTemplate custom = new SqlTemplate(config);
```

## 拦截特征

Druid 拦截时抛出的异常 message 含 `"wall"` 关键字。`SqlTemplate` 据此区分：

```java
if (e.getMessage() != null && e.getMessage().contains(Messages.WALL_KEYWORD)) {
    throw SqlException.wallBlocked(e.getMessage());
}
```

## 验证覆盖

`FilterDemoTest` 109 个测试用例：

| 类别 | 用例数 | 示例 |
|---|---|---|
| 正常复杂查询 | 12 | JOIN, GROUP BY, 子查询, CASE WHEN, UNION, EXISTS |
| 增删改操作 | 8 | INSERT 单行/多行/SET/SELECT, UPDATE/DELETE  |
| DDL 攻击 | 12 | DROP/ALTER/TRUNCATE/CREATE/RENAME |
| 绕过手法 | 10 | 注释/大小写/多空格/反引号/制表符/换行/括号 |
| 注入攻击 | 30+ | 多语句/UNION/报错/时间盲注/XPATH/ORDER/LIMIT |
| 合规边界 | 4 | LOAD_FILE 在 selectOnly 下合规 |

## 关键源码

| 文件 | 行数 | 内容 |
|---|---|---|
| `SqlTemplateConfig.java:37-49` | 13 行 | selectOnly() |
| `SqlTemplateConfig.java:51-57` | 7 行 | readWrite() |
| `SqlTemplate.java:46-55` | 10 行 | WallFilter 注入 |
