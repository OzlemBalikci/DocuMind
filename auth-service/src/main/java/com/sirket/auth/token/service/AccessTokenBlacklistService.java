package com.sirket.auth.token.service;

import java.time.Instant;

public interface AccessTokenBlacklistService {

	void revokeUntilExpiry(String jti, Instant expiresAt);

	boolean isRevoked(String jti);
}
