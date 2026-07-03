package com.pgaot.sql.jpa.repository;

import com.pgaot.sql.api.JpaTemplate;
import com.pgaot.sql.jpa.entity.ApiTokenEntity;

import java.util.List;

/**
 * API Token 仓储.
 */
public class TokenRepository {

    private final JpaTemplate jpa;

    public TokenRepository(JpaTemplate jpa) { this.jpa = jpa; }

    /** 创建 token */
    public ApiTokenEntity create(String userId, String name, String tokenHash,
                                  String prefix, String scopes, java.time.LocalDateTime expiresAt) {
        ApiTokenEntity t = new ApiTokenEntity();
        t.setUserId(userId);
        t.setName(name);
        t.setTokenHash(tokenHash);
        t.setPrefix(prefix);
        t.setScopes(scopes);
        t.setExpiresAt(expiresAt);
        jpa.save(t);
        return t;
    }

    /** 按哈希查找 */
    public ApiTokenEntity findByHash(String tokenHash) {
        List<ApiTokenEntity> list = jpa.query(
                "FROM ApiTokenEntity WHERE tokenHash = ?1", ApiTokenEntity.class, tokenHash);
        return list.isEmpty() ? null : list.getFirst();
    }

    /** 用户的所有 token */
    public List<ApiTokenEntity> listByUser(String userId) {
        return jpa.query("FROM ApiTokenEntity WHERE userId = ?1 ORDER BY createdAt DESC",
                ApiTokenEntity.class, userId);
    }

    /** 吊销（含所有权校验） */
    public boolean revoke(String userId, Long tokenId) {
        ApiTokenEntity t = jpa.findById(ApiTokenEntity.class, tokenId);
        if (t == null || !t.getUserId().equals(userId)) return false;
        jpa.delete(ApiTokenEntity.class, tokenId);
        return true;
    }

    /** 更新最后使用时间 */
    public void touchLastUsed(Long id) {
        ApiTokenEntity t = jpa.findById(ApiTokenEntity.class, id);
        if (t != null) {
            t.setLastUsed(java.time.LocalDateTime.now());
            jpa.update(t);
        }
    }
}
