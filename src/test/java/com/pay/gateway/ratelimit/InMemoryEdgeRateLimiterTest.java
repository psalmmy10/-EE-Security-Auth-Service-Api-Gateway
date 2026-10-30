package com.pay.gateway.ratelimit;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

class InMemoryEdgeRateLimiterTest {

    private final InMemoryEdgeRateLimiter limiter =
            new InMemoryEdgeRateLimiter(Clock.fixed(Instant.parse("2026-10-28T10:00:20Z"), ZoneOffset.UTC));

    @Test
    void rejectsOverLimitWithRetryAfter() {
        for (int i = 0; i < 2; i++) {
            StepVerifier.create(limiter.tryAcquire("ip:1.2.3.4", 2, Duration.ofMinutes(1)))
                    .expectNextMatches(RateLimitDecision::allowed).verifyComplete();
        }
        StepVerifier.create(limiter.tryAcquire("ip:1.2.3.4", 2, Duration.ofMinutes(1)))
                .expectNextMatches(d -> !d.allowed() && d.retryAfterSeconds() == 40)
                .verifyComplete();
    }
}
