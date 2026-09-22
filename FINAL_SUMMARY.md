# Final Engineering Summary

## Plan and rationale
Built a URL shortener incrementally, task-by-task (see decomposition table
in the top-level plan / `ARCHITECTURE.md`): data model → short-code
generation → create API → redirect/click-logging → analytics →
reliability (rate limiting + caching) → error handling → tests → docs.
Each task used AI to produce a first draft, which was then reviewed,
and either accepted, edited, or rejected against concrete correctness,
security, or scope criteria — never merged unreviewed. Full trace in
`AI_EXECUTION_LOG.md`.

## Artifacts produced
- Working Spring Boot service: create short URL, redirect, analytics,
  rate limiting, caching (see `README.md` for API and run instructions)
- `ARCHITECTURE.md` — components, control flow, design-decision table
- `SCENARIOS.md` — greenfield / brownfield / ambiguous walkthroughs
- `AI_EXECUTION_LOG.md` — per-task traceability of AI-generated vs.
  edited vs. rejected output, with rationale
- Unit tests (`Base62EncoderTest`, `UrlShortenerServiceTest`) and
  integration tests (`UrlControllerIntegrationTest`)

## Risks, trade-offs, and validation
| Area | Risk / trade-off | Mitigation / validation |
|---|---|---|
| Short codes | Sequential/enumerable | Acceptable for stated scope; documented for future revisit |
| Rate limiting | In-memory, not shared across instances | Documented explicitly; would move to Redis-backed Bucket4j for multi-instance deployment |
| Click logging | Async, best-effort (could drop under extreme load) | Acceptable for analytics (non-critical path); would need a durable queue for anything billing-adjacent |
| Analytics scope | Ambiguous requirement | Explicitly scoped down to what can be honestly reported (raw clicks, timestamps, referrer) rather than fabricating fields (geo, browser) with no real data source |
| Data store | H2 in-memory | Swappable for Postgres/MySQL via config only, due to repository abstraction |
| Build verification | Not compiled/run in this sandbox (no Maven Central access) | Flagged directly in README rather than claimed as tested; standard Spring Boot 3.3/Java 17 APIs only, no unusual dependencies |

## Assumptions
- No authentication/authorization required (not stated in the assignment)
- "Analytics" means click-level tracking, not business/revenue reporting
- Single-instance deployment is acceptable for a prototype; multi-instance
  rate-limit coordination is out of scope but documented

## Limitations
- No horizontal-scaling story for rate limiting or cache (both in-process)
- No admin/delete API for short URLs
- No geographic or device-level analytics (deliberately, per the
  ambiguous-requirement decision above)
- Build was reviewed manually rather than machine-verified in this
  environment; recommend running `mvn clean install` locally before
  presenting this as final

## Engineer ownership statement
Every design decision in this repository — the short-code strategy, the
rate-limiter scoping, the analytics interpretation, the error taxonomy —
was made or corrected by the engineer after reviewing AI-generated
first drafts, not accepted by default. AI accelerated the first pass of
each task; correctness, security review, and the final call on trade-offs
rested with the engineer throughout, as required by the assignment's
"Controlled Oversight" principle.
