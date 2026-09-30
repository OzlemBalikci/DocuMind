package com.documind.backend.usage;

import com.documind.backend.chat.Message;
import com.documind.backend.common.BaseEntity;
import com.documind.backend.document.Document;
import com.documind.backend.job.ProcessingJob;
import com.documind.backend.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "llm_usage_logs")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LlmUsageLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private ProcessingJob processingJob;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 30)
    private LlmOperation operation;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "embedding_count")
    private Integer embeddingCount;

    @Column(name = "latency_ms", nullable = false)
    private Integer latencyMs;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
