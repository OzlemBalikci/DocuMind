package com.documind.user.deletion;

import com.documind.backend.common.exception.ReauthenticationRequiredException;
import com.documind.common.messaging.UserDeletedEvent;
import com.documind.user.outbox.OutboxEvent;
import com.documind.user.outbox.OutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AccountDeletionService {

	private final OutboxRepository outbox;
	private final ObjectMapper objectMapper;
	private final Duration reauthMaxAge;

	public AccountDeletionService(
			OutboxRepository outbox,
			ObjectMapper objectMapper,
			@Value("${app.account.reauth-max-age:5m}") Duration reauthMaxAge) {
		this.outbox = outbox;
		this.objectMapper = objectMapper;
		this.reauthMaxAge = reauthMaxAge;
	}

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
		Instant authTime = authTime(jwt);
		if (authTime == null || authTime.isBefore(Instant.now().minus(reauthMaxAge))) {
			throw new ReauthenticationRequiredException();
		}
	}

	private static Instant authTime(Jwt jwt) {
		return jwt.getClaimAsInstant("auth_time");
	}
}
