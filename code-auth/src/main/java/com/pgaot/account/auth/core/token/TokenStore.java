package com.pgaot.account.auth.core.token;

import com.pgaot.account.auth.common.model.TokenInfo;
import com.pgaot.sql.api.SqlTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API Token 存储 — MySQL 实现（通过 code-sql）.
 */
public class TokenStore {

    private final SqlTemplate sql;

    public TokenStore(SqlTemplate sql) {
        this.sql = sql;
        initTable();
    }

    private void initTable() {
        sql.unsafe("CREATE TABLE IF NOT EXISTS api_token (" +
                "id BIGINT NOT NULL AUTO_INCREMENT, " +
                "user_id VARCHAR(64) NOT NULL, " +
                "name VARCHAR(128) DEFAULT NULL, " +
                "token_hash VARCHAR(64) NOT NULL UNIQUE, " +
                "prefix VARCHAR(12) NOT NULL, " +
                "scopes TEXT NOT NULL, " +
                "last_used DATETIME DEFAULT NULL, " +
                "expires_at DATETIME DEFAULT NULL, " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (id))");
    }

    /** 创建 token */
    public TokenInfo create(String userId, String name, String tokenHash, String prefix, String scopes, Long expiresAtSec) {
        sql.sql("INSERT INTO api_token (user_id, name, token_hash, prefix, scopes) VALUES (?,?,?,?,?)",
                userId, name, tokenHash, prefix, scopes);
        List<Map<String, Object>> rows = sql.sql(
                "SELECT * FROM api_token WHERE token_hash=? ORDER BY id DESC LIMIT 1", tokenHash);
        if (rows.isEmpty()) return null;
        return mapToInfo(rows.get(0));
    }

    /** 根据哈希查找 token */
    public Map<String, Object> findByHash(String tokenHash) {
        List<Map<String, Object>> rows = sql.sql(
                "SELECT * FROM api_token WHERE token_hash=?", tokenHash);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 获取用户所有 token */
    public List<TokenInfo> listByUser(String userId) {
        return sql.<List<Map<String, Object>>>sql(
                "SELECT * FROM api_token WHERE user_id=? ORDER BY created_at DESC", userId)
                .stream().map(this::mapToInfo).collect(Collectors.toList());
    }

    /** 吊销 token */
    public void revoke(long id) {
        sql.sql("DELETE FROM api_token WHERE id=?", id);
    }

    /** 更新最后使用时间 */
    public void updateLastUsed(long id) {
        sql.sql("UPDATE api_token SET last_used=NOW() WHERE id=?", id);
    }

    private TokenInfo mapToInfo(Map<String, Object> row) {
        TokenInfo t = new TokenInfo();
        t.setId(((Number) row.get("id")).longValue());
        t.setUserId((String) row.get("user_id"));
        t.setName((String) row.get("name"));
        t.setPrefix((String) row.get("prefix"));
        t.setScopes((String) row.get("scopes"));
        if (row.get("last_used") != null) t.setLastUsed(row.get("last_used").toString());
        if (row.get("expires_at") != null) t.setExpiresAt(row.get("expires_at").toString());
        t.setCreatedAt(row.get("created_at").toString());
        return t;
    }
}
