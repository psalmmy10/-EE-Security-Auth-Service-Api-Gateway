package com.pay.gateway.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;

/**
 * Fixed-window counter shared by all gateway instances (INCR + EXPIRE on first hit), fully non-blocking.
 * Fails OPEN if Redis is unreachable: the edge stays up and each service's own limits still apply.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.gateway.rate-limit", name = "store", havingValue = "redis")
public class RedisEdgeRateLimiter implements EdgeRateLimiter {

    private static final String PREFIX = "gw:rl:";

    private final ReactiveStringRedisTemplate redis;
    private final Clock clock = Clock.systemUTC();

    @Override
    public Mono<RateLimitDecision> tryAcquire(String key, int limit, Duration window) {
        long now = clock.millis();
        long size = window.toMillis();
        long windowStart = now - (now % size);
        String redisKey = PREFIX + key + ":" + windowStart;

        return redis.opsForValue().increment(redisKey)
                .flatMap(count -> count == 1L
                        ? redis.expire(redisKey, window.plusSeconds(1)).thenReturn(count)
                        : Mono.just(count))
                .map(count -> count <= limit
                        ? RateLimitDecision.allow()
                        : new RateLimitDecision(false, Math.max(1, (windowStart + size - now + 999) / 1000)))
                .onErrorResume(ex -> {
                    log.error("Redis unavailable for edge rate limiting; allowing request (fail open)", ex);
                    return Mono.just(RateLimitDecision.allow());
                });
    }
}
