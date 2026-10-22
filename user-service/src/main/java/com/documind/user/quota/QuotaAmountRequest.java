package com.documind.user.quota;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record QuotaAmountRequest(@NotNull UUID operationId, @Min(1) int amount) {
}
