package com.documind.user.quota;

import com.documind.backend.common.exception.NotFoundException;
import com.documind.backend.common.exception.QuotaExceededException;
import com.documind.user.profile.UserProfile;
import com.documind.user.profile.UserProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class QuotaService {

	private final UserProfileRepository profiles;
	private final QuotaOperationRepository operations;

	public QuotaService(UserProfileRepository profiles, QuotaOperationRepository operations) {
		this.profiles = profiles;
		this.operations = operations;
	}

	@Transactional
	public QuotaResponse consume(UUID id, int amount, UUID operationId) {
		if (operations.insertIfAbsent(operationId, id, amount, QuotaOperation.Type.CONSUME.name()) == 0) {
			return current(id);
		}
		if (profiles.consumeQuota(id, amount, Instant.now()) == 0) {
			if (!profiles.existsById(id)) {
				throw new NotFoundException();
			}
			throw new QuotaExceededException();
		}
		return current(id);
	}

	@Transactional
	public QuotaResponse release(UUID id, int amount, UUID operationId) {
		if (operations.insertIfAbsent(operationId, id, amount, QuotaOperation.Type.RELEASE.name()) == 0) {
			return current(id);
		}
		int updated = profiles.releaseQuota(id, amount, Instant.now());
		if (updated == 0) {
			UserProfile profile = profiles.findById(id).orElseThrow(NotFoundException::new);
			log.warn("Quota release {} for {} updated 0 rows (used={})", amount, id, profile.getQuotaUsed());
			throw new IllegalStateException("Cannot release " + amount + " quota for " + id);
		}
		return current(id);
	}

	private QuotaResponse current(UUID id) {
		return QuotaResponse.from(profiles.findById(id).orElseThrow(NotFoundException::new));
	}
}
