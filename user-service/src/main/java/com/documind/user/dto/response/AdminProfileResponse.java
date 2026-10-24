package com.documind.user.dto.response;

import com.documind.user.model.UserProfile;

import java.time.Instant;
import java.util.UUID;

public record AdminProfileResponse(
		UUID id,
		String email,
		String fullName,
		int quotaLimit,
		int quotaUsed,
		int quotaRemaining,
		Instant createdAt,
		Instant updatedAt) {

	public static AdminProfileResponse from(UserProfile profile) {
		return new AdminProfileResponse(
				profile.getId(),
				profile.getEmail(),
				profile.getFullName(),
				profile.getQuotaLimit(),
				profile.getQuotaUsed(),
				profile.getQuotaLimit() - profile.getQuotaUsed(),
				profile.getCreatedAt(),
				profile.getUpdatedAt());
	}
}
