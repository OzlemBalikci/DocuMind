package com.documind.document.document.entity;

import com.documind.common.persistence.AuditedEntity;
import com.documind.document.document.domain.DocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends AuditedEntity {

	@Column(name = "user_id", nullable = false, updatable = false)
	private Long userId;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(name = "original_filename", nullable = false, length = 255)
	private String originalFilename;

	@Column(name = "content_type", nullable = false, length = 100)
	private String contentType;

	@Column(name = "size_bytes", nullable = false)
	private Long sizeBytes;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "checksum_sha256", nullable = false, updatable = false, length = 64)
	private String checksumSha256;

	@Column(name = "storage_path", nullable = false, length = 500)
	private String storagePath;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DocumentStatus status;

	@Column(name = "page_count")
	private Integer pageCount;

	@Column(name = "chunk_count", nullable = false)
	private int chunkCount;

	@Column(name = "embedding_model", length = 100)
	private String embeddingModel;

	@Column(name = "error_message", length = 500)
	private String errorMessage;

	@Column(name = "indexed_at")
	private Instant indexedAt;

	public static Document pending(
			Long userId,
			String title,
			String originalFilename,
			String contentType,
			long sizeBytes,
			String checksumSha256,
			String storagePath) {
		Document document = new Document();
		document.userId = userId;
		document.title = title;
		document.originalFilename = originalFilename;
		document.contentType = contentType;
		document.sizeBytes = sizeBytes;
		document.checksumSha256 = checksumSha256;
		document.storagePath = storagePath;
		document.status = DocumentStatus.PENDING;
		document.chunkCount = 0;
		return document;
	}

	public void markFailed(String errorMessage) {
		this.status = DocumentStatus.FAILED;
		this.errorMessage = errorMessage;
	}

	public void changeStatus(DocumentStatus newStatus) {
		this.status = newStatus;
	}
}
