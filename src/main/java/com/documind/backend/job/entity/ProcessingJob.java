package com.documind.backend.job.entity;

import com.documind.backend.chat.entity.Message;
import com.documind.backend.common.entity.AuditedEntity;
import com.documind.backend.common.util.Texts;
import com.documind.backend.document.entity.Document;
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
@Table(name = "processing_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessingJob extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "document_id")
	private Document document;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "message_id")
	private Message message;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private JobType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private JobStatus status = JobStatus.QUEUED;

	@Column(nullable = false)
	private Short attempts = 0;

	@Column(name = "error_message", length = 500)
	private String errorMessage;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	public static ProcessingJob forDocument(JobType type, Document document) {
		if (type == JobType.ANSWER_QUESTION) {
			throw new IllegalArgumentException("ANSWER_QUESTION document job olamaz");
		}
		ProcessingJob job = new ProcessingJob();
		job.type = type;
		job.document = document;
		return job;
	}

	public static ProcessingJob answerQuestion(Message message) {
		ProcessingJob job = new ProcessingJob();
		job.type = JobType.ANSWER_QUESTION;
		job.message = message;
		return job;
	}

	public void start() {
		status = JobStatus.RUNNING;
		startedAt = Instant.now();
		attempts++;
	}

	public void done() {
		status = JobStatus.DONE;
		finishedAt = Instant.now();
	}

	public void fail(String reason) {
		status = JobStatus.FAILED;
		finishedAt = Instant.now();
		errorMessage = Texts.truncate(reason, 500);
	}

	public void requeue() {
		status = JobStatus.QUEUED;
		startedAt = null;
	}

}
