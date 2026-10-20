package com.documind.auth.token.service;

import com.documind.auth.user.entity.User;

import java.time.Instant;

public interface RefreshTokenService {

	record IssuedRefresh(String rawToken, Instant expiresAt) {
	}

	record RotatedRefresh(User user, IssuedRefresh refresh) {
	}

	IssuedRefresh issue(User user, String userAgent, String ipAddress);

	RotatedRefresh rotate(String rawToken, String userAgent, String ipAddress);

	void revoke(String rawToken);

	void revokeAllForUser(Long userId);
}
