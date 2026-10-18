package com.documind.gateway.ratelimit;

import com.documind.gateway.ratelimit.RateLimiter.Decision;
import com.documind.gateway.ratelimit.RateLimitProperties.Policy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Instant;

@Component
public class RateLimitFilters {

    private final RateLimitProperties properties;
    private final RateLimiter limiter;
    private final KeyResolver keys;

    public RateLimitFilters(RateLimitProperties properties, RateLimiter limiter, KeyResolver keys) {
        this.properties = properties;
        this.limiter = limiter;
        this.keys = keys;
    }

    /** Route tanımlanırken BİR KEZ çağrılır; döndürülen filtre her istekte yeniden kullanılır. */
    public HandlerFilterFunction<ServerResponse, ServerResponse> of(String policyName) {
        Policy policy = properties.policies().get(policyName);
        if (policy == null) {
            throw new IllegalArgumentException("Unknown rate limit policy: " + policyName);
        }

        return (request, next) -> {
            Decision decision = limiter.check(policyName, policy, keys.resolve(policy.key(), request));

            if (!decision.allowed()) {
                long retryAfter = Math.max(1, decision.retryAfter().toSeconds());
                return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Retry-After", String.valueOf(retryAfter))
                        .header("RateLimit-Limit", String.valueOf(decision.limit()))
                        .header("RateLimit-Remaining", "0")
                        .header("RateLimit-Reset", String.valueOf(retryAfter))
                        // Kendi ApiError modelinle değiştir.
                        .body(new RateLimitError(
                                429, "TOO_MANY_REQUESTS",
                                "İstek limiti aşıldı. " + retryAfter + " saniye sonra tekrar deneyin.",
                                request.path(), Instant.now()));
            }

            ServerResponse response = next.handle(request);
            if (decision.remaining() >= 0) {
                response.headers().set("RateLimit-Limit", String.valueOf(decision.limit()));
                response.headers().set("RateLimit-Remaining", String.valueOf(decision.remaining()));
            }
            return response;
        };
    }

    record RateLimitError(int status, String error, String message, String path, Instant timestamp) {
    }
}