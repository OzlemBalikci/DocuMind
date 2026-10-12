package com.documind.backend.usage.entity;

import com.documind.backend.chat.entity.Message;
import com.documind.backend.common.entity.CreatedOnlyEntity;
import com.documind.backend.common.util.Texts;
import com.documind.backend.document.entity.Document;
import com.documind.backend.job.entity.ProcessingJob;
import com.documind.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "llm_usage_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LlmUsageLog extends CreatedOnlyEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "document_id")
	private Document document;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "message_id")
	private Message message;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "job_id")
	private ProcessingJob job;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private LlmOperation operation;

	@Column(nullable = false, length = 100)
	private String model;

	@Column(name = "prompt_tokens")
	private Integer promptTokens;

	@Column(name = "completion_tokens")
	private Integer completionTokens;

	@Column(name = "embedding_count")
	private Integer embeddingCount;

	@Column(name = "latency_ms", nullable = false)
	private Integer latencyMs;

	@Column(nullable = false)
	private boolean success;

	@Column(name = "error_message", length = 500)
	private String errorMessage;

	@Builder
	private LlmUsageLog(User user, Document document, Message message, ProcessingJob job, LlmOperation operation,
			String model, Integer promptTokens, Integer completionTokens, Integer embeddingCount, Integer latencyMs,
			Boolean success, String errorMessage) {
		this.user = user;
		this.document = document;
		this.message = message;
		this.job = job;
		this.operation = operation;
		this.model = model;
		this.promptTokens = promptTokens;
		this.completionTokens = completionTokens;
		this.embeddingCount = embeddingCount;
		this.latencyMs = latencyMs;
		this.success = Boolean.TRUE.equals(success);
		this.errorMessage = Texts.truncate(errorMessage, 500);
	}

}
