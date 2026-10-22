package com.documind.user.keycloak;

import com.documind.user.common.KeycloakAdminException;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ServiceTokenProvider {

	private final RestClient restClient;
	private final String tokenUri;
	private final String clientId;
	private final String clientSecret;
	private final AtomicReference<CachedToken> cached = new AtomicReference<>();

	public ServiceTokenProvider(RestClient restClient, String tokenUri, String clientId, String clientSecret) {
		this.restClient = restClient;
		this.tokenUri = tokenUri;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}

	public String accessToken() {
		CachedToken current = cached.get();
		if (current != null && current.expiresAt().isAfter(Instant.now().plusSeconds(30))) {
			return current.value();
		}
		try {
			MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			form.add("grant_type", "client_credentials");
			form.add("client_id", clientId);
			form.add("client_secret", clientSecret);
			@SuppressWarnings("unchecked")
			Map<String, Object> response = restClient.post()
					.uri(tokenUri)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(Map.class);
			if (response == null || response.get("access_token") == null) {
				throw new KeycloakAdminException("Keycloak did not return an access token");
			}
			long expiresIn = ((Number) response.getOrDefault("expires_in", 60)).longValue();
			CachedToken next = new CachedToken(
					response.get("access_token").toString(),
					Instant.now().plusSeconds(Math.max(30, expiresIn - 10)));
			cached.set(next);
			return next.value();
		} catch (KeycloakAdminException ex) {
			throw ex;
		} catch (RuntimeException ex) {
			throw new KeycloakAdminException("Could not obtain Keycloak access token", ex);
		}
	}

	private record CachedToken(String value, Instant expiresAt) {
	}
}
