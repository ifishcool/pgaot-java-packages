package com.pgaot.account.auth.common.config;

/**
 * JWT 配置.
 */
public class LoginConfig {

    private final String jwtSecret;
    private final long accessExpires;
    private final long refreshExpires;

    /** access_token 默认 12h, refresh_token 默认 24d */
    public LoginConfig(String jwtSecret) {
        this(jwtSecret, 43200, 2073600);
    }

    /**
     * @param accessExpiresSeconds  access_token 有效期（秒）
     * @param refreshExpiresSeconds refresh_token 有效期（秒）
     */
    public LoginConfig(String jwtSecret, long accessExpiresSeconds, long refreshExpiresSeconds) {
        this.jwtSecret = jwtSecret;
        this.accessExpires = accessExpiresSeconds;
        this.refreshExpires = refreshExpiresSeconds;
    }

    /** JWT 签名密钥 */
    public String getJwtSecret() { return jwtSecret; }

    /** access_token 有效秒数 */
    public long getAccessExpires() { return accessExpires; }

    /** refresh_token 有效秒数 */
    public long getRefreshExpires() { return refreshExpires; }
}
