package com.pgaot.account.auth.api.model;

import com.pgaot.account.auth.core.jwt.TokenClaims;

/** 从 JWT 解析出的当前登录用户 */
public class LoginUser {

    private final TokenClaims claims;

    public LoginUser(TokenClaims claims) { this.claims = claims; }

    /** 用户唯一标识 */
    public String getUserId() { return claims.getUserId(); }

    /** JWT 唯一 ID（单设备登录校验用） */
    public String getJti() { return claims.getJti(); }

    /** 额外字段 */
    public Object get(String key) { return claims.get(key); }

    /** 额外字段（字符串） */
    public String getString(String key) { return claims.getString(key); }
}
