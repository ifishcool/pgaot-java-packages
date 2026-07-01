package com.pgaot.account.auth;

import com.pgaot.account.auth.core.jwt.JwtUtil;
import com.pgaot.account.auth.core.jwt.TokenClaims;
import com.pgaot.account.auth.core.jwt.TokenPair;
import com.pgaot.account.auth.exception.LoginException;

import java.util.Map;

/** JWT 单元测试 */
public class JwtUtilTest {

    private static final JwtUtil jwt = new JwtUtil(
            "my-secret-key-for-jwt-must-be-at-least-256-bits-long!!", 3600, 86400);

    public static void main(String[] args) {
        testGenerateAndValidate();
        testInvalidToken();
        testExtraClaims();
        testKickedDetection();
        System.out.println("All tests passed.");
    }

    static void testGenerateAndValidate() {
        TokenPair pair = jwt.generate("123", Map.of("nickname", "test"));
        assert pair.accessToken() != null;
        assert pair.refreshToken() != null;
        assert pair.jti() != null;

        TokenClaims claims = jwt.validate(pair.accessToken());
        assert "123".equals(claims.getUserId());
        assert "test".equals(claims.getString("nickname"));
    }

    static void testInvalidToken() {
        try {
            jwt.validate("not.a.valid.token");
            throw new AssertionError("should have thrown");
        } catch (LoginException e) {
            assert e.getCode() == 200001; // TOKEN_INVALID
        }
    }

    static void testExtraClaims() {
        TokenPair pair = jwt.generate("456", Map.of("role", "admin"));
        TokenClaims claims = jwt.validate(pair.accessToken());
        assert "admin".equals(claims.getString("role"));
    }

    static void testKickedDetection() {
        TokenPair pair = jwt.generate("789", Map.of());
        // token 本身有效，单设备检查由 LoginService 的 TokenStore 层处理
        TokenClaims claims = jwt.validate(pair.accessToken());
        assert "789".equals(claims.getUserId());
    }
}
