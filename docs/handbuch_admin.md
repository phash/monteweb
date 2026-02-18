# MonteWeb Handbuch fuer Administration

**Version:** 1.0 | **Stand:** Februar 2026 | **Rollen:** SECTION_ADMIN (Bereichsleitung) & SUPERADMIN

---

## Inhaltsverzeichnis

1. [Rollenueberblick](#1-rollenueberblick)
2. [Section Admin -- Bereichsverwaltung](#2-section-admin--bereichsverwaltung)
3. [SuperAdmin -- Systemverwaltung](#3-superadmin--systemverwaltung)
4. [Benutzerverwaltung](#4-benutzerverwaltung)
5. [Raumverwaltung](#5-raumverwaltung)
6. [Schulbereiche verwalten](#6-schulbereiche-verwalten)
7. [Familienverwaltung](#7-familienverwaltung)
8. [Modul-Verwaltung](#8-modul-verwaltung)
9. [Putz-Organisation konfigurieren](#9-putz-organisation-konfigurieren)
10. [Stundenverwaltung und Jahresabrechnung](#10-stundenverwaltung-und-jahresabrechnung)
11. [Design und Einstellungen](#11-design-und-einstellungen)
12. [Fehlermanagement](#12-fehlermanagement)
13. [Sicherheit und Rollenlogik](#13-sicherheit-und-rollenlogik)
14. [Best Practices](#14-best-practices)
15. [Haeufige Fragen (FAQ)](#15-haeufige-fragen-faq)

---

## 1. Rollenueberblick

### 1.1 Unterschied Section Admin vs. SuperAdmin

| Funktion | SECTION_ADMIN | SUPERADMIN |
|----------|:-------------:|:----------:|
| Nutzer im eigenen Bereich verwalten | Ja | Ja (alle) |
| Raeume im eigenen Bereich verwalten | Ja | Ja (alle) |
| Sonderrollen vergeben (PUTZORGA, ELTERNBEIRAT) | Ja (im Bereich) | Ja (global) |
| Raeume erstellen | Ja (im Bereich) | Ja (ueberall) |
| Schulbereiche verwalten | Nein | Ja |
| Familien verwalten | Nein | Ja |
| Module aktivieren/deaktivieren | Nein | Ja |
| Putz-Konfiguration | Nein | Ja |
| Design/Theme aendern | Nein | Ja |
| Bundesland/Schulferien | Nein | Ja |
| Kommunikationsregeln | Nein | Ja |
| Stundenregelung | Nein | Ja |
| Jahresabrechnung | Nein | Ja |
| Fehlermanagement | Nein | Ja |
| Audit-Log | Nein | Ja |
| Stundenbericht | Nein | Ja |

### 1.2 Navigation

**Section Admin:**
- Seitenleiste zeigt den Menuepunkt **"Bereichsverwaltung"**
- Kein Zugriff auf den Menuepunkt "Verwaltung"

**SuperAdmin:**
- Seitenleiste zeigt den Menuepunkt **"Verwaltung"** mit Unterpunkten:
  - Dashboard, Benutzer, Raeume, Schulbereiche, Familien, Module, Stundenbericht, Putz-Orga, Jahresabrechnung, Fehlermeldungen, Design & Branding

---

## 2. Section Admin -- Bereichsverwaltung

### 2.1 Zugang

Die Bereichsverwaltung erreichen Sie ueber **"Bereichsverwaltung"** in der Seitenleiste.

> **Voraussetzung:** Ihnen muss die Rolle SECTION_ADMIN mit einem oder mehreren Bereichen zugewiesen sein. Dies geschieht durch den SuperAdmin.

### 2.2 Bereich waehlen

Oben in der Bereichsverwaltung befindet sich der **Bereichs-Selektor**:
- Zeigt alle Ihnen zugewiesenen Bereiche
- Waehlen Sie den Bereich, den Sie verwalten moechten
- Alle Ansichten passen sich dem gewaehlten Bereich an

### 2.3 Tab: Benutzer im Bereich

Der Benutzer-Tab zeigt alle Nutzer, die Raeumen in diesem Bereich angehoeren:

**Funktionen:**
- **Suchen** -- Name oder E-Mail
- **Filtern** -- Nach Rolle (Alle, Lehrkraft, Elternteil, Schueler/in)
- **Nutzerdetails** einsehen

**Sonderrollen vergeben:**

Als Section Admin koennen Sie fuer Ihren Bereich Sonderrollen vergeben:

| Sonderrolle | Beschreibung | Auswirkung |
|-------------|-------------|------------|
| **PUTZORGA** | Putz-Organisation | Kann Putztermine fuer den Bereich konfigurieren |
| **ELTERNBEIRAT** | Elternvertretung | Erweiterte Rechte: Termine, Formulare auf Bereichsebene |

1. Nutzer in der Liste suchen
2. Auf **Sonderrollen-Button** klicken
3. Rolle PUTZORGA oder ELTERNBEIRAT zuweisen/entziehen

### 2.4 Tab: Raeume im Bereich

Der Raeume-Tab zeigt alle Raeume des gewaehlten Bereichs:

**Funktionen:**
- **Suchen** -- Raeume nach Name suchen
- **Filtern** -- Nach Raumtyp (Klasse, Gruppe, Projekt etc.)
- **Mitglieder anzeigen** -- Anzahl und Details der Raummitglieder
- **Raum erstellen** -- Neuen Raum im Bereich anlegen

**Raum erstellen:**
1. Klicken Sie auf **"Raum erstellen"**
2. Raumname, Typ und Beschreibung eingeben
3. Der Raum wird automatisch dem gewaehlten Bereich zugeordnet

---

## 3. SuperAdmin -- Systemverwaltung

### 3.1 Admin-Dashboard

Unter **Verwaltung** erreichen Sie das Admin-Dashboard mit Schnellzugriff auf:

| Kachel | Beschreibung |
|--------|-------------|
| **Benutzer** | Benutzer verwalten, Rollen zuweisen |
| **Raeume** | Raeume erstellen und verwalten |
| **Schulbereiche** | Krippe, KiGa, GS, MS, OS |
| **Familien** | Familienverbuende verwalten |
| **Module** | Module aktivieren/deaktivieren |
| **Jahresabrechnung** | Elternstunden-Abrechnung |

---

## 4. Benutzerverwaltung

### 4.1 Benutzerliste

Unter **Verwaltung > Benutzer**:

**Suche und Filter:**
- Freitextsuche (Name oder E-Mail)
- Filter nach **Rolle** (Superadmin, Bereichsleitung, Lehrkraft, Elternteil, Schueler/in)
- Filter nach **Sonderrolle** (Putz-Orga, Elternbeirat)
- Filter nach **Status** (Aktiv, Ausstehend)

### 4.2 Neue Benutzer freischalten

Registrierte Nutzer, die noch nicht freigeschaltet sind, erscheinen unter **"Neue Benutzer"** (oben auf der Seite):
1. Nutzername und E-Mail pruefen
2. **"Freischalten"** klicken
3. Der Nutzer kann sich ab sofort anmelden

### 4.3 Benutzer bearbeiten

Klicken Sie auf einen Benutzer, um die Detailansicht zu oeffnen:

**Tab: Profil**
- Vorname, Nachname, E-Mail
- **Zugewiesene Rollen** -- Multi-Select zwischen: SUPERADMIN, SECTION_ADMIN, TEACHER, PARENT, STUDENT
- **Sonderrollen** -- PUTZORGA, ELTERNBEIRAT zuweisen
- **Status** -- Aktiv/Gesperrt

**Tab: Raeume**
- Alle Raum-Mitgliedschaften des Nutzers
- Raeume hinzufuegen oder entfernen
- Raum-Rollen aendern (LEADER, MEMBER)
- **Mitglied verschieben/kopieren** -- Zu einem anderen Raum

**Tab: Familie**
- Familienzugehoerigkeit anzeigen
- Nutzer zu einer Familie hinzufuegen
- Familienmitgliedschaft entfernen

### 4.4 Rollen zuweisen

**Hauptrollen:**
Ein Nutzer kann mehrere Hauptrollen haben (z.B. TEACHER + PARENT). Die verfuegbaren Rollen sind:

| Rolle | Badge-Farbe | Beschreibung |
|-------|:-----------:|-------------|
| SUPERADMIN | Rot | Vollzugriff |
| SECTION_ADMIN | Orange | Bereichsverwaltung |
| TEACHER | Blau | Lehrkraft |
| PARENT | Gruen | Elternteil |
| STUDENT | Grau | Schueler/in |

**Section Admin konfigurieren:**
Wenn Sie einem Nutzer die Rolle SECTION_ADMIN zuweisen:
1. Waehlen Sie SECTION_ADMIN in den zugewiesenen Rollen
2. Unter **"Zustaendige Bereiche"** waehlen Sie die Schulbereiche aus
3. Der Nutzer kann nur diese Bereiche verwalten

---

## 5. Raumverwaltung

### 5.1 Raumuebersicht

Unter **Verwaltung > Raeume**:

- Alle Raeume auflisten (aktiv und deaktiviert)
- **Filter** nach Bereich und Raumtyp
- **Suche** nach Raumname

### 5.2 Raum erstellen

1. Klicken Sie auf **"Raum erstellen"** (auf der Entdeckungsseite)
2. Name, Beschreibung, Typ eingeben
3. **Schulbereich** zuweisen

> **Hinweis:** Bei Raeumen vom Typ "Klasse" wird automatisch ein Standard-Ordner in der Dateiablage erstellt.

### 5.3 Raum bearbeiten

Klicken Sie auf das Bearbeiten-Icon eines Raums:
- Name, Beschreibung, Typ aendern
- Schulbereich aendern/zuweisen

### 5.4 Raum deaktivieren/reaktivieren

- **Deaktivieren**: Raum wird fuer Nutzer unsichtbar, Daten bleiben erhalten
- **Reaktivieren**: Raum wird wieder sichtbar

### 5.5 Raum loeschen

> **Achtung:** Beim Loeschen werden alle Mitgliedschaften, Diskussionen, Dateien und Chat-Nachrichten unwiderruflich entfernt!

1. Klicken Sie auf das Loeschen-Icon
2. Bestaetigen Sie die Warnung

### 5.6 Mitglieder verwalten

Im Admin-Kontext koennen Sie:
- Mitglieder zu Raeumen hinzufuegen
- Mitglieder entfernen
- Raum-Rollen aendern (LEADER/MEMBER)
- **Mitglieder verschieben**: Nutzer von einem Raum in einen anderen verschieben
- **Mitglieder kopieren**: Nutzer in einem zusaetzlichen Raum eintragen

---

## 6. Schulbereiche verwalten

*(Nur SUPERADMIN)*

### 6.1 Bereiche verwalten

Unter **Verwaltung > Schulbereiche**:

Die Ansicht zeigt alle Bereiche mit zugeordneten Raeumen in einer Baumstruktur:

```
Kinderhaus (3 Raeume)
  ├── Sonnengruppe
  ├── Sternengruppe
  └── Mondgruppe
Grundstufe (4 Raeume)
  ├── ...
Mittelstufe (2 Raeume)
  ├── ...
```

### 6.2 Bereich erstellen

1. Klicken Sie auf **"Bereich anlegen"**
2. Eingeben:
   - **Name** (z.B. "Grundstufe")
   - **Beschreibung** (optional)
   - **Reihenfolge** (Sortierung in der Anzeige)
3. Speichern

### 6.3 Bereich bearbeiten

- Name, Beschreibung, Reihenfolge aendern

### 6.4 Bereich loeschen

> **Achtung:** Zugeordnete Raeume verlieren ihre Bereichszuordnung!

1. Klicken Sie auf "Bereich loeschen"
2. Bestaetigen

### 6.5 Section Admins zuweisen

Section Admins fuer einen Bereich weisen Sie ueber die **Benutzerverwaltung** zu:
1. Nutzer suchen
2. Rolle SECTION_ADMIN zuweisen
3. Zustaendige Bereiche auswaehlen

---

## 7. Familienverwaltung

*(Nur SUPERADMIN)*

### 7.1 Familienuebersicht

Unter **Verwaltung > Familien**:
- Alle Familienverbuende auflisten
- Suche nach Familienname
- Familien mit Stunden-Befreiung sind markiert

### 7.2 Familie bearbeiten

Klicken Sie auf eine Familie:

**Tab: Info**
- Familienname aendern

**Tab: Mitglieder**
- Eltern und Kinder anzeigen
- Mitglieder hinzufuegen/entfernen

**Tab: Stunden**
- Elternstunden und Putzstunden einsehen
- Soll/Ist-Vergleich

### 7.3 Stunden-Befreiung

Familien koennen von der Elternstunden-Pflicht befreit werden:

1. In der Familienliste auf **"Befreit"** toggle klicken
2. Oder in der Familie: **"Von Elternstunden befreien"**

Befreite Familien:
- Erscheinen im Bericht als "Befreit"
- Muessen keine Stunden leisten
- Koennen aber trotzdem freiwillig Jobs annehmen

### 7.4 Familie loeschen

> **Achtung:** Alle Mitgliedschaften werden entfernt!

---

## 8. Modul-Verwaltung

*(Nur SUPERADMIN)*

### 8.1 Module aktivieren/deaktivieren

Unter **Verwaltung > Module**:

| Modul | Standard | Beschreibung |
|-------|:--------:|-------------|
| **Nachrichten** | Aktiv | Direktnachrichten und Gruppen-Chats |
| **Dateiablage** | Aktiv | Dateiablage pro Raum mit Sichtbarkeitssteuerung |
| **Jobbboerse** | Aktiv | Elternstunden mit Bewerbungen und Stundenkonto |
| **Putz-Orga** | Aktiv | Putzaktionen, Slots, QR-Check-in |
| **Kalender** | Aktiv | Raum/Bereich/Schul-Termine mit RSVP |
| **Formulare** | Aktiv | Umfragen und Einwilligungen mit Export |
| **Fotobox** | Aktiv | Foto-Galerien in Raeumen |
| **Fundgrube** | Aktiv | Schulweite Fundgrube fuer verlorene Gegenstaende |

**Auswirkung der Deaktivierung:**
- Menuepunkt verschwindet fuer alle Nutzer
- Backend-Endpunkte geben 404 zurueck
- Bestehende Daten bleiben erhalten (bei Reaktivierung wieder sichtbar)

---

## 9. Putz-Organisation konfigurieren

*(SUPERADMIN oder Nutzer mit Sonderrolle PUTZORGA)*

### 9.1 Zugang

- **SUPERADMIN**: Verwaltung > Putz-Orga
- **PUTZORGA**: Eigener Menuepunkt "Putz-Verwaltung" in der Seitenleiste

### 9.2 Putzaktion erstellen

1. Klicken Sie auf **"Neue Putzaktion"**
2. Fuellen Sie aus:

| Feld | Beschreibung |
|------|-------------|
| **Titel** | z.B. "Mittwochs-Putz Kinderhaus" |
| **Schulbereich** | Welcher Bereich? |
| **Zeitraum** | Beginn- und Endzeit |
| **Mind. Teilnehmer** | Minimale Anzahl Helfer |
| **Max. Teilnehmer** | Maximale Anzahl Helfer |
| **Stundengutschrift** | Wie viele Stunden werden gutgeschrieben |

3. **Art der Putzaktion:**

| Art | Einstellung |
|-----|-------------|
| **Wiederkehrend** | Wochentag waehlen (Mo-Fr) |
| **Einmalig** | Konkretes Datum waehlen |

> **Tipp:** Im Datumswaehler werden **Feiertage** (rot) und **Schulferien** (orange) markiert, damit Sie keine Termine auf freie Tage legen.

### 9.3 Termine generieren

Fuer wiederkehrende Putzaktionen:
1. Klicken Sie auf **"Termine generieren"**
2. Zeitraum waehlen (Von-Bis)
3. MonteWeb generiert automatisch alle Termine im Zeitraum

### 9.4 QR-Codes exportieren

1. Klicken Sie auf **"QR-Codes PDF"**
2. Zeitraum waehlen
3. PDF mit QR-Codes wird generiert -- zum Ausdrucken und Aufhaengen am Putzort

### 9.5 PutzOrga-Verwaltung

Als SUPERADMIN koennen Sie PutzOrga-Nutzer zuweisen:
1. Schulbereich waehlen
2. Elternteil suchen
3. **"Zuweisen"** klicken
4. Der Nutzer erhaelt die PUTZORGA-Sonderrolle fuer diesen Bereich

### 9.6 Anmeldungen einsehen

Fuer jede Putzaktion:
1. Klicken Sie auf **"Anmeldungen anzeigen"**
2. Sie sehen: Familie, Status, Stunden

---

## 10. Stundenverwaltung und Jahresabrechnung

*(Nur SUPERADMIN)*

### 10.1 Stundenregelung

Unter **Verwaltung > Design & Branding** (Abschnitt "Stundenregelung"):

| Einstellung | Beschreibung | Beispiel |
|-------------|-------------|---------|
| **Gesamtstunden pro Familie/Jahr** | Jahresziel fuer Elternstunden | 20 Stunden |
| **davon Putzstunden** | Anteil fuer Putzstunden | 6 Stunden |

### 10.2 Stundenbericht

Unter **Verwaltung > Stundenbericht**:

Der Bericht zeigt alle Familien mit:
- Familienname
- Elternstunden (geleistet/Soll)
- Putzstunden (geleistet/Soll)
- Fortschritt (Prozent)
- **Ampel-System:**
  - **Gruen** -- Ziel erreicht oder auf gutem Weg
  - **Gelb** -- Noch etwas Nachholbedarf
  - **Rot** -- Deutlich hinter dem Ziel

**Export:** CSV oder PDF

### 10.3 Jahresabrechnung (Billing)

Unter **Verwaltung > Jahresabrechnung**:

Die Jahresabrechnung ermoeglicht eine periodenbasierte Abrechnung:

1. **Zeitraum erstellen:**
   - Bezeichnung (z.B. "Schuljahr 2025/2026")
   - Start- und Enddatum
   - Klicken Sie auf **"Zeitraum erstellen"**

2. **Aktiver Zeitraum:**
   - Zeigt alle Familien mit Stunden-Saldo
   - Familienmitglieder aufklappbar
   - Durchschnitt und Gesamt-Stunden

3. **Zeitraum abschliessen:**
   - **"Abrechnung abschliessen"** klicken
   - Der Bericht wird eingefroren
   - Eine neue Periode wird automatisch erstellt
   - Alte Perioden sind unter "Vergangene Abrechnungen" einsehbar

---

## 11. Design und Einstellungen

*(Nur SUPERADMIN)*

### 11.1 Theme konfigurieren

Unter **Verwaltung > Design & Branding**:

**Farbschema:**

| Einstellung | Beschreibung |
|-------------|-------------|
| Primaerfarbe | Hauptfarbe der Anwendung |
| Primaerfarbe (Hover) | Hover-Zustand |
| Hintergrund | Seitenhintergrund |
| Karten-Hintergrund | Hintergrund von Karten |
| Sidebar-Hintergrund | Farbe der Seitenleiste |
| Textfarbe | Haupttextfarbe |
| Sekundaere Textfarbe | Fuer weniger wichtigen Text |
| Rahmenfarbe | Farbe von Rahmen und Trennlinien |

Eine **Vorschau** zeigt die Aenderungen in Echtzeit an.

**Logo:**
- Schullogo hochladen (wird in Navigation und Login angezeigt)
- Schulname als Fallback, wenn kein Logo vorhanden

### 11.2 Regionale Einstellungen

**Bundesland:**
- Waehlen Sie das Bundesland Ihrer Schule
- Bestimmt die angezeigten gesetzlichen Feiertage
- Alle 16 deutschen Bundeslaender verfuegbar
- Standard: Bayern (BY)

**Schulferien:**
- Manuell konfigurierbar
- Fuer jeden Ferienzeitraum: Name, Von-Datum, Bis-Datum
- Beispiel: "Herbstferien", 28.10.2025 - 01.11.2025
- **"Ferienzeit hinzufuegen"** / **"Entfernen"**

### 11.3 Kommunikationsregeln

Konfigurieren Sie, wer mit wem kommunizieren darf:

| Regel | Standard | Aenderbar |
|-------|----------|:---------:|
| Lehrer ↔ Eltern | Erlaubt | Nein |
| Lehrer ↔ Schueler | Erlaubt | Ja |
| Eltern ↔ Eltern | Gesperrt | Ja |
| Schueler ↔ Schueler | Gesperrt | Ja |

---

## 12. Fehlermanagement

*(Nur SUPERADMIN)*

### 12.1 Fehlermeldungen

Unter **Verwaltung > Fehlermeldungen**:

MonteWeb erfasst automatisch Frontend-Fehler mit Fingerprint-basierter Deduplizierung:

| Spalte | Beschreibung |
|--------|-------------|
| Fehlertyp | Art des Fehlers |
| Nachricht | Fehlermeldung |
| Auftreten | Wie oft der Fehler aufgetreten ist |
| Erstmals/Zuletzt | Zeitstempel |
| Status | NEU / GEMELDET / BEHOBEN / IGNORIERT |

### 12.2 Status verwalten

Fuer jeden Fehler koennen Sie den Status aendern:
- **NEU** -- Unbearbeitet
- **GEMELDET** -- Wurde weitergeleitet
- **BEHOBEN** -- Fehler ist behoben
- **IGNORIERT** -- Wird nicht behandelt

### 12.3 GitHub Issue erstellen

Falls konfiguriert, koennen Sie direkt ein GitHub Issue erstellen:

1. **GitHub konfigurieren** (einmalig):
   - Repository (Format: `owner/repo`)
   - Personal Access Token (PAT)
2. Bei einem Fehler: **"GitHub Issue erstellen"** klicken
3. Issue wird automatisch mit Fehlerdetails erstellt

---

## 13. Sicherheit und Rollenlogik

### 13.1 Authentifizierung

- **JWT-basiert**: 15 Minuten Access Token + 7 Tage Refresh Token
- **Rate-Limiting** auf Auth-Endpunkten (Brute-Force-Schutz)
- **Passwort-Mindestlaenge**: 8 Zeichen
- **BCrypt-Hashing** fuer Passwoerter

### 13.2 Rollenhierarchie

```
SUPERADMIN (alles)
  └── SECTION_ADMIN (Bereich)
        └── TEACHER (Raum-LEADER + Content)
              └── PARENT (Familie + Jobs + Putz)
                    └── STUDENT (Basis-Ansicht)
```

### 13.3 Sonderrollen

Sonderrollen sind zusaetzliche Berechtigungen neben der Hauptrolle:

| Sonderrolle | Vergabe durch | Wirkung |
|-------------|--------------|---------|
| PUTZORGA | Section Admin / SuperAdmin | Zugriff auf Putz-Konfiguration fuer den Bereich |
| ELTERNBEIRAT | Section Admin / SuperAdmin | Erweiterte Rechte: Termine und Formulare auf Bereichsebene |

### 13.4 Datenauswertung

- **Audit-Log** protokolliert alle administrativen Aktionen
- Nachvollziehbar: Wer hat was wann getan
- Filtermoeglich nach Zeitraum, Nutzer, Aktion

### 13.5 DSGVO

| Massnahme | Beschreibung |
|-----------|-------------|
| Datenexport | Nutzer koennen ihre Daten als JSON exportieren |
| Konto-Loeschung | Vollstaendige Anonymisierung |
| Anonyme Formulare | Antworten ohne Nutzerzuordnung |
| Audit-Log | Nachvollziehbarkeit |
| BCrypt | Passwort-Hashing |
| JWT | Kurzlebige Tokens |

---

## 14. Best Practices

### 14.1 Ersteinrichtung

Empfohlene Reihenfolge:

1. **Schulbereiche anlegen** (Kinderhaus, Grundstufe, Mittelstufe, Oberstufe etc.)
2. **Design konfigurieren** (Logo, Farben, Schulname)
3. **Bundesland und Schulferien** eintragen
4. **Module aktivieren/deaktivieren** je nach Bedarf
5. **Stundenregelung** festlegen (Jahresstunden, Putzstunden-Anteil)
6. **Raeume erstellen** und Bereichen zuordnen
7. **Nutzer anlegen** oder Registrierung freigeben
8. **Section Admins** fuer Bereiche zuweisen
9. **Putzaktionen** konfigurieren
10. **Jahresabrechnung** starten

### 14.2 Laufender Betrieb

- **Regelmaessig pruefen:** Neue Benutzer freischalten
- **Section Admins einsetzen:** Delegieren Sie Bereichsverwaltung
- **Putzorganisation delegieren:** PUTZORGA-Sonderrolle an Eltern vergeben
- **Stundenbericht** regelmaessig einsehen (Ampel-System)
- **Fehlermeldungen** pruefen und bei Bedarf GitHub Issues erstellen
- **Schulferien** jaehrlich aktualisieren
- **Jahresabrechnung** am Schuljahresende abschliessen

### 14.3 Sicherheitsempfehlungen

- Minimale Rollenverteilung: Nur so viele SUPERADMINs wie noetig
- Section Admins fuer die taegliche Bereichsverwaltung nutzen
- Regelmaessig Audit-Log pruefen
- Bei Mitarbeiterwechsel: Rollen zeitnah anpassen
- Starke Passwoerter empfehlen (mindestens 12 Zeichen)

### 14.4 Familien-Management

- Familien von Anfang an korrekt einrichten
- **Stunden-Befreiung** fuer berechtigte Familien setzen
- Bei getrennten Eltern: Kinder koennen mehreren Familien zugeordnet sein
- Stundensaldo regelmaessig pruefen

---

## 15. Haeufige Fragen (FAQ)

### Section Admin

**Wie werde ich Section Admin?**
- Ein SuperAdmin weist Ihnen die Rolle SECTION_ADMIN zu und waehlt Ihre Bereiche aus.

**Kann ich Nutzer anlegen?**
- Nein, nur der SuperAdmin kann Nutzer erstellen. Sie koennen aber Sonderrollen vergeben und Nutzer zu Raeumen zuweisen.

**Kann ich Module deaktivieren?**
- Nein, das ist dem SuperAdmin vorbehalten.

### SuperAdmin

**Wie lege ich einen neuen Schulbereich an?**
- Verwaltung > Schulbereiche > "Bereich anlegen" > Name, Beschreibung, Reihenfolge eingeben.

**Wie aendere ich die Schulferien?**
- Verwaltung > Design & Branding > Abschnitt "Schulferien & Feiertage" > Ferienzeiten hinzufuegen/entfernen.

**Was passiert, wenn ich ein Modul deaktiviere?**
- Der Menuepunkt verschwindet, Backend-Endpunkte geben 404 zurueck. Bestehende Daten bleiben erhalten.

**Wie funktioniert die Jahresabrechnung?**
1. Abrechnungszeitraum erstellen (z.B. Schuljahr 2025/2026)
2. Waehrend des Jahres sammeln Familien Stunden
3. Am Schuljahresende: "Abrechnung abschliessen"
4. Bericht wird eingefroren, neue Periode startet automatisch

**Wie erstelle ich QR-Codes fuer die Putz-Orga?**
- Verwaltung > Putz-Orga > Putzaktion waehlen > "QR-Codes PDF" > Zeitraum waehlen > PDF downloaden und ausdrucken.

**Wie delegiere ich die Putz-Organisation?**
- Verwaltung > Putz-Orga > "PutzOrga-Verwaltung" > Bereich waehlen > Elternteil suchen > "Zuweisen". Der Nutzer erhaelt die PUTZORGA-Sonderrolle.

**Was sehe ich im Stundenbericht?**
- Alle Familien mit Elternstunden, Putzstunden, Fortschritt und Ampel-System. Export als CSV oder PDF moeglich.

**Wie befreie ich eine Familie von Elternstunden?**
- Verwaltung > Familien > Familie suchen > "Von Elternstunden befreien" oder in der Familienliste den "Befreit"-Toggle nutzen.

---

*MonteWeb Admin-Handbuch -- Version 1.0 -- Februar 2026*
