package com.documind.backend.auth.entity;

import com.documind.backend.common.entity.CreatedOnlyEntity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
}
