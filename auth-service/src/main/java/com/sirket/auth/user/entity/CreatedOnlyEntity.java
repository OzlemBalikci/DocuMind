package com.sirket.auth.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@MappedSuperclass
public abstract class CreatedOnlyEntity extends BaseEntity {

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

}
