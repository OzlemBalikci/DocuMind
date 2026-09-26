package com.interviewpilot.backend.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads {@code .env} from the working directory before Spring starts.
 * Avoids broken {@code ${POSTGRES_PASSWORD}} resolution when Windows env vars or {@code $} in passwords disagree with Docker.
 */
public final class LocalDotEnvLoader {

	private LocalDotEnvLoader() {
	}

	public static void applyIfLocalProfile() {
		if (!usesLocalProfile()) {
			return;
		}
		Path envFile = Path.of(System.getProperty("user.dir"), ".env");
		if (!Files.isRegularFile(envFile)) {
			return;
		}
		Map<String, String> values = parseEnvFile(envFile);
		applyCredential("spring.datasource.username", first(values, "local.postgres.username", "POSTGRES_USER"),
				"SPRING_DATASOURCE_USERNAME", "interviewpilot_app");
		applyCredential("spring.datasource.password", postgresPassword(values), "SPRING_DATASOURCE_PASSWORD", null);
		applyCredential("spring.rabbitmq.password", first(values, "local.rabbitmq.password", "RABBITMQ_PASSWORD"),
				"LOCAL_RABBITMQ_PASSWORD", null);
		applyCredential("app.jwt.secret", first(values, "local.jwt.secret", "JWT_SECRET"), "LOCAL_JWT_SECRET", null);
	}

	private static boolean usesLocalProfile() {
		String profiles = System.getenv("SPRING_PROFILES_ACTIVE");
		if (profiles == null || profiles.isBlank()) {
			profiles = System.getProperty("spring.profiles.active", "local");
		}
		for (String profile : profiles.split(",")) {
			if ("local".equalsIgnoreCase(profile.trim())) {
				return true;
			}
		}
		return false;
	}

	private static String postgresPassword(Map<String, String> values) {
		String fromLocal = values.get("local.postgres.password");
		if (fromLocal != null && !fromLocal.isBlank()) {
			return unescapeCompose(fromLocal);
		}
		String fromPostgres = values.get("POSTGRES_PASSWORD");
		if (fromPostgres != null && !fromPostgres.isBlank()) {
			return unescapeCompose(fromPostgres);
		}
		return null;
	}

	private static void applyCredential(String propertyKey, String fromFile, String envKey, String defaultValue) {
		if (isSet(System.getenv(envKey)) || isSet(System.getProperty(propertyKey))) {
			return;
		}
		String value = fromFile != null && !fromFile.isBlank() ? fromFile : defaultValue;
		if (value == null || value.isBlank()) {
			return;
		}
		System.setProperty(propertyKey, value);
	}

	private static boolean isSet(String value) {
		return value != null && !value.isBlank();
	}

	private static String first(Map<String, String> values, String... keys) {
		for (String key : keys) {
			String value = values.get(key);
			if (value != null && !value.isBlank()) {
				return unescapeCompose(value);
			}
		}
		return null;
	}

	private static String unescapeCompose(String value) {
		return value.replace("$$", "$");
	}

	static Map<String, String> parseEnvFile(Path envFile) {
		Map<String, String> result = new HashMap<>();
		List<String> lines;
		try {
			lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not read .env at " + envFile.toAbsolutePath(), ex);
		}
		for (String line : lines) {
			String trimmed = line.strip();
			if (trimmed.isEmpty() || trimmed.startsWith("#")) {
				continue;
			}
			int eq = trimmed.indexOf('=');
			if (eq <= 0) {
				continue;
			}
			String key = trimmed.substring(0, eq).strip();
			String value = trimmed.substring(eq + 1).strip();
			if (key.startsWith("\uFEFF")) {
				key = key.substring(1);
			}
			result.put(key, value);
		}
		return result;
	}

}
