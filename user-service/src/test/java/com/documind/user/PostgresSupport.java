package com.documind.user;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public final class PostgresSupport {

	private PostgresSupport() {
	}

	public static PostgreSQLContainer container() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
	}

	public static void register(DynamicPropertyRegistry registry, PostgreSQLContainer postgres) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("spring.task.scheduling.enabled", () -> "false");
	}
}
