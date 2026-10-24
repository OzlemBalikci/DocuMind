package com.documind.user.service.impl;

import com.documind.common.security.CurrentUser;
import com.documind.user.common.AccountDeletionPendingException;
import com.documind.user.dto.response.AdminProfileResponse;
import com.documind.user.dto.response.UserProfileResponse;
import com.documind.user.model.OutboxEvent;
import com.documind.user.model.UserProfile;
import com.documind.user.repository.OutboxRepository;
import com.documind.user.repository.UserProfileRepository;
import com.documind.user.service.ProfileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class ProfileServiceImpl implements ProfileService {

	private final UserProfileRepository profiles;
	private final OutboxRepository outbox;
	private final int defaultQuotaLimit;

	public ProfileServiceImpl(
			UserProfileRepository profiles,
			OutboxRepository outbox,
			@Value("${app.account.default-quota-limit:100}") int defaultQuotaLimit) {
		this.profiles = profiles;
		this.outbox = outbox;
		this.defaultQuotaLimit = defaultQuotaLimit;
	}

	@Override
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

	@Override
	@Transactional(readOnly = true)
	public Page<AdminProfileResponse> listAll(Pageable pageable) {
		return profiles.findAll(pageable).map(AdminProfileResponse::from);
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
