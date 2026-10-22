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
		try {
			restClient.put()
					.uri(properties.getAdminBaseUrl() + "/admin/realms/{realm}/users/{id}",
							properties.getRealm(), id)
					.headers(headers -> headers.setBearerAuth(tokens.accessToken()))
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("enabled", false))
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientResponseException ex) {
			if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
				return;
			}
			throw new KeycloakAdminException("Could not disable Keycloak user " + id, ex);
		} catch (RuntimeException ex) {
			throw new KeycloakAdminException("Could not disable Keycloak user " + id, ex);
		}
	}

	public void logoutUser(UUID id) {
		try {
			restClient.post()
					.uri(properties.getAdminBaseUrl() + "/admin/realms/{realm}/users/{id}/logout",
							properties.getRealm(), id)
					.headers(headers -> headers.setBearerAuth(tokens.accessToken()))
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientResponseException ex) {
			if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
				return;
			}
			throw new KeycloakAdminException("Could not logout Keycloak user " + id, ex);
		} catch (RuntimeException ex) {
			throw new KeycloakAdminException("Could not logout Keycloak user " + id, ex);
		}
	}

	public void deleteUser(UUID id) {
		try {
			restClient.delete()
					.uri(properties.getAdminBaseUrl() + "/admin/realms/{realm}/users/{id}",
							properties.getRealm(), id)
					.headers(headers -> headers.setBearerAuth(tokens.accessToken()))
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientResponseException ex) {
			if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
				return;
			}
			throw new KeycloakAdminException("Could not delete Keycloak user " + id, ex);
		} catch (RuntimeException ex) {
			throw new KeycloakAdminException("Could not delete Keycloak user " + id, ex);
		}
	}
}
