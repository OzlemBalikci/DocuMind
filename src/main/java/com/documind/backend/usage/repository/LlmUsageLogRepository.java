package com.documind.backend.usage.repository;

import com.documind.backend.usage.entity.LlmUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmUsageLogRepository extends JpaRepository<LlmUsageLog, Long> {
}
