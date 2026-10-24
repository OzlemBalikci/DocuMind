package com.documind.user.service.impl;

import com.documind.platform.exception.ReauthenticationRequiredException;
import com.documind.platform.messaging.UserDeletedEvent;
import com.documind.user.model.OutboxEvent;
import com.documind.user.repository.OutboxRepository;
import com.documind.user.service.AccountDeletionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AccountDeletionServiceImpl implements AccountDeletionService {

	private final OutboxRepository outbox;
	private final ObjectMapper objectMapper;
	private final Duration reauthMaxAge;

	public AccountDeletionServiceImpl(
			OutboxRepository outbox,
			ObjectMapper objectMapper,
			@Value("${app.account.reauth-max-age:5m}") Duration reauthMaxAge) {
		this.outbox = outbox;
		this.objectMapper = objectMapper;
		this.reauthMaxAge = reauthMaxAge;
	}

	@Override
	@Transactional
	public void request(Jwt jwt) {
		requireRecentLogin(jwt);
		UUID id = UUID.fromString(jwt.getSubject());
		try {
			outbox.save(OutboxEvent.pending(
					"User",
					id,
					OutboxEvent.Type.USER_DELETED,
					objectMapper.writeValueAsString(new UserDeletedEvent(id))));
		} catch (DataIntegrityViolationException ignored) {
		}
	}

	private void requireRecentLogin(Jwt jwt) {
		Instant reference = recentAuthenticationInstant(jwt);
		if (reference == null || reference.isBefore(Instant.now().minus(reauthMaxAge))) {
			throw new ReauthenticationRequiredException();
		}
	}

	/**
	 * Keycloak {@code auth_time} is tied to the SSO session and can stay old after {@code prompt=login}.
	 * For a freshly issued access token, {@code iat} reflects the recent code exchange.
	 */
	private static Instant recentAuthenticationInstant(Jwt jwt) {
		Instant authTime = authTime(jwt);
		Instant issuedAt = jwt.getIssuedAt();
		if (authTime == null) {
			return issuedAt;
		}
		if (issuedAt == null) {
			return authTime;
		}
		return authTime.isAfter(issuedAt) ? authTime : issuedAt;
	}

	private static Instant authTime(Jwt jwt) {
		Instant parsed = jwt.getClaimAsInstant("auth_time");
		if (parsed != null) {
			return parsed;
		}
		Object raw = jwt.getClaim("auth_time");
		if (raw instanceof Number number) {
			return Instant.ofEpochSecond(number.longValue());
		}
		if (raw instanceof String text && !text.isBlank()) {
			return Instant.ofEpochSecond(Long.parseLong(text.trim()));
		}
		return null;
	}
}
