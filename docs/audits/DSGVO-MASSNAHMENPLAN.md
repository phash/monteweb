# DSGVO-Maßnahmenplan – MonteWeb

**Erstellt:** 2026-02-28
**Grundlage:** DSGVO-Datenschutzprüfung (Bericht: `dsgvo-report.html`)
**Gesamturteil:** TEILWEISE KONFORM
**Verantwortlich:** Datenschutzbeauftragter / Schulleitung

---

## Übersicht der Findings

| Priorität | Anzahl |
|-----------|--------|
| KRITISCH  | 3      |
| HOCH      | 5      |
| MITTEL    | 7      |
| HINWEIS   | 6      |
| **Gesamt**| **21** |

---

## Findings & Maßnahmen

### KRITISCH

---

#### DSGVO-K-01: JWT-Secret Klartext-Fallback in application.yml
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 32 – Sicherheit der Verarbeitung |
| **Fundstelle** | `backend/src/main/resources/application.yml`, Zeile 88 |
| **Risiko** | Bekanntes Default-Secret `dev-only-secret-change-in-production-...` ermöglicht Token-Fälschung → Vollzugriff auf alle Nutzerdaten |
| **Maßnahme** | Fallback-Wert entfernt; `${JWT_SECRET}` ohne Default; App startet nicht ohne gesetztes Secret |
| **Status** | ✅ BEHOBEN (2026-02-28) |
| **Commit** | siehe Git-History |

---

#### DSGVO-K-02: Kinderdaten ohne DSFA (Datenschutz-Folgenabschätzung)
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 35 – Datenschutz-Folgenabschätzung |
| **Fundstelle** | Gesamtsystem; Schüler-Accounts, Fotobox, Formulare |
| **Risiko** | Verarbeitung von Kinderdaten (unter 16 Jahren) ohne vorherige DSFA; hohes Risiko bei Datenpannen |
| **Maßnahme** | DSFA-Dokument durch rechtlich begleitete Datenschutzberatung erstellen; Ergebnis vor Produktivbetrieb dokumentieren |
| **Verantwortlich** | Schulleitung + externer Datenschutzberater |
| **Deadline** | 90 Tage nach Erstbetrieb |
| **Status** | 🔴 OFFEN |

---

#### DSGVO-K-03: Jitsi meet.jit.si als automatischer US-Server
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 44 – Drittlandübermittlung; Art. 49 – Ausnahmen |
| **Fundstelle** | `TenantConfig.jitsiServerUrl`, `V09x` Migrations |
| **Risiko** | Standardmäßige Übermittlung von Kommunikationsdaten an US-Server ohne Rechtsgrundlage (kein SCCs, kein Adequacy Decision für Videodaten) |
| **Maßnahme (Code)** | Java-Default auf `null` gesetzt; Migration V102 setzt bestehende Defaults zurück; Aktivierung ohne eigene URL per `BusinessException` blockiert |
| **Maßnahme (Org.)** | Eigenen Jitsi-Server in EU hosten oder Jitsi-Modul deaktiviert lassen |
| **Status** | ✅ BEHOBEN – Code (2026-02-28); 🟡 IN BEARBEITUNG – Org. |

---

### HOCH

---

#### DSGVO-H-01: E-Mail-Adresse in Deletion-Logs
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 5 Abs. 1 lit. f – Integrität und Vertraulichkeit; Datenminimierung in Logs |
| **Fundstelle** | `UserDeletionScheduler.java`, Zeile 34 |
| **Risiko** | E-Mail-Adresse gelöschter Nutzer verbleibt in Anwendungs-Logs; widerspricht Löschkonzept |
| **Maßnahme** | `log.info("Deleted user {} ({})", id, email)` → `log.info("Deleted user {}", id)` |
| **Status** | ✅ BEHOBEN (2026-02-28) |

---

