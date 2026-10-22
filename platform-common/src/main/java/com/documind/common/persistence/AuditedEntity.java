package com.documind.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.Instant;

@Getter
@MappedSuperclass
public abstract class AuditedEntity extends CreatedAtEntity {

	@Column(name = "updated_at", nullable = false)
	protected Instant updatedAt;

	@PrePersist
	protected void onAuditCreate() {
		if (updatedAt == null) {
			updatedAt = createdAt != null ? createdAt : Instant.now();
		}
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = Instant.now();
	}
}
