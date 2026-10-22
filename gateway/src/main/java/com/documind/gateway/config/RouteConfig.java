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
		return proxy("users", "/api/users/**", properties.getUserUri(), "users", properties, "users")
				.and(proxy("documents", "/api/documents/**", properties.getDocumentUri(), "documents", properties, "documents"))
				.and(proxy("chat", "/api/chat/**", properties.getChatUri(), "chat", properties, "chat"));
	}

	private RouterFunction<ServerResponse> proxy(String id,
			String pattern,
			String uri,
			String breakerId,
			GatewayEdgeProperties properties,
			String rateLimitPolicy) {
		RouterFunctions.Builder route = edge(id, properties)
				.route(RequestPredicates.path(pattern), HandlerFunctions.http())
				.before(BeforeFilterFunctions.uri(uri));
		if (rateLimitPolicy != null) {
			route = route.filter(rateLimits.of(rateLimitPolicy));
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
