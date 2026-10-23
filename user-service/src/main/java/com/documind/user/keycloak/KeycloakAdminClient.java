package com.documind.user.keycloak;

import com.documind.user.common.KeycloakAdminException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

public class KeycloakAdminClient {

	private final KeycloakProperties properties;
	private final RestClient restClient;
	private final ServiceTokenProvider tokens;

	public KeycloakAdminClient(
			KeycloakProperties properties,
			RestClient restClient,
			ServiceTokenProvider tokens) {
		this.properties = properties;
		this.restClient = restClient;
		this.tokens = tokens;
	}

	public void disableUser(UUID id) {
		keycloakCall("disable", id, () ->
				restClient.put()
						.uri(userUri(), properties.getRealm(), id)
						.headers(h -> h.setBearerAuth(tokens.accessToken()))
						.contentType(MediaType.APPLICATION_JSON)
						.body(Map.of("enabled", false))
						.retrieve()
						.toBodilessEntity());
	}

	public void logoutUser(UUID id) {
		keycloakCall("logout", id, () ->
				restClient.post()
						.uri(userUri() + "/logout", properties.getRealm(), id)
						.headers(h -> h.setBearerAuth(tokens.accessToken()))
						.retrieve()
						.toBodilessEntity());
	}

	public void deleteUser(UUID id) {
		keycloakCall("delete", id, () ->
				restClient.delete()
						.uri(userUri(), properties.getRealm(), id)
						.headers(h -> h.setBearerAuth(tokens.accessToken()))
						.retrieve()
						.toBodilessEntity());
	}

	private String userUri() {
		return properties.getAdminBaseUrl() + "/admin/realms/{realm}/users/{id}";
	}

	private static void keycloakCall(String action, UUID id, Runnable call) {
		try {
			call.run();
		} catch (RestClientResponseException ex) {
			if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
				return;
			}
			throw new KeycloakAdminException("Could not " + action + " Keycloak user " + id, ex);
		} catch (RuntimeException ex) {
			throw new KeycloakAdminException("Could not " + action + " Keycloak user " + id, ex);
		}
	}
}
