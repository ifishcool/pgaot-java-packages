package com.pgaot.account.auth.core.strategy;

import java.util.Map;

/**
 * 登录策略接口 — 扩展新登录方式只需实现此接口.
 *
 * <p>实现类需要完成"登录参数 → 第三方API调用 → 返回用户信息"的完整流程.
 * 如果用户不存在，策略内部自行注册并返回新的 UserInfo.
 *
 * <pre>{@code
 * public class WechatStrategy implements LoginStrategy {
 *     public UserInfo authenticate(Map<String, Object> params) {
 *         String code = (String) params.get("code");
 *         // 调微信 API 获取 openid + 用户信息
 *         // 查 DB: openid 对应哪个本地 userId？不存在则注册
 *         return new UserInfo("localUserId", "昵称", "头像");
 *     }
 * }
 * }</pre>
 */
public interface LoginStrategy {

    /**
     * 认证用户 — 策略内部处理"首次登录自动注册"逻辑.
     *
     * @param params 登录参数，由调用方传入，如 Map.of("code", "xxx")
     * @return 用户信息（userId 为你在自己系统中的用户标识）
     */
    UserInfo authenticate(Map<String, Object> params);
}
