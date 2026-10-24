package com.documind.user.dto.response;

import com.documind.user.model.UserProfile;

import java.util.UUID;

public record QuotaResponse(
		UUID id,
		int quotaLimit,
		int quotaUsed,
		int quotaRemaining) {

	public static QuotaResponse from(UserProfile profile) {
		return new QuotaResponse(
				profile.getId(),
				profile.getQuotaLimit(),
				profile.getQuotaUsed(),
				profile.getQuotaLimit() - profile.getQuotaUsed());
	}
}
