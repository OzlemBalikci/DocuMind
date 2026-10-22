package com.documind.user.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.keycloak")
public class KeycloakProperties {

	private String adminBaseUrl = "http://localhost:8180";
	private String tokenUri = "http://localhost:8180/realms/documind/protocol/openid-connect/token";
	private String realm = "documind";
	private Client admin = new Client();
	private Client internal = new Client();

	public String getAdminBaseUrl() {
		return adminBaseUrl;
	}

	public void setAdminBaseUrl(String adminBaseUrl) {
		this.adminBaseUrl = adminBaseUrl;
	}

	public String getTokenUri() {
		return tokenUri;
	}

	public void setTokenUri(String tokenUri) {
		this.tokenUri = tokenUri;
	}

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public Client getAdmin() {
		return admin;
	}

	public void setAdmin(Client admin) {
		this.admin = admin;
	}

	public Client getInternal() {
		return internal;
	}

	public void setInternal(Client internal) {
		this.internal = internal;
	}

	public static class Client {

		private String clientId = "";
		private String clientSecret = "";

		public String getClientId() {
			return clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public String getClientSecret() {
			return clientSecret;
		}

		public void setClientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
		}
	}
}
