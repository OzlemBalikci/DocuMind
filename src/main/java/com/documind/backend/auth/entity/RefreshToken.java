package com.documind.backend.auth.entity;

import com.documind.backend.common.entity.CreatedOnlyEntity;
import com.documind.backend.common.util.Texts;
import com.documind.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends CreatedOnlyEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "family_id", nullable = false)
	private UUID familyId;

	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "replaced_by_id")
	private RefreshToken replacedBy;

	@Column(name = "user_agent", length = 255)
	private String userAgent;

	@Column(name = "ip_address", columnDefinition = "inet")
	@ColumnTransformer(write = "?::inet")
	private String ipAddress;

	public static RefreshToken create(User user, UUID familyId, String tokenHash, Instant expiresAt, String userAgent,
			String ipAddress) {
		if (expiresAt == null || !expiresAt.isAfter(Instant.now())) {
			throw new IllegalArgumentException("expires_at gelecekte olmalı");
		}
		RefreshToken token = new RefreshToken();
		token.user = user;
		token.familyId = familyId;
		token.tokenHash = tokenHash;
		token.expiresAt = expiresAt;
		token.userAgent = Texts.truncate(userAgent, 255);
		token.ipAddress = ipAddress;
		return token;
	}

	public boolean isActive() {
		return revokedAt == null && expiresAt.isAfter(Instant.now());
	}

	public void revoke() {
		if (revokedAt == null) {
			revokedAt = Instant.now();
		}
	}

	public void revokeAsReplacedBy(RefreshToken successor) {
		if (successor == null) {
			throw new IllegalArgumentException("successor boş olamaz");
		}
		revoke();
		replacedBy = successor;
	}

}
