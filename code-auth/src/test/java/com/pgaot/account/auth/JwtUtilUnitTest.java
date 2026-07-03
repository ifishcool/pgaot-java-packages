package com.pgaot.account.auth;

import com.pgaot.account.auth.core.jwt.JwtUtil;
import com.pgaot.account.auth.core.jwt.TokenClaims;
import com.pgaot.account.auth.core.jwt.TokenPair;
import com.pgaot.account.auth.exception.LoginException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilUnitTest {
    private final JwtUtil jwt = new JwtUtil("test-secret-key-min-32-chars!!!!!", 3600, 7200);

    @Test
    void shouldGenerateAndValidateToken() {
        TokenPair pair = jwt.generate("user-1", Map.of("nickname", "Alice"));
        assertNotNull(pair.accessToken());
        assertNotNull(pair.refreshToken());

        TokenClaims claims = jwt.validate(pair.accessToken());
        assertEquals("user-1", claims.getUserId());
        assertEquals("Alice", claims.getString("nickname"));
    }

    @Test
    void shouldRejectInvalidToken() {
        assertThrows(LoginException.class, () -> jwt.validate("invalid.token.here"));
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtUtil shortLived = new JwtUtil("test-secret-key-min-32-chars!!!!!", -1, -1);
        TokenPair pair = shortLived.generate("user-1", null);
        assertThrows(LoginException.class, () -> shortLived.validate(pair.accessToken()));
    }
}
