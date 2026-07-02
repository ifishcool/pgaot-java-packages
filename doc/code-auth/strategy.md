# 策略模式扩展

## 功能

支持多种登录方式（云塔、微信、QQ、手机号等），新增方式不改核心引擎。

## 架构

```
                 LoginStrategy (接口)
                       │
              authenticate(params) → UserInfo
                       │
        ┌──────────────┼──────────────┐
        │              │              │
  YuntowerStrategy  WechatStrategy  PhoneStrategy
  (当前实现)         (扩展示例)      (扩展示例)
```

## 核心接口

```java
// LoginStrategy.java
public interface LoginStrategy {
    UserInfo authenticate(Map<String, Object> params);
}

// UserInfo.java
public record UserInfo(
    String userId,
    String nickname,
    String avatar,
    Map<String, Object> extra
) {
    public UserInfo(String userId, String nickname, String avatar) {
        this(userId, nickname, avatar, null);
    }
}
```

## 注册中心

```java
// StrategyRegistry.java
public class StrategyRegistry {
    private final Map<String, LoginStrategy> strategies = new ConcurrentHashMap<>();

    public StrategyRegistry register(String key, LoginStrategy strategy) {
        strategies.put(key, strategy);
        return this;  // 链式调用
    }

    public LoginStrategy get(String key) {
        LoginStrategy s = strategies.get(key);
        if (s == null) throw LoginException.unsupportedType(key);
        return s;
    }
}
```

## 扩展示例：微信登录

```java
// Step 1: 实现接口
public class WechatStrategy implements LoginStrategy {
    private final WechatApiClient client;

    @Override
    public UserInfo authenticate(Map<String, Object> params) {
        String code = String.valueOf(params.get("code"));
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code 不能为空");
        }
        // 调微信 API: code → accessToken → userInfo
        WechatUserInfo wx = client.getUserInfo(client.getAccessToken(code));
        return new UserInfo(wx.getOpenId(), wx.getNickname(), wx.getAvatar());
    }
}

// Step 2: 注册
StrategyRegistry registry = new StrategyRegistry()
    .register(LoginType.YUNTOWER, yuntowerStrategy)
    .register("wechat", wechatStrategy);

// Step 3: 构建 LoginService
LoginService service = new LoginService(registry, loginConfig, tokenStore);

// Step 4: 使用
LoginResult r = LoginEntry.login("wechat", Map.of("code", "xxx"));
```

## 设计要点

- `StrategyRegistry` 用 `ConcurrentHashMap`——LoginService 单例，多线程并发登录需线程安全
- `register()` 返回 this，链式调用
- `get()` 找不到直接抛异常——未注册是程序错误，应快速失败
- 接口只一个方法——新增方式只需关注"params → UserInfo"

## 当前实现：YuntowerStrategy

```java
class YuntowerStrategy implements LoginStrategy {
    private final YuntowerAccountClient yuntower;
    private final Function<String, String> uidBinder;

    @Override
    public UserInfo authenticate(Map<String, Object> params) {
        String code = String.valueOf(params.get("code"));
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("code 不能为空");

        TokenResponse token = yuntower.auth().getToken(code);
        UserProfileResponse profile = yuntower.user().getUserInfo(token.getAccessToken());

        String localUserId = uidBinder.apply(profile.getUid());
        return new UserInfo(localUserId, profile.getNickname(), profile.getAvatar());
    }
}
```

## 关键源码

| 文件 | 内容 |
|---|---|
| `LoginStrategy.java` | 策略接口 |
| `StrategyRegistry.java:19-47` | 注册 + 查找 |
| `UserInfo.java` | record |
| `YuntowerStrategy.java:14-34` | 云塔实现 |
