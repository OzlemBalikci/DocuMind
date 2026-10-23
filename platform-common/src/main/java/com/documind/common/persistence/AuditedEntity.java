package com.documind.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.Instant;

@Getter
@MappedSuperclass
public abstract class AuditedEntity extends CreatedAtEntity {

	@Column(name = "updated_at", nullable = false)
	protected Instant updatedAt;

	@Override
	protected void onCreate() {
		super.onCreate();
		if (updatedAt == null) {
			updatedAt = createdAt;
		}
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = Instant.now();
	}
}
