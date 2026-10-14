package com.documind.backend.auth.controller;

import com.documind.backend.auth.dto.AuthResponse;
import com.documind.backend.auth.dto.IssuedAuthSession;
import com.documind.backend.auth.dto.LoginRequest;
import com.documind.backend.auth.dto.LogoutRequest;
import com.documind.backend.auth.dto.MeResponse;
import com.documind.backend.auth.dto.RefreshRequest;
import com.documind.backend.auth.dto.RegisterRequest;
import com.documind.backend.auth.security.AuthPrincipal;
import com.documind.backend.auth.security.RefreshTokenCookieSupport;
import com.documind.backend.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final RefreshTokenCookieSupport refreshTokenCookieSupport;

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(
			@Valid @RequestBody RegisterRequest body,
			HttpServletRequest httpRequest) {
		IssuedAuthSession session = authService.register(
				body,
				userAgent(httpRequest),
				clientIp(httpRequest));
		return withRefreshCookie(HttpStatus.CREATED, session);
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(
			@Valid @RequestBody LoginRequest body,
			HttpServletRequest httpRequest) {
		IssuedAuthSession session = authService.login(
				body,
				userAgent(httpRequest),
				clientIp(httpRequest));
		return withRefreshCookie(HttpStatus.OK, session);
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refresh(
			@RequestBody(required = false) RefreshRequest body,
			HttpServletRequest httpRequest) {
		String refreshToken = firstNonBlank(
				refreshTokenCookieSupport.readRefresh(httpRequest),
				body != null ? body.refreshToken() : null);
		IssuedAuthSession session = authService.refresh(
				refreshToken,
				userAgent(httpRequest),
				clientIp(httpRequest));
		return withRefreshCookie(HttpStatus.OK, session);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
			@Valid @RequestBody LogoutRequest body,
			HttpServletRequest httpRequest) {
		String refreshToken = firstNonBlank(
				refreshTokenCookieSupport.readRefresh(httpRequest),
				body.refreshToken());
		authService.logout(refreshToken, body.accessToken());
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, refreshTokenCookieSupport.clearCookie().toString())
				.build();
	}

	@GetMapping("/me")
	public MeResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
		return authService.getMe(principal.userId());
	}

	private ResponseEntity<AuthResponse> withRefreshCookie(HttpStatus status, IssuedAuthSession session) {
		ResponseCookie cookie = refreshTokenCookieSupport.toSetCookie(
				session.refreshToken(),
				session.refreshExpiresAt());
		return ResponseEntity.status(status)
				.header(HttpHeaders.SET_COOKIE, cookie.toString())
				.body(session.response());
	}

	private static String firstNonBlank(String first, String second) {
		if (first != null && !first.isBlank()) {
			return first;
		}
		if (second != null && !second.isBlank()) {
			return second;
		}
		return null;
	}

	private static String userAgent(HttpServletRequest request) {
		return request.getHeader(HttpHeaders.USER_AGENT);
	}

	private static String clientIp(HttpServletRequest request) {
		return request.getRemoteAddr();
	}
}
