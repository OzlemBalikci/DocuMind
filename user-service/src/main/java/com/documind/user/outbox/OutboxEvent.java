package com.documind.user.outbox;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends CreatedAtEntity {

	public enum Type {
		USER_DELETED
	}

	@Id
	private UUID id;

	@Column(name = "aggregate_type", nullable = false, length = 50)
	private String aggregateType;

	@Column(name = "aggregate_id", nullable = false)
	private UUID aggregateId;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 80)
	private Type eventType;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private String payload;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OutboxStatus status;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "available_at", nullable = false)
	private Instant availableAt;

	@Column(name = "processed_at")
	private Instant processedAt;

	@Column(name = "last_error", columnDefinition = "text")
	private String lastError;

	public static OutboxEvent pending(String aggregateType, UUID aggregateId, Type eventType, String payload) {
		OutboxEvent event = new OutboxEvent();
		event.id = UUID.randomUUID();
		event.aggregateType = aggregateType;
		event.aggregateId = aggregateId;
		event.eventType = eventType;
		event.payload = payload;
		event.status = OutboxStatus.PENDING;
		event.attempts = 0;
		event.availableAt = Instant.now();
		event.createdAt = Instant.now();
		return event;
	}

	public void markProcessing() {
		this.status = OutboxStatus.PROCESSING;
		this.availableAt = Instant.now();
	}

	public void markProcessed() {
		this.status = OutboxStatus.PROCESSED;
		this.processedAt = Instant.now();
		this.lastError = null;
	}

	public void markFailed(String error, Instant retryAt, int maxAttempts) {
		this.attempts += 1;
		this.lastError = error;
		if (this.attempts >= maxAttempts) {
			this.status = OutboxStatus.DEAD;
			return;
		}
		this.status = OutboxStatus.FAILED;
		this.availableAt = retryAt;
	}

	public void recover() {
		this.status = OutboxStatus.PENDING;
		this.availableAt = Instant.now();
	}
}
