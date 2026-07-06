# PGAOT Code Web

[![JDK](https://img.shields.io/badge/JDK-21%2B-blue)](https://adoptium.net/)

REST API 层 — 统一响应体 + 全局异常 + Swagger 文档 + 注解驱动认证。

---

## 环境要求

- JDK 21+
- code-sql + code-auth + code-datasheet + code-log

## 安装

```xml
<dependency>
    <groupId>com.pgaot</groupId>
    <artifactId>code-web</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 启动

```java
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
```

启动后：
- **API 文档**: http://localhost:8080/doc.html

---

## BaseController

所有 Controller 继承 `BaseController`，提供：

| 方法 | 说明 |
|---|---|
| `getUserId()` | 当前用户 ID |
| `getNickname()` | 当前用户昵称 |
| `getClientIp()` | 客户端 IP |

## 注解

| 注解 | 机制 | 作用 |
|---|---|---|
| `@RequiredAuth` | AspectJ `@Before` | JWT 校验，注入 userId/nickname |
| `@RequiredScope("scope")` | AspectJ `@Before` | API Token 权限校验 |
| `@Auditable(action, tableName)` | AuditableProxy | 审计日志记录 |

```java
@RequiredAuth                          // ← Aspect 自动拦截
@RestController
@RequestMapping("/api/data")
public class DataController extends BaseController {

    @PostMapping("/update")
    public ApiResponse<Void> update() {
        doUpdate(getUserId());          // ← 继承自 BaseController
        return ApiResponse.ok();
    }
}
```

---

## API 响应格式

统一响应体 `ApiResponse<T>`：

```json
// 成功（有数据）
{"code": 0, "data": {"userId": "alice"}, "traceId": "a1b2c3d4"}

// 成功（无数据）
{"code": 0, "data": {}, "traceId": "a1b2c3d4"}

// 失败
{"code": 401, "message": "未登录", "traceId": "a1b2c3d4"}
```



## 项目结构

```
code-web/src/main/java/com/pgaot/web/
├── annotation/          @RequiredAuth / @RequiredScope / @Auditable
├── aspect/              AuthAspect（AOP 拦截）
├── proxy/               AuditableProxy（@Auditable 代理）
├── config/              SwaggerConfig（OpenAPI 文档）
├── common/               ApiResponse + GlobalExceptionHandler
├── controller/           BaseController + auth/ datasheet/ audit/
├── param/auth/           LoginRequest（入参）
└── vo/                   LoginVO（出参）
```

## Docker 部署

```bash
# 构建（项目根目录）
docker build -t pgaot/code-web .

# 运行
docker run -d -p 8080:8080 \
  -e CODE_SQL_URL=jdbc:mysql://host:3306/db \
  -e CODE_SQL_USER=user -e CODE_SQL_PASS=pass \
  -e CODE_AUTH_JWT_SECRET=secret \
  -e CODE_AUTH_REDIS_URI=redis://host:6379/1 \
  -e YUNTOWER_APP_ID=id -e YUNTOWER_APP_SECRET=secret \
  --name pgaot-api pgaot/code-web

# 或 docker compose
docker compose up -d
```

## License

GPL-3.0

