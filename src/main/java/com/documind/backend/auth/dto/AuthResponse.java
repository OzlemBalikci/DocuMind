package com.documind.backend.auth.dto;


import java.time.Instant;

public record AuthResponse(
        String accessToken,
        Instant accessExpiresAt,
        String refreshToken,
        Instant refreshExpiredAt,
        UserResponse user
) { }
