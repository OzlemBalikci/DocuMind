package com.documind.user.keycloak;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class KeycloakAdminConfig {

	@Bean
	RestClient adminRestClient() {
		HttpClient http = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1)
				.connectTimeout(Duration.ofSeconds(3))
				.build();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
		factory.setReadTimeout(Duration.ofSeconds(10));
		return RestClient.builder().requestFactory(factory).build();
	}

	@Bean
	ServiceTokenProvider keycloakAdminTokenProvider(RestClient adminRestClient, KeycloakProperties properties) {
		return new ServiceTokenProvider(
				adminRestClient,
				properties.getTokenUri(),
				properties.getAdmin().getClientId(),
				properties.getAdmin().getClientSecret());
	}

	@Bean
	ServiceTokenProvider keycloakInternalTokenProvider(RestClient adminRestClient, KeycloakProperties properties) {
		return new ServiceTokenProvider(
				adminRestClient,
				properties.getTokenUri(),
				properties.getInternal().getClientId(),
				properties.getInternal().getClientSecret());
	}

	@Bean
	KeycloakAdminClient keycloakAdminClient(
			KeycloakProperties properties,
			RestClient adminRestClient,
			ServiceTokenProvider keycloakAdminTokenProvider) {
		return new KeycloakAdminClient(properties, adminRestClient, keycloakAdminTokenProvider);
	}
}
