# Plankonzept: Modulares Schul-Intranet „EduHub"

> **Version:** 0.2 – Aktualisiert
> **Zielgruppe:** Montessori-Schulkomplexe (Krippe → Oberstufe)
> **Deployment:** Self-Hosted (Docker)
> **Client:** Responsive Web-App (PWA)
> **Tech-Stack:** Java 21 + Spring Boot 3 (Backend), Vue 3 + TypeScript (Frontend), PostgreSQL, Redis

---

## 1. Vision & Projektziele

Das System bildet einen digitalen Schulkomplex ab, in dem Krippe, Kindergarten, Grundschule, Mittelschule und Oberstufe unter einem Dach organisiert werden — mit gemeinsamer Nutzerbasis, aber individuell konfigurierbaren Bereichen. Andere Montessori-Schulen sollen das System eigenständig betreiben können, mit eigenem Branding und eigener Konfiguration.

### Kernziele

- **Modularität:** Schulbereiche und Features lassen sich per Konfiguration aktivieren/deaktivieren.
- **Mandantenfähig im Branding:** Jede Instanz ist visuell anpassbar (Logo, Farben, Bezeichnungen).
- **Robustheit:** Das System muss für 1.000+ aktive Nutzer stabil laufen, auf einem einzelnen Server.
- **Einfache Betreibbarkeit:** Docker-Compose-Setup, das ein technisch versierter Schuladmin deployen kann.
- **Datenschutz:** DSGVO-konform, alle Daten bleiben auf dem eigenen Server.

---

## 2. Nutzerrollen & Berechtigungsmodell

### 2.1 Globale Rollen

