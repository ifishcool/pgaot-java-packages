package com.pgaot.account.auth.core.jwt;

/**
 * JWT 工具类内部使用的 access_token + refresh_token 对
 */
public record TokenPair(String accessToken, String refreshToken, String jti, long expiresIn) {

    /**
     * 访问凭证
     */
    @Override
    public String accessToken() {
        return accessToken;
    }

    /**
     * 刷新凭证
     */
    @Override
    public String refreshToken() {
        return refreshToken;
    }

    /**
     * JWT 唯一 ID
     */
    @Override
    public String jti() {
        return jti;
    }

    /**
     * 过期时间（秒）
     */
    @Override
    public long expiresIn() {
        return expiresIn;
    }
}
