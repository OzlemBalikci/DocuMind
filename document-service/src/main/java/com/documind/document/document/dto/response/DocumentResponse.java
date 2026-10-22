package com.documind.document.document.dto.response;

import com.documind.document.document.domain.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
		UUID id,
		String title,
		DocumentStatus status,
		String originalFilename,
		String contentType,
		Long sizeBytes,
		Instant createdAt,
		Instant updatedAt
) {}
