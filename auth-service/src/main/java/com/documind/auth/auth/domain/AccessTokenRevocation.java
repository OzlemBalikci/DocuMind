package com.documind.auth.auth.domain;

import com.documind.auth.token.service.AccessTokenBlacklistService;
import com.documind.auth.token.service.TokenService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccessTokenRevocation {

	private final TokenService tokenService;
	private final AccessTokenBlacklistService accessTokenBlacklistService;

	public void revokeIfPresent(String accessToken) {
		if (accessToken == null || accessToken.isBlank()) {
			return;
		}
		try {
			TokenService.AccessClaims claims = tokenService.parseAccess(accessToken);
			accessTokenBlacklistService.revokeUntilExpiry(claims.jti(), claims.expiresAt());
		} catch (JwtException ignored) {
			// Expired or malformed access token should not fail the surrounding operation.
		}
	}

	public void revokeIfOwnedByUser(Long userId, String accessToken) {
		if (accessToken == null || accessToken.isBlank()) {
			return;
		}
		try {
			TokenService.AccessClaims claims = tokenService.parseAccess(accessToken);
			if (userId.equals(claims.userId())) {
				accessTokenBlacklistService.revokeUntilExpiry(claims.jti(), claims.expiresAt());
			}
		} catch (JwtException ignored) {
			// Account deletion still proceeds when the access token cannot be parsed.
		}
	}
}
