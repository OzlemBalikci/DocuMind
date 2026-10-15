package com.documind.gateway.config;

import com.documind.gateway.GatewayEdgeProperties;
import com.documind.gateway.ratelimit.RateLimitConfig;
import com.documind.gateway.ratelimit.RateLimitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
@EnableConfigurationProperties({GatewayEdgeProperties.class, RateLimitProperties.class})
public class RouteConfig {

	private final RateLimitConfig rateLimiter;
	private final ResilienceConfig resilience;
	private final CorsConfig cors;

	public RouteConfig(RateLimitConfig rateLimiter, ResilienceConfig resilience, CorsConfig cors) {
		this.rateLimiter = rateLimiter;
		this.resilience = resilience;
		this.cors = cors;
	}

	@Bean
	RouterFunction<ServerResponse> gatewayRoutes(GatewayEdgeProperties properties) {
		return login(properties)
				.and(proxy("auth", "/api/auth/**", properties.getAuthUri(), "auth", properties, false))
				.and(proxy("documents", "/api/documents/**", properties.getDocumentUri(), "documents", properties, false))
				.and(proxy("chat", "/api/chat/**", properties.getChatUri(), "chat", properties, true));
	}

	private RouterFunction<ServerResponse> login(GatewayEdgeProperties properties) {
		return edge("auth-login", properties)
				.POST("/api/auth/login", HandlerFunctions.http())
				.before(BeforeFilterFunctions.uri(properties.getAuthUri()))
				.filter(rateLimiter.loginLimit(properties))
				.filter(resilience.breaker("auth-login"))
				.after(cors::apply)
				.build();
	}

	private RouterFunction<ServerResponse> proxy(String id,
			String pattern,
			String uri,
			String breakerId,
			GatewayEdgeProperties properties,
			boolean rateLimited) {
		RouterFunctions.Builder route = edge(id, properties)
				.route(RequestPredicates.path(pattern), HandlerFunctions.http())
				.before(BeforeFilterFunctions.uri(uri));
		if (rateLimited) {
			route = route.filter(rateLimiter.chatLimit(properties));
		}
		return route
				.filter(resilience.breaker(breakerId))
				.after(cors::apply)
				.build();
	}

	private RouterFunctions.Builder edge(String id, GatewayEdgeProperties properties) {
		return GatewayRouterFunctions.route(id)
				.before(BeforeFilterFunctions.requestSize(properties.getMaxRequestSize()));
	}
}