#### DSGVO-H-02: Kein Auftragsverarbeitungsvertrag (AVV) mit MinIO / Solr / SMTP
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 28 – Auftragsverarbeiter |
| **Fundstelle** | Docker Compose: minio, solr, smtp-relay |
| **Risiko** | Fehlender AVV bei externer Hosting-Nutzung; bei Self-Hosting intern nicht erforderlich, aber zu dokumentieren |
| **Maßnahme** | Bei externem Hosting: AVV mit Anbietern abschließen. Bei Self-Hosting: interne Verarbeitungsanweisung dokumentieren |
| **Verantwortlich** | Schulleitung / IT-Betrieb |
| **Deadline** | Vor Produktivbetrieb |
| **Status** | 🔴 OFFEN |

---

#### DSGVO-H-03: LDAP Bind-Passwort im Klartext in DB
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 32 – Sicherheit der Verarbeitung |
| **Fundstelle** | `tenant_config.ldap_bind_password`, `TenantConfig.ldapBindPassword` |
| **Risiko** | LDAP-Credentials für das gesamte Active Directory unverschlüsselt in PostgreSQL |
| **Maßnahme** | AES-256-GCM-Verschlüsselung implementiert: `AesEncryptionService` leitet Schlüssel via SHA-256 aus JWT-Secret ab; `ldap_bind_password` und `github_pat` werden als `ENC(base64(IV+ciphertext))` gespeichert. V103-Migration löscht Plaintext-Werte; Admin muss nach Deployment neu eingeben. API-Response enthält nie den Klartext (`ldapBindPassword`/`githubPat` = null, nur `githubPatConfigured`-Boolean). |
| **Deadline** | 60 Tage (sofern LDAP-Modul aktiv) |
| **Status** | ✅ BEHOBEN (2026-02-28) |

---

#### DSGVO-H-04: Keine erzwungene Einwilligung für Fotobox-Aufnahmen
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 6 Abs. 1 lit. a; Art. 9 (besondere Kategorien); KUG §22 |
| **Fundstelle** | Fotobox-Modul; `consent_records`-Tabelle vorhanden, aber nicht erzwungen |
| **Risiko** | Aufnahmen von Personen (insb. Kindern) ohne dokumentierte Einwilligung; `PHOTO_CONSENT` in DB angelegt, aber nicht als Pflichtvoraussetzung |
| **Maßnahme** | Backend-Enforcement: `FotoboxController.uploadImages()` prüft via `UserModuleApi.hasActiveConsent(userId, "PHOTO_CONSENT")`, wirft HTTP 403 ohne Consent. Frontend `RoomFotobox.vue` zeigt Info-Banner mit Link zu Datenschutz-Einstellungen und deaktiviert Upload-Button. |
| **Deadline** | 90 Tage |
| **Status** | ✅ BEHOBEN (2026-02-28) |

---

#### DSGVO-H-05: Terms-of-Service nicht bei Login erzwungen
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 7 – Bedingungen für die Einwilligung |
| **Fundstelle** | `terms_acceptances`-Tabelle; `PrivacyController`; Login-Flow |
| **Risiko** | Nutzer können das System verwenden, ohne aktuelle AGB/Datenschutzerklärung akzeptiert zu haben |
| **Maßnahme** | `TermsAcceptanceFilter` (`OncePerRequestFilter`) prüft nach Authentifizierung ob aktuelle `terms_version` via `UserModuleApi.hasAcceptedTerms()` akzeptiert wurde. Gibt HTTP 451 mit `{"termsRequired":true,"termsVersion":"..."}` zurück. Whitelist: auth/privacy/config/error-reports/actuator/ws/wopi-Endpoints. Frontend-Guard war bereits aktiv. |
| **Deadline** | 60 Tage |
| **Status** | ✅ BEHOBEN (2026-02-28) |

---

### MITTEL

---

#### DSGVO-M-01: requestUrl in Error-Reports enthält Query-Parameter
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 5 Abs. 1 lit. c – Datenminimierung |
| **Fundstelle** | `ErrorReportService.submitReport()` |
| **Risiko** | Query-Parameter (z. B. `?token=…`, `?userId=…`) können personenbezogene Daten enthalten |
| **Maßnahme** | `sanitizeUrl()`-Methode entfernt Query-Parameter vor Speicherung; nur URL-Pfad wird gespeichert |
| **Status** | ✅ BEHOBEN (2026-02-28) |

---

