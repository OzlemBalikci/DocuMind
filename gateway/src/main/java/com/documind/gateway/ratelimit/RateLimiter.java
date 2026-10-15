package com.documind.gateway.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    public record Decision(boolean allowed, int limit, long remaining, Duration retryAfter) {
        static Decision pass(int limit) {
            return new Decision(true, limit, -1, Duration.ZERO);
        }
    }

    private final ProxyManager<byte[]> proxyManager;
    private final MeterRegistry meters;

    public RateLimiter(ProxyManager<byte[]> proxyManager, MeterRegistry meters) {
        this.proxyManager = proxyManager;
        this.meters = meters;
    }

    public Decision check(String policyName, RateLimitProperties.Policy policy, String key) {
        try {
            byte[] redisKey = ("rl:" + policyName + ":" + key).getBytes(StandardCharsets.UTF_8);
            ConsumptionProbe probe = proxyManager.builder()
                    .build(redisKey, () -> configuration(policy))
                    .tryConsumeAndReturnRemaining(1);

            if (probe.isConsumed()) {
                count(policyName, "allowed");
                return new Decision(true, policy.capacity(), probe.getRemainingTokens(), Duration.ZERO);
            }
            count(policyName, "Limited");
            return new Decision(false, policy.capacity(), 0, Duration.ofNanos(probe.getNanosToWaitForRefill()));
        } catch (RuntimeException ex) {
            count(policyName, "store_error");
            log.warn("Rate limit store error, policy={}, mode={}", policyName, policy.onStoreError(), ex);
            return policy.onStoreError() == RateLimitProperties.FailMode.OPEN
                    ? Decision.pass(policy.capacity())
                    : new Decision(false, policy.capacity(), 0, Duration.ofSeconds(5));
        }
    }

    private static BucketConfiguration configuration(RateLimitProperties.Policy policy) {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(policy.capacity())
                        .refillGreedy(policy.capacity(), policy.period())
                        .build())
                .build();
    }

    public void count(String policy, String outcome) {
        meters.counter("gateway.ratelimit", "policy", policy, "outcome", outcome).increment();
    }
}
