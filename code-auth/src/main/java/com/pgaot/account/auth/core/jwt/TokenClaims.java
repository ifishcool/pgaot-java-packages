package com.pgaot.account.auth.core.jwt;

import java.util.Date;
import java.util.Map;

/**
 * JWT 解析结果，包含 userId、jti、过期时间等 payload 字段.
 *
 * <p>每次 LoginEntry.validate() 校验通过后，LoginUser 内部包装此对象.
 */
public class TokenClaims {

    private final String userId;
    private final String jti;
    private final Date issuedAt;
    private final Date expiration;
    private final Map<String, Object> extra;

    public TokenClaims(String userId, String jti, Date issuedAt, Date expiration,
                       Map<String, Object> extra) {
        this.userId = userId;
        this.jti = jti;
        this.issuedAt = issuedAt;
        this.expiration = expiration;
        this.extra = extra;
    }

    /** 用户唯一标识 */
    public String getUserId() { return userId; }

    /** JWT 唯一 ID — 单设备登录校验关键字段 */
    public String getJti() { return jti; }

    /** 签发时间 */
    public Date getIssuedAt() { return issuedAt; }

    /** 过期时间 */
    public Date getExpiration() { return expiration; }

    /** 获取额外字段（Object） */
    public Object get(String key) { return extra.get(key); }

    /** 获取额外字段（String） */
    public String getString(String key) { return (String) extra.get(key); }
}
