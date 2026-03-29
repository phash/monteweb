# Verbleibende Items aus dem Review vom 29.03.2026

Dieses Dokument listet alle Findings aus dem Security-, DSGVO- und Code-Quality-Review, die **nicht** im ersten Fix-Batch umgesetzt wurden und noch ausstehen.

---

## Bereits umgesetzt (Batch 1)

- [x] DOMPurify Update (Mutation-XSS)
- [x] npm audit fix (undici, picomatch, etc.)
- [x] Solr Filter-Query Injection (Type-Whitelist)
- [x] CSV Import: Random Passwords + Force-Change
- [x] WOPI JWT Secret mandatory
- [x] nginx CSP (frame-src, worker-src, static header inheritance)
- [x] iframe Sandbox (VideoEmbed, WopiEditor)
- [x] Service Worker: Opaque Response Caching, Auth-Endpoint-Exclusion, Push-URL-Validation
- [x] JSON Injection in MaintenanceModeFilter + TermsAcceptanceFilter
- [x] TOTP Replay Detection (userId statt hashCode)
- [x] PublicTenantConfigInfo: Infrastruktur-Module filtern
- [x] 5 fehlende DeletionListeners (Bookmarks, Tasks, Wiki, ProfileFields, Notifications)
- [x] Datenexport um 5 Module erweitert
- [x] TOTP Secrets verschluesselt (AES-GCM)
- [x] @Transactional(readOnly=true) auf CleaningService + JobboardService

---

## Offen: Security

### MEDIUM

| # | Finding | Aufwand | Beschreibung |
|---|---------|--------|-------------|
| S-M1 | **Access Token aus sessionStorage entfernen** | Hoch | Umstellung auf Cookie-only JWT (httpOnly + SameSite=Strict). Erfordert CSRF-Protection. Betrifft: auth.ts, client.ts, SecurityConfig.java, alle API-Interceptors |
| S-M2 | **WebSocket Reconnect Race Condition** | Niedrig | Token-Refresh-Event in useWebSocket.ts abhoeren, Reconnect bei neuem Token erzwingen |
| S-M3 | **SW Cache-Isolation pro User** | Mittel | User-spezifischen Cache-Namespace im Service Worker, damit bei Shared Devices keine Cross-User-Cache-Leaks |
| S-M4 | **Rate Limiting: Proxy-Konfiguration haerten** | Niedrig | Dokumentation/Verifikation dass nginx/Caddy X-Real-IP immer ueberschreibt. Optional: Trusted-Proxy-Allowlist im RateLimitFilter |
| S-M5 | **CSP Konsolidierung** | Niedrig | CSP nur an einer Stelle definieren (nginx ODER SecurityConfig), nicht doppelt |
| S-M6 | **Password-Reset Token: Gueltigkeit auf 2h reduzieren** | Niedrig | `PasswordResetService.java` -- TOKEN_VALIDITY von 24h auf 2h |

### LOW

| # | Finding | Beschreibung |
|---|---------|-------------|
| S-L1 | Error Report Endpoint: Strengere Rate-Limits | Unauthenticated, 10K Fingerprint-Cap grosszuegig |
| S-L2 | openhtmltopdf XXE-Pruefung | Pruefen ob User-Content an PDF-Generator geht |
| S-L3 | Cookie SameSite=Strict auch fuer HTTP-Dev | Aktuell Lax fuer HTTP |
| S-L4 | Backend Error Messages nicht direkt in Toast anzeigen | i18n-Error-Codes statt rohe Backend-Messages |

---

## Offen: DSGVO / Datenschutz

### HIGH

| # | Finding | Aufwand | DSGVO-Art. | Beschreibung |
|---|---------|--------|-----------|-------------|
| D-H1 | **Altersverifikation fuer Schueler** | Mittel | Art. 8 | Bei Rollenwechsel zu STUDENT automatisch Consent-Dokumentation erzwingen. Eltern-Link vor Aktivierung |
| D-H2 | **Foto-Consent fuer abgebildete Personen** | Mittel | Art. 6, KUG | Foto-Melde-Funktion, Upload-Warnung "Haben alle abgebildeten Personen zugestimmt?", Policy in Nutzungsbedingungen |
| D-H3 | **Datenexport: Binaerdaten** | Mittel | Art. 20 | ZIP-Export mit tatsaechlichen Dateien (Fotos, Uploads), nicht nur Metadaten |

### MEDIUM

| # | Finding | Aufwand | DSGVO-Art. | Beschreibung |
|---|---------|--------|-----------|-------------|
| D-M1 | **Admin-Impersonation: Messaging blockieren** | Niedrig | Art. 5(1)(b) | Bei Impersonation DMs nicht lesbar machen, oder spezifisches Audit-Logging |
| D-M2 | **DB/MinIO Encryption-at-Rest** | Mittel | Art. 32 | Encrypted Volumes oder MinIO SSE konfigurieren. Besonders kritisch: Kinderfotos |
| D-M3 | **Push-Payloads minimieren** | Niedrig | Art. 44ff | Keine Personennamen in Push-Notifications ("Neue Nachricht" statt "Max hat geschrieben...") |
| D-M4 | **DSFA erstellen** | Hoch | Art. 35 | Datenschutz-Folgenabschaetzung dokumentieren (Pflicht bei Kinderdaten) |
| D-M5 | **Breach-Detection Infrastruktur** | Hoch | Art. 33 | Monitoring fuer ungewoehnliche Datenzugriffe, Bulk-Exports, Login-Anomalien |
| D-M6 | **Nachrichten-Retention-Policy** | Niedrig | Art. 5(1)(e) | Konfigurierbares Retention fuer Chat-Nachrichten (z.B. Schuljahresende) |

