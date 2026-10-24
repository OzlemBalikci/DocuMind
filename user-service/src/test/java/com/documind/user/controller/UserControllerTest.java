package com.documind.user.controller;


import com.documind.common.security.TestJwtSupport;
import com.documind.user.JwtDecoderTestConfig;

import com.documind.user.PostgresSupport;

import com.documind.user.repository.UserProfileRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.context.annotation.Import;

import org.springframework.http.HttpHeaders;

import org.springframework.http.MediaType;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.test.context.ActiveProfiles;

import org.springframework.test.context.DynamicPropertyRegistry;

import org.springframework.test.context.DynamicPropertySource;

import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

import org.testcontainers.junit.jupiter.Container;

import org.testcontainers.junit.jupiter.Testcontainers;

import org.testcontainers.postgresql.PostgreSQLContainer;

import org.testcontainers.utility.DockerImageName;



import java.time.Instant;

import java.util.List;

import java.util.Map;

import java.util.UUID;

import java.util.concurrent.CountDownLatch;

import java.util.concurrent.ExecutorService;

import java.util.concurrent.Executors;

import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;



import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



@SpringBootTest

@AutoConfigureMockMvc

@ActiveProfiles("test")

@Import(JwtDecoderTestConfig.class)

@Testcontainers(disabledWithoutDocker = true)

class UserControllerTest {



	@Container

	static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));



	@DynamicPropertySource

	static void datasource(DynamicPropertyRegistry registry) {

		PostgresSupport.register(registry, postgres);

	}



	@Autowired

	MockMvc mockMvc;



	@Autowired

	UserProfileRepository profiles;



	@Test

	void missingTokenIsUnauthorized() throws Exception {

		mockMvc.perform(get("/api/users/me"))

				.andExpect(status().isUnauthorized());

	}



	@Test

	void meCreatesProfileFromJwt() throws Exception {

		UUID id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

		mockMvc.perform(get("/api/users/me").with(userJwt(id)))

				.andExpect(status().isOk())

				.andExpect(jsonPath("$.id").value(id.toString()))

				.andExpect(jsonPath("$.email").value(email(id)));

	}



	@Test

	void parallelMeCreatesSingleProfile() throws Exception {

		UUID id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

		int threads = 12;

		ExecutorService pool = Executors.newFixedThreadPool(threads);

		CountDownLatch ready = new CountDownLatch(threads);

		CountDownLatch start = new CountDownLatch(1);

		AtomicInteger successes = new AtomicInteger();

		for (int i = 0; i < threads; i++) {

			pool.submit(() -> {

				ready.countDown();

				start.await();

				mockMvc.perform(get("/api/users/me").with(userJwt(id))).andExpect(status().isOk());

				successes.incrementAndGet();

				return null;

			});

		}

		assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

		start.countDown();

		pool.shutdown();

		assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

		assertThat(successes.get()).isEqualTo(threads);

		assertThat(profiles.findAll().stream().filter(profile -> profile.getId().equals(id)).count()).isEqualTo(1);

	}



	@Test

	void duplicateEmailIsRejected() throws Exception {

		UUID first = UUID.fromString("11111111-1111-1111-1111-111111111111");

		UUID second = UUID.fromString("22222222-2222-2222-2222-222222222222");

		String shared = "shared@example.com";

		mockMvc.perform(get("/api/users/me").with(userJwt(first, shared)))

				.andExpect(status().isOk())

				.andExpect(jsonPath("$.email").value(shared));

		mockMvc.perform(get("/api/users/me").with(userJwt(second, shared)))

				.andExpect(status().isConflict())

				.andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));

		assertThat(profiles.findById(second)).isEmpty();

	}



	@Test

	void adminEndpointForbiddenForUser() throws Exception {

		UUID id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

		mockMvc.perform(get("/api/users").with(userJwt(id)))

				.andExpect(status().isForbidden());

	}



	@Test

	void wrongAudienceIsUnauthorized() throws Exception {

		UUID id = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

		String token = TestJwtSupport.token(id, "user@example.com", "other-api", Instant.now(), "USER");

		mockMvc.perform(get("/api/users/me")

						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)

						.accept(MediaType.APPLICATION_JSON))

				.andExpect(status().isUnauthorized());

	}



	@Test

	void deleteRequiresRecentLogin() throws Exception {

		UUID id = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

		mockMvc.perform(delete("/api/users/me").with(jwt().jwt(jwt -> jwt

						.subject(id.toString())

						.claim("email", email(id))

						.issuedAt(Instant.now().minusSeconds(3600))

						.claim("auth_time", Instant.now().minusSeconds(3600).getEpochSecond())

						.claim("realm_access", Map.of("roles", List.of("USER"))))

				.authorities(new SimpleGrantedAuthority("ROLE_USER"))))

				.andExpect(status().isForbidden());

	}



	@Test

	void deleteRejectsMissingAuthTime() throws Exception {

		UUID id = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

		mockMvc.perform(delete("/api/users/me").with(jwt().jwt(jwt -> jwt

						.subject(id.toString())

						.claim("email", email(id))

						.issuedAt(Instant.now().minusSeconds(3600))

						.claim("realm_access", Map.of("roles", List.of("USER"))))

				.authorities(new SimpleGrantedAuthority("ROLE_USER"))))

				.andExpect(status().isForbidden());

	}



	@Test

	void deleteWithRecentLoginEnqueuesOutbox() throws Exception {

		UUID id = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

		mockMvc.perform(delete("/api/users/me").with(jwt().jwt(jwt -> jwt

						.subject(id.toString())

						.claim("email", email(id))

						.claim("auth_time", Instant.now().getEpochSecond())

						.claim("realm_access", Map.of("roles", List.of("USER"))))

				.authorities(new SimpleGrantedAuthority("ROLE_USER"))))

				.andExpect(status().isAccepted());

		mockMvc.perform(get("/api/users/me").with(userJwt(id)))

				.andExpect(status().isConflict())

				.andExpect(jsonPath("$.code").value("ACCOUNT_DELETION_PENDING"));

	}



	private static RequestPostProcessor userJwt(UUID id) {

		return userJwt(id, email(id));

	}



	private static RequestPostProcessor userJwt(UUID id, String email) {

		return jwt().jwt(token -> token

						.subject(id.toString())

						.claim("email", email)

						.claim("name", "Ada Lovelace")

						.claim("realm_access", Map.of("roles", List.of("USER"))))

				.authorities(new SimpleGrantedAuthority("ROLE_USER"));

	}



	private static String email(UUID id) {

		return id + "@example.com";

	}

}


