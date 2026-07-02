# PGAOT Java

[![JDK](https://img.shields.io/badge/JDK-21%2B-blue)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-GPL--3.0-green)](LICENSE)

PGAOT 平台 Java 二方包 Monorepo。

---

## 模块

| 模块                    | 说明                                                |
| ----------------------- | --------------------------------------------------- |
| [code-auth](code-auth/)           | 通用认证框架 — JWT + 单设备登录 + Redis 持久化            |
| [code-sql](code-sql/)             | 通用 SQL 引擎 — Druid 防火墙 + SQL自定义 + JPA 支持       |
| [code-datasheet](code-datasheet/) | 多租户数据表平台 — MySQL GRANT 隔离 + Druid AST 表名替换 |

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

## 如何发布

按 tag 前缀区分模块：

```bash
git tag code-sql/v1.0.0       # 发布 code-sql
git tag code-auth/v1.0.0      # 发布 code-auth
git tag code-datasheet/v1.0.0 # 发布 code-datasheet
git push --tags
```

## License

GPL-3.0
