package com.documind.user.quota;

import com.documind.user.profile.UserProfile;

import java.util.UUID;

public record QuotaResponse(
		UUID id,
		String email,
		String fullName,
		int quotaLimit,
		int quotaUsed) {

	public static QuotaResponse from(UserProfile profile) {
		return new QuotaResponse(
				profile.getId(),
				profile.getEmail(),
				profile.getFullName(),
				profile.getQuotaLimit(),
				profile.getQuotaUsed());
	}
}
