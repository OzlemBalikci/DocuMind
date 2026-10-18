package com.sirket.auth.token.service.impl;

import com.sirket.auth.token.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;


@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenCleanupService {

    private static final Duration RETENTION_AFTER_EXPIRY = Duration.ofDays(30);

    private final RefreshTokenRepository repository;
    private final Clock clock;

    @Scheduled(cron = "${app.jwt.refresh-cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void deleteExpiredTokens() {
        Instant cutoff = clock.instant().minus(RETENTION_AFTER_EXPIRY);
        int deleted = repository.deleteExpiredBefore(cutoff);
        if (deleted > 0) {
            log.info("Deleted {} expired refresh token(s) with expires_at before {}", deleted, cutoff);
        }
    }
}
