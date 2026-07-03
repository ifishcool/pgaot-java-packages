package com.pgaot.account.auth;

import com.pgaot.account.auth.api.LoginEntry;
import com.pgaot.account.auth.common.model.TokenInfo;
import com.pgaot.account.auth.core.token.scope.Scope;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiTokenTest {

    private static final String USER = "alice-junit";
    private static String token, superToken;
    private static long superTokenId;

    @BeforeAll
    static void requireDb() {
        assumeTrue(EnvLoader.hasDb(), "跳过：需要数据库");
    }

    @BeforeAll
    static void cleanOldTokens() {
        try {
            for (TokenInfo old : LoginEntry.tokens().list(USER))
                LoginEntry.tokens().revoke(USER, old.getId());
        } catch (Exception ignored) {}
    }

    @Test @Order(1)
    void createDatasheetToken() {
        TokenInfo t = LoginEntry.tokens().create(USER, "数据表访问", List.of(Scope.Datasheet.DATA));
        assertNotNull(t.getToken());
        assertTrue(t.getToken().startsWith("pat_"));
        token = t.getToken();
    }

    @Test @Order(2)
    void createSuperAdminToken() {
        TokenInfo t = LoginEntry.tokens().create(USER, "超级管理员", List.of(Scope.SUPER));
        assertTrue(t.getToken().startsWith("pat_"));
        superToken = t.getToken();
        superTokenId = t.getId();
    }

    @Test @Order(3)
    void validateWithCorrectScope() {
        String uid = LoginEntry.tokens().validate(token, "datasheet:data");
        assertEquals(USER, uid);
    }

    @Test @Order(4)
    void rejectWrongScope() {
        assertThrows(Exception.class, () ->
                LoginEntry.tokens().validate(token, "storage:upload"));
    }

    @Test @Order(5)
    void superAdminPassesAnyScope() {
        assertDoesNotThrow(() ->
                LoginEntry.tokens().validate(superToken, "storage:upload"));
    }

    @Test @Order(6)
    void rejectFakeToken() {
        assertThrows(Exception.class, () ->
                LoginEntry.tokens().validate("pat_fake_1234567890abcdef", "datasheet:data"));
    }

    @Test @Order(7)
    void listAllTokens() {
        List<TokenInfo> list = LoginEntry.tokens().list(USER);
        assertEquals(2, list.size());
    }

    @Test @Order(8)
    void revokeToken() {
        LoginEntry.tokens().revoke(USER, superTokenId);
    }

    @Test @Order(9)
    void revokedTokenFailsValidation() {
        assertThrows(Exception.class, () ->
                LoginEntry.tokens().validate(superToken, "datasheet:data"));
    }

    @Test @Order(10)
    void listShowsOneRemaining() {
        assertEquals(1, LoginEntry.tokens().list(USER).size());
    }
}
