package com.pgaot.account.auth;

import com.pgaot.account.auth.api.store.RedisTokenStore;
import com.pgaot.account.auth.api.store.TokenStore;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
class TokenStoreTest {

    private static TokenStore store;

    @BeforeAll
    static void requireRedis() {
        EnvLoader.load();
        String uri = EnvLoader.get("CODE_AUTH_REDIS_URI");
        assumeTrue(uri != null && !uri.isBlank(), "跳过：需要 Redis");
        store = new RedisTokenStore(uri);
    }

    @Test
    void shouldSaveAndRetrieveJti() {
        store.save("user-1", "jti-a", 43200);
        assertEquals("jti-a", store.getJti("user-1"));
    }

    @Test
    void shouldOverwriteJtiOnSecondLogin() {
        store.save("user-1", "jti-a", 43200);
        store.save("user-1", "jti-b", 43200);
        assertEquals("jti-b", store.getJti("user-1"));
    }

    @AfterAll
    static void cleanup() {
        if (store != null) store.remove("user-1");
    }
}
