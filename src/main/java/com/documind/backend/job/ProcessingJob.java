package com.documind.backend.job;

import com.documind.backend.chat.Message;
import com.documind.backend.common.BaseEntity;
import com.documind.backend.document.Document;
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
@Table(name = "processing_jobs")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProcessingJob extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "document_id")
	private Document document;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "message_id")
	private Message message;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 30)
	private JobType type;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	@Builder.Default
	private JobStatus status = JobStatus.QUEUED;

	@Column(name = "attempts", nullable = false)
	@Builder.Default
	private Short attempts = 0;

	@Column(name = "error_message", length = 500)
	private String errorMessage;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

}
