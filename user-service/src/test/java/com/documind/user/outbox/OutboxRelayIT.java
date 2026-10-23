package com.documind.user.outbox;

import com.documind.user.JwtDecoderTestConfig;
import com.documind.user.PostgresSupport;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.outbox.max-attempts=3")
@ActiveProfiles("test")
@Import(JwtDecoderTestConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class OutboxRelayIT {

	static final WireMockServer WIRE_MOCK = new WireMockServer(wireMockConfig().dynamicPort());

	static {
		WIRE_MOCK.start();
	}

	@Container
	static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

	@DynamicPropertySource
	static void props(DynamicPropertyRegistry registry) {
		PostgresSupport.register(registry, postgres);
		String base = "http://127.0.0.1:" + WIRE_MOCK.port();
		registry.add("app.keycloak.admin-base-url", () -> base);
		registry.add("app.keycloak.token-uri", () -> base + "/token");
		registry.add("app.clients.document-base-url", () -> base);
		registry.add("app.clients.chat-base-url", () -> base);
	}

	@Autowired
	OutboxRelay relay;

	@Autowired
	OutboxRepository outbox;

	@Autowired
	PlatformTransactionManager transactionManager;

	@BeforeAll
	static void stubs() {
		stubHappyPath();
	}

	@AfterEach
	void reset() {
		WIRE_MOCK.resetRequests();
		stubHappyPath();
	}

	@Test
	void twoRelaysProcessEventOnce() throws Exception {
		UUID owner = UUID.randomUUID();
		outbox.save(OutboxEvent.pending("User", owner, OutboxEvent.Type.USER_DELETED, "{}"));
		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		for (int i = 0; i < 2; i++) {
			pool.submit(() -> {
				ready.countDown();
				start.await();
				relay.relay();
				return null;
			});
		}
		assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
		start.countDown();
		pool.shutdown();
		assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

		OutboxEvent stored = outbox.findByAggregateIdAndEventType(owner, OutboxEvent.Type.USER_DELETED).orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
		WIRE_MOCK.verify(1, deleteRequestedFor(urlMatching("/admin/realms/documind/users/" + owner)));
	}

	@Test
	void downstream500RetriesThenDead() {
		UUID owner = UUID.randomUUID();
		WIRE_MOCK.stubFor(delete(urlPathMatching("/internal/owners/.*")).willReturn(serverError()));
		outbox.save(OutboxEvent.pending("User", owner, OutboxEvent.Type.USER_DELETED, "{}"));
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		for (int i = 0; i < 3; i++) {
			relay.relay();
			tx.executeWithoutResult(status -> outbox.findByAggregateIdAndEventType(owner, OutboxEvent.Type.USER_DELETED)
					.ifPresent(event -> {
						if (event.getStatus() == OutboxStatus.FAILED) {
							event.recover();
						}
					}));
		}
		OutboxEvent stored = outbox.findByAggregateIdAndEventType(owner, OutboxEvent.Type.USER_DELETED).orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(OutboxStatus.DEAD);
		assertThat(stored.getAttempts()).isEqualTo(3);
	}

	private static void stubHappyPath() {
		WIRE_MOCK.stubFor(post(urlEqualTo("/token"))
				.willReturn(okJson("{\"access_token\":\"test-token\",\"expires_in\":300}")));
		WIRE_MOCK.stubFor(put(urlMatching("/admin/realms/documind/users/.*")).willReturn(noContent()));
		WIRE_MOCK.stubFor(post(urlMatching("/admin/realms/documind/users/.*/logout")).willReturn(noContent()));
		WIRE_MOCK.stubFor(delete(urlMatching("/admin/realms/documind/users/.*")).willReturn(noContent()));
		WIRE_MOCK.stubFor(delete(urlMatching("/internal/owners/.*")).willReturn(noContent()));
	}
}
