package com.documind.gateway.ratelimit;

import com.documind.gateway.ratelimit.RateLimitProperties.KeyType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.Optional;

@Component
public class KeyResolver {

	public String resolve(KeyType type, ServerRequest request) {
		return switch (type) {
			case IP -> "ip:" + clientIp(request);
			case USER_OR_IP -> authenticatedUser()
					.map(id -> "user:" + id)
					.orElseGet(() -> "ip:" + clientIp(request));
		};
	}

	private Optional<String> authenticatedUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
			String subject = jwt.getSubject();
			if (subject != null && !subject.isBlank()) {
				return Optional.of(subject);
			}
		}
		return Optional.empty();
	}

	private String clientIp(ServerRequest request) {
		return request.servletRequest() instanceof HttpServletRequest servlet
				? servlet.getRemoteAddr()
				: "unknown";
	}
}
