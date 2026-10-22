package com.documind.document.document.repository;

import com.documind.document.document.domain.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public interface DocumentSummary {

	UUID getId();
	String getTitle();
	String getOriginalFilename();
	String getContentType();
	Long getSizeBytes();
	DocumentStatus getStatus();
	Integer getPageCount();
	int getChunkCount();
	String getErrorMessage();
	Instant getCreatedAt();
	Instant getUpdatedAt();
	Instant getIndexedAt();
}
