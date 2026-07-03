package com.pgaot.account.auth.core.token.scope;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限范围 — 格式: module:action:target，支持 * 通配符.
 *
 * <pre>
 * datasheet:read:table:123   → 模块=datasheet, 操作=read, 目标=table:123
 * datasheet:read:*            → 任意目标
 * datasheet:*:*               → datasheet 下任意操作
 * *:*:*                       → 超级管理员
 * </pre>
 */
public class Scope {

    private final String value;
    private final String[] parts;

    public Scope(String value) {
        this.value = value;
        this.parts = value.split(":");
    }

    /** 检查此 scope 是否覆盖 required */
    public boolean matches(String required) {
        if ("*".equals(parts[0])) return true; // *:*:* matches everything
        String[] requiredParts = required.split(":");
        int maxLen = Math.max(parts.length, requiredParts.length);
        for (int i = 0; i < maxLen; i++) {
            String sp = i < parts.length ? parts[i] : "*";
            String rp = i < requiredParts.length ? requiredParts[i] : "*";
            if ("*".equals(sp) || "*".equals(rp)) continue;
            if (!sp.equals(rp)) return false;
        }
        return true;
    }

    /** 检查 scopes 列表中是否有任一匹配 required */
    public static boolean matchesAny(List<String> scopes, String required) {
        if (scopes == null || scopes.contains("*:*:*")) return true;
        return scopes.stream().anyMatch(s -> new Scope(s).matches(required));
    }

    public String value() { return value; }

    @Override public String toString() { return value; }

    // ════════════════════════════════════════
    // 预定义 Scope — 仅映射实际存在的功能

    // ════════════════════════════════════════

    /** code-datasheet 数据表模块 */
    public static class Datasheet {
        public static final String DATA = "datasheet:data";  // 数据操作（读写删）
    }

    /** 全局管理员 */
    public static final String SUPER = "*:*:*";
}
