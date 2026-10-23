package com.documind.user.profile;

import com.documind.common.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Getter
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends AuditedEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = 255)
	private String email;

	@Column(name = "full_name", length = 100)
	private String fullName;

	@Column(name = "quota_limit", nullable = false)
	private int quotaLimit = 100;

	@Column(name = "quota_used", nullable = false)
	private int quotaUsed = 0;

	void refresh(String email, String fullName) {
		if (!Objects.equals(this.email, email)) this.email = email;
		if (!Objects.equals(this.fullName, fullName)) this.fullName = fullName;
	}
}
