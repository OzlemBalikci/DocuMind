package com.documind.gateway.ratelimit;

import com.documind.gateway.ratelimit.RateLimitProperties.KeyType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Component
public class KeyResolver {

	public String resolve(KeyType type, ServerRequest request) {
		return switch (type) {
			case IP -> "ip:" + clientIp(request);
			case USER_OR_IP -> authenticatedUser(request)
					.map(id -> "user:" + id)
					.orElseGet(() -> "ip:" + clientIp(request));
		};
	}

	private Optional<String> authenticatedUser(ServerRequest request) {
		String authorization = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);
		if (authorization == null || authorization.isBlank() || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
			return Optional.empty();
		}
		String token = authorization.substring(7).trim();
		if (token.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(sha256Hex(token));
	}

	private static String sha256Hex(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 unavailable", ex);
		}
	}

	private String clientIp(ServerRequest request) {
		return request.servletRequest() instanceof HttpServletRequest servlet
				? servlet.getRemoteAddr()
				: "unknown";
	}
}
