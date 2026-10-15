package com.documind.gateway.ratelimit;


import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

	@Bean(destroyMethod = "shutdown")
	RedisClient rateLimitRedisClient(RateLimitProperties properties) {
		return RedisClient.create(properties.redisUri());
	}

	@Bean
    ProxyManager<byte[]> rateLimitProxyManager(RedisClient client) {
		return LettuceBasedProxyManager.builderFor(client)
				.withExpirationStrategy(
						ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(30)))
				.build();
	}

}
