# Three Scenarios: Decomposition, Execution, Validation

## 1. Greenfield — Short-code generation strategy

**Decomposition**
- Define requirement: turn a long URL into a short, unique, URL-safe code.
- Identify the two realistic strategies (random+collision-check vs.
  deterministic encoding of an auto-increment key).
- Pick one, implement, test round-trip correctness.

**AI-assisted execution**
- Prompted for "a short-code generator for a URL shortener." AI's first
  draft returned a random 7-character alphanumeric string generator with a
  `while (repository.existsByShortCode(code)) { regenerate(); }` loop.
- **Rejected** that approach as the primary strategy: it adds an
  unnecessary DB round-trip per collision and collision probability rises
  as the table grows — a real (if small) scaling and complexity cost with
  no offsetting benefit for this use case.
- **Directed AI to regenerate** using Base62 encoding of the entity's
  DB-assigned primary key instead — deterministic, collision-free by
  construction. Accepted that draft, then edited it manually to extract it
  into a standalone `Base62Encoder` utility (originally inlined in the
  service) so it could be unit-tested independently of Spring/JPA.

**Validation**
- Unit tests assert round-trip (`encode` → `decode` → original ID) across
  edge cases (0, small numbers, large numbers near `Long.MAX_VALUE/2`).
- Manual review confirmed no collision path is needed, simplifying the
  service's create-flow logic and removing a class of retry-related bugs.
- Trade-off documented: codes are enumerable/guessable. Accepted since no
  "private link" requirement exists in the assignment; flagged for a real
  product.

## 2. Brownfield — Adding rate limiting to a working endpoint

**Starting point:** `POST /api/urls` already worked end-to-end
(validate → dedupe → generate code → persist) with no reliability control.

**Decomposition**
- Identify the impacted surface: only the controller method needs a new
  check; the service and repository layers are untouched.
- Choose a rate-limiting algorithm and a key (per-client vs. global).
- Wire it in without changing the existing request/response contract or
  breaking the integration tests already written against it.

**AI-assisted execution**
- Asked AI to "add rate limiting to this endpoint." First draft used a
  single shared `AtomicInteger` counter reset every minute for *all*
  clients combined.
- **Rejected**: a global limiter means one high-volume client (or a retry
  storm from a single bad actor) exhausts the quota for every other
  client — the opposite of the isolation a rate limiter should provide.
- **Edited** to a per-key (client IP) token bucket
  (`ConcurrentHashMap<String, Bucket>`), keeping the algorithm (token
  bucket) but changing the scoping. This was a manual rewrite, not another
  AI round — the fix was small enough to make directly once the flaw was
  identified.

**Validation**
- Confirmed the existing `UrlControllerIntegrationTest` still passes
  unmodified (no contract change).
- Manually reasoned through the concurrency behavior: `AtomicLong` per
  bucket avoids lost updates under concurrent requests to the same key.
- Documented as a known limitation: in-memory, per-instance state won't
  coordinate across multiple app instances behind a load balancer — called
  out explicitly rather than silently shipped as if it were
  production-complete.

## 3. Ambiguous — "Add analytics"

**The ambiguity:** the assignment says the service needs "analytics" with
no definition of what to measure — raw hits? Unique visitors? Geographic
breakdown? Referrer sources?

**Decomposition of the ambiguity itself**
- Listed plausible interpretations and what each would require:
  - Raw hit count → no extra infrastructure, just log every redirect.
  - Unique-visitor count → requires a client identity mechanism (cookie,
    auth, or IP-based dedup with its own accuracy problems) that nothing
    else in the assignment calls for.
  - Referrer/source breakdown → cheap to add (just read the `Referer`
    header) and gives useful signal without extra scope.
- **Decision:** implement raw hit count + timestamp + referrer per click.
  Documented explicitly (see `ClickEvent` javadoc) rather than silently
  picking an interpretation and hoping it matches expectations — the
  point of flagging ambiguity is so a reviewer can correct the
  interpretation cheaply if it's wrong, rather than discovering a mismatch
  later.

**AI-assisted execution**
- AI's initial suggestion for "analytics" jumped straight to a full
  dashboard-style aggregation (clicks per day, per country, per browser)
  using data this system has no way to reliably capture (no geo-IP
  lookup, no user-agent parsing was requested).
- **Rejected the scope creep**: implementing fields we can't populate
  accurately would produce analytics that look more sophisticated than
  they are — a correctness/honesty problem, not just a style one.
- Scoped it back down to what the system can actually and honestly report:
  total clicks + recent click timestamps + referrer, matching the decision
  above.

**Validation**
- Integration test confirms `GET /api/urls/{code}/analytics` returns
  `404` for an unknown code (guards against silently returning empty/zero
  stats for a link that doesn't exist, which would be misleading).
- Manual check: click logging is async and best-effort — documented that
  under extreme load a click could theoretically be dropped if the async
  executor's queue is full, which is an acceptable trade-off for
  analytics (non-critical path) but would not be acceptable for, say,
  billing.
