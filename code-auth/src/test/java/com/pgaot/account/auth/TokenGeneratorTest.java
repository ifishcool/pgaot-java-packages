package com.pgaot.account.auth;

import com.pgaot.account.auth.core.token.TokenGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenGeneratorTest {

    @Test
    void shouldGeneratePatPrefixToken() {
        String token = TokenGenerator.generate();
        assertTrue(token.startsWith("pat_"));
        assertTrue(token.length() > 12);
    }

    @Test
    void shouldGenerateUniqueTokens() {
        String t1 = TokenGenerator.generate();
        String t2 = TokenGenerator.generate();
        assertNotEquals(t1, t2);
    }

    @Test
    void shouldHashConsistently() {
        String hash = TokenGenerator.hash("hello");
        assertEquals(64, hash.length()); // SHA-256 hex = 64 chars
        assertEquals(hash, TokenGenerator.hash("hello"));
    }

    @Test
    void shouldFormatPrefixAndSuffix() {
        String token = "pat_abc123def456ghi789";
        assertEquals("pat_abc123de", TokenGenerator.prefix(token));
        assertEquals("...i789", TokenGenerator.suffix(token));

        assertEquals("short", TokenGenerator.suffix("short"));
        assertEquals("sh", TokenGenerator.prefix("sh"));
    }
}
