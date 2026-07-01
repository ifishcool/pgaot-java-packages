package com.pgaot.account.auth.common.util;

/** 参数校验工具 */
public final class Assert {
    private Assert() {}

    /** 字符串为 null 或空白时抛出 IllegalArgumentException */
    public static void notBlank(String str, String message) {
        if (str == null || str.isBlank()) throw new IllegalArgumentException(message);
    }
}
