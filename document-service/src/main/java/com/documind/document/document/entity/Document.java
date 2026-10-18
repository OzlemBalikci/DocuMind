package com.documind.document.document.entity;

import com.documind.common.persistence.AuditedEntity;
import com.documind.document.document.domain.DocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends AuditedEntity {

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(name = "storage_key", length = 500)
	private String storageKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DocumentStatus status;

	@Column(name = "original_filename", length = 255)
	private String originalFilename;

	@Column(name = "content_type", length = 127)
	private String contentType;

	@Column(name = "size_bytes")
	private Long sizeBytes;

	public static Document pending(Long userId,
			String title,
			String originalFilename,
			String contentType,
			long sizeBytes) {
		Document document = new Document();
		document.userId = userId;
		document.title = title;
		document.originalFilename = originalFilename;
		document.contentType = contentType;
		document.sizeBytes = sizeBytes;
		document.status = DocumentStatus.PENDING;
		return document;
	}

	public void markUploaded(String storageKey) {
		this.storageKey = storageKey;
		this.status = DocumentStatus.UPLOADED;
	}

	public void markFailed() {
		this.status = DocumentStatus.FAILED;
	}

	public void changeStatus(DocumentStatus newStatus) {
		this.status = newStatus;
	}
}
