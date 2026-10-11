package com.documind.backend.chat.entity;

import com.documind.backend.common.entity.AuditedEntity;
import com.documind.backend.common.util.Texts;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "conversation_id", nullable = false)
	private Conversation conversation;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MessageRole role;

	@Column(name = "content", columnDefinition = "text")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MessageStatus status = MessageStatus.DONE;

	@Column(length = 100)
	private String model;

	@Column(name = "prompt_tokens")
	private Integer promptTokens;

	@Column(name = "completion_tokens")
	private Integer completionTokens;

	@Column(name = "latency_ms")
	private Integer latencyMs;

	@Column(name = "error_message", length = 500)
	private String errorMessage;

	@Column(name = "answered_at")
	private Instant answeredAt;

	public static Message userMessage(Conversation conversation, String content) {
		Message message = new Message();
		message.conversation = conversation;
		message.role = MessageRole.USER;
		message.content = content;
		message.status = MessageStatus.DONE;
		return message;
	}

	public static Message assistantPlaceholder(Conversation conversation) {
		Message message = new Message();
		message.conversation = conversation;
		message.role = MessageRole.ASSISTANT;
		message.status = MessageStatus.PENDING;
		return message;
	}

	public void markRunning() {
		status = MessageStatus.RUNNING;
	}

	public void complete(String answer, String model, Integer promptTokens, Integer completionTokens,
			Integer latencyMs) {
		if (answer == null || answer.isBlank()) {
			throw new IllegalArgumentException("Boş cevap");
		}
		this.content = answer;
		this.model = model;
		this.promptTokens = promptTokens;
		this.completionTokens = completionTokens;
		this.latencyMs = latencyMs;
		this.status = MessageStatus.DONE;
		this.answeredAt = Instant.now();
	}

	public void fail(String reason) {
		this.status = MessageStatus.FAILED;
		this.errorMessage = Texts.truncate(reason, 500);
	}

}
