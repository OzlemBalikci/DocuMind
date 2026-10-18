package com.documind.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class UserHeaderFilter extends OncePerRequestFilter {

	static final Set<String> BLOCKED = Set.of(
			"x-user-id",
			"x-user-email",
			"x-internal-user",
			"forwarded");

	static final Set<String> CLIENT_IP_HEADERS = Set.of(
			"x-forwarded-for",
			"x-real-ip");

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		filterChain.doFilter(new StrippedIdentityRequest(request), response);
	}

	private static final class StrippedIdentityRequest extends HttpServletRequestWrapper {

		private final String clientIp;

		private StrippedIdentityRequest(HttpServletRequest request) {
			super(request);
			this.clientIp = request.getRemoteAddr();
		}

		@Override
		public String getHeader(String name) {
			if (blocked(name)) {
				return null;
			}
			if (clientIpHeader(name)) {
				return clientIp;
			}
			return super.getHeader(name);
		}

		@Override
		public Enumeration<String> getHeaders(String name) {
			if (blocked(name)) {
				return Collections.emptyEnumeration();
			}
			if (clientIpHeader(name)) {
				return Collections.enumeration(Collections.singletonList(clientIp));
			}
			return super.getHeaders(name);
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			Set<String> names = new java.util.LinkedHashSet<>();
			Enumeration<String> existing = super.getHeaderNames();
			while (existing.hasMoreElements()) {
				String name = existing.nextElement();
				if (!blocked(name) && !clientIpHeader(name)) {
					names.add(name);
				}
			}
			names.add("X-Forwarded-For");
			names.add("X-Real-IP");
			return Collections.enumeration(names);
		}

		private static boolean blocked(String name) {
			return name != null && BLOCKED.contains(name.toLowerCase(Locale.ROOT));
		}

		private static boolean clientIpHeader(String name) {
			return name != null && CLIENT_IP_HEADERS.contains(name.toLowerCase(Locale.ROOT));
		}
	}
}
