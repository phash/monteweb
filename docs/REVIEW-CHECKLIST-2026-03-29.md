# Review Checklist 29.03.2026 (Post-Fix Verification)

## CRITICAL -- Must Fix Immediately

- [x] **C1: AesEncryptionService.decrypt() broken** -- `ByteBuffer.wrap(combined, 0, GCM_IV_LENGTH).array()` returns full backing array, not 12-byte slice. All decryption fails. Affects: TOTP 2FA, GitHub PAT, LDAP password. Fix: use `System.arraycopy()` or `Arrays.copyOfRange()`
- [x] **C2: @EnableAsync missing on MonteWebApplication** -- All 18 DeletionListeners + SolrIndexingListener + notification listeners run synchronously. Fix: add `@EnableAsync` annotation

## HIGH -- Should Fix

- [x] **H1: safeExportModule() NPE risk** -- `@Autowired(required=false)` fields can be null. Catch `Exception` not just `NoSuchBeanDefinitionException`
- [x] **H2: NotificationService.exportUserData() leaks crypto keys** -- PushSubscription entities expose p256dhKey/authKey. Map to DTOs
- [x] **H3: Password reset token 24h too long** -- Change `TOKEN_EXPIRY_HOURS` from 24 to 2
- [ ] **H4: forcePasswordChange flag never checked on login** -- Flag is set but never read. Add to LoginResponse + frontend handling
- [x] **H5: Solr escapeQuery() incomplete** -- Missing escape for `"`, `*`, `?`, `/`

## SECURITY -- Frontend Config

- [x] **S1: nginx/nginx.conf CSP outdated** -- Missing frame-src, worker-src, media-src, connect-src wss: ws:
- [x] **S2: nginx/nginx.conf static block missing security headers** -- Same issue as frontend/nginx.conf had
- [x] **S3: Caddyfile missing CSP header** -- Has other security headers but no CSP
- [x] **S4: package.json dompurify min version** -- Bump from `^3.3.1` to `^3.3.2`

## TEST STABILITY

- [x] **T1: scrollTo mock missing** -- reduced errors from 26 to 22 (remaining are pre-existing unmocked API calls) -- 26 unhandled errors in tests. Add `Element.prototype.scrollTo = vi.fn()` to vitest.setup.ts

## TEST COVERAGE (53.65% → 56.22% statements, 1990 tests, 183 files)

### Backend -- New Code Tests
- [ ] **TC1: AesEncryptionService tests** -- encrypt/decrypt roundtrip, null handling, legacy plaintext passthrough
- [ ] **TC2: DeletionListener tests** -- At least test that event triggers cleanup for 2-3 listeners
- [ ] **TC3: Data export completeness test** -- Verify all modules included in export

### Frontend -- Coverage Improvements (target: maintain >=50%)
- [x] **TC4: ParentLetterCreateView tests** -- 24 tests, 0% -> covered
- [x] **TC5: ParentLetterDetailView tests** -- 30 tests, 0% -> covered
- [x] **TC6: RecipientStatusTable + VariableHelpMenu tests** -- 13+15 tests, 0% -> covered
- [x] **TC7: MessagesView coverage** -- 25 tests added
- [x] **TC8: PostComposer coverage** -- 28 tests added
- [x] **TC9: usePushNotifications tests** -- 20 tests added

## VERIFICATION

- [x] **V1: Backend compiles cleanly (Docker Java 21)**
- [x] **V2: Frontend 1990 tests pass (183 files)**
- [x] **V3: Frontend coverage 56.22% (>= 50%)**
- [ ] **V4: Start server locally**
- [ ] **V5: Run E2E tests**
