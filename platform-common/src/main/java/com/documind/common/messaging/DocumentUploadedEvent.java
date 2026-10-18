package com.documind.common.messaging;

public record DocumentUploadedEvent(long documentId, long userId) {
}
