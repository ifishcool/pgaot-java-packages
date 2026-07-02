package com.pgaot.account.auth.core.strategy;

import com.pgaot.account.auth.exception.LoginException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 策略注册中心 — 按 key 注册和查找 LoginStrategy.
 *
 * <pre>{@code
 * StrategyRegistry registry = new StrategyRegistry()
 *     .register(LoginType.YUNTOWER, new YuntowerStrategy(...))
 *     .register("wechat", new WechatStrategy(...));
 * }</pre>
 */
public class StrategyRegistry {

    private final Map<String, LoginStrategy> strategies = new ConcurrentHashMap<>();

    /**
     * 注册策略.
     *
     * @param key      登录方式标识，如 LoginType.YUNTOWER
     * @param strategy 策略实现
     * @return this（链式调用）
     */
    public StrategyRegistry register(String key, LoginStrategy strategy) {
        strategies.put(key, strategy);
        return this;
    }

    /**
     * 查找策略.
     *
     * @param key 登录方式标识
     * @throws LoginException 未注册时抛出
     */
    public LoginStrategy get(String key) {
        LoginStrategy s = strategies.get(key);
        if (s == null) throw LoginException.unsupportedType(key);
        return s;
    }
}
