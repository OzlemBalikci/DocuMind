package com.documind.backend.user;

import com.documind.backend.common.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AuditedEntity {

	@Column(nullable = false, length = 255)
	private String email;

	@Setter
	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Setter
	@Column(name = "full_name", nullable = false, length = 100)
	private String fullName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role = Role.USER;

	@Builder
	private User(String email, String passwordHash, String fullName) {
		this.email = email.trim().toLowerCase();
		this.passwordHash = passwordHash;
		this.fullName = fullName;
	}

}
