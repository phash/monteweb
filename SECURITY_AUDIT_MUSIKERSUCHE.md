# Security Audit Report: phash/musikersuche

**Date:** 2026-02-26
**Auditor:** Automated Security Audit (Claude)
**Repository:** https://github.com/phash/musikersuche
**Commit:** `03aa2ab` (HEAD at time of audit)
**Scope:** Full-stack security review of the Musikersuche musician networking platform

---

## Executive Summary

Musikersuche is a microservices-based musician networking platform built with Java 21 / Spring Boot 3.4, Netflix DGS (GraphQL), Vue 3, PostgreSQL 16 + PostGIS, Redis, and RabbitMQ. The application consists of 10 backend services, an API gateway, and a Vue 3 SPA frontend, deployed via Docker Compose with nginx as a reverse proxy.

A prior security audit (2026-02-20) identified and fixed three critical issues (WebSocket auth, internal API keys, header injection). This audit identifies **8 Critical**, **10 High**, **14 Medium**, and **10 Low** severity findings, along with several informational notes.

### Risk Rating Summary

| Severity | Count | Status |
|----------|-------|--------|
| **CRITICAL** | 8 | Requires immediate attention |
| **HIGH** | 10 | Should be addressed promptly |
| **MEDIUM** | 14 | Should be addressed in next sprint |
| **LOW** | 10 | Best-practice improvements |
| **INFO** | 7 | Informational / hardening suggestions |

---

## Table of Contents

