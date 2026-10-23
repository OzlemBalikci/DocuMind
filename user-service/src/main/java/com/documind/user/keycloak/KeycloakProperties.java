package com.documind.user.keycloak;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.keycloak")
public class KeycloakProperties {

	private String adminBaseUrl = "http://localhost:8180";
	private String tokenUri = "http://localhost:8180/realms/documind/protocol/openid-connect/token";
	private String realm = "documind";
	private Client admin = new Client();
	private Client internal = new Client();

	@Getter
	@Setter
	public static class Client {
		private String clientId = "";
		private String clientSecret = "";
	}
}
