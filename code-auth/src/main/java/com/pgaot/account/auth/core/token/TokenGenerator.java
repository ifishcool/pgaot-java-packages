package com.pgaot.account.auth.core.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Base64;

/** pat_ 前缀 + 随机字节 → SHA-256 哈希 */
public class TokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "pat_";

    /** 生成新 token: pat_ + 32字节随机 → Base64 */
    public static String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 哈希 */
    public static String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }

    /** 取 token 前缀用于日志显示: pat_xxxxxxxx */
    public static String prefix(String token) {
        return token.length() >= 12 ? token.substring(0, 12) : token;
    }

    /** 取 token 尾巴用于用户识别: ...xxxx */
    public static String suffix(String token) {
        return token.length() >= 8 ? "..." + token.substring(token.length() - 4) : token;
    }
}
