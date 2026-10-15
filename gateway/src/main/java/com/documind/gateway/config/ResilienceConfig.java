package com.documind.gateway.config;

import org.springframework.cloud.gateway.server.mvc.filter.CircuitBreakerFilterFunctions;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class ResilienceConfig {

	public HandlerFilterFunction<ServerResponse, ServerResponse> breaker(String id) {
		return CircuitBreakerFilterFunctions.circuitBreaker(id);
	}
}
