package com.sirket.auth.auth.dto.response;

import java.time.Instant;

public record QuotaResponse(
        int dailyLimit,
        int usedToday,
        int remaining,
        Instant resetsAt
) { }
