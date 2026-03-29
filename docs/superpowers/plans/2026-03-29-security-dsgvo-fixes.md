# Security, DSGVO & Code Quality Fixes

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all CRITICAL/HIGH security vulnerabilities, DSGVO compliance gaps, and key code quality issues identified in the comprehensive review.

**Architecture:** Targeted fixes across frontend (npm deps, nginx, iframes, SW) and backend (injection fixes, DeletionListeners, data export, TOTP encryption). All fixes are isolated to specific files with no cross-dependencies between tasks.

**Tech Stack:** Java 21 + Spring Boot 3.4, Vue 3 + TypeScript, nginx, Workbox SW

---

### Task 1: Frontend Dependency Security Fixes

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`

- [ ] **Step 1: Update DOMPurify and fix npm audit vulnerabilities**

```bash
cd frontend && npm update dompurify && npm audit fix
```

Expected: DOMPurify updated to >=3.3.2, undici/picomatch/brace-expansion/yaml vulnerabilities resolved.

- [ ] **Step 2: Verify no remaining high/critical vulnerabilities**

```bash
cd frontend && npm audit
```

Expected: 0 high/critical vulnerabilities remaining (moderate from serialize-javascript via vite-plugin-pwa is acceptable).

- [ ] **Step 3: Run frontend tests to verify no regressions**

```bash
cd frontend && npm test
```

Expected: All ~1835 tests pass.

---

### Task 2: nginx CSP & Security Headers

**Files:**
- Modify: `frontend/nginx.conf`

- [ ] **Step 1: Fix CSP to add frame-src, worker-src, media-src**

In `nginx.conf`, update the CSP header to:
```nginx
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; font-src 'self'; connect-src 'self' wss: ws:; frame-src 'self' https://www.youtube-nocookie.com https://player.vimeo.com; worker-src 'self'; media-src 'self' blob:; frame-ancestors 'self'; base-uri 'self'; form-action 'self';" always;
```

- [ ] **Step 2: Fix static file location block to inherit security headers**

Replace the static file caching block with:
```nginx
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    access_log off;
}
```

---

### Task 3: iframe Sandbox Attributes

**Files:**
- Modify: `frontend/src/components/common/VideoEmbed.vue`
- Modify: `frontend/src/views/WopiEditorView.vue`

- [ ] **Step 1: Add sandbox to VideoEmbed.vue**

In `VideoEmbed.vue`, add sandbox attribute to the iframe:
```html
<iframe
  :src="embedUrl"
  frameborder="0"
  sandbox="allow-scripts allow-same-origin allow-presentation"
  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
  allowfullscreen
  loading="lazy"
  :title="t('feed.videoEmbed')"
/>
```

- [ ] **Step 2: Add sandbox to WopiEditorView.vue**

In `WopiEditorView.vue`, add sandbox attribute to the iframe:
```html
<iframe
  v-else
  :src="iframeSrc"
  class="wopi-iframe"
  frameborder="0"
  sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-popups-to-escape-sandbox"
  allowfullscreen
  :title="t('wopi.editorTitle')"
