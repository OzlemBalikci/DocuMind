package com.documind.user.quota;

import com.documind.common.persistence.CreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "quota_operations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuotaOperation extends CreatedAtEntity {

	public enum Type {
		CONSUME,
		RELEASE
	}

	@Id
	@Column(name = "operation_id")
	private UUID operationId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false)
	private int amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Type type;
}
