package com.documind.document.document.repository;

import com.documind.document.document.domain.DocumentStatus;

import java.time.Instant;

public interface DocumentSummary {

	Long getId();
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
