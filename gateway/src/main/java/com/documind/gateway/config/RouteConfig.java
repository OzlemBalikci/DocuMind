package com.documind.gateway.config;

import com.documind.gateway.GatewayEdgeProperties;
import com.documind.gateway.ratelimit.RateLimitFilters;
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

	private final RateLimitFilters rateLimits;
	private final ResilienceConfig resilience;
	private final CorsConfig cors;

	public RouteConfig(RateLimitFilters rateLimits, ResilienceConfig resilience, CorsConfig cors) {
		this.rateLimits = rateLimits;
		this.resilience = resilience;
		this.cors = cors;
	}

	@Bean
	RouterFunction<ServerResponse> gatewayRoutes(GatewayEdgeProperties properties) {
		return login(properties)
				.and(register(properties))
				.and(passwordReset(properties))
				.and(refresh(properties))
				.and(proxy("auth", "/api/auth/**", properties.getAuthUri(), "auth", properties, false))
				.and(proxy("documents", "/api/documents/**", properties.getDocumentUri(), "documents", properties, false))
				.and(proxy("chat", "/api/chat/**", properties.getChatUri(), "chat", properties, true));
	}

	private RouterFunction<ServerResponse> login(GatewayEdgeProperties properties) {
		return edge("auth-login", properties)
				.POST("/api/auth/login", HandlerFunctions.http())
				.before(BeforeFilterFunctions.uri(properties.getAuthUri()))
				.filter(rateLimits.of("login"))
				.filter(resilience.breaker("auth-login"))
				.after(cors::stripDownstreamCors)
				.build();
	}

	private RouterFunction<ServerResponse> register(GatewayEdgeProperties properties) {
		return edge("auth-register", properties)
				.POST("/api/auth/register", HandlerFunctions.http())
				.before(BeforeFilterFunctions.uri(properties.getAuthUri()))
				.filter(rateLimits.of("register"))
				.filter(resilience.breaker("auth"))
				.after(cors::stripDownstreamCors)
				.build();
	}

	private RouterFunction<ServerResponse> passwordReset(GatewayEdgeProperties properties) {
		return edge("auth-password-reset", properties)
				.route(RequestPredicates.path("/api/auth/forgot-password")
						.or(RequestPredicates.path("/api/auth/reset-password")), HandlerFunctions.http())
				.before(BeforeFilterFunctions.uri(properties.getAuthUri()))
				.filter(rateLimits.of("password-reset"))
				.filter(resilience.breaker("auth"))
				.after(cors::stripDownstreamCors)
				.build();
	}

	private RouterFunction<ServerResponse> refresh(GatewayEdgeProperties properties) {
		return edge("auth-refresh", properties)
				.POST("/api/auth/refresh", HandlerFunctions.http())
				.before(BeforeFilterFunctions.uri(properties.getAuthUri()))
				.filter(rateLimits.of("refresh"))
				.filter(resilience.breaker("auth"))
				.after(cors::stripDownstreamCors)
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
			route = route.filter(rateLimits.of("chat"));
		}
		return route
				.filter(resilience.breaker(breakerId))
				.after(cors::stripDownstreamCors)
				.build();
	}

	private RouterFunctions.Builder edge(String id, GatewayEdgeProperties properties) {
		return GatewayRouterFunctions.route(id)
				.before(BeforeFilterFunctions.requestSize(properties.getMaxRequestSize()));
	}
}
