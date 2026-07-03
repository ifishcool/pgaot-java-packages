package com.pgaot.account.auth;

import com.pgaot.account.auth.core.token.scope.Scope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScopeTest {

    @Test
    void shouldMatchExactScope() {
        Scope s = new Scope("datasheet:read");
        assertTrue(s.matches("datasheet:read"));
    }

    @Test
    void shouldRejectDifferentModule() {
        Scope s = new Scope("datasheet:read");
        assertFalse(s.matches("storage:upload"));
    }

    @Test
    void shouldMatchWildcard() {
        Scope s = new Scope("datasheet:*");
        assertTrue(s.matches("datasheet:read"));
        assertTrue(s.matches("datasheet:write"));
    }

    @Test
    void superAdminMatchesAll() {
        assertTrue(Scope.matchesAny(List.of("*:*:*"), "datasheet:data"));
        assertTrue(Scope.matchesAny(List.of("*:*:*"), "storage:upload"));
        assertTrue(Scope.matchesAny(List.of("*:*:*"), "anything:at:all:deep"));
    }

    @Test
    void shouldMatchAnyFromList() {
        assertTrue(Scope.matchesAny(List.of("datasheet:data", "storage:upload"),
                "datasheet:data"));
        assertFalse(Scope.matchesAny(List.of("datasheet:data"),
                "storage:upload"));
    }

    @Test
    void nullScopesShouldFail() {
        assertFalse(Scope.matchesAny(null, "datasheet:data"));
    }
}
