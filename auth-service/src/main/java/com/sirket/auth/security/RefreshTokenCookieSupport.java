package com.sirket.auth.security;

import com.sirket.auth.common.config.AuthCookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieSupport {

	private final AuthCookieProperties properties;
	private final Clock clock;

	public ResponseCookie toSetCookie(String rawRefresh, Instant expiresAt) {
		Duration maxAge = Duration.between(clock.instant(), expiresAt);
		if (maxAge.isNegative()) {
			maxAge = Duration.ZERO;
		}
		return baseCookie(rawRefresh).maxAge(maxAge).build();
	}

	public ResponseCookie clearCookie() {
		return baseCookie("").maxAge(Duration.ZERO).build();
	}

	public String readRefresh(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		String name = properties.getName();
		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) {
				String value = cookie.getValue();
				return (value == null || value.isBlank()) ? null : value;
			}
		}
		return null;
	}

	private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
		return ResponseCookie.from(properties.getName(), value)
				.httpOnly(true)
				.secure(properties.isSecure())
				.path(properties.getPath())
				.sameSite(properties.getSameSite());
	}
}
