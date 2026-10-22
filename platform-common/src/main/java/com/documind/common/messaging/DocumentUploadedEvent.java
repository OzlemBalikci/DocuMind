package com.documind.common.messaging;

import java.util.UUID;

public record DocumentUploadedEvent(UUID documentId, UUID ownerId) {
}
