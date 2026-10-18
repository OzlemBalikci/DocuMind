package com.sirket.auth.token.entity;

import com.sirket.auth.common.util.Texts;
import com.sirket.auth.user.entity.CreatedOnlyEntity;
import com.sirket.auth.user.entity.User;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

	@JdbcTypeCode(SqlTypes.CHAR)
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
			String ipAddress, Instant now) {
		if (now == null) {
			throw new IllegalArgumentException("now must not be null");
		}
		if (expiresAt == null || !expiresAt.isAfter(now)) {
			throw new IllegalArgumentException("expiresAt must be in the future");
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

	public boolean isActive(Instant now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}

	public void revoke(Instant now) {
		if (revokedAt == null) {
			revokedAt = now;
		}
	}

	public void revokeAsReplacedBy(RefreshToken successor, Instant now) {
		if (successor == null) {
			throw new IllegalArgumentException("successor must not be null");
		}
		revoke(now);
		if (replacedBy == null) {
			replacedBy = successor;
		}
	}

}
