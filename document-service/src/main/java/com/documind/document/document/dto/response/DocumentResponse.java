package com.documind.document.document.dto.response;

import com.documind.document.document.domain.DocumentStatus;

import java.time.Instant;

public record DocumentResponse(
        Long id,
        String title,
        DocumentStatus status,   // JSON: "PENDING", "UPLOADED", ...
        String originalFilename,
        String contentType,
        Long sizeBytes,
        Instant createdAt,
        Instant updatedAt
) {}
