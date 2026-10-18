package com.sirket.auth.auth.service.impl;

import com.sirket.auth.auth.domain.AccessTokenRevocation;
import com.sirket.auth.auth.domain.AccountDeletionPolicy;
import com.sirket.auth.auth.domain.CredentialAuthenticator;
import com.sirket.auth.auth.domain.UserRegistrationPolicy;
import com.sirket.auth.auth.dto.request.LoginRequest;
import com.sirket.auth.auth.dto.request.RegisterRequest;
import com.sirket.auth.auth.dto.response.AuthResponse;
import com.sirket.auth.auth.dto.response.IssuedAuthSession;
import com.sirket.auth.auth.dto.response.UserResponse;
import com.sirket.auth.auth.service.AuthService;
import com.sirket.auth.common.audit.AuditService;
import com.sirket.auth.token.service.RefreshTokenService;
import com.sirket.auth.token.service.TokenService;
import com.sirket.auth.user.entity.User;
import com.documind.backend.common.exception.InvalidRefreshTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserRegistrationPolicy userRegistrationPolicy;
	private final CredentialAuthenticator credentialAuthenticator;
	private final AccountDeletionPolicy accountDeletionPolicy;
	private final AccessTokenRevocation accessTokenRevocation;
	private final TokenService tokenService;
	private final RefreshTokenService refreshTokenService;
	private final AuditService auditService;

	@Override
	@Transactional
	public IssuedAuthSession register(RegisterRequest request, String userAgent, String ipAddress) {
		User user = userRegistrationPolicy.register(request);
		auditService.authEvent("register", user.getId(), ipAddress);
		return issueTokens(user, userAgent, ipAddress);
	}

	@Override
	@Transactional
	public IssuedAuthSession login(LoginRequest request, String userAgent, String ipAddress) {
		User user = credentialAuthenticator.authenticate(request.email(), request.password());
		auditService.authEvent("login", user.getId(), ipAddress);
		return issueTokens(user, userAgent, ipAddress);
	}

	@Override
	@Transactional(noRollbackFor = InvalidRefreshTokenException.class)
	public IssuedAuthSession refresh(String refreshToken, String userAgent, String ipAddress) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new InvalidRefreshTokenException();
		}

		RefreshTokenService.RotatedRefresh rotated =
				refreshTokenService.rotate(refreshToken, userAgent, ipAddress);

		TokenService.AccessToken access = tokenService.issueAccess(rotated.user().getId());
		auditService.authEvent("refresh", rotated.user().getId(), ipAddress);
		return toIssuedSession(rotated.user(), access, rotated.refresh());
	}

	@Override
	public void logout(String refreshToken, String accessToken) {
		refreshTokenService.revoke(refreshToken);
		accessTokenRevocation.revokeIfPresent(accessToken);
	}

	@Override
	@Transactional
	public void deleteAccount(Long userId, String password, String accessToken) {
		User user = accountDeletionPolicy.deleteAccount(userId, password, accessToken);
		auditService.authEvent("delete_account", user.getId(), null);
	}

	private IssuedAuthSession issueTokens(User user, String userAgent, String ipAddress) {
		TokenService.AccessToken access = tokenService.issueAccess(user.getId());
		RefreshTokenService.IssuedRefresh refresh = refreshTokenService.issue(user, userAgent, ipAddress);
		return toIssuedSession(user, access, refresh);
	}

	private IssuedAuthSession toIssuedSession(User user,
			TokenService.AccessToken access,
			RefreshTokenService.IssuedRefresh refresh) {
		AuthResponse response = new AuthResponse(
				AuthResponse.TOKEN_TYPE_BEARER,
				access.token(),
				access.expiresAt(),
				access.expiresInSeconds(),
				toUserResponse(user));
		return new IssuedAuthSession(response, refresh.rawToken(), refresh.expiresAt());
	}

	private UserResponse toUserResponse(User user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getFullName(),
				user.getRole());
	}
}
