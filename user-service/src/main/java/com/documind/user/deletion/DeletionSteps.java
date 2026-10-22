package com.documind.user.deletion;

import com.documind.user.common.DownstreamPurgeException;
import com.documind.user.keycloak.KeycloakAdminClient;
import com.documind.user.keycloak.ServiceTokenProvider;
import com.documind.user.profile.UserProfileRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class DeletionSteps {

	private final KeycloakAdminClient keycloak;
	private final UserProfileRepository profiles;
	private final DownstreamProperties downstream;
	private final RestClient restClient;
	private final ServiceTokenProvider internalTokens;

	public DeletionSteps(
			KeycloakAdminClient keycloak,
			UserProfileRepository profiles,
			DownstreamProperties downstream,
			RestClient adminRestClient,
			@Qualifier("keycloakInternalTokenProvider") ServiceTokenProvider internalTokens) {
		this.keycloak = keycloak;
		this.profiles = profiles;
		this.downstream = downstream;
		this.restClient = adminRestClient;
		this.internalTokens = internalTokens;
	}

	public void execute(UUID ownerId) {
		keycloak.disableUser(ownerId);
		keycloak.logoutUser(ownerId);
		purgeDocuments(ownerId);
		purgeChat(ownerId);
		deleteProfile(ownerId);
		keycloak.deleteUser(ownerId);
	}

	private void purgeDocuments(UUID ownerId) {
		purge(downstream.getDocumentBaseUrl(), ownerId);
	}

	private void purgeChat(UUID ownerId) {
		purge(downstream.getChatBaseUrl(), ownerId);
	}

	private void deleteProfile(UUID ownerId) {
		profiles.deleteById(ownerId);
	}

	private void purge(String baseUrl, UUID ownerId) {
		try {
			restClient.delete()
					.uri(baseUrl + "/internal/owners/{ownerId}", ownerId)
					.headers(headers -> headers.setBearerAuth(internalTokens.accessToken()))
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientResponseException ex) {
			if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
				return;
			}
			throw new DownstreamPurgeException("Purge failed for " + baseUrl, ex);
		} catch (RuntimeException ex) {
			throw new DownstreamPurgeException("Purge failed for " + baseUrl, ex);
		}
	}
}
