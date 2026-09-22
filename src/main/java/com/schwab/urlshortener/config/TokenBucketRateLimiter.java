package com.schwab.urlshortener.config;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * BROWNFIELD SCENARIO — added to an already-working create-URL endpoint
 * (see SCENARIOS.md). The endpoint worked fine without this; the task was
 * to add rate limiting without touching the existing controller/service
 * contract or breaking existing tests.
 *
 * Simple in-memory token bucket, keyed by client IP. AI's first draft used
 * a single shared bucket for all clients — REJECTED, because that would let
 * one noisy client starve every other client's quota. Reworked to a
 * per-key bucket (ConcurrentHashMap) so limits are isolated per caller.
 *
 * Known limitation (documented, not hidden): this is per-instance, in-memory
 * state. It does not coordinate across multiple app instances behind a load
 * balancer. A production version would back this with Redis (e.g.
 * Bucket4j + Redis) for a shared limit. Flagged in FINAL_SUMMARY.md as a
 * scaling trade-off accepted for prototype scope.
 */
public class TokenBucketRateLimiter {

    private static class Bucket {
        final AtomicLong tokens;
        volatile long lastRefillTimestamp;

        Bucket(long tokens, long lastRefillTimestamp) {
            this.tokens = new AtomicLong(tokens);
            this.lastRefillTimestamp = lastRefillTimestamp;
        }
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final long capacity;
    private final long refillPerMinute;

    public TokenBucketRateLimiter(long capacity, long refillPerMinute) {
        this.capacity = capacity;
        this.refillPerMinute = refillPerMinute;
    }

    public boolean tryConsume(String key) {
        Bucket bucket = buckets.computeIfAbsent(key,
                k -> new Bucket(capacity, System.currentTimeMillis()));
        refill(bucket);
        return bucket.tokens.getAndUpdate(t -> t > 0 ? t - 1 : t) > 0;
    }

    private void refill(Bucket bucket) {
        long now = System.currentTimeMillis();
        long elapsedMs = now - bucket.lastRefillTimestamp;
        long tokensToAdd = (elapsedMs * refillPerMinute) / 60_000L;
        if (tokensToAdd > 0) {
            bucket.tokens.updateAndGet(t -> Math.min(capacity, t + tokensToAdd));
            bucket.lastRefillTimestamp = now;
        }
    }
}
