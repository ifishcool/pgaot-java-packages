# 测试规范

## 命名

- `*Test.java` → 单元测试，不依赖外部服务（数据库、Redis 等）
- `*DemoTest.java` → 演示/集成测试，需外部依赖

## 位置

```
<module>/src/test/java/com/pgaot/<module>/
```

## 运行

```bash
cd <module>
export $(cat .env | xargs)     # 集成测试需要
mvn compile test-compile
mvn exec:java -Dexec.mainClass="com.pgaot.<module>.XxxTest" -Dexec.classpathScope="test"
```

## ErrorCode 去重校验

每次改完 ErrorCode 后必跑：

```bash
cd <module>
mvn compile
java -cp target/classes com.pgaot.<module>.common.code.ErrorCode
# 输出: 错误码校验通过，共 X 个
```

## 现有测试

| 模块 | 测试类 | 用例数 | 类型 |
|---|---|---|---|
| code-sql | `FilterDemoTest` | 109 | 集成（需数据库） |
| code-sql | `NewApiTest` | 71 | 单元 + 集成 |
| code-sql | `ComplexSqlTest` | — | 集成 |
| code-sql | `JpaDemoTest` | — | 集成 |
| code-auth | `JwtUtilTest` | — | 单元 |

## 测试内容说明

### FilterDemoTest（code-sql）

测试 Druid WallFilter 拦截能力：
- 正常复杂查询（12 个）— JOIN、GROUP BY、子查询等
- 增删改操作（8 个）— INSERT/UPDATE/DELETE 各写法
- DDL 攻击（12 个）— DROP/ALTER/TRUNCATE 等
- 绕过手法（10 个）— 注释/大小写/多空格/反引号等
- 注入攻击（30+ 个）— 多语句/UNION/报错/盲注/XPATH 等
- 合规边界（4 个）— 合法 SQL 是否被误拦

### NewApiTest（code-sql）

测试新 API 功能：
- 错误码去重、IResultCode 接口、分段编号
- SqlException 静态工厂方法（6 个）
- PageQuery 参数校验、offset 计算
- PageResponse 工厂方法/convert/pages 计算
- 数据库分页查询 + 连接池（需数据库）

### JpaDemoTest（code-sql）

模拟 code-auth 登录后用户落库的完整 CRUD 流程。

### JwtUtilTest（code-auth）

JWT 生成、校验、过期、签名错误处理。
