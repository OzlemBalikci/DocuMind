package com.documind.user.keycloak;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class KeycloakResourceServerIT {

	@Container
	static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg16"));

	@Container
	static GenericContainer<?> keycloak = new GenericContainer<>(DockerImageName.parse("quay.io/keycloak/keycloak:26.0"))
			.withExposedPorts(8080)
			.withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
			.withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
			.withEnv("KC_HOSTNAME_STRICT", "false")
			.withEnv("KC_HTTP_ENABLED", "true")
			.withCommand("start-dev", "--import-realm")
			.withCopyFileToContainer(MountableFile.forHostPath(realmFile()), "/opt/keycloak/data/import/realm-documind.json")
			.waitingFor(Wait.forHttp("/realms/documind").forPort(8080).withStartupTimeout(Duration.ofMinutes(3)));

	@Autowired
	MockMvc mockMvc;

	@DynamicPropertySource
	static void keycloakProps(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
		registry.add("spring.flyway.enabled", () -> "true");
		String issuer = "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080) + "/realms/documind";
		registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuer);
		registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
				() -> issuer + "/protocol/openid-connect/certs");
		registry.add("app.keycloak.admin-base-url",
				() -> "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080));
		registry.add("app.keycloak.token-uri", () -> issuer + "/protocol/openid-connect/token");
		registry.add("app.keycloak.admin.client-id", () -> "user-service-admin");
		registry.add("app.keycloak.admin.client-secret", () -> "local-dev-user-service-admin-secret");
		registry.add("app.keycloak.internal.client-id", () -> "user-service-internal");
		registry.add("app.keycloak.internal.client-secret", () -> "local-dev-user-service-internal-secret");
	}

	@BeforeAll
	static void createPasswordGrantClient() {
		String base = "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080);
		String adminToken = token(base, "master", "admin-cli", "admin", "admin");
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(adminToken);
		headers.setContentType(MediaType.APPLICATION_JSON);
		String body = """
				{
				  "clientId": "documind-test",
				  "enabled": true,
				  "publicClient": true,
				  "directAccessGrantsEnabled": true,
				  "standardFlowEnabled": false,
				  "protocol": "openid-connect",
				  "protocolMappers": [{
				    "name": "audience-documind-api",
				    "protocol": "openid-connect",
				    "protocolMapper": "oidc-audience-mapper",
				    "config": {
				      "included.custom.audience": "documind-api",
				      "access.token.claim": "true"
				    }
				  }]
				}
				""";
		new RestTemplate().postForEntity(
				base + "/admin/realms/documind/clients",
				new HttpEntity<>(body, headers),
				Void.class);
	}

	@Test
	void realTokenReachesMe() throws Exception {
		String base = "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080);
		String adminToken = token(base, "master", "admin-cli", "admin", "admin");
		createUser(base, adminToken, "it@example.com", "Passw0rd!");
		String access = token(base, "documind", "documind-test", "it@example.com", "Passw0rd!");
		mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("it@example.com"));
	}

	@Test
	void missingTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/users/me"))
				.andExpect(status().isUnauthorized());
	}

	private static void createUser(String base, String adminToken, String email, String password) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(adminToken);
		headers.setContentType(MediaType.APPLICATION_JSON);
		String body = """
				{
				  "username": "%s",
				  "email": "%s",
				  "enabled": true,
				  "emailVerified": true,
				  "firstName": "IT",
				  "lastName": "User",
				  "credentials": [{ "type": "password", "value": "%s", "temporary": false }]
				}
				""".formatted(email, email, password);
		new RestTemplate().postForEntity(
				base + "/admin/realms/documind/users",
				new HttpEntity<>(body, headers),
				Void.class);
	}

	@SuppressWarnings("unchecked")
	private static String token(String base, String realm, String clientId, String username, String password) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "password");
		form.add("client_id", clientId);
		form.add("username", username);
		form.add("password", password);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		ResponseEntity<Map> response = new RestTemplate().postForEntity(
				base + "/realms/" + realm + "/protocol/openid-connect/token",
				new HttpEntity<>(form, headers),
				Map.class);
		assertThat(response.getBody()).isNotNull();
		return response.getBody().get("access_token").toString();
	}

	private static Path realmFile() {
		Path moduleRelative = Path.of("..", "keycloak", "realm-documind.json").normalize().toAbsolutePath();
		if (Files.exists(moduleRelative)) {
			return moduleRelative;
		}
		Path rootRelative = Path.of("keycloak", "realm-documind.json").toAbsolutePath();
		if (Files.exists(rootRelative)) {
			return rootRelative;
		}
		throw new IllegalStateException("realm-documind.json not found");
	}
}
