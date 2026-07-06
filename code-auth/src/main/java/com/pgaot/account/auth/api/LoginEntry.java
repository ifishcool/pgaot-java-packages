package com.pgaot.account.auth.api;

import java.util.Map;
import java.util.function.Supplier;

import com.pgaot.account.auth.api.model.LoginResult;
import com.pgaot.account.auth.api.model.LoginUser;
import com.pgaot.account.auth.core.LoginService;
import com.pgaot.account.auth.core.yuntower.YuntowerAuthFactory;
import com.pgaot.account.auth.exception.LoginException;
import com.pgaot.sql.api.JpaTemplate;
import com.pgaot.sql.jpa.entity.ApiTokenEntity;
import com.pgaot.sql.jpa.repository.TokenRepository;

/**
 * 统一登录入口.
 *
 * <p>
 * 环境变量: YUNTOWER_APP_ID, YUNTOWER_APP_SECRET, CODE_AUTH_JWT_SECRET
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

    private static Supplier<LoginService> serviceProvider = YuntowerAuthFactory::fromEnv;
    private static Supplier<ApiTokenManager> tokenProvider = LoginEntry::createDefaultTokenManager;
    private static LoginService service;
    private static ApiTokenManager tokens;

    private LoginEntry() {
    }

    public static synchronized void configure(LoginService loginService, ApiTokenManager tokenManager) {
        serviceProvider = () -> loginService;
        tokenProvider = () -> tokenManager;
        service = loginService;
        tokens = tokenManager;
    }

    public static synchronized void configureProviders(Supplier<LoginService> loginServiceProvider,
            Supplier<ApiTokenManager> tokenManagerProvider) {
        serviceProvider = loginServiceProvider;
        tokenProvider = tokenManagerProvider;
        service = null;
        tokens = null;
    }

    public static synchronized void resetDefaults() {
        serviceProvider = YuntowerAuthFactory::fromEnv;
        tokenProvider = LoginEntry::createDefaultTokenManager;
        service = null;
        tokens = null;
    }

    /**
     * 登录 — 不抛异常，通过 {@link LoginResult#isSuccess()} 判断成败.
     *
     * @param type   登录方式，使用 {@link LoginType} 常量
     * @param params 登录参数
     * @return 成功时含 accessToken + 用户信息，失败时含 code + message
     */
    public static LoginResult login(String type, Map<String, Object> params) {
        try {
            return getService().login(type, params);
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
        return getService().validate(jwtToken);
    }

    /** 退出登录 — 删除 token 存储记录 */
    public static void logout(String jwtToken) {
        getService().logout(jwtToken);
    }

    /** API Token 管理 */
    public static ApiTokenManager tokens() {
        return getTokens();
    }

    private static synchronized LoginService getService() {
        if (service == null) {
            service = serviceProvider.get();
        }
        return service;
    }

    private static synchronized ApiTokenManager getTokens() {
        if (tokens == null) {
            tokens = tokenProvider.get();
        }
        return tokens;
    }

    private static ApiTokenManager createDefaultTokenManager() {
        return new ApiTokenManager(new TokenRepository(JpaTemplate.fromEnv("", true, ApiTokenEntity.class)));
    }
}