1. [Critical Findings](#1-critical-findings)
2. [High Severity Findings](#2-high-severity-findings)
3. [Medium Severity Findings](#3-medium-severity-findings)
4. [Low Severity Findings](#4-low-severity-findings)
5. [Informational Findings](#5-informational-findings)
6. [Previously Remediated Issues](#6-previously-remediated-issues)
7. [Positive Security Controls](#7-positive-security-controls)
8. [Recommendations Summary](#8-recommendations-summary)

---

## 1. Critical Findings

### C-01: JWT Secret Hardcoded as Default Value in API Gateway

**File:** `services/api-gateway/src/main/resources/application.yml:26`
**CVSS:** 9.8 (Critical)
**Category:** CWE-798 (Use of Hard-Coded Credentials)

```yaml
jwt:
  secret: ${JWT_SECRET:your-super-secret-jwt-key-that-should-be-at-least-256-bits-long-for-hs256}
```

**Impact:** If the `JWT_SECRET` environment variable is not set (e.g., in development or a misconfigured production deployment), the application falls back to a publicly known default secret. An attacker can forge arbitrary JWT tokens, impersonating any user including superadmins.

**Note:** The auth-service correctly has `${JWT_SECRET}` with no default (line 66), but the API gateway — which validates every incoming JWT — has this dangerous fallback. The `docker-compose.prod.yml` uses `${JWT_SECRET:?JWT_SECRET is required}` which mitigates this for production Docker deployments, but any standalone or dev deployment is vulnerable.

**Recommendation:**
- Remove the default value: `secret: ${JWT_SECRET}`
- Add startup validation that fails fast if JWT_SECRET is not set
- Rotate the JWT secret if this default was ever used in any environment

---

### C-02: No GraphQL Query Depth or Complexity Limiting

**File:** `services/api-gateway/` (entire GraphQL configuration)
**CVSS:** 7.5 (High)
**Category:** CWE-400 (Uncontrolled Resource Consumption)

The GraphQL schema spans 5,484 lines with deeply nested types (e.g., `MusicianProfile` -> `BandProfile` -> `BandMember` -> `MusicianProfile` creating circular references). No query depth limiting, query complexity analysis, or max query cost configuration was found.

**Impact:** An attacker can craft deeply nested or aliased queries to cause excessive CPU/memory consumption, leading to denial of service:

```graphql
query {
  musicianProfile(id: "...") {
    bands { members { musicianProfile { bands { members { musicianProfile { ... } } } } } }
  }
}
```

**Recommendation:**
- Add Netflix DGS `QueryComplexityInstrumentation` with a max complexity limit
- Add max query depth limiting (recommended: depth 10-15)
- Add query cost analysis for pagination fields
- Consider persisted/allowlisted queries for production

---

### C-03: Internal Services Trust X-User-Id Header Without Validation

**File:** `services/media-service/src/main/java/.../controller/MediaFileController.java:37`
**CVSS:** 8.1 (High)
**Category:** CWE-287 (Improper Authentication)

Backend microservices (media-service, profile-service, chat-service, event-service, search-service, admin-service) accept `X-User-Id` from the API gateway as trusted user identity:

```java
@PostMapping("/upload")
public ResponseEntity<MediaFileDTO> uploadMedia(
    @RequestHeader("X-User-Id") UUID userId, ...)
```

While the API gateway sets this header after JWT validation, the downstream services have **no mechanism to verify** the request actually came from the API gateway and not directly from an attacker. The media-service has no `InternalApiKeyFilter`, and services like admin-service and search-service have wildcard CORS (`allowedOrigins("*")`).

**Impact:** If an attacker can reach any backend service directly (e.g., via network misconfiguration, SSRF, or exposed ports), they can set `X-User-Id` to any UUID and act as any user.

**Recommendation:**
- Apply `InternalApiKeyFilter` to ALL services that accept `X-User-Id` (currently only auth-service has it)
- Verify `X-Internal-Api-Key` on every request that trusts `X-User-Id`
- Ensure no backend services are exposed outside the Docker network
- Consider mTLS for service-to-service communication

---

### C-04: Elasticsearch Security Disabled in Production

**File:** `docker-compose.prod.yml:51`
**CVSS:** 7.5 (High)
**Category:** CWE-306 (Missing Authentication for Critical Function)

```yaml
elasticsearch:
  environment:
    - xpack.security.enabled=false
```

Elasticsearch X-Pack security is explicitly disabled in the **production** compose file. Any service on the Docker network can read/write/delete data in Elasticsearch without authentication.

**Impact:** An attacker with access to the internal network (e.g., via compromised service or container escape) can read, modify, or delete all indexed data. Elasticsearch may contain sensitive user data, search indices, and forum content.

**Recommendation:**
- Enable `xpack.security.enabled=true`
- Configure authentication credentials for Elasticsearch
- Use TLS for Elasticsearch connections
- Limit network access to only services that need it

---

### C-05: Wildcard CORS on Internal Services

**Files:**
- `services/admin-service/src/main/java/.../config/WebConfig.java:13` — `.allowedOrigins("*")`
- `services/search-service/src/main/java/.../config/WebConfig.java:15` — `.allowedOrigins("*")`
- `services/chat-service/src/main/java/.../config/WebSocketConfig.java:48` — `.setAllowedOriginPatterns("*")`

**CVSS:** 7.4 (High)
**Category:** CWE-942 (Overly Permissive Cross-domain Whitelist)

Multiple backend services allow requests from any origin. Combined with C-03 (trusting X-User-Id), this means a malicious website could make cross-origin requests to backend services and impersonate users.

**Recommendation:**
- Restrict CORS origins to only the API gateway's origin or remove CORS entirely on internal services
- Internal services should not be directly accessible from browsers

---

### C-06: Excessive JWT Token Lifetime (24 Hours Default)

**File:** `.env.example:35` and `docker-compose.prod.yml:143`
**CVSS:** 6.5 (Medium-High)
**Category:** CWE-613 (Insufficient Session Expiration)

```
JWT_EXPIRATION_MS=86400000  # 24 hours
```

While the auth-service application.yml has a 15-minute default (`access-token-validity-ms: 900000`), the `JWT_EXPIRATION_MS` environment variable in `.env.example` is set to 86,400,000ms (24 hours) and is passed to auth-service in `docker-compose.prod.yml`. This overrides the secure default.

**Impact:** If a JWT access token is stolen (XSS, leaked logs, compromised device), it remains valid for 24 hours — far too long for an access token.

**Recommendation:**
- Set `JWT_EXPIRATION_MS` to 900000 (15 minutes) in `.env.example`
- Remove the environment variable override in `docker-compose.prod.yml` (let application.yml default apply)
- Ensure the refresh token flow is properly used for session extension

---

### C-07: Hardcoded Admin Password in Flyway Migration

**File:** `services/auth-service/src/main/resources/db/migration/V2__seed_admin_user.sql`
**CVSS:** 9.1 (Critical)
**Category:** CWE-798 (Use of Hard-Coded Credentials)

The database seed migration inserts an admin user (`admin@musikersuche.org`) with a BCrypt hash of the password `admin123`. A comment says "IMPORTANT: Change this password immediately after first login!" but there is **no enforcement mechanism**. This migration runs on every fresh database setup.

**Impact:** Every new deployment has a well-known superadmin password. If the admin password is not changed (which requires manual action), any attacker can log in as the superadmin.

**Recommendation:**
- Generate a random password at deploy time and print it once to logs
- Add a forced password change on first admin login
- Or use an initialization script outside Flyway that generates credentials dynamically

---

### C-08: Invoice Query IDOR — Unauthenticated Access to Payment Data

**File:** `services/api-gateway/src/main/java/.../datafetcher/PaymentDataFetcher.java:91`
**CVSS:** 8.6 (High)
**Category:** CWE-639 (Authorization Bypass Through User-Controlled Key)

The GraphQL query `invoice(id: UUID!)` fetches any invoice by its UUID **without any authentication or authorization check**. There is no `@PreAuthorize` annotation and no ownership verification.

**Impact:** Any unauthenticated user can query any invoice by UUID, leaking payment details, user IDs, amounts, and subscription information. While UUIDs are not easily guessable, they may be leaked through other endpoints or logs.

**Recommendation:**
- Add authentication requirement to the `invoice` query
- Add ownership check: only the invoice owner or an admin should be able to view it
- Apply the same authorization pattern used in other payment mutations

---

## 2. High Severity Findings

### H-01: XSS via Unsanitized Markdown Rendering (v-html)

**File:** `frontend/src/views/blog/BlogPostView.vue:121`
**CVSS:** 6.1 (Medium)
**Category:** CWE-79 (Cross-site Scripting)

```vue
<div v-html="htmlContent"></div>
```

```typescript
const htmlContent = computed(() => {
  if (!post.value) return ''
  return marked(post.value.content)  // No sanitization!
})
```

Blog post content is rendered using `marked()` (Markdown parser) and injected via `v-html` without any HTML sanitization (no DOMPurify). While blog content appears to come from static files rather than user input, `marked()` passes through raw HTML by default.

**Impact:** If an attacker can control or inject blog content (e.g., through a future admin CMS or content injection), they can execute arbitrary JavaScript in users' browsers.

**Recommendation:**
- Install and use DOMPurify: `npm install dompurify @types/dompurify`
- Sanitize before rendering: `DOMPurify.sanitize(marked(post.value.content))`
- Configure `marked` with `{ sanitize: false, breaks: true }` and rely on DOMPurify

---

### H-02: Token Storage in localStorage (XSS Exfiltration Risk)

**File:** `frontend/src/services/tokenStorage.ts`
**CVSS:** 6.1 (Medium)
**Category:** CWE-922 (Insecure Storage of Sensitive Information)

JWT tokens are stored in `localStorage` with only base64 encoding as "obfuscation":

```typescript
function encode(value: string): string {
  const reversed = value.split('').reverse().join('')
  return btoa(reversed)  // Trivially reversible
}
```

**Impact:** Any XSS vulnerability (including H-01) can steal both access and refresh tokens. The base64+reverse encoding provides zero security — it is trivially reversible. Unlike `httpOnly` cookies, `localStorage` is accessible to any JavaScript running in the page context.

**Recommendation:**
- Store tokens in `httpOnly`, `Secure`, `SameSite=Strict` cookies instead of localStorage
- If localStorage must be used, implement token binding or use short-lived tokens with background refresh
- Remove the false sense of security from the encoding — document that it is only for CodeQL compliance

---

### H-03: Rate Limiting Defaults Are Too Permissive

**File:** `services/auth-service/src/main/resources/application.yml:111-119`
**CVSS:** 5.9 (Medium)
**Category:** CWE-307 (Improper Restriction of Excessive Authentication Attempts)

```yaml
rate-limit:
  login:
    max-attempts: ${RATE_LIMIT_LOGIN_MAX:20}    # 20 attempts per minute!
    window-seconds: ${RATE_LIMIT_LOGIN_WINDOW:60}
  register:
    max-attempts: ${RATE_LIMIT_REGISTER_MAX:20}  # 20 registrations per 5 min!
    window-seconds: ${RATE_LIMIT_REGISTER_WINDOW:300}
  password-reset:
    max-attempts: ${RATE_LIMIT_PASSWORD_RESET_MAX:10}
    window-seconds: ${RATE_LIMIT_PASSWORD_RESET_WINDOW:300}
```

The `.env.example` has stricter values (5 login/5min, 3 register/hour), but the `application.yml` defaults are extremely permissive — 20 login attempts per minute allows rapid brute-force attacks.

**Impact:** An attacker can attempt 20 passwords per minute per IP, making brute-force attacks practical for weak passwords.

**Recommendation:**
- Reduce defaults to match `.env.example`: 5 login/5min, 3 register/hour, 3 reset/hour
- Add account-level rate limiting (not just IP-based)
- Implement exponential backoff after consecutive failures
- Consider CAPTCHA after 3 failed attempts

---

### H-04: GraphQL Introspection and GraphiQL Enabled by Default

**File:** `services/api-gateway/src/main/resources/application.yml:17-22`
**CVSS:** 5.3 (Medium)
**Category:** CWE-200 (Information Exposure)

```yaml
dgs:
  graphql:
    graphiql:
      enabled: ${GRAPHIQL_ENABLED:true}   # Default: enabled!
graphql:
  introspection:
    enabled: ${GRAPHQL_INTROSPECTION_ENABLED:true}  # Default: enabled!
```

Both GraphiQL (interactive query explorer) and introspection are enabled by default. While `.env.example` sets them to `false`, the application defaults expose the entire schema.

**Impact:** Attackers can enumerate all queries, mutations, types, and fields to map the entire API attack surface. GraphiQL provides a convenient testing interface.

**Recommendation:**
- Change defaults to `false`: `${GRAPHIQL_ENABLED:false}` and `${GRAPHQL_INTROSPECTION_ENABLED:false}`
- Use the production profile to ensure these are disabled
- Add startup logging when introspection is enabled as a warning

---

### H-05: Media Service Has No Authentication

**File:** `services/media-service/src/main/java/.../controller/MediaFileController.java`
**CVSS:** 6.5 (Medium-High)
**Category:** CWE-306 (Missing Authentication for Critical Function)

The media-service has no Spring Security configuration, no JWT validation, and no `InternalApiKeyFilter`. It trusts `X-User-Id` headers blindly and has endpoints like `/api/media/{id}` and `/api/media/files/{id}/thumbnail` that are completely open (no `X-User-Id` required).

**Impact:** Any user can access any media file metadata. Public files are served without authentication. Private files check `X-User-Id` but this header can be spoofed by any caller.

**Recommendation:**
- Add `InternalApiKeyFilter` to media-service
- Implement proper authentication for direct access endpoints
- Use signed URLs for file access instead of open endpoints

---

### H-06: Account Enumeration via Registration Endpoint

**File:** `services/auth-service/src/main/java/.../service/AuthService.java:87-88`
**CVSS:** 5.3 (Medium)
**Category:** CWE-204 (Observable Response Discrepancy)

```java
public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
        throw AuthException.conflict("Email already registered");
    }
    ...
}
```

The registration endpoint throws a distinct error ("Email already registered") when an email exists, while the password reset correctly avoids this (line 334: "Always return success to prevent email enumeration").

**Impact:** Attackers can enumerate valid email addresses by attempting to register with known emails and observing the error response.

**Recommendation:**
- Return a generic response for registration (e.g., "Check your email to continue")
- Send different emails based on whether the account exists (verification vs "you already have an account")
- Apply the same pattern already used in password reset

---

### H-07: Missing CSRF Protection (CSRF Disabled Globally)

**Files:**
- `services/api-gateway/src/main/java/.../config/SecurityConfig.java:36`
- `services/auth-service/src/main/java/.../config/SecurityConfig.java:43`

```java
.csrf(AbstractHttpConfigurer::disable)
```

CSRF protection is disabled across all services. While this is common for stateless JWT APIs, the application uses `allowCredentials(true)` in CORS and tokens in localStorage, meaning a CSRF-like attack via cross-origin form POST to the GraphQL endpoint could potentially be exploited if combined with other vulnerabilities.

**Recommendation:**
- For JWT-only: acceptable IF origins are properly restricted (currently not — see C-05)
- Add CSRF token for any cookie-based sessions
- Ensure CORS origins are strictly configured on the API gateway
- Consider double-submit cookie pattern for GraphQL mutations

---

### H-08: No Password Complexity Validation on Backend

**File:** `services/auth-service/src/main/java/.../service/AuthService.java:99`
**Category:** CWE-521 (Weak Password Requirements)

```java
.passwordHash(passwordEncoder.encode(request.getPassword()))
```

The auth service hashes passwords with bcrypt (good) but there is no server-side validation of password complexity. Password rules are likely only enforced on the frontend, which can be bypassed.

**Recommendation:**
- Add server-side password validation: minimum length (8+), complexity requirements
- Use Bean Validation annotations on the `RegisterRequest` DTO
- Reject commonly used passwords (HIBP / top-10000 list)

---

### H-09: Redis Without Password in Development Config

**File:** `services/auth-service/src/main/resources/application.yml:35`
**Category:** CWE-798 (Use of Hard-Coded Credentials)

```yaml
redis:
  password: ${REDIS_PASSWORD:}  # Empty default
```

Multiple services default to an empty Redis password. While production compose enforces `${REDIS_PASSWORD:?}`, development/standalone deployments run with an unauthenticated Redis.

**Impact:** Redis stores rate-limiting counters, session data, and cache. An attacker accessing Redis can bypass rate limits, inject cache data, or read sensitive information.

**Recommendation:**
- Require `REDIS_PASSWORD` in all environments
- Use Redis ACLs in production

---

### H-10: Stack Trace Leakage in Error Responses

**Files:**
- `services/media-service/src/main/java/.../exception/GlobalExceptionHandler.java:56`
- `services/chat-service/src/main/java/.../exception/GlobalExceptionHandler.java:54`

```java
.message("Internal server error: " + ex.getMessage())
```

These services include raw exception messages in API error responses. This can leak SQL error details, file paths, class names, and internal architecture information. Other services correctly return generic messages ("An unexpected error occurred").

**Recommendation:** Replace `ex.getMessage()` with a generic error message in all `GlobalExceptionHandler` catch-all methods.

---

## 3. Medium Severity Findings

### M-01: Hardcoded Database Credentials in application.yml

**Files:**
- `services/auth-service/src/main/resources/application.yml:14-15`
- `services/media-service/src/main/resources/application.yml:19-20`

```yaml
datasource:
  url: jdbc:postgresql://localhost:5432/musikersuche_auth
  username: musikersuche
  password: musikersuche_dev
```

All services have hardcoded development database credentials. These are default values but are committed to the repository.

**Recommendation:**
- Use environment variable placeholders with no defaults for credentials
- Add Spring Cloud Vault or similar secrets management

---

### M-02: Missing Issuer and Audience Claims in JWT

**File:** `services/auth-service/src/main/java/.../security/JwtTokenProvider.java:42-53`

The JWT tokens lack `iss` (issuer) and `aud` (audience) claims, and the validators don't check these:

```java
return Jwts.builder()
    .subject(user.getId().toString())
    .claim("email", user.getEmail())
    // No .issuer() or .audience()
    .signWith(secretKey)
    .compact();
```

**Impact:** If a JWT from another service using the same secret (e.g., in a development environment) is presented, it will be accepted. Missing issuer/audience makes token confusion attacks easier.

**Recommendation:**
- Add `.issuer("musikersuche-auth")` and `.audience().add("musikersuche-api")`
- Validate these claims in the API gateway's JWT filter

---

### M-03: SECURITY.md is a Template (Not Customized)

**File:** `SECURITY.md`

The security policy file is the default GitHub template with placeholder version numbers (5.1.x, 5.0.x, 4.0.x) that don't match the actual project. No real vulnerability reporting process is defined.

**Recommendation:** Replace with actual supported versions and a real vulnerability reporting process (security@musikersuche.org or GitHub Security Advisories).

---

### M-04: Docker Mailserver Using `latest` Tag

**File:** `docker-compose.prod.yml:562`

```yaml
mailserver:
  image: ghcr.io/docker-mailserver/docker-mailserver:latest
```

Using `:latest` in production can introduce unexpected breaking changes or vulnerabilities.

**Recommendation:** Pin to a specific version tag.

---

### M-05: WebSocket Allows All Origins

**File:** `services/chat-service/src/main/java/.../config/WebSocketConfig.java:48`

```java
registry.addEndpoint("/ws/chat").setAllowedOriginPatterns("*");
```

**Recommendation:** Restrict to the production domain only.

---

### M-06: Frontend Dockerfile Copies Entire Context

**File:** `frontend/Dockerfile:26`

```dockerfile
COPY . .
```

This copies the entire frontend directory including potential `.env` files, test data, and `node_modules`.

**Recommendation:** Add a `.dockerignore` to exclude sensitive files, or use a more specific COPY pattern.

---

### M-07: Monitoring Ports Not Explicitly Blocked

**File:** `docker-compose.prod.yml`

While monitoring ports (Prometheus 9090, Grafana 3000, Alertmanager 9093) are commented out, the api-gateway port 8080 is exposed, and all services are on the same flat Docker network. Internal services can communicate freely without segmentation.

**Recommendation:**
- Create separate networks (frontend, backend, monitoring)
- Only expose api-gateway to the nginx network
- Block direct access to backend service ports

---

### M-08: No Request Size Limiting on GraphQL

**File:** `services/api-gateway/`

No maximum query size or request body size is configured for the GraphQL endpoint, allowing potentially massive queries.

**Recommendation:** Add `spring.graphql.max-request-size` or nginx `client_max_body_size` specific to `/graphql`.

---

### M-09: OAuth Token Exchange Not Fully Validated

**File:** `services/auth-service/src/main/java/.../service/OAuth2Service.java`

The OAuth flow accepts tokens from Google/Facebook but the CSRF state parameter is managed client-side. Server-side state validation should be implemented.

**Recommendation:** Implement server-side CSRF state token for OAuth flows (store in Redis, validate on callback).

---

### M-10: Refresh Token Not Bound to Client

**File:** `services/auth-service/src/main/java/.../service/AuthService.java:694`

Refresh tokens are opaque UUIDs with no binding to the client (no IP, no device fingerprint). A stolen refresh token can be used from any device.

**Recommendation:** Consider binding refresh tokens to client fingerprint or requiring re-authentication on device change.

---

### M-11: `api-gateway:8080` Exposed on Host in Production

**File:** `docker-compose.prod.yml:96-97`

```yaml
api-gateway:
  ports:
    - "8080:8080"
```

The API gateway is directly exposed on port 8080, bypassing nginx's security headers, rate limiting, and SSL termination.

**Recommendation:** Remove the port mapping; let nginx proxy exclusively to the API gateway via the internal Docker network.

---

### M-12: Frontend Port 3000 Also Exposed

**File:** `docker-compose.prod.yml:624-625`

```yaml
frontend:
  ports:
    - "3000:80"
```

Similar to M-11, the frontend is directly accessible on port 3000 without nginx's security headers.

**Recommendation:** Remove the port mapping.

---

### M-13: PayPal Webhook Signature Bypass in Sandbox Mode

**File:** `services/payment-service/src/main/java/.../service/WebhookService.java:53-61`

When `payPalConfig.isSandbox()` is true and signature verification fails, the webhook is still processed with only a warning log. If the service is accidentally deployed in sandbox mode in production, webhook forgery becomes possible.

**Recommendation:** Enforce webhook signature verification regardless of sandbox mode, or add a startup check that sandbox mode is not enabled in production profiles.

---

### M-14: No Content Length Validation on Chat Messages

**File:** `services/chat-service/src/main/java/.../dto/SendMessageRequest.java:28`

The `content` field has no `@Size(max=...)` constraint. A user could send arbitrarily large messages, causing memory/database issues.

**Recommendation:** Add `@Size(max = 10000)` (or appropriate limit) to the content field.

---

## 4. Low Severity Findings

### L-01: Missing `HttpOnly` Flag Documentation

Token storage uses localStorage instead of httpOnly cookies. Document the security trade-off and rationale.

### L-02: Verbose Error Logging May Leak Sensitive Data

**File:** `services/auth-service/src/main/java/.../security/JwtTokenProvider.java:92-100`

JWT validation errors are logged at `error` level. Ensure token values are never logged.

### L-03: No Security Headers on Direct Service Access

Backend services don't set security headers (X-Frame-Options, X-Content-Type-Options, etc.). While nginx adds these, direct service access bypasses them (see M-11).

### L-04: `server_tokens off` Configured (Good) but Missing `X-Powered-By` Suppression

Spring Boot sets `X-Powered-By` headers by default. These should be removed.

### L-05: CI Pipeline Uses Non-Pinned Action Versions

**File:** `.github/workflows/ci.yml`

```yaml
- uses: actions/checkout@v4        # Not SHA-pinned
- uses: dorny/paths-filter@v3      # Not SHA-pinned
```

**Recommendation:** Pin to full SHA hashes for supply-chain security.

### L-06: `continue-on-error: true` on Lint and Type Check

**File:** `.github/workflows/ci.yml:165,169`

Lint and typecheck errors are silently ignored in CI.

### L-07: Debug Logging Enabled by Default in API Gateway

**File:** `services/api-gateway/src/main/resources/application.yml:80-82`

```yaml
logging:
  level:
    com.musikersuche: DEBUG
```

Debug logging should not be the default in a production-facing service.

### L-08: No Content-Security-Policy for API Responses

The nginx CSP header only applies to the frontend. API responses don't have CSP headers, which matters for error pages.

---

### L-09: Internal API Key Comparison Uses `String.equals()` (Timing Attack)

**File:** `services/auth-service/src/main/java/.../security/InternalApiKeyFilter.java:36`

`expectedApiKey.equals(apiKey)` is not constant-time. An attacker with network access to internal services could theoretically determine the API key character by character via timing side-channels.

**Recommendation:** Use `MessageDigest.isEqual()` or Spring's `SecurityUtils.constantTimeEquals()`.

---

### L-10: Android `allowBackup=true` and No Certificate Pinning

**Files:**
- `musikersuche-android/app/src/main/AndroidManifest.xml:30` — `android:allowBackup="true"`
- `musikersuche-android/app/src/main/res/xml/network_security_config.xml` — No `<pin-set>` elements

Application data backup is enabled on Android, and no certificate pinning is configured. The Android app does correctly use `EncryptedSharedPreferences` for token storage and has proper debug/release URL separation.

---

## 5. Informational Findings

### I-01: Good — BCrypt Password Hashing
BCrypt is used for password hashing via Spring Security's `BCryptPasswordEncoder`. This is industry standard.

### I-02: Good — Non-Root Docker Containers
Backend Dockerfiles create and use non-root users (`appuser:appgroup`). Good practice.

### I-03: Good — Multi-Stage Docker Builds
All Dockerfiles use multi-stage builds to minimize image attack surface.

### I-04: Good — Health Checks on All Services
Docker health checks are configured for all services.

### I-05: Good — Prometheus Metrics Restricted
Actuator endpoints are limited to `health,info,metrics,prometheus` with `show-details: when-authorized`.

### I-06: Good — Email Enumeration Prevention in Password Reset
The password reset endpoint always returns success regardless of whether the email exists.

### I-07: Good — TLS Configuration
nginx uses TLS 1.2/1.3 only with strong cipher suites. HSTS is enabled with preload.

---

## 6. Previously Remediated Issues

The following issues were identified in the prior audit (2026-02-20) and have been properly fixed:

| ID | Issue | Status |
|----|-------|--------|
| **C-02 (prev)** | WebSocket accepted `X-User-Id` header instead of JWT | **FIXED** — STOMP JWT auth via `ChannelInterceptor` |
| **C-03 (prev)** | No authentication on internal API endpoints | **FIXED** — `InternalApiKeyFilter` on auth-service |
| **C-04 (prev)** | CRLF injection in Content-Disposition filename | **FIXED** — `sanitizeFilename()` + `ContentDisposition` builder |

---

## 7. Positive Security Controls

The application implements several good security practices:

1. **JWT-based stateless authentication** with separate access/refresh tokens
2. **Rate limiting** on auth endpoints (login, register, password reset) using Redis
3. **Global API rate limiting** with tier-based limits (anonymous/authenticated/premium)
4. **BCrypt password hashing** with Spring Security
5. **CORS configuration** on the API gateway (though needs tightening on internal services)
6. **nginx security headers**: HSTS, X-Frame-Options, X-Content-Type-Options, X-XSS-Protection, Referrer-Policy, Permissions-Policy, CSP
7. **TLS 1.2/1.3** only with strong cipher suites
8. **Non-root Docker containers** with multi-stage builds
9. **Health checks** on all services
10. **File upload validation** using Apache Tika for MIME type detection
11. **Filename sanitization** to prevent CRLF injection and path traversal
12. **Email verification** for new accounts
13. **Token revocation** on password reset and account deactivation
14. **GDPR data export** endpoint for user data
15. **Soft delete** for account deletion (GDPR compliance)
16. **Concurrency groups** in CI to prevent parallel runs
17. **Dependency version overrides** for known CVEs (logback, protobuf, commons-compress, guava, grpc)

---

## 8. Recommendations Summary

### Immediate Actions (Critical)

1. **Remove JWT secret default** from api-gateway `application.yml` → fail-fast if not set
2. **Add GraphQL query depth/complexity limiting** (max depth 15, max complexity 500)
3. **Add InternalApiKeyFilter to ALL backend services** that accept X-User-Id
4. **Enable Elasticsearch security** (`xpack.security.enabled=true`)
5. **Fix wildcard CORS** on admin-service, search-service, chat-service
6. **Fix JWT expiration** — use 15-minute default, remove 24h override

### Short-Term (High)

7. Add DOMPurify for all `v-html` content
8. Migrate token storage to httpOnly cookies (or accept and document the risk)
9. Tighten rate limiting defaults (5 login/5min, 3 register/hr)
10. Disable introspection and GraphiQL by default
11. Add authentication to media-service
12. Fix account enumeration in registration
13. Add server-side password complexity validation
14. Require Redis password in all environments

### Medium-Term

15. Remove hardcoded database credentials from application.yml
16. Add JWT issuer/audience claims
17. Update SECURITY.md with real vulnerability reporting process
18. Pin Docker image versions (especially mailserver)
19. Implement network segmentation (separate Docker networks)
20. Remove exposed ports (8080, 3000) in production compose
21. Add request size limits for GraphQL
22. Implement OAuth state parameter validation server-side

### Long-Term

23. Consider mTLS for service-to-service communication
24. Implement token binding for refresh tokens
25. Pin GitHub Actions to SHA hashes
26. Add SAST/DAST scanning to CI pipeline
27. Implement API request signing for internal communication
28. Add WAF rules for common attack patterns

---

## Appendix: Files Reviewed

### Backend Services (549 Java files)
- `services/api-gateway/` — GraphQL API gateway, JWT auth filter, rate limiting, security config
- `services/auth-service/` — Registration, login, OAuth, JWT provider, rate limiting, password reset
- `services/chat-service/` — WebSocket config, JWT auth, STOMP interceptor
- `services/media-service/` — File upload, storage, thumbnail, download controller
- `services/profile-service/` — Musician/band profiles, search
- `services/search-service/` — Favorites, matching, notifications
- `services/event-service/` — Gigs, events
- `services/payment-service/` — PayPal integration
- `services/admin-service/` — Admin functions, audit log
- `services/area-service/` — Communities, forum
- `shared/` — DTOs, exceptions, JWT claims

### Frontend (177 Vue/TS files)
- `frontend/src/services/apollo.ts` — Apollo Client, JWT refresh, WebSocket auth
- `frontend/src/services/tokenStorage.ts` — Token persistence (localStorage)
- `frontend/src/store/auth.ts` — Auth state management
- `frontend/src/views/blog/BlogPostView.vue` — v-html usage

### Infrastructure
- `docker-compose.yml` — Development compose
- `docker-compose.prod.yml` — Production compose (740 lines)
- `nginx/nginx.conf` — Reverse proxy, SSL, security headers
- `services/*/Dockerfile` — All 10 service Dockerfiles + frontend
- `.github/workflows/ci.yml` — CI pipeline
- `deploy.sh` — Deployment script
- `.env.example` — Environment template
- `pom.xml` — Parent POM, dependency management

### Configuration
- `services/*/src/main/resources/application.yml` — All service configs
- `services/*/src/main/resources/application-production.yml` — Production overrides
- `monitoring/` — Prometheus, Grafana, Alertmanager configs

---

*This audit was performed as a static code review. Dynamic testing (penetration testing) is recommended to validate findings and discover runtime-specific vulnerabilities.*
