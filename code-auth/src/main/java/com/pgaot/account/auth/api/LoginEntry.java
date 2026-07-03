package com.pgaot.account.auth.api;

import com.pgaot.account.auth.api.LoginType;

import com.pgaot.account.auth.api.model.LoginResult;
import com.pgaot.account.auth.api.model.LoginUser;

import com.pgaot.account.auth.exception.LoginException;
import com.pgaot.account.auth.core.LoginService;
import com.pgaot.sql.api.JpaTemplate;
import com.pgaot.sql.jpa.entity.ApiTokenEntity;
import com.pgaot.sql.jpa.repository.TokenRepository;

import com.pgaot.account.auth.core.yuntower.YuntowerAuthFactory;

import java.util.Map;

/**
 * 统一登录入口.
 *
 * <p>环境变量: YUNTOWER_APP_ID, YUNTOWER_APP_SECRET, CODE_AUTH_JWT_SECRET
 *
 * <pre>{@code
 * LoginResult r = LoginEntry.login(LoginType.YUNTOWER, Map.of("code", "xxx"));
 * if (r.isSuccess()) {
 *     LoginUser u = LoginEntry.validate(r.getAccessToken());
 *     LoginEntry.logout(r.getAccessToken());
 * }
 * }</pre>
 */
public final class LoginEntry {

    private static final LoginService SERVICE = YuntowerAuthFactory.fromEnv();
    private static final ApiTokenManager TOKENS = new ApiTokenManager(
            new TokenRepository(JpaTemplate.fromEnv("", true, ApiTokenEntity.class)));

    private LoginEntry() {}

    /**
     * 登录 — 不抛异常，通过 {@link LoginResult#isSuccess()} 判断成败.
     *
     * @param type   登录方式，使用 {@link LoginType} 常量
     * @param params 登录参数
     * @return 成功时含 accessToken + 用户信息，失败时含 code + message
     */
    public static LoginResult login(String type, Map<String, Object> params) {
        try {
            return SERVICE.login(type, params);
        } catch (LoginException e) {
            return new LoginResult(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return new LoginResult(-1, e.getMessage());
        }
    }

    /**
     * 校验 JWT + 单设备登录检查.
     *
     * @param jwtToken 请求头 Authorization: Bearer 后面的 token
     * @return 当前登录用户信息
     * @throws LoginException token无效 / 过期 / 被挤下线
     */
    public static LoginUser validate(String jwtToken) {
        return SERVICE.validate(jwtToken);
    }

    /** 退出登录 — 删除 token 存储记录 */
    public static void logout(String jwtToken) {
        SERVICE.logout(jwtToken);
    }

    /** API Token 管理 */
    public static ApiTokenManager tokens() { return TOKENS; }
}