/>
```

---

### Task 4: Service Worker Security

**Files:**
- Modify: `frontend/src/sw.ts`

- [ ] **Step 1: Remove opaque response caching and exclude auth endpoints**

Change all three `CacheableResponsePlugin({ statuses: [0, 200] })` to `CacheableResponsePlugin({ statuses: [200] })`.

Add auth endpoint exclusion to the broad API cache route. Change:
```typescript
({ url }) => /\/api\/v1\//.test(url.pathname),
```
to:
```typescript
({ url }) => /\/api\/v1\//.test(url.pathname) && !/\/api\/v1\/auth\//.test(url.pathname),
```

- [ ] **Step 2: Validate push notification URLs**

In the `notificationclick` handler, validate the target URL:
```typescript
self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const targetUrl = event.notification.data?.url || '/'
  // Only allow same-origin navigation
  const safeUrl = targetUrl.startsWith('/') && !targetUrl.startsWith('//') ? targetUrl : '/'
  // ...use safeUrl instead of targetUrl
})
```

---

### Task 5: Solr Filter Query Injection Fix

**Files:**
- Modify: `backend/src/main/java/com/monteweb/search/internal/service/SolrSearchService.java`

- [ ] **Step 1: Add type whitelist validation**

Add a constant set of valid types and validate before query:
```java
private static final Set<String> VALID_DOC_TYPES = Set.of(
    "USER", "ROOM", "POST", "EVENT", "FILE", "WIKI", "TASK"
);
```

In the search method, before `solrQuery.addFilterQuery`, add:
```java
if (type != null && !type.equalsIgnoreCase("ALL")) {
    String upperType = type.toUpperCase();
    if (!VALID_DOC_TYPES.contains(upperType)) {
        return List.of();
    }
    solrQuery.addFilterQuery("doc_type:" + upperType);
}
```

---

### Task 6: JSON Injection Fixes in Filters

**Files:**
- Modify: `backend/src/main/java/com/monteweb/auth/internal/config/MaintenanceModeFilter.java`
- Modify: `backend/src/main/java/com/monteweb/auth/internal/config/TermsAcceptanceFilter.java`

- [ ] **Step 1: Fix MaintenanceModeFilter to use Jackson**

Add `ObjectMapper` field and inject via constructor. Replace the manual JSON construction:
```java
ObjectMapper mapper = new ObjectMapper();
Map<String, Object> body = Map.of("maintenance", true, "message", message);
String json = mapper.writeValueAsString(body);
```

- [ ] **Step 2: Fix TermsAcceptanceFilter to use Jackson**

Same pattern:
```java
ObjectMapper mapper = new ObjectMapper();
Map<String, Object> body = Map.of("termsRequired", true, "termsVersion", termsVersion);
String json = mapper.writeValueAsString(body);
```

---

### Task 7: TOTP Replay Detection Fix

**Files:**
- Modify: `backend/src/main/java/com/monteweb/auth/internal/service/TotpService.java`

- [ ] **Step 1: Replace hashCode() with userId in Redis key**

Change the `verifyCode` method signature to accept `userId` parameter, and change the Redis key from:
```java
String redisKey = TOTP_USED_PREFIX + Integer.toHexString(secret.hashCode()) + ":" + timeStep;
```
to:
```java
String redisKey = TOTP_USED_PREFIX + userId + ":" + timeStep;
```

Update all callers of `verifyCode` to pass the userId.

---

### Task 8: WOPI JWT Secret Validation

**Files:**
- Modify: `backend/src/main/java/com/monteweb/files/internal/controller/WopiController.java`

- [ ] **Step 1: Reject PutFile when JWT secret is not configured**

Replace the conditional JWT skip with a rejection:
```java
if (onlyofficeJwtSecret == null || onlyofficeJwtSecret.isBlank()
        || onlyofficeJwtSecret.equals("changeme-onlyoffice-jwt-secret")) {
    log.warn("WOPI PutFile rejected: OnlyOffice JWT secret not configured");
    return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
}
```

---

### Task 9: CSV Import Force-Password-Change

**Files:**
- Modify: `backend/src/main/java/com/monteweb/admin/internal/service/CsvImportService.java`
- Modify: `backend/src/main/java/com/monteweb/user/internal/model/User.java` (if `forcePasswordChange` field doesn't exist)

- [ ] **Step 1: Add forcePasswordChange flag to imported users**

If User entity already has a `forcePasswordChange` field, set it to `true` for all CSV-imported users. If not, add the field:
```java
@Column(name = "force_password_change")
private boolean forcePasswordChange = false;
```

Create Flyway migration `V114__add_force_password_change.sql`:
```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS force_password_change BOOLEAN DEFAULT false;
```

Set `user.setForcePasswordChange(true)` in CsvImportService after creating the user.

- [ ] **Step 2: Generate random passwords instead of hardcoded**

Replace `DEFAULT_PASSWORD = "changeme123"` with:
```java
private String generateRandomPassword() {
    byte[] bytes = new byte[16];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
}
```

---

### Task 10: Public Config Information Filtering

**Files:**
- Modify: `backend/src/main/java/com/monteweb/admin/PublicTenantConfigInfo.java` or its `from()` factory method

- [ ] **Step 1: Filter infrastructure modules from public config**

In the `from()` method, filter the modules map to only include frontend-relevant toggles:
```java
private static final Set<String> PUBLIC_MODULES = Set.of(
    "messaging", "files", "jobboard", "cleaning", "calendar",
    "forms", "fotobox", "fundgrube", "bookmarks", "tasks", "wiki", "profilefields"
);

// In from():
Map<String, Boolean> publicModules = allModules.entrySet().stream()
    .filter(e -> PUBLIC_MODULES.contains(e.getKey()))
    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
