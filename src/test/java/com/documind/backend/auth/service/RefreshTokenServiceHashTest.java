package com.documind.backend.auth.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenServiceHashTest {

    @Test
    void hash_isSha256Hex64Chars() {
        String hex = RefreshTokenService.hash("sample-refresh-token");
        assertEquals(64, hex.length());
        assertTrue(hex.matches("[0-9a-f]+"));
        assertEquals(hex, RefreshTokenService.hash("sample-refresh-token"));
    }
}
