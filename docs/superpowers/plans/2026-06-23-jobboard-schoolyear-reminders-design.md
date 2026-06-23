# Design: Jobbörse — Schuljahr-Sicht & Job-Reminder

**Datum:** 2026-06-23
**Modul:** `jobboard` (+ `notification`-Facade, `user`-Facade, Frontend)
**Status:** Design freigegeben, bereit für Implementierungsplan

## Problemstellung

Eltern brauchen drei Dinge in der Jobbörse:

1. **Schuljahr-Sicht:** Die Job-Übersicht soll standardmäßig das **laufende Schuljahr** zeigen, mit einem Umschalter auf vergangene Jahre.
2. **Abrechnung pro Schuljahr:** Für die Jahresabrechnung zählt nur, was **in diesem Schuljahr** erledigt (bestätigt) wurde.
3. **Reminder:** Jobs, die **4 Wochen nach dem Termin** noch nicht abgeschlossen/bestätigt sind, lösen eine E-Mail an den Bearbeiter (Eltern bzw. Admin zur Bestätigung) aus — und ein zweiter Anstoß **2 Wochen vor Jahresende**.

## Ist-Zustand (Befund aus Codebase-Exploration)

- **`BillingPeriod` ist bereits das „Schuljahr"-Konstrukt:** Entität mit `name` (z.B. „Schuljahr 2024/2025"), `start_date`, `end_date`, `status` (ACTIVE/CLOSED), `report_data` (gefrorener Snapshot beim Schließen). Genau eine Periode ist ACTIVE.
- **Die Abrechnung pro Schuljahr existiert schon:** `BillingService.generateReport(period)` filtert bestätigte Stunden über `confirmedAt` auf den Periodenzeitraum. Passende Range-Queries existieren bereits im `JobAssignmentRepository`:
  - `sumConfirmedNormalHoursByFamilyIdAndDateRange(familyId, from, to)`
  - `sumConfirmedCleaningJobHoursByFamilyIdAndDateRange(familyId, from, to)`
  - `CleaningModuleApi.getCleaningHoursForFamilyInRange(...)` (Legacy-QR-Putzstunden)
- **Admin-Jahresabrechnung** läuft über `BillingController` (`/api/v1/billing/periods/...`, Report + PDF/CSV-Export + Close) und ist im Frontend `AdminBilling.vue` period-scoped.
- **Lücke 1 — Eltern-Sicht ist all-time:** `GET /api/v1/jobs/family/{familyId}/hours` (`getFamilyHours`) und `/api/v1/jobs/report` nutzen die **nicht** datumsgefilterten Queries (`sumConfirmedNormalHoursByFamilyId` ohne Range) → zeigen Lebenszeit-Summen statt laufendes Schuljahr. Es gibt **keinen** Schuljahr-Umschalter für Eltern (`JobBoardView.vue` hat nur Kategorie-/Event-/Datums-Filter).
- **Lücke 2 — keine Job-Reminder:** Es existiert kein Reminder. Vorbild vorhanden: `ParentLetterReminderService` (`@Scheduled`-Service, Deadline-basiert), `FormAutoCloseService`. `@EnableScheduling` steht auf `MonteWebApplication`. E-Mail: `EmailService.sendGenericEmail(to, subject, body)` (Plain-Text, `@ConditionalOnProperty(monteweb.email.enabled=true)`). In-App: `NotificationModuleApi.sendNotification(userId, type, title, message, link, refType, refId)` (In-App + Push; sendet **kein** E-Mail selbst).

## Gewählter Ansatz

**Wiederverwenden statt parallel bauen.** `BillingPeriod` bleibt die einzige Quelle der Wahrheit fürs Schuljahr; wir machen das bestehende period-scoping für Eltern sichtbar/wählbar. Reminder analog zum erprobten `ParentLetterReminderService`-Pattern.

Verworfene Alternative: eigenes Schuljahr-Modell + event-getriebene Reminder — dupliziert Logik, würde von der Admin-Abrechnung divergieren.

**Bestätigte Entscheidungen (Brainstorming):**
- Schuljahr-Basis = **bestehende `BillingPeriod`** (nicht datumsabgeleitet).
- Umschalter erscheint **nur in historischen Ansichten** (Stundenkonto, erledigte/meine Jobs, Admin-Report). Der „Offene Jobs"-Tab bleibt immer aktuell.
- Verwaiste OPEN-Jobs (kein Bearbeiter) → **Ersteller + Jobboard-Admins** erinnern.

---

## Teil A — Schuljahr-Scoping (Eltern-Sicht)

### Backend (Modul `jobboard`, nur über Facades, keine `internal/`-Importe)