#### DSGVO-M-02: Admin-Profilansichten nicht im DataAccessLog
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 15 – Auskunftsrecht; Art. 32 – Nachweisbarkeit |
| **Fundstelle** | `AdminUserController.java` |
| **Risiko** | Admins können Nutzerprofile abrufen ohne Audit-Trail; Exporte und Löschungen werden bereits geloggt |
| **Maßnahme** | `GET /api/v1/admin/users/{id}` Endpoint schreibt `DataAccessLog`-Eintrag mit Aktion `ADMIN_USER_VIEW` |
| **Status** | ✅ BEHOBEN (2026-02-28) |

---

#### DSGVO-M-03: Keine automatische Löschung von Error-Reports
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 5 Abs. 1 lit. e – Speicherbegrenzung |
| **Fundstelle** | `error_reports`-Tabelle; `RetentionCleanupService` |
| **Risiko** | Error-Reports enthalten UserId und RequestUrl; keine Löschfrist definiert |
| **Maßnahme** | `RetentionCleanupService` löscht jetzt: RESOLVED/IGNORED nach 90 Tagen, NEW/REPORTED nach 365 Tagen. `ErrorReportRepository` hat neue `@Modifying` JPQL-Query. Cross-Module-Boundary-Regel beachtet: Delegation via `AdminModuleApi.cleanupOldErrorReports()`. |
| **Deadline** | 30 Tage |
| **Status** | ✅ BEHOBEN (2026-02-28) |

---

#### DSGVO-M-04: Fehlende IP-Logging-Kontrolle
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 5 Abs. 1 lit. c – Datenminimierung |
| **Fundstelle** | nginx-Konfiguration; Spring Boot Logging |
| **Risiko** | Access-Logs mit IP-Adressen könnten personenbezogene Daten enthalten |
| **Maßnahme** | nginx `map`-Direktive maskiert letztes IPv4-Oktett auf `.0` (`$masked_ip`). Neues `log_format dsgvo` verwendet `$masked_ip` statt `$remote_addr`. `access_log` im `server {}`-Block nutzt DSGVO-Format. |
| **Deadline** | 60 Tage |
| **Status** | ✅ BEHOBEN (2026-02-28) |

---

#### DSGVO-M-05: VAPID-Keys ohne Rotation
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 32 – Technische Maßnahmen |
| **Fundstelle** | `application.yml`: `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY` |
| **Risiko** | Keine Rotation der Push-Notification-Keys definiert |
| **Maßnahme** | VAPID-Key-Rotationsverfahren in Betriebsdokumentation aufnehmen (empfohlen: jährlich) |
| **Deadline** | Dokumentation: 30 Tage |
| **Status** | 🔴 OFFEN |

---

#### DSGVO-M-06: Keine Datenschutzerklärung vorausgefüllt
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 13 – Informationspflicht bei Erhebung |
| **Fundstelle** | `tenant_config.privacy_policy_text` – leer bei Neuinstallation |
| **Risiko** | Keine Datenschutzerklärung für Nutzer sichtbar bis Admin manuell befüllt |
| **Maßnahme** | V070-Migration seeded BaySchO-konforme Datenschutzerklärung mit Platzhaltern (`[SCHULNAME]`, `[ADRESSE]`, etc.) in `tenant_config.privacy_policy_text`. Admin kann Text im Admin-Panel anpassen. |
| **Deadline** | 60 Tage |
| **Status** | ✅ BEHOBEN (V070, 2026-02-28) |

---

#### DSGVO-M-07: Messaging-Bilder ohne Zugriffskontrolle auf Thumbnails
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 32 – Sicherheit; Art. 5 Abs. 1 lit. f |
| **Fundstelle** | `message_images`-Thumbnails; MinIO-Bucket |
| **Risiko** | Thumbnails möglicherweise direkt über MinIO-URL zugänglich ohne JWT-Prüfung |
| **Maßnahme** | `MessagingController` proxied alle Bildabrufe (original + thumbnail) via `requireParticipant()`-Auth-Check. MinIO-Bucket ist nur intern erreichbar (Docker-Netzwerk). Kein öffentlicher Direktzugriff. |
| **Deadline** | Vor Produktivbetrieb prüfen |
| **Status** | ✅ BEHOBEN (2026-02-28) |

