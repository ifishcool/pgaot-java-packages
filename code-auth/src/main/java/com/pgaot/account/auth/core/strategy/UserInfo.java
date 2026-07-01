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
    private final Map<String, Object> extra;

    /** @param userId   你在自己系统中的用户唯一标识 */
    /** @param nickname 昵称 */
    /** @param avatar   头像 URL */
    public UserInfo(String userId, String nickname, String avatar) {
        this(userId, nickname, avatar, null);
    }

    /**
     * @param userId   你在自己系统中的用户唯一标识
     * @param nickname 昵称
     * @param avatar   头像 URL
     * @param extra    额外字段，会写入 JWT，后续 LoginUser 可读取
     */
    public UserInfo(String userId, String nickname, String avatar, Map<String, Object> extra) {
        this.userId = userId;
        this.nickname = nickname;
        this.avatar = avatar;
        this.extra = extra;
    }

    public String getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public String getAvatar() { return avatar; }
    public Map<String, Object> getExtra() { return extra; }
}
