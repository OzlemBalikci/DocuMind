package com.documind.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

	static final String HEADER = "X-Correlation-Id";

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String current = request.getHeader(HEADER);
		String correlationId = current == null || current.isBlank() ? UUID.randomUUID().toString() : current.trim();
		MDC.put("correlationId", correlationId);
		response.setHeader(HEADER, correlationId);
		try {
			filterChain.doFilter(new CorrelationIdRequest(request, correlationId), response);
		} finally {
			MDC.remove("correlationId");
		}
	}

	private static final class CorrelationIdRequest extends HttpServletRequestWrapper {

		private final String correlationId;

		private CorrelationIdRequest(HttpServletRequest request, String correlationId) {
			super(request);
			this.correlationId = correlationId;
		}

		@Override
		public String getHeader(String name) {
			if (HEADER.equalsIgnoreCase(name)) {
				return correlationId;
			}
			return super.getHeader(name);
		}

		@Override
		public Enumeration<String> getHeaders(String name) {
			if (HEADER.equalsIgnoreCase(name)) {
				return Collections.enumeration(Collections.singletonList(correlationId));
			}
			return super.getHeaders(name);
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			Set<String> names = new LinkedHashSet<>();
			Enumeration<String> existing = super.getHeaderNames();
			while (existing.hasMoreElements()) {
				names.add(existing.nextElement());
			}
			names.add(HEADER);
			return Collections.enumeration(names);
		}
	}
}
