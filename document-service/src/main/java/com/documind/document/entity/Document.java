package com.documind.document.entity;

import com.documind.common.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends AuditedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "owner_id", nullable = false)
	private UUID ownerId;

	@Column(name = "file_name", nullable = false, length = 255)
	private String fileName;

	@Column(name = "content_type", nullable = false, length = 100)
	private String contentType;

	@Column(name = "size_bytes", nullable = false)
	private long sizeBytes;

	@Column(name = "storage_key", nullable = false, length = 500)
	private String storageKey;

	@Column(nullable = false, length = 20)
	private String status;

	public static Document create(
			UUID ownerId,
			String fileName,
			String contentType,
			long sizeBytes,
			String storageKey) {
		Document document = new Document();
		document.ownerId = ownerId;
		document.fileName = fileName;
		document.contentType = contentType;
		document.sizeBytes = sizeBytes;
		document.storageKey = storageKey;
		document.status = "UPLOADED";
		return document;
	}
}