1. **Leser-Endpoint für wählbare Schuljahre:**
   `GET /api/v1/jobs/school-years` → `List<SchoolYearInfo>` mit `{id, name, startDate, endDate, active}`, sortiert nach `startDate` absteigend. Für jeden eingeloggten Nutzer lesbar (das bestehende `/api/v1/billing/periods` ist admin-beschränkt). Quelle: `BillingPeriodRepository.findAllByOrderByStartDateDesc()`.

2. **Stundenkonto period-scoped:**
   `GET /api/v1/jobs/family/{familyId}/hours?periodId={uuid}` — `periodId` optional.
   - **Ohne `periodId` → Default = aktive Periode** (vorher: all-time). Das realisiert „aktuelles Schuljahr ausgewählt".
   - Berechnung über die bereits existierenden Range-Queries (normal/cleaning + `CleaningModuleApi`-Range). Effektiv dieselbe Logik wie `BillingService.generateReport` pro Familie — ggf. extrahierbare gemeinsame Methode.
   - Fällt zurück auf all-time nur, wenn **keine** Periode existiert (defensiv).

3. **Erledigte Assignments der Familie period-scoped:**
   `GET /api/v1/jobs/family/{familyId}/assignments?periodId={uuid}` — neue Range-Variante von `findConfirmedByFamilyId`, die `confirmedAt` im Periodenzeitraum filtert (neue Repo-Methode `findConfirmedByFamilyIdAndDateRange`).

4. **Admin-Report:** bereits period-scoped über `BillingController` → unverändert weiterverwenden.

### Frontend