### LOW

| # | Finding | DSGVO-Art. | Beschreibung |
|---|---------|-----------|-------------|
| D-L1 | IP-Adresse in terms_acceptances | Art. 5(1)(c) | Entweder entfernen oder Rechtsgrundlage dokumentieren |
| D-L2 | Audit-Log Manipulationsschutz | Art. 5(2) | Write-once Tabelle oder externes Log-Forwarding |
| D-L3 | Zugriffs-Logging auf Schueler-Profile | Art. 30 | Loggen wenn nicht-Familie/nicht-Lehrer Schueler-Profile ansehen |
| D-L4 | Consent-Records: Typen dokumentieren | Art. 7 | Consent-Types als Enum definieren, Rechtsgrundlage je Typ |
| D-L5 | ENCRYPTION_SECRET mandatory in Prod | Art. 32 | Startup-Fail wenn nicht gesetzt (nicht nur Warnung) |

---

## Offen: Code Quality / Architektur

### HIGH

| # | Finding | Aufwand | Beschreibung |
|---|---------|--------|-------------|
| Q-H1 | **Modularity-Test re-enablen** | Mittel | user<->family Zyklus aufloesen: AdminUserController in admin-Modul verschieben, Admin-Methoden via UserModuleApi exponieren |
| Q-H2 | **Shared MinioStorageService** | Mittel | 7 duplizierte Storage-Services (Fotobox, Messaging, Fundgrube, Feed, Job, ParentLetter, Files) konsolidieren. Magic-Byte-Detection, Upload, Download, Delete, Thumbnail in shared Utility |

### MEDIUM

| # | Finding | Aufwand | Beschreibung |
|---|---------|--------|-------------|
| Q-M1 | **Unbounded findAll() ersetzen** | Mittel | `UserService.findUsersForDigest()` (alle User), `BillingService.generateReport()` (N+1), `TaskService.findAllTasksForIndexing()` (N+1), `FileService.findAllFiles()` |
| Q-M2 | **Grosse Vue-Komponenten refactoren** | Mittel | CalendarView (1441 LOC), RoomDetailView (1275), AdminUsers (1241), ProfileView (1124), MessagesView (1122) in Sub-Components aufteilen |
| Q-M3 | **Frontend Error-Handling standardisieren** | Mittel | Einheitliches Pattern mit i18n-Keys statt hardcoded englische Strings und inkonsistente catch-Patterns |
| Q-M4 | **JaCoCo in CI enforced** | Niedrig | `./mvnw test` in CI mit JaCoCo-Check (70% Minimum) |
| Q-M5 | **Actuator in nginx haerten** | Niedrig | Aktuell: `/actuator/` wird von nginx geblocked ABER mit `deny all` -- besser nur `/actuator/health` und `/actuator/prometheus` whitelisten |

### LOW

| # | Finding | Beschreibung |
|---|---------|-------------|
| Q-L1 | catch(Exception e) reduzieren | 50+ Stellen mit breitem Exception-Catching |
| Q-L2 | N+1 Queries / EntityGraph | Fehlende @EntityGraph / JOIN FETCH fuer Room.members, Family.members |
| Q-L3 | WebSocket als Pinia Store | useWebSocket Module-Level State in Pinia Store kapseln |
| Q-L4 | Auth Store Event Listener Cleanup | monteweb:token-refreshed Listener wird nie entfernt |
| Q-L5 | Redis Healthcheck Password Exposure | docker-compose.yml: REDISCLI_AUTH statt -a Flag |
| Q-L6 | DTOs in Service-Klassen verschachtelt | JobboardService interne DTOs in dto/ Package verschieben |
| Q-L7 | Fehlende DB-Indexes | cleaning_registrations, messages -- Index-Audit gegen @Query Methoden |

---

## Priorisierte Empfehlung fuer naechste Batches

### Batch 2 (Kurzfristig -- Security + DSGVO-Pflicht)
1. D-H1: Altersverifikation Schueler
2. D-H2: Foto-Consent Warnung
3. D-M4: DSFA erstellen
4. S-M6: Password-Reset Token 2h
5. S-M5: CSP konsolidieren
6. D-M3: Push-Payloads minimieren

### Batch 3 (Mittelfristig -- Architektur)
1. Q-H1: Modularity-Test re-enablen (user/family Zyklus)
2. Q-H2: Shared MinioStorageService
3. Q-M1: Unbounded findAll() ersetzen
4. Q-M4: JaCoCo CI

### Batch 4 (Langfristig -- Infrastruktur + UX)
1. D-M2: DB/MinIO Encryption-at-Rest
2. D-M5: Breach-Detection
3. S-M1: Cookie-only JWT
4. Q-M2: Grosse Vue-Komponenten refactoren
5. Q-M3: Frontend Error-Handling
