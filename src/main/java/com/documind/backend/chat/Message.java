package com.documind.backend.chat;

import com.documind.backend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Message extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "conversation_id", nullable = false)
	private Conversation conversation;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	@Builder.Default
	private MessageRole role = MessageRole.USER;

	@Column(name = "content", columnDefinition = "text")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	@Builder.Default
	private MessageStatus status = MessageStatus.DONE;

	@Column(name = "model", length = 100)
	private String model;

	@Column(name = "prompt_tokens")
	private Integer promptTokens;

	@Column(name = "completion_tokens")
	private Integer completionTokens;

	@Column(name = "latency_ms")
	private Integer latencyMs;

	@Column(name = "error_message", length = 500)
	private String errorMessage;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "answered_at")
	private Instant answeredAt;

}