---

### HINWEISE

---

#### DSGVO-N-01: Dark-Mode-Einstellung in DB (keine Auswirkung auf DSGVO, aber Datensparsamkeit)
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 25 – Privacy by Design |
| **Fundstelle** | `users.dark_mode` |
| **Risiko** | Gering – reine Präferenz, kein sensibler Datenwert |
| **Maßnahme** | Alternativ: im Browser-LocalStorage speichern (kein personenbezogener Datenbankwert) |
| **Status** | 💡 AKZEPTIERT (DB-Speicherung ermöglicht geräteübergreifende Synchronisation) |

---

#### DSGVO-N-02: GitHub PAT im Klartext in DB
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 32 – Sicherheit (Secrets Management) |
| **Fundstelle** | `tenant_config.github_pat` |
| **Risiko** | GitHub Personal Access Token unverschlüsselt gespeichert; kein Personenbezug, aber Sicherheitsrisiko |
| **Maßnahme** | Im Zuge von H-03 mitbehoben: `github_pat` wird ebenfalls via `AesEncryptionService` (AES-256-GCM) verschlüsselt gespeichert. API sendet `githubPatConfigured`-Boolean statt Klartext. |
| **Status** | ✅ BEHOBEN (2026-02-28, zusammen mit H-03) |

---

#### DSGVO-N-03: Keine Sitzungs-Timeout-Konfiguration
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 32 – Technische Maßnahmen |
| **Fundstelle** | JWT: Access 15min, Refresh 7d (application.yml) |
| **Risiko** | 7-Tage Refresh-Token bei Schulcomputern mit gemeinsamem Zugang kritisch |
| **Maßnahme** | Refresh-Token-Laufzeit konfigurierbar machen; Empfehlung: Kürzer bei Shared-Devices. Als Admin-Einstellung dokumentieren |
| **Status** | 💡 AKZEPTIERT bis Release; Betriebshinweis in Installationsguide |

---

#### DSGVO-N-04: Kein Consent-Tracking für Neuigkeiten-Digest
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 6 Abs. 1 lit. a – Einwilligung |
| **Fundstelle** | `users.digest_frequency`; E-Mail-Versand |
| **Risiko** | Digest-E-Mails ohne explizite Opt-in-Einwilligung (könnte als Direktmarketing gewertet werden) |
| **Maßnahme** | Standard: Digest deaktiviert (NONE). Opt-in durch Nutzer. Bereits implementiert – Hinweis in Datenschutzerklärung aufnehmen |
| **Status** | 💡 AKZEPTIERT (Opt-in bereits Standard) |

---

#### DSGVO-N-05: TOTP-Recovery-Codes unverschlüsselt
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 32 – Technische Maßnahmen |
| **Fundstelle** | `users.totp_recovery_codes` (TEXT[]) |
| **Risiko** | Recovery-Codes in Klartext (sind Einmalkodes, aber dennoch) |
| **Maßnahme** | `TotpService.hashRecoveryCode()` speichert neue Codes als BCrypt-Hash. `verifyRecoveryCode()` unterstützt Legacy-Plaintext als Fallback (wird bei Nutzung konsumiert). `AuthService.confirm2fa()` speichert Hashes, gibt Plaintext einmalig zurück. |
| **Deadline** | 60 Tage |
| **Status** | ✅ BEHOBEN (2026-02-28) |

---

#### DSGVO-N-06: Solr-Suchindex enthält Personendaten
| Feld | Inhalt |
|------|--------|
| **DSGVO-Artikel** | Art. 17 – Recht auf Löschung; Art. 5 Abs. 1 lit. e |
| **Fundstelle** | Solr-Index; `SearchService` |
| **Risiko** | Bei Nutzerlöschung muss Solr-Index synchron bereinigt werden |
| **Maßnahme** | `SearchDeletionListener` reagiert auf `UserDeletionExecutedEvent`, ruft `SolrIndexingService.deleteUserDocuments(userId)` auf (löscht `USER:{id}`-Dokument via `deleteById`). Nur aktiv wenn `monteweb.modules.solr.enabled=true`. |
| **Status** | ✅ BEHOBEN (2026-02-28) |

