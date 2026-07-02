package com.pgaot.account.auth.core.jwt;

import com.pgaot.account.auth.exception.LoginException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** JWT 生成 + 解析 + 验证 */
public class JwtUtil {

    private final SecretKey key;
    private final long accessExpires;
    private final long refreshExpires;

    /**
     * @param secret                签名密钥
     * @param accessExpiresSeconds  access_token 有效秒数
     * @param refreshExpiresSeconds refresh_token 有效秒数
     */
    public JwtUtil(String secret, long accessExpiresSeconds, long refreshExpiresSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpires = accessExpiresSeconds;
        this.refreshExpires = refreshExpiresSeconds;
    }

    /** 生成 JWT token 对（access + refresh） */
    public TokenPair generate(String userId, Map<String, Object> extra) {
        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        String accessToken = buildToken(userId, jti, extra, now, accessExpires);
        String refreshToken = buildToken(userId, jti, extra, now, refreshExpires);
        return new TokenPair(accessToken, refreshToken, jti, accessExpires);
    }

    /**
     * 验证 JWT 并返回 payload.
     *
     * @throws LoginException TOKEN_INVALID
     */
    public TokenClaims validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload();
            Map<String, Object> extra = new HashMap<>();
            claims.forEach((k, v) -> {
                if (!"sub".equals(k) && !"jti".equals(k) && !"iat".equals(k) && !"exp".equals(k))
                    extra.put(k, v);
            });
            return new TokenClaims(claims.getSubject(), claims.getId(),
                    claims.getIssuedAt(), claims.getExpiration(), extra);
        } catch (JwtException | IllegalArgumentException e) {
            throw LoginException.tokenInvalid(e.getMessage());
        }
    }

    private String buildToken(String userId, String jti, Map<String, Object> extra,
                              Date now, long expires) {
        var builder = Jwts.builder()
                .subject(userId).id(jti)
                .issuedAt(now).expiration(new Date(now.getTime() + expires * 1000));
        if (extra != null) extra.forEach(builder::claim);
        return builder.signWith(key).compact();
    }
}
