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
			"x-internal-user");

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		filterChain.doFilter(new StrippedIdentityRequest(request), response);
	}

	private static final class StrippedIdentityRequest extends HttpServletRequestWrapper {

		private StrippedIdentityRequest(HttpServletRequest request) {
			super(request);
		}

		@Override
		public String getHeader(String name) {
			if (blocked(name)) {
				return null;
			}
			return super.getHeader(name);
		}

		@Override
		public Enumeration<String> getHeaders(String name) {
			if (blocked(name)) {
				return Collections.emptyEnumeration();
			}
			return super.getHeaders(name);
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			return Collections.enumeration(Collections.list(super.getHeaderNames()).stream()
					.filter(name -> !blocked(name))
					.toList());
		}

		private static boolean blocked(String name) {
			return name != null && BLOCKED.contains(name.toLowerCase(Locale.ROOT));
		}
	}
}
