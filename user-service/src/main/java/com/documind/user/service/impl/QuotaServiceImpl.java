package com.documind.user.service.impl;

import com.documind.platform.exception.NotFoundException;
import com.documind.platform.exception.QuotaExceededException;
import com.documind.user.dto.response.QuotaResponse;
import com.documind.user.model.QuotaOperationType;
import com.documind.user.model.UserProfile;
import com.documind.user.repository.QuotaOperationRepository;
import com.documind.user.repository.UserProfileRepository;
import com.documind.user.service.QuotaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class QuotaServiceImpl implements QuotaService {

	private final UserProfileRepository profiles;
	private final QuotaOperationRepository operations;

	public QuotaServiceImpl(UserProfileRepository profiles, QuotaOperationRepository operations) {
		this.profiles = profiles;
		this.operations = operations;
	}

	@Override
	@Transactional
	public QuotaResponse consume(UUID profileId, int amount, UUID operationId) {
		if (operations.insertIfAbsent(operationId, profileId, amount, QuotaOperationType.CONSUME.name()) == 0) {
			return current(profileId);
		}
		if (profiles.consumeQuota(profileId, amount, Instant.now()) == 0) {
			if (!profiles.existsById(profileId)) {
				throw new NotFoundException();
			}
			throw new QuotaExceededException();
		}
		return current(profileId);
	}

	@Override
	@Transactional
	public QuotaResponse release(UUID profileId, int amount, UUID operationId) {
		if (operations.insertIfAbsent(operationId, profileId, amount, QuotaOperationType.RELEASE.name()) == 0) {
			return current(profileId);
		}
		int updated = profiles.releaseQuota(profileId, amount, Instant.now());
		if (updated == 0) {
			UserProfile profile = profiles.findById(profileId).orElseThrow(NotFoundException::new);
			log.warn("Quota release {} for {} updated 0 rows (used={})", amount, profileId, profile.getQuotaUsed());
			throw new IllegalStateException("Cannot release " + amount + " quota for " + profileId);
		}
		return current(profileId);
	}

	private QuotaResponse current(UUID profileId) {
		return QuotaResponse.from(profiles.findById(profileId).orElseThrow(NotFoundException::new));
	}
}
