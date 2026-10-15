package com.documind.backend.auth.dto;

import java.time.Instant;

public record QuotaResponse(
        int dailyLimit,
        int usedToday,
        int remaining,
        Instant resetsAt
) { }