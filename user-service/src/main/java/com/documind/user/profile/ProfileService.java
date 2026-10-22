package com.documind.user.profile;

import com.documind.common.security.CurrentUser;
import com.documind.user.common.AccountDeletionPendingException;
import com.documind.user.outbox.OutboxEvent;
import com.documind.user.outbox.OutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class ProfileService {

	private final UserProfileRepository profiles;
	private final OutboxRepository outbox;
	private final int defaultQuotaLimit;

	public ProfileService(
			UserProfileRepository profiles,
			OutboxRepository outbox,
			@Value("${app.account.default-quota-limit:100}") int defaultQuotaLimit) {
		this.profiles = profiles;
		this.outbox = outbox;
		this.defaultQuotaLimit = defaultQuotaLimit;
	}

	@Transactional
	public UserProfileResponse getOrCreate(Jwt jwt) {
		CurrentUser user = CurrentUser.from(jwt);
		if (outbox.existsByAggregateIdAndEventType(user.id(), OutboxEvent.Type.USER_DELETED)) {
			throw new AccountDeletionPendingException();
		}
		String email = Objects.requireNonNullElse(user.email(), "");
		String name = firstNonBlank(jwt.getClaimAsString("name"), jwt.getClaimAsString("preferred_username"));
		profiles.insertIfAbsent(user.id(), email, name, defaultQuotaLimit);
		UserProfile profile = profiles.findById(user.id()).orElseThrow();
		profile.refresh(email, name);
		return UserProfileResponse.from(profile);
	}

	@Transactional(readOnly = true)
	public List<UserProfileResponse> listAll() {
		return profiles.findAll().stream().map(UserProfileResponse::from).toList();
	}

	private static String firstNonBlank(String... values) {
		if (values == null) {
			return null;
		}
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}
}
