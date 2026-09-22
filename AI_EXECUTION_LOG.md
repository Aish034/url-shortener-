# AI-Assisted Execution Log (Traceability)

Format per entry: **Task → Prompted for → Generated → Accepted / Edited / Rejected → Rationale**

---

**Task 1 — Project scaffold**
Prompted for: standard Spring Boot Maven project with Web, JPA, H2,
Validation, Cache, Actuator, Caffeine, test starters.
Generated: `pom.xml` with those dependencies.
Accepted as-is. Rationale: boilerplate with no judgment calls to make.

**Task 2 — Data model**
Prompted for: a `ShortUrl` entity with id, code, original URL, timestamps.
Generated: minimal entity without `expiresAt` or `clickCount`.
Edited: added `expiresAt` (TTL support) and `clickCount` fields, plus
`isExpired()` — required by the assignment's "reliability" and
"analytics" requirements that the first pass didn't anticipate.

**Task 3 — Short-code generation**
See `SCENARIOS.md` §1 (Greenfield) for full detail.
Generated: random-string + collision-retry approach.
**Rejected** as primary strategy; redirected to Base62(auto-increment ID).
Edited: extracted into standalone, independently-testable `Base62Encoder`.

**Task 4 — Create-URL API**
Prompted for: POST endpoint that validates and stores a URL.
Generated: controller + service without idempotency handling.
Edited: added `findByOriginalUrl` dedupe check — first draft would have
created duplicate rows for repeated identical requests (e.g. client
retries after a timeout), which is a real correctness gap, not a style
preference.

**Task 5 — Redirect + click logging**
Prompted for: GET endpoint that resolves a code and redirects.
Generated: synchronous click-count increment inside the redirect method.
Edited: moved click logging to a separate `@Async` service method writing
to a `ClickEvent` table instead of incrementing a counter in the hot path.
Rationale: keeps the redirect fast and gives us a queryable click history
for analytics, not just a running total.

**Task 6 — Analytics API**
See `SCENARIOS.md` §3 (Ambiguous) for the interpretation decision.
Generated: over-scoped dashboard-style aggregation (geo, browser,
daily breakdown).
**Rejected**: fields we cannot populate accurately. Rescoped to total
clicks + recent timestamps + referrer.

**Task 7 — Rate limiting**
See `SCENARIOS.md` §2 (Brownfield) for full detail.
Generated: single global shared counter.
**Rejected**: allows one client to starve all others.
Edited: per-client-IP token bucket via `ConcurrentHashMap`.

**Task 8 — Error handling**
Prompted for: consistent error responses across the API.
Generated: `@RestControllerAdvice` with handlers for not-found and
validation errors.
Edited: added handlers for `ShortUrlExpiredException` (→ 410) and
`RateLimitExceededException` (→ 429), which the first draft didn't cover
because those exception types didn't exist yet at prompt time — added
after Tasks 3 and 7 introduced them, to keep error handling complete.

**Task 9 — Tests**
Prompted for: unit tests for the service and integration tests for the
controller.
Generated: reasonable first-draft coverage of the happy path.
Edited: added explicit tests for the expired-link case, the duplicate-URL
idempotency case, and the 404 cases for both redirect and analytics —
these weren't in the AI's first draft and are exactly the kind of edge
case a reviewer would ask about, so they were added manually as a quality
gate before considering the test suite done.

**Task 10 — Documentation**
Generated collaboratively with AI, then manually reviewed line-by-line
against the actual code to remove any claims not backed by what was
implemented (e.g. an early draft of `ARCHITECTURE.md` claimed
"horizontally scalable rate limiting," which was corrected to explicitly
flag the per-instance limitation instead, once traced back to the actual
`ConcurrentHashMap`-based implementation).

---

## Quality gates applied before treating any task as done
1. **Compiles** — code reviewed for correct types/imports (not
   machine-verified in this sandbox; see README).
2. **Tests** — every service/controller change has a corresponding test
   asserting both the happy path and at least one failure/edge case.
3. **Security review** — input validation on the URL field (`@Pattern`
   requiring `http(s)://`), no raw SQL (JPA/parameterized queries only),
   no secrets in config.
4. **Human sign-off** — every AI-generated draft listed above was read in
   full and either accepted, edited, or rejected with a stated reason
   before being kept in the codebase. No AI output was merged unreviewed.
