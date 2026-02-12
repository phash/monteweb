# MonteWeb — Rollen & Berechtigungen

Dieses Dokument beschreibt alle Rollen im MonteWeb-System, ihre Fähigkeiten, Kommunikationswege und Zugriffsrechte auf die verschiedenen Module und Funktionen.

---

## Inhaltsverzeichnis

1. [Rollenhierarchie](#1-rollenhierarchie)
2. [Systemrollen (UserRole)](#2-systemrollen-userrole)
3. [Sonderrollen (Special Roles)](#3-sonderrollen-special-roles)
4. [Raum-Rollen (RoomRole)](#4-raum-rollen-roomrole)
5. [Familien-Rollen (FamilyMemberRole)](#5-familien-rollen-familymemberrole)
6. [Fotobox-Berechtigungsstufen](#6-fotobox-berechtigungsstufen)
7. [Kommunikationswege & Regeln](#7-kommunikationswege--regeln)
8. [Modulberechtigungen im Detail](#8-modulberechtigungen-im-detail)
9. [Gesamtübersicht (Matrix)](#9-gesamtübersicht-matrix)

---

## 1. Rollenhierarchie

MonteWeb verwendet ein mehrstufiges Berechtigungssystem:

```
Ebene 1: Systemrolle (UserRole)          — gilt systemweit
Ebene 2: Sonderrolle (Special Roles)     — zusätzliche Rechte, optional, ggf. bereichsbezogen
Ebene 3: Raum-Rolle (RoomRole)           — gilt pro Raum
Ebene 4: Familien-Rolle (FamilyMemberRole) — gilt innerhalb der Familie
Ebene 5: Modul-Berechtigungen (z.B. Fotobox) — gilt pro Raum/Modul
```

Die Systemrolle bestimmt die Grundrechte. Sonderrollen erweitern diese punktuell. Raum-Rollen regeln den Zugriff innerhalb einzelner Räume.

---

## 2. Systemrollen (UserRole)

Definiert in `backend/src/main/java/com/monteweb/user/UserRole.java`

| Rolle | Beschreibung | Typischer Nutzer |
|-------|-------------|-----------------|
| **SUPERADMIN** | Voller Systemzugriff. Kann alles konfigurieren, alle Daten einsehen und alle Aktionen durchführen. | Schulleitung, IT-Admin |
| **SECTION_ADMIN** | Verwaltungszugriff auf einen Schulbereich (z.B. Grundschule). Kann Putz-Orga und Admin-Funktionen für den eigenen Bereich nutzen. | Bereichsleitung |
| **TEACHER** | Pädagogisches Personal. Erhält in Klassenräumen automatisch die LEADER-Rolle. Kann Diskussionen, Events und Formulare erstellen. | Lehrkräfte, Erzieher |
| **PARENT** | Elternteil/Erziehungsberechtigter. Kann Familien verwalten, Jobs erstellen, Putztermine wahrnehmen. | Eltern |
| **STUDENT** | Schüler. Eingeschränkte Rechte, insbesondere bei Kommunikation und Jobbörse. | Schüler ab Mittelschule |

### Implizite Rechte nach Systemrolle

**SUPERADMIN:**
- Zugriff auf alle Admin-Endpoints (`/api/v1/admin/**`)
- Zugriff auf Actuator-Endpoints (`/actuator/**`)
- Kann alle Räume einsehen, archivieren und löschen
- Kann Benutzerrollen ändern und Sonderrollen zuweisen
- Kann Schulbereiche erstellen, bearbeiten und deaktivieren
- Kann schulweite Events und Formulare erstellen
- Kann Module aktivieren/deaktivieren
- Kann Theme und Logo konfigurieren
- Kann Audit-Log einsehen
- Kann Kommunikationsregeln konfigurieren

**SECTION_ADMIN:**
- Kann Putz-Orga für den eigenen Bereich verwalten
- Kann bereichsweite Events und Formulare erstellen
- Kann Nachrichten an alle Rollen senden
- Gleiche Messaging-Rechte wie TEACHER

**TEACHER:**
- Wird in KLASSE-Räumen automatisch als LEADER eingetragen
- Kann Diskussions-Threads in allen Räumen erstellen und verwalten (auch ohne LEADER-Rolle)
- Kann bereichsweite Events und Formulare erstellen
- Kann Nachrichten an alle Rollen senden
- Kann schulweite/bereichsweite Feed-Posts erstellen

**PARENT:**
- Kann Familien erstellen und verwalten
- Kann sich für Jobs und Putztermine anmelden
- Kann Diskussions-Threads erstellen (wenn in den Raum-Einstellungen erlaubt)
- Kann andere Eltern einladen (Familien-Einladungen)

**STUDENT:**
- Kann keine Jobs erstellen
- Kann sich nicht für Putztermine anmelden
- Kann nicht an Schüler senden (außer wenn aktiviert)
- Kann keine Familien erstellen
- Eingeschränkte Thread-Sichtbarkeit (nur ALLE, ggf. KINDER)

---

## 3. Sonderrollen (Special Roles)

Definiert als String-Array im User-Objekt (`User.specialRoles`).

Sonderrollen sind keine eigenständigen Rollen, sondern Zusatzberechtigungen, die einzelnen Nutzern zugewiesen werden. Sie können global oder bereichsbezogen (mit Section-ID) vergeben werden.

### ELTERNBEIRAT

**Format:** `ELTERNBEIRAT` (global) oder `ELTERNBEIRAT:{sectionId}` (bereichsbezogen)

| Bereich | Berechtigung |
|---------|-------------|
| Kalender/Events | Events erstellen und bearbeiten (alle Scopes) |
| Feed | Schulweite und bereichsweite Posts erstellen |
| Formulare | Formulare erstellen (alle Scopes) |
| Putz-Orga | Putztermine erstellen, bearbeiten, löschen; Admin-Dashboard einsehen |

### PUTZORGA

**Format:** `PUTZORGA:{sectionId}` (immer bereichsbezogen)

| Bereich | Berechtigung |
|---------|-------------|
| Putz-Orga | Putztermine erstellen, bearbeiten, löschen; Slots verwalten; QR-Codes generieren; Admin-Dashboard einsehen |

---

## 4. Raum-Rollen (RoomRole)

Definiert in `backend/src/main/java/com/monteweb/room/RoomRole.java`

Jeder Nutzer hat pro Raum, dem er angehört, eine Raum-Rolle.

| Rolle | Beschreibung |
|-------|-------------|
| **LEADER** | Raum-Verantwortlicher. Kann den Raum bearbeiten, Mitglieder verwalten, Diskussionen moderieren, Beitrittsanfragen genehmigen/ablehnen, Fotobox konfigurieren. |
| **MEMBER** | Standard-Mitglied (typisch: Schüler). Zugang zu allen Rauminhalten, Chat (MAIN + STUDENTS-Kanal). |
| **PARENT_MEMBER** | Eltern-Mitglied. Zugang zu Rauminhalten, Chat (MAIN + PARENTS-Kanal), sieht ELTERN-Threads. |
| **GUEST** | Eingeschränkter Zugang. Nur Lesezugriff. |

### Rechte pro Raum-Rolle

| Aktion | LEADER | MEMBER | PARENT_MEMBER | GUEST |
|--------|--------|--------|---------------|-------|
| Raum bearbeiten (Name, Beschreibung) | Ja | — | — | — |
| Raum-Einstellungen ändern | Ja | — | — | — |
| Avatar hochladen/löschen | Ja | — | — | — |
| Mitglieder hinzufügen/entfernen | Ja | — | — | — |
| Mitglieder-Rollen ändern | Ja | — | — | — |
| Beitrittsanfragen einsehen/genehmigen/ablehnen | Ja | — | — | — |
| Diskussions-Threads erstellen | Ja | Wenn erlaubt* | Wenn erlaubt* | — |
| Diskussions-Threads archivieren/löschen | Ja | — | — | — |
| Diskussions-Antworten schreiben | Ja | Ja** | Ja** | — |
| Feed-Posts im Raum erstellen | Ja | Ja | Ja | — |
| Chat MAIN-Kanal | Ja | Ja | Ja | — |
| Chat PARENTS-Kanal | Ja | — | Ja | — |
| Chat STUDENTS-Kanal | Ja | Ja | — | — |
| Fotobox-Einstellungen ändern | Ja | — | — | — |
| Fotobox-Threads erstellen | Ja | Konfig.*** | Konfig.*** | — |
| Fotobox-Bilder hochladen | Ja | Konfig.*** | Konfig.*** | — |
| Fotobox-Bilder ansehen | Ja | Ja | Ja | Konfig.*** |
| Raum-Events erstellen | Ja | — | — | — |
| Raum-Formulare erstellen | Ja | — | — | — |
| Raum-Dateien hochladen | Ja | Ja | Ja | — |

\* Nur wenn `allowMemberThreadCreation = true` in den Raum-Einstellungen und Nutzer ist PARENT.
\*\* Nur wenn `discussionMode = FULL` (nicht bei ANNOUNCEMENTS_ONLY oder DISABLED).
\*\*\* Abhängig von der Fotobox-Standardberechtigung des Raums (`defaultPermission`).

### Automatische Rollenzuweisung

- **TEACHER → LEADER**: Wenn ein TEACHER zu einem KLASSE-Raum hinzugefügt wird, wird automatisch die Rolle LEADER gesetzt.
- **SUPERADMIN**: Hat implizit auf alle Räume LEADER-äquivalente Rechte, ohne Mitglied zu sein.
- **TEACHER**: Hat in Räumen, in denen sie Mitglied sind, erweiterte Rechte (Thread-Erstellung, Thread-Verwaltung), auch ohne LEADER-Rolle.

---

## 5. Familien-Rollen (FamilyMemberRole)

Definiert in `backend/src/main/java/com/monteweb/family/internal/model/FamilyMemberRole.java`

| Rolle | Beschreibung |
|-------|-------------|
| **PARENT** | Elternteil im Familienverbund. Kann Einladungscodes generieren, Mitglieder einladen, Kinder verknüpfen. |
| **CHILD** | Kind im Familienverbund. Kann mehreren Familien zugeordnet sein (getrennte Eltern). |

### Familien-Berechtigungen

| Aktion | Wer darf? |
|--------|----------|
| Familie erstellen | UserRole.PARENT oder SUPERADMIN |
| Einladungscode generieren | Familienmitglied (PARENT-Rolle in Familie) |
| Mitglied per User-Suche einladen | Familienmitglied |
| Einladung annehmen/ablehnen | Eingeladener Nutzer |
| Kind verknüpfen | Familienmitglied |
| Mitglied entfernen | Familienmitglied (sich selbst) oder SUPERADMIN |
| Alle Familien auflisten | Nur SUPERADMIN |
| Stundenkonto einsehen | Familienmitglieder |

**Wichtig:** Ein Elternteil gehört zu genau einem Familienverbund. Stunden aus Jobbörse und Putz-Orga werden dem Familienverbund gutgeschrieben (nicht dem Einzelnen). Putzstunden haben ein eigenes Unterkonto.

---

## 6. Fotobox-Berechtigungsstufen

Definiert in `backend/src/main/java/com/monteweb/fotobox/FotoboxPermissionLevel.java`

Die Fotobox nutzt ein dreistufiges, hierarchisches Berechtigungssystem:

```
CREATE_THREADS  (höchste Stufe)
    ↑
POST_IMAGES
    ↑
VIEW_ONLY       (niedrigste Stufe)
```

| Stufe | Rechte |
|-------|--------|
| **VIEW_ONLY** | Foto-Threads und Bilder ansehen, Lightbox nutzen |
| **POST_IMAGES** | Zusätzlich: Bilder in bestehende Threads hochladen (max. 20 pro Request) |
| **CREATE_THREADS** | Zusätzlich: Neue Foto-Threads erstellen |

### Berechtigungsauflösung

Die tatsächliche Berechtigung wird wie folgt ermittelt (in `FotoboxPermissionService`):

1. **SUPERADMIN** → immer `CREATE_THREADS`
2. **Raum-LEADER** → immer `CREATE_THREADS`
3. **Sonstige Raum-Mitglieder** → Raum-Standardberechtigung (`FotoboxRoomSettings.defaultPermission`)

### Eigentumsrechte

- **Thread-Eigentümer** oder LEADER/SUPERADMIN kann eigene Threads bearbeiten und löschen
- **Bild-Eigentümer** oder LEADER/SUPERADMIN kann eigene Bilder bearbeiten (Caption, Sortierung) und löschen

### Bild-Zugriff

Da `<img>`-Tags keine Authorization-Header senden können, akzeptieren Fotobox-Bild-Endpoints den JWT-Token auch als Query-Parameter: `?token=<jwt>`

---

## 7. Kommunikationswege & Regeln

### 7.1 Direktnachrichten (Messaging-Modul)

Die zentrale Kommunikationsregel-Logik befindet sich in `MessagingService.enforceCommRules()`.

#### Kommunikationsmatrix

| Absender ↓ / Empfänger → | SUPERADMIN | SECTION_ADMIN | TEACHER | PARENT | STUDENT |
|--------------------------|:----------:|:-------------:|:-------:|:------:|:-------:|
| **SUPERADMIN** | Ja | Ja | Ja | Ja | Ja |
| **SECTION_ADMIN** | Ja | Ja | Ja | Ja | Ja |
| **TEACHER** | Ja | Ja | Ja | Ja | Ja |
| **PARENT** | Ja | Ja | Ja | Konfig.* | **Nein** |
| **STUDENT** | Ja | Ja | Ja | **Nein** | Konfig.** |

\* Konfigurierbar über `TenantConfig.parentToParentMessaging` (Standard: **deaktiviert**)
\*\* Konfigurierbar über `TenantConfig.studentToStudentMessaging` (Standard: **deaktiviert**)

**Grundregeln:**
- **Staff-Rollen** (SUPERADMIN, SECTION_ADMIN, TEACHER) können immer an alle Rollen Nachrichten senden
- **Parent ↔ Parent**: Standardmäßig deaktiviert, konfigurierbar durch SUPERADMIN
- **Student ↔ Student**: Standardmäßig deaktiviert, konfigurierbar durch SUPERADMIN
- **Parent ↔ Student**: Immer verboten (sofern keiner eine Staff-Rolle hat)

### 7.2 Raum-Chat

Der Raum-Chat bietet drei Kanäle mit rollenbasiertem Zugang:

| Kanal | LEADER | MEMBER | PARENT_MEMBER | GUEST |
|-------|:------:|:------:|:-------------:|:-----:|
| **MAIN** (Alle) | Ja | Ja | Ja | — |
| **PARENTS** (Eltern) | Ja | — | Ja | — |
| **STUDENTS** (Schüler) | Ja | Ja | — | — |

- Nur **LEADER** kann PARENTS- und STUDENTS-Kanäle erstellen
- Chat muss in den Raum-Einstellungen aktiviert sein (`chatEnabled`)
- Echtzeit-Kommunikation via WebSocket (`/ws/messages`)

### 7.3 Diskussions-Threads

Diskussions-Threads sind ein strukturierterer Kommunikationskanal innerhalb von Räumen.

#### Diskussionsmodus (pro Raum konfigurierbar)

| Modus | Beschreibung |
|-------|-------------|
| **FULL** | Threads und Antworten erlaubt (Standard) |
| **ANNOUNCEMENTS_ONLY** | Nur LEADER/TEACHER erstellen Threads, keine Antworten möglich |
| **DISABLED** | Diskussionen vollständig deaktiviert |

#### Thread-Erstellung

| Rolle | Darf Threads erstellen? |
|-------|------------------------|
| LEADER (Raum-Rolle) | Immer |
| TEACHER (Systemrolle) | Immer (in Räumen, in denen sie Mitglied sind) |
| SUPERADMIN | Immer |
| PARENT / PARENT_MEMBER | Nur wenn `allowMemberThreadCreation = true` |
| STUDENT / MEMBER | Nein |
| GUEST | Nein |

#### Thread-Verwaltung (Archivieren, Löschen)

Nur LEADER, TEACHER und SUPERADMIN.

#### Thread-Zielgruppen (Audience)

Threads können eine Zielgruppe haben, die die Sichtbarkeit steuert:

| Zielgruppe | Wer sieht den Thread? |
|-----------|----------------------|
| **ALLE** | Alle Raum-Mitglieder |
| **ELTERN** | Nur PARENT_MEMBER + Staff (LEADER, TEACHER, SUPERADMIN, SECTION_ADMIN) |
| **KINDER** | Nur MEMBER (Schüler) + Staff — nur wenn `childDiscussionEnabled = true` im Raum |

Staff-Rollen (LEADER, TEACHER, SUPERADMIN, SECTION_ADMIN) sehen immer alle Threads, unabhängig von der Zielgruppe.

### 7.4 Feed-Posts & Kommentare

| Aktion | Wer darf? |
|--------|----------|
| Raum-Post erstellen | Alle Raum-Mitglieder |
| Bereichs-/Schul-Post erstellen | SUPERADMIN, SECTION_ADMIN, TEACHER, ELTERNBEIRAT |
| Post bearbeiten | Nur der Autor |
| Post löschen | Autor oder SUPERADMIN |
| Post anheften (Pin) | Autor mit entsprechenden Rechten |
| Kommentar hinzufügen | Alle authentifizierten Nutzer (die den Post sehen können) |

### 7.5 Benachrichtigungen (Notifications)

Benachrichtigungen werden automatisch bei bestimmten Ereignissen erzeugt:

| Ereignis | Empfänger | Typ |
|----------|----------|-----|
| Neuer Feed-Post | Raum-Mitglieder | `POST` |
| Neuer Kommentar | Post-Autor | `COMMENT` |
| Neue Direktnachricht | Gesprächspartner | `MESSAGE` |
| Systemmeldung | Betroffene Nutzer | `SYSTEM` |
| Neuer Diskussions-Thread | Alle Raum-Mitglieder (außer Ersteller) | `DISCUSSION_THREAD` |
| Neue Thread-Antwort | Thread-Teilnehmer | `DISCUSSION_REPLY` |
| Event erstellt | Betroffene Nutzer (Raum/Bereich/Schule) | `EVENT_CREATED` |
| Event geändert | RSVP-Teilnehmer | `EVENT_UPDATED` |
| Event abgesagt | RSVP-Teilnehmer | `EVENT_CANCELLED` |
| Formular veröffentlicht | Zielgruppe | `FORM_PUBLISHED` |
| Einwilligung erforderlich | Zielgruppe | `CONSENT_REQUIRED` |
| Beitrittsanfrage gesendet | Raum-LEADER | `ROOM_JOIN_REQUEST` |
| Beitrittsanfrage genehmigt | Antragsteller | `ROOM_JOIN_APPROVED` |
| Beitrittsanfrage abgelehnt | Antragsteller | `ROOM_JOIN_DENIED` |
| Familien-Einladung | Eingeladener | `FAMILY_INVITATION` |
| Einladung angenommen | Einladender | `FAMILY_INVITATION_ACCEPTED` |
| Putzstunden bestätigt | Betroffener | `CLEANING_COMPLETED` |
| Job-Stunden bestätigt | Betroffener | `JOB_COMPLETED` |

**Zustellwege:**
- **In-App**: WebSocket Push (`/user/{userId}/queue/notifications`)
- **Web Push**: Via VAPID (optional, konfigurierbar via `monteweb.push.enabled`)
- **E-Mail**: Optional, konfigurierbar via `monteweb.email.enabled`

---

## 8. Modulberechtigungen im Detail

### 8.1 Raumverwaltung (Room)

| Aktion | SUPERADMIN | TEACHER | SECTION_ADMIN | PARENT | STUDENT |
|--------|:----------:|:-------:|:-------------:|:------:|:-------:|
| Raum erstellen | Ja | Ja | Ja | Ja | Ja |
| Raum bearbeiten | Ja | Ja (wenn Mitglied) | — | Nur als LEADER | Nur als LEADER |
| Raum archivieren | Ja | — | — | — | — |
| Raum löschen | Ja | — | — | — | — |
| Mitglieder verwalten | Ja | Ja (wenn Mitglied) | — | Nur als LEADER | — |
| Beitrittsanfragen verwalten | Ja | Ja (wenn Mitglied) | — | Nur als LEADER | — |
| Beitrittsanfrage senden | — | Ja | Ja | Ja | Ja |
| Raum im Feed stummschalten | Ja | Ja | Ja | Ja | Ja |
| Alle Räume inkl. archivierte sehen | Ja | — | — | — | — |
| Räume durchsuchen (Browse) | Ja | Ja | Ja | Ja | Ja |

**Raum-Beitrittspolitik (JoinPolicy):**

| Policy | Verhalten |
|--------|----------|
| **OPEN** | Jeder kann direkt beitreten |
| **REQUEST** | Beitritt nur per Anfrage (LEADER genehmigt) |
| **INVITE_ONLY** | Nur per Einladung durch LEADER |

### 8.2 Kalender/Events (Calendar)

Berechtigungen sind scope-basiert:

| Scope | Wer darf Events erstellen/bearbeiten/löschen? |
|-------|----------------------------------------------|
| **ROOM** | LEADER des Raums, SUPERADMIN, ELTERNBEIRAT |
| **SECTION** | TEACHER, SECTION_ADMIN, SUPERADMIN, ELTERNBEIRAT |
| **SCHOOL** | Nur SUPERADMIN, ELTERNBEIRAT |

| Aktion | Berechtigung |
|--------|-------------|
| Events im persönlichen Kalender sehen | Alle authentifizierten Nutzer |
| Raum-Events sehen | Raum-Mitglieder |
| RSVP abgeben (Teilnahme/Vielleicht/Absage) | Alle authentifizierten Nutzer |
| Event absagen | Ersteller oder Berechtigte (s.o.) |

### 8.3 Formulare & Umfragen (Forms)

Berechtigungen sind scope-basiert (analog zu Kalender):

| Scope | Wer darf Formulare erstellen? |
|-------|------------------------------|
| **ROOM** | LEADER des Raums, SUPERADMIN, ELTERNBEIRAT |
| **SECTION** | TEACHER, SECTION_ADMIN, SUPERADMIN, ELTERNBEIRAT |
| **SCHOOL** | Nur SUPERADMIN, ELTERNBEIRAT |

| Aktion | Berechtigung |
|--------|-------------|
| Formular erstellen | Scope-abhängig (s.o.) |
| Formular bearbeiten | Ersteller (nur im Status DRAFT) |
| Formular veröffentlichen | Ersteller (nur im Status DRAFT, mind. 1 Frage) |
| Formular schließen | Ersteller (nur im Status PUBLISHED) |
| Formular löschen | Ersteller (nur im Status DRAFT) |
| Formular ausfüllen | Alle authentifizierten Nutzer (wenn veröffentlicht, einmalig) |
| Ergebnisse einsehen | Ersteller oder SUPERADMIN |
| Einzelantworten einsehen | Ersteller oder SUPERADMIN (nur nicht-anonyme Formulare) |
| CSV/PDF-Export | Ersteller oder SUPERADMIN |

**Formulartypen:**
- **SURVEY**: Umfrage (kann anonym sein)
- **CONSENT**: Einwilligung (Ja/Nein, nicht anonym)

### 8.4 Jobbörse (Jobboard)

| Aktion | SUPERADMIN | TEACHER | SECTION_ADMIN | PARENT | STUDENT |
|--------|:----------:|:-------:|:-------------:|:------:|:-------:|
| Job erstellen | — | — | Ja | Ja | **Nein** |
| Eigenen Job bearbeiten | — | — | Ja | Ja | — |
| Eigenen Job löschen | — | — | Ja | Ja | — |
| Beliebigen Job löschen | — | — | Ja | — | — |
| Sich für Job bewerben | **Nein** | **Nein** | **Nein** | Ja | — |
| Stunden bestätigen | — | — | — | Ja (als Ersteller) | — |
| Report einsehen | Ja | — | — | — | — |
| CSV/PDF-Export | Ja | — | — | — | — |

**Wichtig:** SUPERADMIN, SECTION_ADMIN und TEACHER können sich nicht für Jobs bewerben und keine Elternstunden leisten. Die Stunden werden dem Familienverbund des Bewerbers gutgeschrieben.

### 8.5 Putz-Organisation (Cleaning)

#### Nutzer-Aktionen

| Aktion | SUPERADMIN | TEACHER | SECTION_ADMIN | PARENT | STUDENT |
|--------|:----------:|:-------:|:-------------:|:------:|:-------:|
| Offene Putztermine sehen | Ja | Ja | Ja | Ja | Ja |
| Für Putztermin anmelden | **Nein** | **Nein** | **Nein** | Ja (mit Familie) | — |
| Eigene Anmeldung stornieren | — | — | — | Ja | — |
| QR-Check-in | — | — | — | Ja | — |
| QR-Check-out | — | — | — | Ja | — |

#### Admin-Aktionen

| Aktion | SUPERADMIN | SECTION_ADMIN | ELTERNBEIRAT | PUTZORGA |
|--------|:----------:|:-------------:|:------------:|:--------:|
| Putz-Konfigurationen verwalten | Ja | Ja | Ja | Ja (eigener Bereich) |
| Termine generieren | Ja | Ja | Ja | Ja (eigener Bereich) |
| Slots bearbeiten/löschen | Ja | Ja | Ja | Ja (eigener Bereich) |
| QR-Codes als PDF generieren | Ja | Ja | Ja | Ja (eigener Bereich) |
| Admin-Dashboard | Ja | Ja | Ja | Ja (eigener Bereich) |

**Wichtig:** Putzstunden werden dem Familienverbund gutgeschrieben (eigenes Unterkonto). SUPERADMIN, SECTION_ADMIN und TEACHER leisten selbst keine Putzstunden.

### 8.6 Dateiverwaltung (Files)

| Aktion | LEADER | MEMBER | PARENT_MEMBER | GUEST |
|--------|:------:|:------:|:-------------:|:-----:|
| Dateien hochladen | Ja | Ja | Ja | — |
| Dateien herunterladen | Ja | Ja | Ja | Ja |
| Dateien löschen | Ja | Eigene | Eigene | — |
| Ordner erstellen | Ja | Ja | Ja | — |

### 8.7 Administration

Alle Admin-Endpoints erfordern die Systemrolle **SUPERADMIN**.

| Aktion | Berechtigung |
|--------|-------------|
| Systemkonfiguration einsehen/ändern | SUPERADMIN |
| Theme konfigurieren | SUPERADMIN |
| Module aktivieren/deaktivieren | SUPERADMIN |
| Logo hochladen | SUPERADMIN |
| Audit-Log einsehen | SUPERADMIN |
| Alle Benutzer auflisten | SUPERADMIN |
| Benutzer-Rollen ändern | SUPERADMIN |
| Sonderrollen zuweisen | SUPERADMIN |
| Schulbereiche erstellen/bearbeiten/löschen | SUPERADMIN |
| Kommunikationsregeln konfigurieren | SUPERADMIN |
| Actuator/Prometheus-Metriken | SUPERADMIN |

### 8.8 Benutzerverwaltung (User)

| Aktion | Berechtigung |
|--------|-------------|
| Eigenes Profil einsehen/bearbeiten | Alle authentifizierten Nutzer |
| DSGVO-Datenexport (eigene Daten) | Alle authentifizierten Nutzer |
| DSGVO-Account löschen/anonymisieren | Alle authentifizierten Nutzer |
| Anderes Nutzerprofil einsehen (eingeschränkt) | Alle authentifizierten Nutzer |
| Nutzer-Suche (für Messaging/Einladungen) | Alle authentifizierten Nutzer |
| Alle Nutzer auflisten | SUPERADMIN |
| Rollen ändern | SUPERADMIN |

---

## 9. Gesamtübersicht (Matrix)

### Systemweite Fähigkeiten nach Rolle

| Fähigkeit | SUPERADMIN | SECTION_ADMIN | TEACHER | PARENT | STUDENT |
|-----------|:----------:|:-------------:|:-------:|:------:|:-------:|
| **Admin-Panel** | Ja | — | — | — | — |
| **Module konfigurieren** | Ja | — | — | — | — |
| **Schulbereiche verwalten** | Ja | — | — | — | — |
| **Benutzer verwalten** | Ja | — | — | — | — |
| **Kommunikationsregeln** | Ja | — | — | — | — |
| **Räume erstellen** | Ja | Ja | Ja | Ja | Ja |
| **Räume archivieren/löschen** | Ja | — | — | — | — |
| **Schulweite Posts** | Ja | Ja | Ja | — | — |
| **Bereichsweite Posts** | Ja | Ja | Ja | — | — |
| **Schulweite Events** | Ja | — | — | — | — |
| **Bereichsweite Events** | Ja | Ja | Ja | — | — |
| **Raum-Events** | Als LEADER | — | Als LEADER | Als LEADER | — |
| **Schulweite Formulare** | Ja | — | — | — | — |
| **Bereichsweite Formulare** | Ja | Ja | Ja | — | — |
| **Raum-Formulare** | Als LEADER | — | Als LEADER | Als LEADER | — |
| **Jobs erstellen** | — | Ja | — | Ja | **Nein** |
| **Jobs annehmen** | **Nein** | **Nein** | **Nein** | Ja | — |
| **Putztermine wahrnehmen** | **Nein** | **Nein** | **Nein** | Ja | — |
| **Putz-Admin** | Ja | Ja | — | — | — |
| **Familie erstellen** | Ja | — | — | Ja | — |
| **Nachrichten an alle** | Ja | Ja | Ja | — | — |
| **Nachrichten an Eltern** | Ja | Ja | Ja | Konfig. | — |
| **Nachrichten an Schüler** | Ja | Ja | Ja | — | Konfig. |
| **Push-Benachrichtigungen** | Ja | Ja | Ja | Ja | Ja |
| **DSGVO-Datenexport** | Ja | Ja | Ja | Ja | Ja |

### Sonderrollen-Erweiterungen

| Fähigkeit | ELTERNBEIRAT | PUTZORGA |
|-----------|:------------:|:--------:|
| Events erstellen (alle Scopes) | Ja | — |
| Formulare erstellen (alle Scopes) | Ja | — |
| Schulweite/Bereichsweite Posts | Ja | — |
| Putz-Admin (Termine, Slots, QR) | Ja | Ja |
| Putz-Dashboard | Ja | Ja |

---

## Anhang: Konfigurierbare Kommunikationsregeln

Diese Regeln werden über die Systemkonfiguration (Admin-Panel) gesteuert:

| Einstellung | Eigenschaft | Standard | Beschreibung |
|------------|-----------|---------|-------------|
| Eltern-Eltern-Messaging | `parentToParentMessaging` | `false` | Erlaubt Direktnachrichten zwischen Eltern |
| Schüler-Schüler-Messaging | `studentToStudentMessaging` | `false` | Erlaubt Direktnachrichten zwischen Schülern |

## Anhang: Raum-Einstellungen mit Berechtigungsrelevanz

| Einstellung | Standard | Beschreibung |
|------------|---------|-------------|
| `discussionMode` | `FULL` | Diskussionsmodus: FULL, ANNOUNCEMENTS_ONLY, DISABLED |
| `allowMemberThreadCreation` | `false` | Erlaubt PARENTs das Erstellen von Diskussions-Threads |
| `childDiscussionEnabled` | `false` | Erlaubt Schülern, KINDER-Threads zu sehen |
| `chatEnabled` | `true` | Aktiviert den Raum-Chat |
| `joinPolicy` | `REQUEST` | Beitrittspolitik: OPEN, REQUEST, INVITE_ONLY |
| `fotobox.enabled` | Konfig. | Aktiviert die Fotobox im Raum |
| `fotobox.defaultPermission` | Konfig. | Standard-Fotobox-Berechtigung für Mitglieder |
