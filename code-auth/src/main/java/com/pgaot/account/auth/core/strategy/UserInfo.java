package com.pgaot.account.auth.core.strategy;

import java.util.Map;

/**
 * 策略认证成功后返回的用户信息，由 LoginService 写入 JWT.
 *
 * <p>extra 字段会写入 JWT payload，后续 LoginUser 可读取.
 */
public class UserInfo {

    private final String userId;
    private final String nickname;
    private final String avatar;
    private final String email;
    private final Map<String, Object> extra;

    public UserInfo(String userId, String nickname, String avatar) {
        this(userId, nickname, avatar, null, null);
    }

    public UserInfo(String userId, String nickname, String avatar, String email) {
        this(userId, nickname, avatar, email, null);
    }

    public UserInfo(String userId, String nickname, String avatar, String email, Map<String, Object> extra) {
        this.userId = userId;
        this.nickname = nickname;
        this.avatar = avatar;
        this.email = email;
        this.extra = extra;
    }

    public String getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public String getAvatar() { return avatar; }
    public String getEmail() { return email; }
    public Map<String, Object> getExtra() { return extra; }
}
