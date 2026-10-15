package com.documind.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.sql.Array;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

	private final List<String> allowedOrigins;


    public CorsConfig(@Value("${app.cors.allowed-origins") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(origin ->!origin.isEmpty())
				.toList();
    }

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
		configuration.setExposedHeaders(List.of("X-Correlation-Id"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(Duration.ofHours(1));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	// Route'a .after(corsConfig::stripDownstreamCors) ile bağlanmalı.
	public ServerResponse stripDownstreamCors(ServerRequest request, ServerResponse response) {
		HttpHeaders headers = response.headers();
		headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
		headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
		headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
		headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS);
		headers.remove(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS);
		headers.remove(HttpHeaders.ACCESS_CONTROL_MAX_AGE);
		return response;
    }
}
