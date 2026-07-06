# PGAOT Java

[![JDK](https://img.shields.io/badge/JDK-21%2B-blue)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-GPL--3.0-green)](LICENSE)

PGAOT 平台 Java 二方包 Monorepo — 父 POM 统一版本管理。

---

## 模块

| 模块                              | 说明                                                             | 测试    |
| --------------------------------- | ---------------------------------------------------------------- | ------- |
| [code-auth](code-auth/)           | 认证框架 — JWT + 单设备登录 + API Token + Redis                  | 25 pass |
| [code-sql](code-sql/)             | SQL 引擎 — Druid 防火墙 + JPA + 多数据源 + 事务                  | 73 pass |
| [code-datasheet](code-datasheet/) | 多租户数据表 — 前缀隔离 + AST 权限校验 + Jackson 导入导出 + 共享 | 62 pass |
| [code-log](code-log/) | 日志框架 — 结构化日志 + 审计日志 + traceId 链路上下文 | 5 pass |

## 安装

所有模块发布到同一 GitHub Packages 仓库，只需配置一次 `~/.m2/settings.xml`：

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>你的GitHub用户名</username>
            <password>你的Token</password>
        </server>
    </servers>
</settings>
```

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/ifishcool/pgaot-java-packages</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.pgaot</groupId>
    <artifactId>code-xxxx</artifactId>
    <version>版本</version>
</dependency>
```

## 本地联调

改完底层模块（如 `code-sql`）后，无需发 GitHub Packages 即可让上层模块引用：

```bash
./install-local.sh code-sql    # 只装一个
./install-local.sh             # 全部按依赖顺序安装
```

Maven 优先从 `~/.m2/repository` 解析依赖，`mvn install` 就是将 jar 装到本地仓库。

## 如何发布

按 tag 前缀区分模块：

```bash
git tag code-sql/v1.0.0       # 发布 code-sql
git tag code-auth/v1.0.0      # 发布 code-auth
git tag code-datasheet/v1.0.0 # 发布 code-datasheet
git tag code-log/v1.0.0       # 发布 code-log
git push --tags
```

## License

GPL-3.0
