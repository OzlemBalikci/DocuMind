package com.documind.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;

import java.time.Instant;

@Getter
@MappedSuperclass
public abstract class CreatedAtEntity {

	@Column(name = "created_at", nullable = false, updatable = false)
	protected Instant createdAt;

	@PrePersist
	protected void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