---

## Organisatorische Maßnahmen

| Maßnahme | Verantwortlich | Deadline | Status |
|----------|---------------|----------|--------|
| DSFA für Kinderdaten erstellen (Art. 35) | Schulleitung + Datenschutzberater | 90 Tage | 🔴 OFFEN |
| AVV mit externen Auftragsverarbeitern abschließen | Schulträger | Vor Produktivbetrieb | 🔴 OFFEN |
| Datenschutzbeauftragten benennen (falls >250 MA oder Kinderdaten) | Schulleitung | Vor Produktivbetrieb | 🔴 OFFEN |
| Verarbeitungsverzeichnis (VVT) nach Art. 30 erstellen | DSB / IT | 60 Tage | 🟡 Vorlage vorhanden: `docs/VVT-VORLAGE.md` |
| Löschkonzept dokumentieren | DSB | 60 Tage | 🟡 IN BEARBEITUNG |
| Datenpannenmeldeverfahren (Art. 33) definieren | DSB | 60 Tage | 🔴 OFFEN |
| Betriebsanweisung für Admins (Zugriffsrechte, Logs) | IT | 30 Tage | 🔴 OFFEN |
| Schulung des pädagogischen Personals (DSGVO-Grundlagen) | Schulleitung | 90 Tage | 🔴 OFFEN |

---

## Behobene Findings (Zusammenfassung)

| ID | Beschreibung | Behoben am | Datei |
|----|-------------|------------|-------|
| K-01 | JWT-Secret Fallback entfernt | 2026-02-28 | `application.yml` |
| K-03 | Jitsi-Default auf null; V102-Migration; Aktivierung ohne URL blockiert | 2026-02-28 | `TenantConfig.java`, `AdminService.java`, `V102__fix_jitsi_default.sql` |
| H-01 | E-Mail aus Deletion-Log entfernt | 2026-02-28 | `UserDeletionScheduler.java` |
| M-01 | requestUrl-Sanitisierung (Query-Parameter) | 2026-02-28 | `ErrorReportService.java` |
| M-02 | Admin-Profilansicht im DataAccessLog | 2026-02-28 | `AdminUserController.java` |

---

## Wiederholungsprüfung

### Checkliste für nächste DSGVO-Prüfung

- [ ] Alle OFFEN-Findings aus diesem Plan geschlossen
- [ ] DSFA erstellt und unterzeichnet
- [ ] VVT vollständig ausgefüllt (`docs/VVT-VORLAGE.md`)
- [ ] AVV mit allen externen Dienstleistern vorhanden
- [ ] Datenschutzerklärung im System befüllt und aktuell
- [ ] Terms-of-Service-Erzwingung bei Login implementiert
- [ ] Fotobox-Einwilligungsflow implementiert
- [ ] MinIO-Bucket-Zugriffskontrolle verifiziert
- [ ] Solr-Löschsynchronisation bei Nutzer-Deletion geprüft
- [ ] TOTP-Recovery-Codes gehasht
- [ ] LDAP-Passwort verschlüsselt (falls LDAP aktiv)
- [ ] Error-Reports-Löschfrist implementiert (90 Tage)
- [ ] nginx IP-Anonymisierung konfiguriert
- [ ] GitHub PAT durch Fine-grained Token mit minimalen Rechten ersetzt

### Empfohlene Prüffrequenz
- **Vierteljährlich:** Code-Review sicherheitsrelevanter Module (Auth, User, Admin)
- **Jährlich:** Vollständige DSGVO-Prüfung (analog zu diesem Report)
- **Anlassbezogen:** Bei neuen Modulen oder wesentlichen Änderungen

---

## Referenzen

- Vollständiger Prüfbericht: `dsgvo-report.html` (im Projektstamm)
- Verarbeitungsverzeichnis-Vorlage: `docs/VVT-VORLAGE.md`
- Backup-Dokumentation: `BACKUP.md`
- Infrastruktur-Änderungen: `INFRA-CHANGELOG.md`
- Lokale Entwicklungsumgebung: `LOCAL-DEV-GUIDE.md`
