package com.documind.auth.token.service;

import java.time.Instant;

public interface TokenService {

	record AccessToken(String token, String jti, Instant expiresAt, long expiresInSeconds) {
	}

	record AccessClaims(Long userId, String jti, Instant expiresAt) {
	}

	AccessToken issueAccess(Long userId);

	AccessClaims parseAccess(String token);
}
