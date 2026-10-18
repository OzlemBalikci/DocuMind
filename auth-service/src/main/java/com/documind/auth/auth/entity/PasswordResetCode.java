package com.documind.auth.auth.entity;

import com.documind.common.persistence.CreatedOnlyEntity;
import com.documind.auth.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.documind.auth.auth.domain.ResetCodeHasher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@Entity
@Table(name = "password_reset_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetCode extends CreatedOnlyEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "code_hash", nullable = false, length = 64)
	private String codeHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "used_at")
	private Instant usedAt;

	@Column(name = "failed_attempts", nullable = false)
	private int failedAttempts;

	public static PasswordResetCode issue(User user, String codeHash, Instant expiresAt) {
		PasswordResetCode code = new PasswordResetCode();
		code.user = user;
		code.codeHash = codeHash;
		code.expiresAt = expiresAt;
		return code;
	}

	public void markUsed(Instant now) {
		this.usedAt = now;
	}

	public void recordFailedAttempt() {
		this.failedAttempts++;
	}

	public boolean isLockedOut(int maxFailedAttempts) {
		return failedAttempts >= maxFailedAttempts;
	}

	public boolean matchesPlainCode(String plainCode, ResetCodeHasher hasher) {
		String presented = hasher.hash(plainCode.trim());
		return MessageDigest.isEqual(
				presented.getBytes(StandardCharsets.UTF_8),
				codeHash.getBytes(StandardCharsets.UTF_8));
	}

	public void recordWrongAttempt(int maxFailedAttempts, Instant now) {
		recordFailedAttempt();
		if (failedAttempts >= maxFailedAttempts) {
			markUsed(now);
		}
	}
}
