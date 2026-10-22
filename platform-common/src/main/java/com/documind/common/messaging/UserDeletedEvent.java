package com.documind.common.messaging;

import java.util.UUID;

public record UserDeletedEvent(UUID ownerId) {
}