```

---

### Task 11: DSGVO -- DeletionListeners (5 new)

**Files:**
- Create: `backend/src/main/java/com/monteweb/bookmark/internal/service/BookmarkDeletionListener.java`
- Create: `backend/src/main/java/com/monteweb/tasks/internal/service/TasksDeletionListener.java`
- Create: `backend/src/main/java/com/monteweb/wiki/internal/service/WikiDeletionListener.java`
- Create: `backend/src/main/java/com/monteweb/profilefields/internal/service/ProfileFieldsDeletionListener.java`
- Create: `backend/src/main/java/com/monteweb/notification/internal/service/NotificationDeletionListener.java`
- Modify: Repositories to add `deleteByUserId` methods where missing
- Modify: Services to add `cleanupUserData` methods

- [ ] **Step 1: Add repository methods**

Add to `BookmarkRepository`: `void deleteByUserId(UUID userId);`
Add to `NotificationRepository`: `void deleteByUserId(UUID userId);`
Add to `ProfileFieldValueRepository`: `void deleteByUserId(UUID userId);`

For Tasks: Add `@Modifying @Query("UPDATE Task t SET t.assigneeId = null WHERE t.assigneeId = :userId")` to TaskRepository
For Tasks: Add `@Modifying @Query("UPDATE Task t SET t.createdBy = null WHERE t.createdBy = :userId")` to TaskRepository
For Wiki: Add `@Modifying @Query("UPDATE WikiPage w SET w.createdBy = null WHERE w.createdBy = :userId")` and similar for `lastEditedBy` to WikiPageRepository
For Wiki: Add `@Modifying @Query("UPDATE WikiPageVersion v SET v.editedBy = null WHERE v.editedBy = :userId")` to WikiPageVersionRepository

- [ ] **Step 2: Add cleanupUserData to services**

Each service gets a `cleanupUserData(UUID userId)` method:
- `BookmarkService`: `bookmarkRepository.deleteByUserId(userId)`
- `TaskService`: anonymize assigneeId + createdBy (set null)
- `WikiService`: anonymize createdBy + lastEditedBy + version editedBy (set null)
- `ProfileFieldsService`: `profileFieldValueRepository.deleteByUserId(userId)`
- `NotificationService`: `notificationRepository.deleteByUserId(userId)` + `pushSubscriptionRepository.deleteByUserId(userId)`

- [ ] **Step 3: Create 5 DeletionListeners**

Follow exact pattern from existing listeners (FeedDeletionListener, MessagingDeletionListener):
- `@Component` with `@ConditionalOnProperty` for conditional modules
- `@Async @EventListener @Transactional` on `onUserDeletion(UserDeletionExecutedEvent event)`
- Delegate to service `cleanupUserData(event.userId())`
- SLF4J logging

Conditionality:
- `BookmarkDeletionListener`: `monteweb.modules.bookmarks.enabled`
- `TasksDeletionListener`: `monteweb.modules.tasks.enabled`
- `WikiDeletionListener`: `monteweb.modules.wiki.enabled`
- `ProfileFieldsDeletionListener`: `monteweb.modules.profilefields.enabled`
- `NotificationDeletionListener`: NOT conditional (core module)

---

### Task 12: DSGVO -- Data Export Completion

**Files:**
- Modify: `backend/src/main/java/com/monteweb/user/internal/service/UserService.java` (exportUserData method)
- Modify: `backend/src/main/java/com/monteweb/notification/NotificationModuleApi.java`
- Modify: `backend/src/main/java/com/monteweb/notification/internal/service/NotificationService.java`

- [ ] **Step 1: Add exportUserData to NotificationModuleApi**

```java
Map<String, Object> exportUserData(UUID userId);
```

Implement in NotificationService:
```java
public Map<String, Object> exportUserData(UUID userId) {
    return Map.of(
        "notifications", notificationRepository.findByUserId(userId),
        "pushSubscriptions", pushSubscriptionRepository.findByUserId(userId)
    );
}
```

- [ ] **Step 2: Add missing modules to UserService.exportUserData**

Add calls for: bookmarks, tasks, wiki, profilefields, notifications using the same `safeExportModule` pattern:
```java
safeExportModule(data, "bookmarks", () -> bookmarkModuleApi.exportUserData(userId));
safeExportModule(data, "tasks", () -> tasksModuleApi.exportUserData(userId));
safeExportModule(data, "wiki", () -> wikiModuleApi.exportUserData(userId));
safeExportModule(data, "profileFields", () -> profileFieldsModuleApi.exportUserData(userId));
safeExportModule(data, "notifications", () -> notificationModuleApi.exportUserData(userId));
```

Inject the missing ModuleApis via constructor with `@Lazy @Autowired(required = false)`.

---

### Task 13: TOTP Secret Encryption

**Files:**
- Modify: `backend/src/main/java/com/monteweb/auth/internal/service/TotpService.java`
- Create: `backend/src/main/resources/db/migration/V114__encrypt_totp_secrets.sql` (or next available number)

- [ ] **Step 1: Encrypt TOTP secrets before storage**

Inject `AesEncryptionService` into TotpService. When enabling TOTP (storing the secret):
```java
user.setTotpSecret(aesEncryptionService.encrypt(secret));
```

When verifying TOTP codes (reading the secret):
```java
String secret = aesEncryptionService.decrypt(user.getTotpSecret());
```

The `AesEncryptionService.decrypt()` already handles legacy plaintext values (passthrough when not prefixed with `ENC(`), so existing unencrypted secrets will continue to work. New secrets will be encrypted.

- [ ] **Step 2: Increase column size for encrypted values**

Create migration (use next available number after Task 9's migration):
```sql
ALTER TABLE users ALTER COLUMN totp_secret TYPE VARCHAR(256);
```

---

### Task 14: @Transactional readOnly Defaults

**Files:**
- Modify: `backend/src/main/java/com/monteweb/cleaning/internal/service/CleaningService.java`
- Modify: `backend/src/main/java/com/monteweb/jobboard/internal/service/JobboardService.java`

- [ ] **Step 1: Add readOnly = true to class-level @Transactional**

Change class-level annotation from `@Transactional` to `@Transactional(readOnly = true)` on both services. Add explicit `@Transactional` (without readOnly) to all mutating methods in each service.

---

### Task 15: Run Full Test Suite

- [ ] **Step 1: Run backend tests**

```bash
cd backend && ./mvnw test
```

Expected: All ~490 tests pass.

- [ ] **Step 2: Run frontend tests**

```bash
cd frontend && npm test
```

Expected: All ~1835 tests pass.
