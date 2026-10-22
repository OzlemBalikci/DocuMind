package com.documind.gateway.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "app.edge.rate-limit")
public record RateLimitProperties(
    @NotBlank String redisUri,
    @NotEmpty Map<String, @Valid Policy> policies)  {

    public record Policy(
            @Min(1) int capacity,
            @NotNull Duration period,
            @NotNull KeyType key,
            @NotNull FailMode onStoreError) {
    }

    public enum KeyType {IP, USER_OR_IP}

    public enum FailMode {OPEN, CLOSED}
}
