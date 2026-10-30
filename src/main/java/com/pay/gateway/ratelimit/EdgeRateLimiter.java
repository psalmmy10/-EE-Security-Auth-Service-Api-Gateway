package com.pay.gateway.ratelimit;

import reactor.core.publisher.Mono;

import java.time.Duration;

/** Strategy: in-memory for a single gateway instance, Redis when several instances share counters. */
public interface EdgeRateLimiter {

    Mono<RateLimitDecision> tryAcquire(String key, int limit, Duration window);
}