1. **Neue Komponente `SchoolYearSelect.vue`** (PrimeVue `Select`): Optionen aus `/jobs/school-years`, Default = aktive Periode. Wiederverwendbar, emittiert `periodId`.
2. **`jobboard`-Store:** neue State-Felder `schoolYears`, `selectedPeriodId`; Actions `fetchSchoolYears()`, `fetchFamilyHours(familyId, periodId?)`, `fetchFamilyAssignments(familyId, periodId?)` reichen `periodId` durch. Auswahl in Route-Query (`?year=`) persistiert, Default = aktive Periode.
3. **Eingebaut in:** `FamilyHoursWidget.vue` (Eltern-Dashboard), `JobBoardView.vue` (Bereich „Meine/Erledigte Jobs"), `AdminJobReport.vue`.
4. **„Offene Jobs"-Tab unverändert** (immer aktuelle offene Jobs).
5. **i18n:** neue Keys unter `jobboard.*` (`schoolYear`, `currentSchoolYear`, …) in `de.ts` + `en.ts`.
6. **Typen:** `SchoolYearInfo`-Interface in `types/jobboard.ts`; `FamilyHoursInfo`-Fetch akzeptiert optional `periodId`.

---

## Teil B — Reminder (E-Mail + In-App)

Neuer **`JobReminderService`** im `jobboard`-Modul:
- `@ConditionalOnProperty(prefix = "monteweb.modules", name = "jobboard.enabled", havingValue = "true")`
- `@Scheduled(cron = "0 30 7 * * *")` (täglich, nach Digest 7:00 / ParentLetter 7:05).
- Injizierte `Clock` → Zeit in Tests steuerbar (statt direktem `Instant.now()`).
- **Kanal je Reminder:** In-App immer (`NotificationModuleApi.sendNotification`, neuer `NotificationType.JOB_OVERDUE`) **+ E-Mail wenn `monteweb.email.enabled`** (`EmailService.sendGenericEmail`, optional via `@Autowired(required=false)` injiziert).

### Welle 1 — 4 Wochen nach Termin

Auswahl: Jobs mit `scheduledDate ≤ heute − 28 Tage`, **nicht „fertig"** (nicht alle aktiven Assignments COMPLETED+`confirmed`), CANCELLED ausgeschlossen, `overdue_reminder_sent_at` noch nicht gesetzt. Jobs **ohne `scheduledDate`** werden übersprungen (kein Termin-Bezug).

Empfänger nach Zustand:
| Zustand | Empfänger | Botschaft |
|---|---|---|
| Assignment ASSIGNED/IN_PROGRESS (nicht abgeschlossen) | Bearbeiter (Eltern) | „bitte abschließen & Stunden eintragen" |
| Assignment COMPLETED, `confirmed=false` | Bestätiger (Lehrer/Section-/Jobboard-Admin) | „bitte bestätigen" |
| OPEN / kein aktives Assignment (verwaist) | Job-Ersteller + Jobboard-Admins | „Job blieb liegen — verlängern/schließen/neu ausschreiben" |

Feuert **einmal** pro Job bzw. Assignment (Idempotenz via Flag). Wiederkehrende Mahnungen sind bewusst nicht vorgesehen (Welle 2 ist der zweite/finale Anstoß).

### Welle 2 — 2 Wochen vor Jahresende

- „Jahresende" = `end_date` der **aktiven** `BillingPeriod`.
- Fenster: `heute ≥ end_date − 14 Tage` **und** Periode noch nicht abgeschlossen **und** `year_end_reminder_sent_at` der Periode nicht gesetzt.
- Aktion: für **alle** noch offenen Jobs/Assignments der aktiven Periode einen finalen Reminder an dieselben Empfänger (gleiche Routing-Tabelle wie Welle 1), damit vor dem Abschluss aufgeräumt wird.
- Läuft **einmal pro Periode** — danach `billing_periods.year_end_reminder_sent_at = now()`.
- Keine aktive Periode → Welle 2 entfällt (Welle 1 läuft unabhängig weiter).

### Empfänger-Auflösung

E-Mail-Adresse + Vorname über `UserModuleApi`. Jobboard-Admin-Empfänger = Nutzer mit Rolle JOBBOARD_ADMIN / SECTION_ADMIN / SUPERADMIN (über `UserModuleApi`-Rollen-Lookup). Bestätiger-Kandidaten = Lehrer/Section-/Jobboard-Admins (analog `JobboardController.confirmAssignment`-Berechtigung).

---

## Datenmodell

**Flyway-Migration `V118__job_reminders.sql`** (3 nullable Spalten, reines Idempotenz-Tracking — keine bestehende Migration ändern):

```sql
ALTER TABLE job_assignments ADD COLUMN overdue_reminder_sent_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE jobs            ADD COLUMN overdue_reminder_sent_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE billing_periods ADD COLUMN year_end_reminder_sent_at TIMESTAMP WITH TIME ZONE;
```

Entsprechende Felder auf `Job`, `JobAssignment`, `BillingPeriod` (Lombok-Entities). Hibernate `ddl-auto: validate` → Migration zwingend.

## Edge Cases / Fehlerbehandlung

- **E-Mail deaktiviert** (`monteweb.email.enabled=false`, Default) → nur In-App + Log-Hinweis. Für garantierte Mails muss der Flag aktiv sein.
- **Keine aktive Periode** → Stundenkonto fällt defensiv auf all-time zurück; Welle-2-Reminder entfällt.
- **Job mit gemischten Assignments** → pro Assignment routen; „verwaist" nur wenn null aktive Assignments.
- **`hoursExempt`-Familien** → werden trotzdem erinnert (es geht ums Erledigen der Arbeit, nicht ums Stunden-Soll).
- **Scheduler-Neustart mittendrin** → Tasks idempotent (Flag-getrieben), abgebrochene Läufe wiederholen sich am Folgetag gefahrlos.
- **Modularität:** ausschließlich Facades (`NotificationModuleApi`, `UserModuleApi`, `CleaningModuleApi`, `AdminModuleApi`) — `MonteWebModularityTests` muss grün bleiben.

## Tests

**Backend:**
- `JobReminderServiceTest` (gemockte Repos/Facades + injizierte `Clock`): Auswahl-Kriterien (28-Tage-Schwelle, Termin null), Empfänger-Routing je Zustand, Idempotenz-Flags (kein Doppelversand), Jahresende-Fenster & Einmaligkeit pro Periode, E-Mail-aus-Pfad.
- Integrationstest für period-scoped Endpoints (`/jobs/school-years`, `/jobs/family/{id}/hours?periodId=`, `…/assignments?periodId=`): Default = aktive Periode, korrektes Range-Filtering.
- `MonteWebModularityTests` bleibt grün.
- CI-Hinweis: geteilte Postgres — keine Seed-Daten ohne `@AfterEach`-Cleanup mutieren.

**Frontend (Vitest):**
- `SchoolYearSelect`-Komponente (Optionen, Default aktive Periode, Emit).
- Store-Actions mit `periodId`.
- `FamilyHoursWidget` zeigt period-scoped Stunden; `AdminJobReport` Jahreswechsel.
- `vi.mock` der API-Module vollständig (Coverage-Lauf strenger als `npm test`).

## Offene Punkte für den Implementierungsplan

- Genaue gemeinsame Hilfsmethode für „Familien-Stunden im Zeitraum" (Wiederverwendung zwischen `BillingService.generateReport` und neuem `getFamilyHours(periodId)`) festlegen, um Logik-Duplikat zu vermeiden.
- Exakte Rollen-Lookup-Methoden auf `UserModuleApi` für Admin-/Bestätiger-Empfänger verifizieren.
