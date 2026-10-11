package com.documind.backend.document.entity;

import com.documind.backend.common.entity.AuditedEntity;
import com.documind.backend.common.util.Texts;
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
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends AuditedEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Setter
	@Column(nullable = false, length = 255)
	private String title;

	@Column(name = "original_filename", nullable = false, length = 255)
	private String originalFilename;

	@Column(name = "content_type", nullable = false, length = 100)
	private String contentType;

	@Column(name = "size_bytes", nullable = false)
	private Long sizeBytes;

	@Column(name = "checksum_sha256", nullable = false, length = 64)
	private String checksumSha256;

	@Column(name = "storage_path", nullable = false, length = 500)
	private String storagePath;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DocumentStatus status = DocumentStatus.PENDING;

	@Column(name = "page_count")
	private Integer pageCount;

	@Column(name = "chunk_count", nullable = false)
	private Integer chunkCount = 0;

	@Column(name = "embedding_model", length = 100)
	private String embeddingModel;

	@Column(name = "error_message", length = 500)
	private String errorMessage;

	@Column(name = "indexed_at")
	private Instant indexedAt;

	@Builder
	private Document(User user, String title, String originalFilename, String contentType, Long sizeBytes,
			String checksumSha256, String storagePath) {
		this.user = user;
		this.title = title;
		this.originalFilename = originalFilename;
		this.contentType = contentType;
		this.sizeBytes = sizeBytes;
		this.checksumSha256 = checksumSha256;
		this.storagePath = storagePath;
	}

	public void startParsing() {
		require(DocumentStatus.PENDING);
		status = DocumentStatus.PARSING;
	}

	public void startEmbedding(Integer pageCount, int chunkCount) {
		require(DocumentStatus.PARSING);
		this.pageCount = pageCount;
		this.chunkCount = chunkCount;
		status = DocumentStatus.EMBEDDING;
	}

	public void markReady(String model) {
		require(DocumentStatus.EMBEDDING);
		this.embeddingModel = model;
		this.indexedAt = Instant.now();
		status = DocumentStatus.READY;
	}

	public void fail(String reason) {
		this.status = DocumentStatus.FAILED;
		this.errorMessage = Texts.truncate(reason, 500);
	}

	private void require(DocumentStatus expected) {
		if (status != expected) {
			throw new IllegalStateException("Beklenen " + expected + ", mevcut " + status);
		}
	}

}
