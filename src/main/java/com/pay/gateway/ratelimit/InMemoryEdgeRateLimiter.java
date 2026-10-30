package com.pay.gateway.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Fixed-window counter per key; lock-free and non-blocking. Correct for ONE gateway instance only. */
@Component
@ConditionalOnProperty(prefix = "app.gateway.rate-limit", name = "store", havingValue = "memory", matchIfMissing = true)
public class InMemoryEdgeRateLimiter implements EdgeRateLimiter {

    private record Window(long expiresAt, AtomicInteger count) {
    }

    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryEdgeRateLimiter() {
        this(Clock.systemUTC());
    }

    InMemoryEdgeRateLimiter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Mono<RateLimitDecision> tryAcquire(String key, int limit, Duration window) {
        long now = clock.millis();
        long size = window.toMillis();
        long windowStart = now - (now % size);
        long windowEnd = windowStart + size;
        Window current = windows.computeIfAbsent(key + ":" + windowStart, k -> new Window(windowEnd, new AtomicInteger()));
        if (current.count().incrementAndGet() <= limit) {
            return Mono.just(RateLimitDecision.allow());
        }
        return Mono.just(new RateLimitDecision(false, Math.max(1, (windowEnd - now + 999) / 1000)));
    }

    @Scheduled(fixedDelayString = "PT30S")
    public void evictExpired() {
        long now = clock.millis();
        windows.values().removeIf(w -> w.expiresAt() < now);
    }
}
