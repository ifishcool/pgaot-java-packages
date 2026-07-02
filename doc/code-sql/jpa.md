# JPA 模式

## 功能

对简单表操作（单表 CRUD）提供 Hibernate SessionFactory 封装。

## 注意

JPA 模式不走 Druid 连接池，Hibernate 通过 `hibernate.connection.url/user/pass` 直连 MySQL。**不受 WallFilter 保护**——适合内部管理后台或信任环境。

## 使用

```java
JpaTemplate jpa = JpaTemplate.fromEnv(UserEntity.class);

// 新增
UserEntity u = new UserEntity();
u.setUserId("user_10001");
u.setNickname("测试");
u.setAvatar("https://cdn.example.com/avatar/10001.png");
jpa.save(u);

// 查询
jpa.findAll(UserEntity.class).forEach(...);

// 更新
u.setNickname("新昵称");
jpa.update(u);

// 删除
jpa.delete(UserEntity.class, u.getId());

// HQL
List<UserEntity> list = jpa.query("FROM UserEntity WHERE nickname LIKE ?", UserEntity.class, "%测试%");

jpa.close();
```

## API

| 方法 | 说明 |
|---|---|
| `fromEnv(Class<?>... entities)` | 从环境变量创建，注册实体 |
| `fromEnv(String name, Class<?>... entities)` | 命名数据源 |
| `findAll(Class<T>)` | 全量查询 |
| `findById(Class<T>, Object id)` | 按主键查 |
| `save(Object)` | 新增（persist） |
| `update(Object)` | 更新（merge） |
| `delete(Class<T>, Object id)` | 删 |
| `query(hql, Class<T>, params...)` | HQL 查询 |
| `close()` | 关闭 SessionFactory |

## 实现

### 构造函数

```java
public static JpaTemplate create(String url, String user, String pass, Class<?>... entities) {
    var cfg = new Configuration()
        .setProperty("hibernate.connection.url", url)
        .setProperty("hibernate.connection.username", user)
        .setProperty("hibernate.connection.password", pass)
        .setProperty("hibernate.hbm2ddl.auto", "none")
        .setProperty("hibernate.show_sql", "false");
    for (Class<?> c : entities) cfg.addAnnotatedClass(c);
    return new JpaTemplate(cfg.buildSessionFactory());
}
```

### fromEnv 复用 EnvConfig

```java
public static JpaTemplate fromEnv(String name, Class<?>... entities) {
    String suffix = (name != null && !name.isBlank()) ? "_" + name : "";
    return create(
        EnvConfig.env(EnvConfig.URL + suffix),   // 引用 EnvConfig 公共常量
        EnvConfig.env(EnvConfig.USER + suffix),
        EnvConfig.env(EnvConfig.PASS + suffix),
        entities);
}
```

## UserEntity

```java
@Entity
@Table(name = "pgaot_user")
public class UserEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String userId;       // 云塔用户唯一标识

    @Column(length = 64)
    private String nickname;

    @Column(length = 512)
    private String avatar;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }
}
```

## 建表 SQL

```sql
-- sql/init.sql
CREATE TABLE pgaot_user (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    VARCHAR(64) NOT NULL UNIQUE   COMMENT '云塔用户唯一标识',
    nickname   VARCHAR(64)                   COMMENT '昵称',
    avatar     VARCHAR(512)                  COMMENT '头像 URL',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 关键源码

| 文件 | 内容 |
|---|---|
| `JpaTemplate.java:28-38` | fromEnv → create |
| `JpaTemplate.java:41-49` | Hibernate Configuration |
| `UserEntity.java` | PGAOT 用户实体 |
| `sql/init.sql` | 建表语句 |

## 依赖

`org.hibernate.orm:hibernate-core:6.6.4`
