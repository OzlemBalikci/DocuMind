package com.documind.user.profile;

import java.util.UUID;

public record UserProfileResponse(
		UUID id,
		String email,
		String fullName,
		int quotaLimit,
		int quotaUsed) {

	public static UserProfileResponse from(UserProfile profile) {
		return new UserProfileResponse(
				profile.getId(),
				profile.getEmail(),
				profile.getFullName(),
				profile.getQuotaLimit(),
				profile.getQuotaUsed());
	}
}