| Rolle | Beschreibung |
|---|---|
| **Superadmin** | Systemweite Konfiguration, Modulverwaltung, Schulbereiche anlegen |
| **Bereichsadmin** | Verwaltet einen Schulbereich (z. B. „Grundschule"), kann Räume/Gruppen anlegen |
| **Lehrer / Betreuer** | Kann Räume leiten, Inhalte erstellen, mit Eltern/Kindern kommunizieren |
| **Elternteil** | Zugang zu Elternbereichen der Räume ihrer Kinder, Jobbörse, Putz-Orga |
| **Schüler / Kind** | Zugang zu eigenen Klassenräumen (ab Grundschule sinnvoll, konfigurierbar) |

### 2.2 Raumbezogene Rollen

Jeder „Raum" (Klasse, Gruppe, Projekt) hat eigene Rollen:

- **Raumleiter** (Lehrer, Betreuer, oder auch Elternteil bei Projekten)
- **Mitglied** (Schüler, Kind)
- **Eltern-Mitglied** (automatisch verknüpft über Kind)
- **Gast** (Lesezugriff, z. B. Schulleitung)

### 2.3 Familienverbund

Ein **Familienverbund** ist eine zentrale Entität im System. Jeder Benutzer mit der Rolle Elternteil kann einen Familienverbund gründen oder einem bestehenden beitreten.

```
Familienverbund
├── Name: "Familie Müller"
├── Eltern: 1–4 Erwachsene (Elternteile, Erziehungsberechtigte)
├── Kinder: [User-IDs] (Schüler/Kinder an der Schule)
├── Stundenkonto: gemeinsames Soll/Ist für Elternstunden.
├── Einladung: per Code oder durch Admin-Verknüpfung
└── Schuljahr-Bindung: Stundenkonto wird pro Schuljahr geführt
```

**Regeln:**
- Ein Elternteil gehört zu genau einem Familienverbund.
- Ein Kind kann mehreren Familienverbünden zugeordnet sein (z. B. getrennte Eltern).
- Alle Eltern im Verbund sehen aggregiert alle Räume aller Kinder des Verbunds.
- Das Stundenkonto (Jobbörse + Putz-Orga) wird pro Familienverbund geführt, nicht pro Person.
- Geleistete Stunden eines Elternteils zählen für den gesamten Verbund.
- Innerhalb des Familienverbunds gibt es einen privaten Nachrichtenbereich (optional) für interne Abstimmung.
- Putzstunden sind Teil der Elternstunden, haben aber einen eigenen Mindestanteil und werden gesondert ausgewiesen und abgerechnet.

**Workflow Familienverbund erstellen:**
1. Elternteil A registriert sich → erstellt Familienverbund.
2. Elternteil A generiert Einladungscode (oder Admin verknüpft manuell).
3. Elternteil B registriert sich → tritt per Code bei.
4. Admin oder Elternteil verknüpft Kinder mit dem Verbund.

---

## 3. Modularer Aufbau

Das System besteht aus einem Kern und optionalen Modulen. Jedes Modul kann per Konfiguration (`modules.enabled`) aktiviert werden.

### 3.1 Kern (immer aktiv)

| Komponente | Funktion |
|---|---|
| **User Management** | Registrierung, Profilverwaltung, Familienverbund |
| **Auth** | Login (lokal + optional LDAP/OIDC/SSO), Passwort-Reset, 2FA |
| **Schulbereiche** | Konfigurierbare Bereiche (Krippe, KiGa, GS, MS, OS …) |
| **Räume** | Generischer Container für Klassen, Gruppen, Kurse |
| **Feed** | Aggregierter Nachrichtenstrom (Raum-Posts + allgemeine Nachrichten) |
| **Benachrichtigungen** | In-App + E-Mail, Push (via PWA) |
| **Admin-Panel** | Systemkonfiguration, Modulsteuerung, Branding |

### 3.2 Optionale Module

| Modul | Beschreibung | Abschnitt |
|---|---|---|
| **Messaging** | Direktnachrichten & Gruppenchat | §5 |
| **Dateiablage** | Dokumente/Unterlagen pro Raum | §4.2 |
| **Jobbörse** | Elternstunden-Tracking & Aufgabenvermittlung | §6 |
| **Putz-Orga** | Opt-in Putzdienst, QR-Check-in, Terminplanung | §7 |
| **Interessensräume** | Nachmittagsbetreuung, AGs, Projekte | §4.3 |
| **Kalender** | Termine pro Raum und schulweit | (Ausbaustufe) |
| **Formulare / Umfragen** | Einverständniserklärungen, Feedback | (Ausbaustufe) |

---

## 4. Räume – Das zentrale Konzept

### 4.1 Der generische Raum

Ein **Raum** ist der zentrale Baustein. „Klassenraum", „Kindergartengruppe", „AG Holzwerkstatt" und „Nachmittagsbetreuung" sind alles Räume mit unterschiedlicher Konfiguration.

```
Raum
├── Typ: KLASSE | GRUPPE | PROJEKT | INTEREST | CUSTOM
├── Schulbereich: Grundschule (optional, kann bereichsübergreifend sein)
├── Leiter: [User-IDs] (Lehrer, Betreuer, oder auch Eltern)
├── Mitglieder: [User-IDs]
├── Eltern-Space: aktiviert/deaktiviert
├── Chat: aktiviert/deaktiviert
├── Dateiablage: aktiviert/deaktiviert
├── Sichtbarkeit: PRIVAT | SCHULBEREICH | SCHULWEIT
└── Zeitraum: Schuljahr 2025/26 (archivierbar)
```

### 4.2 Raumfunktionen

Jeder Raum hat (je nach Konfiguration):

**Info-Board / Posts:** Pinnwand für Ankündigungen des Raumleiters. Eltern sehen den Eltern-Space, Schüler den Schüler-Space. Beiträge können Dateien enthalten und als „wichtig" markiert werden. Neue Posts erscheinen automatisch im Feed der Raummitglieder.

**Dateiablage:** Strukturierte Ordner für Unterlagen, Arbeitsblätter, Fotos. Upload durch Raumleiter, Download durch Mitglieder. Eltern-Space hat separate Dateiablage.

**Mitgliederliste:** Übersicht aller Mitglieder mit Rolle. Raumleiter können Mitglieder verwalten.

### 4.3 Interessensräume & Projekte

Interessensräume unterscheiden sich von Klassen:

- Können **bereichsübergreifend** sein (Kinder aus GS + MS gemeinsam).
- **Leitung** muss kein Lehrer sein (z. B. Elternteil leitet „Koch-AG").
- Haben einen eigenen Typ `PROJEKT` oder `INTEREST`.
- Können zeitlich begrenzt sein (Projektwochen).

---

## 5. Feed-System

### 5.1 Konzept

Der Feed ist die zentrale Startseite für jeden Nutzer. Er aggregiert chronologisch alle relevanten Inhalte und ersetzt das bisherige „Schwarze Brett"-Konzept als generischeres System.

### 5.2 Feed-Quellen

| Quelle | Sichtbar für | Beispiel |
|---|---|---|
| **Raum-Posts** | Mitglieder des Raums (rollenabhängig: Eltern-Space nur für Eltern) | „Klasse 3a: Ausflug am Freitag" |
| **Schulbereichs-Nachrichten** | Alle Mitglieder eines Schulbereichs | „Grundschule: Elternsprechtag am 20.03." |
| **Schulweite Nachrichten** | Alle Nutzer der Instanz | „Schulverwaltung: Ferienkalender aktualisiert" |
| **Elternbeirat** | Alle Eltern (oder konfigurierbar) | „Elternbeirat: Protokoll der letzten Sitzung" |
| **System-Banner** | Kontextabhängig (z. B. nur für Nicht-Eingetragene) | „⚠️ Putztermin GS am 15.03. — noch 2 Helfer gesucht!" |
| **Jobbörse-Highlights** | Alle berechtigten Nutzer | „Neuer Job: Gartenarbeit Schulhof (2h)" |

### 5.3 Post-Modell

```
FeedPost
├── id: UUID
├── source_type: ROOM | SECTION | SCHOOL | BOARD | SYSTEM
├── source_id: Raum-ID / Schulbereich-ID / null
├── author_id: User-ID
├── title: "Ausflug am Freitag"
├── content: Rich-Text (Markdown oder HTML)
├── attachments: [Datei-IDs]
├── target_audience: ALL | PARENTS | STUDENTS | TEACHERS
├── is_pinned: boolean
├── is_important: boolean (visuelle Hervorhebung)
├── visibility_start / visibility_end: optional (zeitgesteuerte Posts)
├── created_at, updated_at
└── comments_enabled: boolean
```

### 5.4 Wer darf posten?

| Ebene | Berechtigung |
|---|---|
| Raum-Post | Raumleiter (Standard), Mitglieder (konfigurierbar) |
| Schulbereichs-Nachricht | Bereichsadmin, Schulleitung |
| Schulweite Nachricht | Superadmin, Schulverwaltung |
| Elternbeirat | Nutzer mit Rolle „Elternbeirat" (spezielle Rolle, zuweisbar durch Admin) |

### 5.5 Spezielle Rollen für Feed-Quellen

Neben den globalen Rollen gibt es zuweisbare Feed-Rollen:

- **Elternbeirat:** Kann schulweite oder bereichsweite Posts im Namen des Elternbeirats veröffentlichen.
- **Schulverwaltung:** Kann offizielle Mitteilungen posten.

Diese Rollen werden vom Superadmin oder Bereichsadmin an einzelne Nutzer vergeben.

### 5.6 System-Banner

System-Banner sind besondere Feed-Einträge, die automatisch generiert werden und prominent am Kopf des Feeds erscheinen. Sie werden kontextabhängig ein-/ausgeblendet:

- **Putz-Banner:** „Putztermin [Schuleinheit] am [Datum] — noch [n] Helfer gesucht!" → Wird nur angezeigt, wenn Minimum nicht erreicht UND der Nutzer nicht bereits eingetragen ist.
- **Stunden-Erinnerung:** „Deine Familie hat erst [x] von 30 Stunden geleistet." → Periodisch, konfigurierbar.
- **Systemmeldungen:** Wartungsfenster, Updates.

---

## 6. Kommunikation & Messaging

### 6.1 Kommunikationsregeln

Die Kommunikation folgt strikten Regeln, die per Konfiguration angepasst werden können:

| Von → An | Erlaubt | Konfigurierbar |
|---|---|---|
| Lehrer → Eltern | ✅ Immer | — |
| Lehrer → Schüler | ✅ Standard an | Abschaltbar |
| Eltern → Lehrer | ✅ Immer | — |
| Schüler → Lehrer | ✅ Standard an | Abschaltbar |
| Eltern ↔ Eltern | ❌ Standard aus | Einschaltbar |
| Schüler ↔ Schüler | ❌ Standard aus | Einschaltbar |
| Familienverbund intern | ✅ Immer | — |

### 6.2 Direktnachrichten

Leichtgewichtiges, internes Messaging-System (kein externer Dienst nötig). Funktionen: Text, Dateianhänge, Lesebestätigung (optional), Archiv.

Begründung gegen externe Integration (Matrix/Rocket.Chat): Für eine Schulumgebung mit klaren Kommunikationsregeln ist ein eigenes, schlankes System besser kontrollierbar und einfacher zu betreiben als ein eingebetteter Chat-Server.

### 6.3 Gruppenchat (optional pro Raum)

Wenn aktiviert, gibt es pro Raum einen Chat — getrennt nach Schüler-Chat und Eltern-Chat. Raumleiter sind in beiden. Moderationsfunktionen (Beiträge löschen, Nutzer stumm schalten) für Raumleiter.

---

## 7. Jobbörse – Elternstunden

### 7.1 Konzept

Montessori-Schulen erfordern typischerweise ~30 Arbeitsstunden pro Familienverbund und Schuljahr. Die Jobbörse organisiert das Angebot und die Abrechnung dieser Stunden.

### 7.2 Datenmodell Job

```
Job
├── Titel: "Gartenarbeit Schulhof"
├── Beschreibung: Freitext + Bilder
├── Kategorie: GARTEN | HANDWERK | BÜRO | VERANSTALTUNG | PUTZEN | CUSTOM
├── Ort: Freitext oder vordefinierte Orte
├── Ersteller: User-ID (Lehrer, Elternteil, Admin)
├── Geplante Dauer: 2h
├── Datum/Zeitfenster: 15.03.2026, 09:00–11:00
├── Max. Teilnehmer: 4
├── Status: OFFEN | ZUGEWIESEN | IN_ARBEIT | ERLEDIGT | ABGESAGT
├── Zugewiesene Personen: [User-IDs]
└── Kategorie-Flag: is_cleaning (für Putz-Sonderlogik)
```

### 7.3 Workflow

1. **Erstellen:** Lehrer/Admin/berechtigtes Elternteil erstellt Job.
2. **Annehmen:** Eltern melden sich auf offene Jobs. Stunden zählen für den Familienverbund.
3. **Durchführen:** Am Tag des Jobs → optional QR-Check-in.
4. **Abschließen:** Ersteller oder Ausführender markiert als erledigt. Tatsächliche Dauer kann angepasst werden.
5. **Bestätigen:** Raumleiter/Admin bestätigt geleistete Stunden.

### 7.4 Stunden-Reporting

Pro **Familienverbund** wird ein Jahresbericht generiert:

- Geleistete Stunden gesamt (alle Mitglieder des Verbunds summiert)
- Aufschlüsselung nach Kategorie (Putzen, Garten, etc.)
- Aufschlüsselung nach Person innerhalb des Verbunds
- Soll/Ist-Vergleich (Ziel: 30h, konfigurierbar pro Instanz)
- Zeitlicher Verlauf (Quartale)
- Exportierbar als PDF / CSV

Für die Schulleitung:

- Übersicht aller Familienverbünde mit Stunden-Status
- Ampelsystem (grün ≥ 30h / gelb 15–29h / rot < 15h, Schwellen konfigurierbar)
- Filter nach Schulbereich / Klasse
- Familienverbünde ohne geleistete Stunden hervorheben

---

## 8. Putz-Orga – Spezialmodul

### 8.1 Konzept: Opt-in-System

Die Putz-Orga basiert auf einem **freiwilligen Anmeldesystem** (Opt-in), nicht auf einer festen Rotation. Für jeden Putztermin melden sich Eltern selbstständig an. Das System steuert über Minimum/Maximum-Grenzen und Feed-Banner.

### 8.2 Konfiguration pro Schuleinheit

Der Admin konfiguriert die Putz-Orga **pro Schuleinheit** (Schulbereich), nicht pro Raum:

```
CleaningConfig (pro Schuleinheit)
├── school_section_id: Grundschule
├── is_active: true
├── recurrence: MONTHLY | BIWEEKLY | CUSTOM
├── default_weekday: FRIDAY
├── default_time_start: 15:00
├── default_time_end: 17:00
├── min_participants: 3 (Minimum — darunter erscheint Banner)
├── max_participants: 6 (Maximum — danach ist der Termin voll)
├── hours_credited: 2.0 (Stunden, die auf Jobbörse gutgeschrieben werden)
├── qr_checkin_required: true
└── reminder_days_before: [7, 3, 1] (Erinnerungen vor dem Termin)
```

### 8.3 Putztermine & Opt-in-Workflow

**Termin-Erstellung:**
- Das System generiert automatisch Termine basierend auf der Recurrence-Regel.
- Admins können einzelne Termine manuell anpassen, verschieben oder absagen.
- Jeder Termin ist ein eigenes Objekt mit Status.

```
CleaningSlot
├── id: UUID
├── school_section_id: Grundschule
├── date: 2026-03-15
├── time_start: 15:00
├── time_end: 17:00
├── min_participants: 3 (geerbt oder überschrieben)
├── max_participants: 6 (geerbt oder überschrieben)
├── status: OPEN | FULL | COMPLETED | CANCELLED
├── qr_code_token: signierter Token (rotiert)
├── registrations: [CleaningRegistration]
└── created_job_id: → verknüpfter Job in der Jobbörse
```

**Opt-in-Workflow:**
1. Nutzer sieht im Feed oder in der Putz-Orga-Sektion offene Termine.
2. Nutzer meldet sich für einen Termin an → `CleaningRegistration` wird erstellt.
3. Solange `registrations.count < min_participants` → Banner im Feed für alle Eltern der Schuleinheit (außer bereits Eingetragene).
4. Bei `registrations.count == max_participants` → Termin ist voll, keine weiteren Anmeldungen.
5. Abmeldung ist möglich bis X Tage vor dem Termin (konfigurierbar). Danach nur per Tausch.

```
CleaningRegistration
├── slot_id: → CleaningSlot
├── user_id: → User (das eingetragene Elternteil)
├── family_id: → Familienverbund (Stunden zählen für den Verbund)
├── status: REGISTERED | CHECKED_IN | COMPLETED | CANCELLED | NO_SHOW
├── registered_at: Timestamp
├── checked_in_at: Timestamp (QR-Check-in)
├── checked_out_at: Timestamp
└── actual_duration_minutes: int (falls abweichend)
```

### 8.4 Feed-Banner-Logik

Das Banner im allgemeinen Feed folgt diesen Regeln:

```
FÜR JEDEN offenen Putztermin der nächsten 14 Tage:
  WENN registrations.count < min_participants:
    FÜR JEDEN Eltern-User in dieser Schuleinheit:
      WENN User NICHT bereits für diesen Termin eingetragen:
        → Zeige Banner: "⚠️ Putzdienst [Schuleinheit] am [Datum]
           — noch [min - count] Helfer gesucht!"
        → Banner enthält direkten "Anmelden"-Button
  WENN registrations.count >= min_participants UND < max_participants:
    → Optionales, dezentes Banner: "Putzdienst am [Datum] — 
       noch [max - count] Plätze frei" (konfigurierbar ob angezeigt)
  WENN registrations.count >= max_participants:
    → Kein Banner
```

### 8.5 Tausch & Absage

- **Tausch anbieten:** Eingetragenes Elternteil kann Termin zum Tausch anbieten → erscheint im Feed.
- **Absage:** Möglich bis zur konfigurierten Frist. Danach nur Tausch. Raumleiter/Admin kann jederzeit ändern.
- **Automatischer Nachrücker:** Wenn jemand absagt und es eine Warteliste gibt (optional), rückt der nächste nach.

### 8.6 QR-Check-in

Am Putz-Standort hängt ein QR-Code (wird vom System generiert, druckbar als PDF/A4).

**Ablauf:**
1. Elternteil öffnet Web-App auf dem Handy.
2. Scannt QR-Code → „Putzdienst einchecken".
3. System erfasst: Wer, Wann, Wo. Der QR-Code enthält eine signierte URL mit Slot-ID + Token.
4. Nach dem Putzen: Auschecken → tatsächliche Dauer wird erfasst.
5. Stunden fließen automatisch in die Jobbörse für den Familienverbund.

**Technisch:** Die Web-App nutzt die Kamera-API des Browsers (kein nativer Scanner nötig). Der QR-Code-Token rotiert pro Termin zur Sicherheit.

### 8.7 Putz-Dashboard

**Für Eltern:**
- Meine nächsten Putztermine
- Offene Termine zum Anmelden
- Vergangene Einsätze

**Für Admin / Schulleitung:**
- Übersicht aller Termine mit Belegungsstatus (Ampel: rot/gelb/grün)
- Anwesenheitsliste pro Termin
- No-Show-Tracking
- Jahresstatistik: Wie oft hat welcher Familienverbund geputzt?

---

## 9. Technische Architektur

### 9.1 Architekturentscheidung: Modularer Monolith

Kein Microservice-Setup — für eine Self-Hosted-Schulsoftware wäre das Overkill. Stattdessen ein **modularer Monolith**: eine Applikation, intern sauber in Module getrennt, aber als ein Artefakt deploybar.

### 9.2 Tech-Stack (entschieden)

**Backend: Java 21 + Spring Boot 3**
- Spring Modulith für saubere Modultrennung
- Spring Security für Auth (OIDC, LDAP, lokale Accounts)
- Spring Data JPA + Flyway für Datenbank-Migrationen
- Spring WebSocket + Redis Pub/Sub für Echtzeit (Chat, Notifications)

**Frontend: Vue 3 + TypeScript + Vite**
- Pinia für State Management
- Vue Router für Navigation
- Component Library: PrimeVue oder Naive UI (gut thembar)
- PWA via Workbox (Offline-Support, Push-Notifications)

**Infrastruktur:**
- PostgreSQL 16 (Hauptdatenbank)
- Redis 7 (Sessions, Cache, WebSocket-Pub/Sub, Feed-Caching)
- MinIO (S3-kompatibler Dateispeicher)
- nginx (Reverse Proxy, SSL-Termination, Static Files)
- Docker Compose (gesamtes Setup)

### 9.3 Systemarchitektur

```
┌─────────────────────────────────────────────────────────────┐
│                    nginx (Reverse Proxy)                     │
│                  SSL / Static Files / PWA                    │
├─────────────────────────┬───────────────────────────────────┤
│                         │                                   │
│   Frontend (SPA)        │       Backend (API)               │
│   Vue 3 + TypeScript    │       Spring Boot 3 (Java 21)     │
│   Vite + PWA            │       Spring Modulith             │
│                         │                                   │
│  ┌───────────────────┐  │  ┌─────────────────────────────┐  │
│  │ Responsive UI     │  │  │ Module:                     │  │
│  │ Theming-Engine    │  │  │  ├─ core-auth               │  │
│  │ (CSS Variables)   │  │  │  ├─ core-users              │  │
│  │ Offline-Cache     │  │  │  ├─ core-family             │  │
│  │ Camera API (QR)   │  │  │  ├─ core-rooms              │  │
│  └───────────────────┘  │  │  ├─ core-feed               │  │
│                         │  │  ├─ core-notifications       │  │
│                         │  │  ├─ mod-messaging            │  │
│                         │  │  ├─ mod-files                │  │
│                         │  │  ├─ mod-jobboard             │  │
│                         │  │  ├─ mod-cleaning             │  │
│                         │  │  └─ mod-calendar             │  │
│                         │  ├─────────────────────────────┤  │
│                         │  │ PostgreSQL 16               │  │
│                         │  │ Redis 7                     │  │
│                         │  │ MinIO (Dateispeicher)       │  │
│                         │  └─────────────────────────────┘  │
└─────────────────────────┴───────────────────────────────────┘
```

### 9.4 Theming & Branding

Jede Schulinstanz kann anpassen:

- **Logo & Favicon** (Upload im Admin-Panel)
- **Farbschema** (Primary, Secondary, Accent — per CSS Custom Properties)
- **Bezeichnungen** (i18n-Keys: „Klasse" ↔ „Gruppe" ↔ „Lerngruppe")
- **Startseite** (welche Widgets, welche Reihenfolge)
- **E-Mail-Templates** (Logo, Footer-Text)

Technisch umgesetzt über ein `theme.json`, das beim Laden der App dynamisch CSS-Variablen setzt. Kein Rebuild nötig.

### 9.5 Authentifizierung

```
┌───────────────────────────────────────┐
│         Auth-Modul (core-auth)        │
├───────────────────────────────────────┤
│                                       │
│  Provider (konfigurierbar):           │
│  ├─ Lokal (Username/Passwort + 2FA)  │
│  ├─ LDAP / Active Directory          │
│  ├─ OIDC (Keycloak, Google, etc.)    │
│  └─ SAML (für größere Träger)        │
│                                       │
│  Features:                            │
│  ├─ Einladungslinks (Admin lädt ein) │
│  ├─ Self-Registration (abschaltbar)  │
│  ├─ Passwort-Reset per E-Mail        │
│  ├─ Session-Management (JWT + Redis) │
│  └─ Familienverbund-Einladung        │
│      (Code-basiert oder manuell)     │
└───────────────────────────────────────┘
```

### 9.6 Datenbank-Schema (Kernentitäten)

```sql
-- Mandantenkonfiguration (eine Instanz = ein Tenant)
tenant_config (
    id UUID PRIMARY KEY,
    name VARCHAR,
    logo_url VARCHAR,
    theme_json JSONB,
    modules_enabled TEXT[],
    target_hours_per_family INT DEFAULT 30,
    ...
)

-- Schulbereiche
school_sections (
    id UUID PRIMARY KEY,
    name VARCHAR,
    slug VARCHAR UNIQUE,
    sort_order INT,
    is_active BOOLEAN DEFAULT TRUE
)

-- Benutzer
users (
    id UUID PRIMARY KEY,
    email VARCHAR UNIQUE,
    password_hash VARCHAR,
    first_name VARCHAR,
    last_name VARCHAR,
    role VARCHAR, -- SUPERADMIN, SECTION_ADMIN, TEACHER, PARENT, STUDENT
    special_roles TEXT[], -- ELTERNBEIRAT, SCHULVERWALTUNG (zusätzlich)
    auth_provider VARCHAR,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP
)

-- Familienverbund
families (
    id UUID PRIMARY KEY,
    name VARCHAR, -- "Familie Müller"
    invite_code VARCHAR UNIQUE,
    created_at TIMESTAMP,
    school_year VARCHAR -- "2025/26"
)

-- Familienverbund-Mitgliedschaft
family_members (
    family_id UUID REFERENCES families,
    user_id UUID REFERENCES users,
    role VARCHAR, -- PARENT | CHILD
    joined_at TIMESTAMP,
    PRIMARY KEY (family_id, user_id)
)

-- Räume
rooms (
    id UUID PRIMARY KEY,
    name VARCHAR,
    type VARCHAR, -- KLASSE, GRUPPE, PROJEKT, INTEREST, CUSTOM
    school_section_id UUID REFERENCES school_sections,
    school_year VARCHAR,
    settings JSONB, -- {chat_enabled, files_enabled, parent_space_enabled, ...}
    is_archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP
)

-- Raum-Mitgliedschaft
room_members (
    room_id UUID REFERENCES rooms,
    user_id UUID REFERENCES users,
    role VARCHAR, -- LEADER, MEMBER, PARENT_MEMBER, GUEST
    joined_at TIMESTAMP,
    PRIMARY KEY (room_id, user_id)
)

-- Feed-Posts (Vereinheitlicht: Raum-Posts, Bereichs- und Schulnachrichten)
feed_posts (
    id UUID PRIMARY KEY,
    source_type VARCHAR, -- ROOM, SECTION, SCHOOL, BOARD, SYSTEM
    source_id UUID, -- room_id oder section_id oder NULL
    author_id UUID REFERENCES users,
    author_role VARCHAR, -- für Anzeige: "Elternbeirat", "Schulverwaltung"
    title VARCHAR,
    content TEXT,
    target_audience VARCHAR, -- ALL, PARENTS, STUDENTS, TEACHERS
    is_pinned BOOLEAN DEFAULT FALSE,
    is_important BOOLEAN DEFAULT FALSE,
    visibility_start TIMESTAMP,
    visibility_end TIMESTAMP,
    comments_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
)

-- Feed-Post-Anhänge
feed_post_attachments (
    id UUID PRIMARY KEY,
    post_id UUID REFERENCES feed_posts,
    file_name VARCHAR,
    file_path VARCHAR,
    mime_type VARCHAR,
    size_bytes BIGINT
)

-- Feed-Post-Kommentare
feed_post_comments (
    id UUID PRIMARY KEY,
    post_id UUID REFERENCES feed_posts,
    author_id UUID REFERENCES users,
    content TEXT,
    created_at TIMESTAMP
)

-- Nachrichten (Direktnachrichten)
conversations (
    id UUID PRIMARY KEY,
    type VARCHAR, -- DIRECT, GROUP, FAMILY
    room_id UUID REFERENCES rooms, -- NULL bei Direktnachrichten
    created_at TIMESTAMP
)

conversation_members (
    conversation_id UUID REFERENCES conversations,
    user_id UUID REFERENCES users,
    last_read_at TIMESTAMP,
    PRIMARY KEY (conversation_id, user_id)
)

messages (
    id UUID PRIMARY KEY,
    conversation_id UUID REFERENCES conversations,
    sender_id UUID REFERENCES users,
    content TEXT,
    sent_at TIMESTAMP,
    edited_at TIMESTAMP
)

-- Jobs (Elternstunden)
jobs (
    id UUID PRIMARY KEY,
    title VARCHAR,
    description TEXT,
    category VARCHAR, -- GARTEN, HANDWERK, BUERO, VERANSTALTUNG, PUTZEN, CUSTOM
    location VARCHAR,
    creator_id UUID REFERENCES users,
    planned_duration_minutes INT,
    scheduled_at TIMESTAMP,
    max_participants INT,
    status VARCHAR, -- OPEN, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED
    is_cleaning BOOLEAN DEFAULT FALSE,
    cleaning_slot_id UUID REFERENCES cleaning_slots, -- Verknüpfung wenn Putz-Job
    created_at TIMESTAMP
)

job_assignments (
    id UUID PRIMARY KEY,
    job_id UUID REFERENCES jobs,
    user_id UUID REFERENCES users,
    family_id UUID REFERENCES families, -- Stunden zählen für diesen Verbund
    status VARCHAR, -- ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED
    actual_duration_minutes INT,
    checked_in_at TIMESTAMP,
    checked_out_at TIMESTAMP,
    confirmed_by UUID REFERENCES users,
    confirmed_at TIMESTAMP
)

-- Putz-Orga: Konfiguration pro Schuleinheit
cleaning_configs (
    id UUID PRIMARY KEY,
    school_section_id UUID REFERENCES school_sections,
    is_active BOOLEAN DEFAULT TRUE,
    recurrence VARCHAR, -- MONTHLY, BIWEEKLY, CUSTOM
    default_weekday INT, -- 1=MO, 5=FR
    default_time_start TIME,
    default_time_end TIME,
    min_participants INT DEFAULT 3,
    max_participants INT DEFAULT 6,
    hours_credited DECIMAL DEFAULT 2.0,
    qr_checkin_required BOOLEAN DEFAULT TRUE,
    reminder_days_before INT[] DEFAULT '{7,3,1}',
    cancellation_deadline_days INT DEFAULT 3
)

-- Putz-Orga: Einzeltermine
cleaning_slots (
    id UUID PRIMARY KEY,
    config_id UUID REFERENCES cleaning_configs,
    school_section_id UUID REFERENCES school_sections,
    date DATE,
    time_start TIME,
    time_end TIME,
    min_participants INT, -- Überschreibbar pro Termin
    max_participants INT,
    status VARCHAR, -- OPEN, FULL, COMPLETED, CANCELLED
    qr_code_token VARCHAR UNIQUE,
    notes TEXT,
    created_at TIMESTAMP
)

-- Putz-Orga: Anmeldungen (Opt-in)
cleaning_registrations (
    id UUID PRIMARY KEY,
    slot_id UUID REFERENCES cleaning_slots,
    user_id UUID REFERENCES users,
    family_id UUID REFERENCES families,
    status VARCHAR, -- REGISTERED, CHECKED_IN, COMPLETED, CANCELLED, NO_SHOW
    registered_at TIMESTAMP,
    checked_in_at TIMESTAMP,
    checked_out_at TIMESTAMP,
    actual_duration_minutes INT,
    UNIQUE (slot_id, user_id)
)

-- Benachrichtigungen
notifications (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users,
    type VARCHAR, -- MESSAGE, POST, JOB, CLEANING, SYSTEM
    title VARCHAR,
    body TEXT,
    link VARCHAR, -- Deep-Link in der App
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP
)
```

---

## 10. Deployment & Betrieb

### 10.1 Docker-Compose-Setup

```yaml
services:
  backend:
    image: eduhub/backend:latest
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/eduhub
      - SPRING_REDIS_HOST=redis
      - MINIO_ENDPOINT=http://minio:9000
      - MODULES_ENABLED=messaging,files,jobboard,cleaning
    depends_on: [db, redis, minio]
    restart: unless-stopped

  frontend:
    image: eduhub/frontend:latest
    restart: unless-stopped

  db:
    image: postgres:16-alpine
    volumes:
      - pgdata:/var/lib/postgresql/data
    environment:
      - POSTGRES_DB=eduhub
      - POSTGRES_USER=eduhub
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    restart: unless-stopped

  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    volumes:
      - filedata:/data
    environment:
      - MINIO_ROOT_USER=${MINIO_USER}
      - MINIO_ROOT_PASSWORD=${MINIO_PASSWORD}
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    ports:
      - "443:443"
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./certs:/etc/nginx/certs:ro
    depends_on: [backend, frontend]
    restart: unless-stopped

volumes:
  pgdata:
  filedata:
```

### 10.2 Systemanforderungen (für ~1.000 User)

- **CPU:** 4 Cores
- **RAM:** 8 GB
- **Storage:** 50 GB SSD (+ Dateispeicher je nach Nutzung)
- **Bandbreite:** Schulnetzwerk reicht, externer Zugriff über HTTPS
- **Backup:** Tägliches PostgreSQL-Dump + MinIO-Backup

### 10.3 Update-Strategie

- Semantic Versioning
- Datenbank-Migrationen via Flyway (Teil des Backend-Images)
- Update = `docker compose pull && docker compose up -d`
- Rollback via Image-Tags
- Changelog + Breaking-Change-Hinweise

---

## 11. Sicherheit & Datenschutz

### 11.1 DSGVO-Compliance

- Alle Daten bleiben auf dem Server der Schule
- Kein Tracking, keine externen Analytics
- Löschkonzept: Accounts + zugehörige Daten löschbar (Recht auf Löschung)
- Datenexport: Nutzer können ihre Daten als JSON/PDF exportieren (Recht auf Datenportabilität)
- Einwilligungsverwaltung: Opt-in für optionale Features (Chat, Push-Notifications)
- Verarbeitungsverzeichnis als Template mitgeliefert

### 11.2 Technische Sicherheit

- HTTPS everywhere (Let's Encrypt / eigenes Zertifikat)
- OWASP Top 10 beachten (SQL Injection, XSS, CSRF, etc.)
- Rate Limiting auf API-Ebene (Spring Boot + Bucket4j oder Redis-basiert)
- Datei-Uploads: Typ-Whitelist, Größenlimit, ClamAV optional
- Session-Timeout konfigurierbar
- Audit-Log für Admin-Aktionen
- QR-Tokens kryptographisch signiert (HMAC)

---

## 12. UI/UX-Konzept

### 12.1 Navigation & Feed

```
┌─────────────────────────────────────────────────┐
│  [Logo]  EduHub              🔔  👤  ☰        │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌─────────────────────────────────────────┐    │
│  │ ⚠️ Putzdienst GS am 15.03. —           │    │
│  │    noch 2 Helfer gesucht! [Anmelden]   │    │
│  └─────────────────────────────────────────┘    │
│                                                 │
│  FEED                                           │
│  ┌──────────────────────────────────────┐       │
│  │ 📌 Schulverwaltung · vor 2h          │       │
│  │ Ferienkalender 2026/27 veröffentlicht│       │
│  ├──────────────────────────────────────┤       │
│  │ Klasse 3a · Eltern-Space · vor 5h    │       │
│  │ Ausflug am Freitag: Packliste        │       │
│  ├──────────────────────────────────────┤       │
│  │ Elternbeirat · gestern               │       │
│  │ Protokoll Sitzung 10.02.             │       │
│  ├──────────────────────────────────────┤       │
│  │ 🆕 Jobbörse · vor 1 Tag             │       │
│  │ Neuer Job: Gartenarbeit (2h)         │       │
│  └──────────────────────────────────────┘       │
│                                                 │
│  ── Navigation (Bottom Bar / Sidebar) ──        │
│  🏠 Feed | 📁 Räume | ✉️ Nachrichten |         │
│  🧹 Putzen | 💼 Jobs | ⚙️ Admin                │
└─────────────────────────────────────────────────┘
```

### 12.2 Mobile-First

Da die meisten Eltern die App am Handy nutzen (Putz-Check-in, schnelle Nachrichten), wird mobile-first designed. PWA ermöglicht „App-Feeling" ohne App-Store, inklusive Push-Notifications und Offline-Lesemodus.

### 12.3 Barrierefreiheit

- WCAG 2.1 AA-konform
- Screenreader-kompatibel
- Tastaturnavigation
- Kontrastreiche Farbwahl im Theming

---

## 13. Entwicklungsphasen

### Phase 1 — Fundament (8–10 Wochen)

- Projektsetup: Spring Boot + Vue 3 + Docker Compose
- CI/CD Pipeline
- Core: Auth (lokal + OIDC), User-Management, Rollen
- Core: Familienverbund (CRUD, Einladungscodes)
- Core: Schulbereiche, Räume (CRUD)
- Basis-UI: Login, Dashboard, Raum-Ansicht
- Admin-Panel: Grundkonfiguration, Modulsteuerung

### Phase 2 — Feed & Kommunikation (6–8 Wochen)

- Feed-System (Raum-Posts, Schulbereichs- und schulweite Nachrichten)
- Feed-Rollen (Elternbeirat, Schulverwaltung)
- System-Banner-Infrastruktur
- Dateiablage pro Raum (Upload, Download, Ordner)
- Direktnachrichten (1:1)
- Benachrichtigungen (In-App + E-Mail)
- PWA-Setup + Push-Notifications

### Phase 3 — Jobbörse (4–6 Wochen)

- Job-Erstellung, Annahme, Workflow
- Stunden-Tracking pro Familienverbund
- Bestätigungsworkflow
- Reporting (Familienverbund-Übersicht, Admin-Dashboard, Ampel)
- PDF/CSV-Export

### Phase 4 — Putz-Orga (4–6 Wochen)

- Cleaning-Config pro Schuleinheit
- Automatische Termin-Generierung
- Opt-in-Anmeldung + Feed-Banner-Integration
- QR-Code-System (Generierung, Druck-PDF, Check-in/out via Kamera-API)
- Tausch-System
- Integration mit Jobbörse (Stunden-Gutschrift)
- Putz-Dashboard (Eltern + Admin)

### Phase 5 — Interessensräume & Feinschliff (4 Wochen)

- Projekt-Räume, bereichsübergreifende Gruppen
- Gruppenchat (optional pro Raum)
- Theming-Engine & Branding-Konfiguration
- Performance-Optimierung & Lasttests

### Phase 6 — Hardening & Launch (2–4 Wochen)

- Security-Audit
- DSGVO-Dokumentation
- Benutzerhandbuch
- Installations-Doku für andere Schulen
- Pilotbetrieb am eigenen Schulkomplex

**Gesamtschätzung: ca. 28–38 Wochen** (bei 1–2 Vollzeit-Entwicklern)

---

## 14. Erweiterungsmöglichkeiten (Zukunft)

- **Kalender-Modul:** iCal-Sync, Termine pro Raum
- **Formulare & Umfragen:** Einverständniserklärungen digital, Feedback
- **Stundenplan-Anzeige:** Import aus bestehenden Systemen
- **Essensbestellung:** Mensa-/Catering-Integration
- **Bibliothek:** Bücher-Ausleihe
- **Marktplatz:** Second-Hand Schulbedarf unter Eltern
- **Multi-Sprache:** Vollständige i18n (Deutsch, Englisch, Italienisch, ...)
- **API für Drittanbieter:** Webhook-System für externe Integrationen
- **Native App (optional):** Capacitor Wrapper für App-Store-Präsenz

---

## 15. Offene Entscheidungen

| # | Frage | Optionen | Status |
|---|---|---|---|
| 1 | Backend-Sprache | ~~Java vs. Python~~ | ✅ Java 21 + Spring Boot 3 |
| 2 | Frontend-Framework | ~~React vs. Vue~~ | ✅ Vue 3 + TypeScript |
| 3 | Datei-Storage | Lokales FS vs. MinIO | MinIO empfohlen |
| 4 | Echtzeit-Kommunikation | WebSocket vs. SSE | WebSocket via Spring + Redis |
| 5 | Vue Component Library | PrimeVue vs. Naive UI vs. Vuetify | Offen |
| 6 | Projektname | EduHub, SchulNetz, MontNet, ... | Offen |
