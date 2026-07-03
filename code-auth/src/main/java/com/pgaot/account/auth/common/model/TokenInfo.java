package com.pgaot.account.auth.common.model;

/** API Token 信息 — 创建成功时返回，后续不再展示完整 token */
public class TokenInfo {
    private Long id;
    private String userId;
    private String name;
    private String token;         // 完整 token（仅创建时返回一次）
    private String prefix;        // pat_xxxx（日志标识用）
    private String scopes;        // JSON 数组: ["datasheet:read:*"]
    private String lastUsed;
    private String expiresAt;
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public String getUserId() { return userId; }
    public void setUserId(String v) { userId = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getToken() { return token; }
    public void setToken(String v) { token = v; }
    public String getPrefix() { return prefix; }
    public void setPrefix(String v) { prefix = v; }
    public String getScopes() { return scopes; }
    public void setScopes(String v) { scopes = v; }
    public String getLastUsed() { return lastUsed; }
    public void setLastUsed(String v) { lastUsed = v; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String v) { expiresAt = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { createdAt = v; }
}
