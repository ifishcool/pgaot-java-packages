package com.pgaot.account.auth.core;

import com.pgaot.account.auth.common.config.LoginConfig;
import com.pgaot.account.auth.core.jwt.JwtUtil;
import com.pgaot.account.auth.core.jwt.TokenClaims;
import com.pgaot.account.auth.core.jwt.TokenPair;
import com.pgaot.account.auth.core.strategy.LoginStrategy;
import com.pgaot.account.auth.core.strategy.StrategyRegistry;
import com.pgaot.account.auth.core.strategy.UserInfo;

import com.pgaot.account.auth.exception.LoginException;
import com.pgaot.account.auth.api.store.TokenStore;

import com.pgaot.account.auth.api.model.LoginResult;
import com.pgaot.account.auth.api.model.LoginUser;

import java.util.HashMap;
import java.util.Map;

/** 核心认证引擎 — 策略认证 → JWT 生成 → token 存储 → 校验 */
public class LoginService {

    private final StrategyRegistry registry;
    private final JwtUtil jwt;
    private final TokenStore tokenStore;

    public LoginService(StrategyRegistry registry, LoginConfig config, TokenStore tokenStore) {
        this.registry = registry;
        this.jwt = new JwtUtil(config.getJwtSecret(), config.getAccessExpires(), config.getRefreshExpires());
        this.tokenStore = tokenStore;
    }

    /** 登录：选策略 → 认证 → 生成 JWT → 存 token → 返回 */
    public LoginResult login(String type, Map<String, Object> params) {
        LoginStrategy strategy = registry.get(type);
        UserInfo userInfo = strategy.authenticate(params);

        Map<String, Object> extra = new HashMap<>();
        extra.put("nickname", userInfo.getNickname());
        extra.put("avatar", userInfo.getAvatar());
        if (userInfo.getExtra() != null) extra.putAll(userInfo.getExtra());

        TokenPair pair = jwt.generate(userInfo.getUserId(), extra);
        tokenStore.save(userInfo.getUserId(), pair.jti(), pair.expiresIn());

        return new LoginResult(pair.accessToken(), pair.refreshToken(),
                userInfo.getUserId(), userInfo.getNickname(), userInfo.getAvatar());
    }

    /** 校验：解析 JWT → 单设备登录检查 */
    public LoginUser validate(String jwtToken) {
        TokenClaims claims = jwt.validate(jwtToken);
        String currentJti = tokenStore.getJti(claims.getUserId());
        if (currentJti != null && !claims.getJti().equals(currentJti)) {
            throw LoginException.tokenKicked();
        }
        return new LoginUser(claims);
    }

    /** 退出：删除 token 存储记录 */
    public void logout(String jwtToken) {
        TokenClaims claims = jwt.validate(jwtToken);
        tokenStore.remove(claims.getUserId());
    }
}
