package com.pgaot.account.auth.api;

import com.pgaot.account.auth.common.model.TokenInfo;
import com.pgaot.account.auth.core.token.TokenGenerator;
import com.pgaot.account.auth.core.token.scope.Scope;
import com.pgaot.account.auth.exception.LoginException;
import com.pgaot.sql.jpa.entity.ApiTokenEntity;
import com.pgaot.sql.jpa.repository.TokenRepository;

import java.util.Arrays;
import java.util.List;

/**
 * API Token 管理器 — 第三方令牌的创建/校验/吊销.
 *
 * <pre>{@code
 * // 创建
 * TokenInfo t = LoginEntry.tokens().create("alice", "数据表只读",
 *     List.of("datasheet:read:*"));
 * String token = t.getToken(); // pat_xxx... 仅此时可见
 *
 * // 校验 + scope 检查
 * LoginEntry.tokens().validate("pat_xxx", "datasheet:read:table:123");
 *
 * // 吊销
 * LoginEntry.tokens().revoke(alice, tokenId);
 * }</pre>
 */
public class ApiTokenManager {

    private final TokenRepository repo;

    public ApiTokenManager(TokenRepository repo) { this.repo = repo; }

    /** 创建 token，返回含完整 token 的 TokenInfo（仅展示一次） */
    public TokenInfo create(String userId, String name, List<String> scopes) {
        return create(userId, name, scopes, null);
    }

    /** 创建带过期时间的 token */
    public TokenInfo create(String userId, String name, List<String> scopes, Long expiresAtSec) {
        String token = TokenGenerator.generate();
        String hash = TokenGenerator.hash(token);
        String scopesJson = "[" + String.join(",", scopes.stream().map(s -> "\"" + s + "\"").toList()) + "]";
        java.time.LocalDateTime expires = expiresAtSec != null
                ? java.time.LocalDateTime.ofEpochSecond(expiresAtSec, 0,
                        java.time.ZoneOffset.ofHours(8))
                : null;
        ApiTokenEntity entity = repo.create(userId, name, hash,
                TokenGenerator.prefix(token), scopesJson, expires);
        TokenInfo info = toInfo(entity);
        info.setToken(token);
        return info;
    }

    /** 校验 token，返回 userId。可选 scope 检查 */
    public String validate(String token, String requiredScope) {
        String hash = TokenGenerator.hash(token);
        ApiTokenEntity entity = repo.findByHash(hash);
        if (entity == null) throw LoginException.apiTokenInvalid("token 无效");

        if (entity.getExpiresAt() != null
                && entity.getExpiresAt().isBefore(java.time.LocalDateTime.now()))
            throw LoginException.apiTokenInvalid("token 已过期");

        if (requiredScope != null) {
            List<String> scopes = parseScopes(entity.getScopes());
            if (!Scope.matchesAny(scopes, requiredScope))
                throw LoginException.apiTokenScopeDenied(requiredScope);
        }

        repo.touchLastUsed(entity.getId());
        return entity.getUserId();
    }

    private static List<String> parseScopes(String json) {
        if (json == null || json.isBlank()) return List.of();
        return Arrays.stream(json.replace("[","").replace("]","").replace("\"","").split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** 吊销 token（含所有权校验） */
    public void revoke(String ownerId, long tokenId) {
        if (!repo.revoke(ownerId, tokenId))
            throw LoginException.apiTokenInvalid("token 不存在或无权操作");
    }

    /** 列出用户的所有 token */
    public List<TokenInfo> list(String userId) {
        return repo.listByUser(userId).stream().map(this::toInfo).toList();
    }

    private TokenInfo toInfo(ApiTokenEntity e) {
        TokenInfo t = new TokenInfo();
        t.setId(e.getId());
        t.setUserId(e.getUserId());
        t.setName(e.getName());
        t.setPrefix(e.getPrefix());
        t.setScopes(e.getScopes());
        if (e.getCreatedAt() != null) t.setCreatedAt(e.getCreatedAt().toString());
        if (e.getLastUsed() != null) t.setLastUsed(e.getLastUsed().toString());
        if (e.getExpiresAt() != null) t.setExpiresAt(e.getExpiresAt().toString());
        return t;
    }
}
