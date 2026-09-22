# Architecture Overview

## Components

```
Client
  │
  ▼
UrlController  ───────────────►  TokenBucketRateLimiter (per-IP, in-memory)
  │
  ├── POST /api/urls  ──► UrlShortenerService ──► ShortUrlRepository ──► H2 (short_urls)
  │                              │
  │                              └── Base62Encoder (short-code generation)
  │
  ├── GET /{code}      ──► UrlShortenerService.resolve() (Caffeine-cached)
  │                              │
  │                              └── AnalyticsService.recordClick() (async) ──► ClickEventRepository ──► H2 (click_events)
  │
  └── GET /api/urls/{code}/analytics ──► AnalyticsService.getAnalytics()

GlobalExceptionHandler wraps every endpoint → consistent JSON error shape.
```

## Tools used
- **Spring Boot 3.3 / Java 17** — chosen over Node/TypeScript specifically
  because it matches the candidate's actual professional Java/Spring Boot
  background (Q2, OYO), which makes design decisions defensible in a
  follow-up technical discussion.
- **Spring Data JPA + H2** — in-memory relational store; swappable for
  Postgres/MySQL via one config change, no code change (repository
  abstraction).
- **Caffeine (via Spring Cache)** — in-process LRU/TTL cache for the
  redirect hot path.
- **Spring Actuator** — health/metrics endpoints for basic observability.
- **JUnit 5 / Mockito / AssertJ / MockMvc** — unit + integration tests.

## Execution approach
Built task-by-task per the decomposition in the final summary: data model →
short-code generation → create API → redirect + click logging → analytics →
reliability (rate limit + cache) → error handling → tests → docs. Each step
was implemented, then reviewed against the AI-assisted-execution
requirements (traceability, quality gates) before moving to the next.

## Control flow: creating and resolving a short URL
1. Client POSTs a long URL (+ optional TTL).
2. Rate limiter checks the client's token bucket; rejects with `429` if empty.
3. Service checks for an existing mapping for that exact URL (idempotency);
   returns it if found and not expired.
4. Otherwise: persist a placeholder row to get a DB-assigned ID, Base62-encode
   that ID into the short code, persist again with the code set.
5. Client requests `GET /{code}`: service resolves via cache-or-DB, throws
   `404`/`410` as appropriate, controller issues a `302` redirect, and
   click logging is dispatched asynchronously so it never adds latency to
   the redirect response.

## Key design decisions (with rationale)
| Decision | Alternative considered | Why this one |
|---|---|---|
| Base62(auto-increment ID) for short codes | Random string + collision retry | Deterministic, collision-free by construction, no retry loop under concurrency |
| Async click logging | Synchronous write in the redirect path | Keeps the user-facing redirect fast; analytics is a side effect, not a blocker |
| In-memory per-IP token bucket | Global shared limiter / no limiter | Isolates noisy clients; documented as not multi-instance-safe (would move to Redis-backed Bucket4j for production) |
| Idempotent create (dedupe by originalUrl) | Always insert a new row | Prevents unbounded table growth from client retries of the same request |
| Cache the resolve path only, not click count | Cache full entity including click count | Avoids cache invalidation on every single click; click totals are queried fresh from `click_events` |

## Known limitations
- Rate limiter state is per-instance (not shared across horizontally scaled
  instances).
- No authentication/authorization — out of scope per the assignment, but
  would be required before any real deployment.
- Short codes are sequential/enumerable (see `Base62Encoder` javadoc) —
  acceptable here since "unguessable/private link" was never a stated
  requirement.
- Build/tests were written and reviewed but not executed in this sandbox
  (no Maven Central network access); see README for details.
