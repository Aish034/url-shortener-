# URL Shortener — AI-Assisted Engineering Prototype

A Spring Boot (Java 17) URL shortener built for the "AI-Proficient Software
Engineer" assignment. See `ARCHITECTURE.md`, `SCENARIOS.md`,
`AI_EXECUTION_LOG.md`, and `FINAL_SUMMARY.md` for the full write-up.

## Setup

**Requirements:** Java 17+, Maven 3.8+ (or use the included wrapper).

```bash
# from the project root
mvn clean install        # compiles, runs unit + integration tests
mvn spring-boot:run       # starts the app on http://localhost:8080
```

> Note: this was developed and reviewed in a sandboxed environment without
> access to Maven Central, so the build could not be executed end-to-end in
> that environment. The code compiles against standard Spring Boot 3.3.x /
> Java 17 APIs with no unusual dependencies; running `mvn clean install` on a
> machine with normal internet access is expected to succeed. Flagged here
> rather than silently claimed as verified — see `FINAL_SUMMARY.md` limitations.

The H2 console is available at `http://localhost:8080/h2-console`
(JDBC URL: `jdbc:h2:mem:urlshortener`, user `sa`, empty password) for
inspecting data during manual testing.

## API

### Create a short URL
```
POST /api/urls
Content-Type: application/json

{
  "originalUrl": "https://example.com/some/very/long/path",
  "ttlMinutes": 1440   // optional; omit for a link that never expires
}
```
Response `201 Created`:
```json
{
  "shortCode": "3v",
  "shortUrl": "http://localhost:8080/3v",
  "originalUrl": "https://example.com/some/very/long/path",
  "createdAt": "2026-09-21T18:00:00Z",
  "expiresAt": "2026-09-22T18:00:00Z"
}
```

### Redirect
```
GET /{code}
```
Returns `302 Found` with a `Location` header, `404` if unknown, `410 Gone`
if expired.

### Analytics
```
GET /api/urls/{code}/analytics
```
Returns total click count and the 20 most recent click timestamps.

### Rate limiting
`POST /api/urls` is rate-limited per client IP (default: 20 requests/minute,
configurable in `application.yml` under `app.rate-limit`). Exceeding it
returns `429 Too Many Requests`.

## Running tests
```bash
mvn test
```
Covers: short-code generation/round-trip (unit), service-layer logic with
mocked repository (unit), and full create→redirect flow plus error cases
(integration, via `MockMvc` against a real H2-backed Spring context).

## Configuration
See `src/main/resources/application.yml` for base URL, short-code length,
cache TTL, and rate-limit settings.


EXAMPLES :::

Example 1::
aishwarya ~ % curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://example.com"}'
  
Output:
{"shortCode":"1","shortUrl":"http://localhost:8080/1","originalUrl":"https://example.com","createdAt":"2026-09-22T16:47:54.389041Z","expiresAt":null}%    

Example 2::
aishwarya ~ % for i in 1 2 3 4 5 6 7; do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/urls \
    -H "Content-Type: application/json" \
    -d "{\"originalUrl\":\"https://example.com/api?x=$i\"}"
done

Output:
201
201
201
201
201
429
429


