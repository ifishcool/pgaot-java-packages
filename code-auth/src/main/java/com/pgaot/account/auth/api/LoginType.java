package com.pgaot.account.auth.api;

/**
 * 登录方式常量
 *
 * <pre>{@code
 * LoginEntry.login(LoginType.YUNTOWER, Map.of("code", "xxx"));
 * }</pre>
 */
public final class LoginType {
    private LoginType() {}

    /** 云塔 OAuth 登录 — params: {code: "xxx"} */
    public static final String YUNTOWER = "yuntower";
}
