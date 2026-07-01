package com.pgaot.account.auth;

import com.pgaot.account.auth.api.store.RedisTokenStore;
import com.pgaot.account.auth.api.store.TokenStore;

/** TokenStore 测试 — Redis 持久化 */
public class TokenStoreTest {

    public static void main(String[] args) {
        String uri = System.getenv("CODE_AUTH_REDIS_URI");
        if (uri == null || uri.isBlank()) {
            System.err.println("请设置 CODE_AUTH_REDIS_URI");
            return;
        }

        TokenStore store = new RedisTokenStore(uri);

        store.save("user-1", "jti-a", 43200);
        System.out.println("设备A登录: jti-a → " + store.getJti("user-1"));

        store.save("user-1", "jti-b", 43200);
        System.out.println("设备B登录: jti-b → " + store.getJti("user-1"));

        System.out.println("\n现在去查 Redis: redis-cli -a vifanlyrs -n 1 get '" + TokenStore.key("user-1") + "'");
        System.out.println("按回车删除...");
        try { System.in.read(); } catch (Exception ignored) {}

        store.remove("user-1");
        System.out.println("已删除");
    }
}
