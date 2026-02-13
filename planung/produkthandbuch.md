# MonteWeb Produkthandbuch

**Version:** 1.0
**Stand:** 2026-02-13
**Anwendung:** MonteWeb -- Modulares Schul-Intranet fuer Montessori-Schulkomplexe

---

## Inhaltsverzeichnis

1. [Einfuehrung](#1-einfuehrung)
2. [Rollen und Berechtigungen](#2-rollen-und-berechtigungen)
3. [Erste Schritte](#3-erste-schritte)
4. [Dashboard](#4-dashboard)
5. [Profil und Einstellungen](#5-profil-und-einstellungen)
6. [Raeume](#6-raeume)
7. [Feed und Beitraege](#7-feed-und-beitraege)
8. [Diskussionen](#8-diskussionen)
9. [Dateien](#9-dateien)
10. [Fotobox](#10-fotobox)
11. [Kalender und Termine](#11-kalender-und-termine)
12. [Formulare und Umfragen](#12-formulare-und-umfragen)
13. [Nachrichten](#13-nachrichten)
14. [Jobboerse (Elternstunden)](#14-jobboerse-elternstunden)
15. [Putzorganisation](#15-putzorganisation)
16. [Familienverbund](#16-familienverbund)
17. [Benachrichtigungen](#17-benachrichtigungen)
18. [Bereichsverwaltung (Section Admin)](#18-bereichsverwaltung-section-admin)
19. [Administration](#19-administration)
20. [Technische Hinweise](#20-technische-hinweise)

---

## 1. Einfuehrung

### 1.1 Was ist MonteWeb?

MonteWeb ist ein modulares, selbst-gehostetes Schul-Intranet, entwickelt fuer Montessori-Schulkomplexe von der Krippe bis zur Oberstufe. Es ersetzt fragmentierte Kommunikationswege (E-Mail, WhatsApp-Gruppen, Papierlisten) durch eine einzige, DSGVO-konforme Plattform.

### 1.2 Kernfunktionen

| Funktion | Beschreibung |
|----------|-------------|
| **Raeume** | Klassen, Gruppen und Projekte mit Mitgliederverwaltung |
| **Feed** | Einheitliche Nachrichten-Timeline mit Beitraegen und Kommentaren |
| **Diskussionen** | Themenbezogene Threads innerhalb von Raeumen |
| **Dateien** | Dateiablage mit Ordnerstruktur und Sichtbarkeitssteuerung |
| **Fotobox** | Foto-Galerien mit Thumbnails und Lightbox-Ansicht |
| **Kalender** | Termine auf Raum-, Bereichs- und Schulebene mit RSVP |
| **Formulare** | Umfragen und Einwilligungsformulare mit Auswertung und Export |
| **Nachrichten** | Direktnachrichten zwischen Nutzern |
| **Jobboerse** | Elternstunden-Verwaltung mit Zeiterfassung |
| **Putzorganisation** | Putzplan mit QR-Code-Check-in |
| **Familienverbund** | Familien-Gruppierung mit gemeinsamen Stundenkonten |
| **Benachrichtigungen** | In-App, Echtzeit (WebSocket) und Push-Benachrichtigungen |

### 1.3 Modularer Aufbau

Nicht jede Schule benoetigt alle Funktionen. Folgende Module koennen individuell aktiviert oder deaktiviert werden:

- Nachrichten (Messaging)
- Dateien (Files)
- Jobboerse (Jobboard)
- Putzorganisation (Cleaning)
- Kalender (Calendar)
- Formulare (Forms)
- Fotobox

Die Modulverwaltung erfolgt durch den Superadmin unter **Administration > Module**.

### 1.4 Responsive Design

MonteWeb passt sich automatisch an das Endgeraet an:

- **Desktop:** Seitenleiste (Sidebar) links mit Navigation
- **Mobil:** Untere Navigationsleiste (Bottom Nav) mit "Mehr"-Menue

<!-- TODO: Screenshot — Desktop-Ansicht mit Sidebar neben Mobile-Ansicht mit Bottom Nav -->

---

## 2. Rollen und Berechtigungen

### 2.1 Hauptrollen

MonteWeb verwendet ein rollenbasiertes Berechtigungssystem mit fuenf Hauptrollen:

| Rolle | Farbe | Beschreibung |
|-------|-------|-------------|
| **SUPERADMIN** | Rot | Vollzugriff auf alle Funktionen, Administration, Audit-Log |
| **SECTION_ADMIN** | Orange | Bereichsverwaltung (z.B. Grundschule), eingeschraenkte Admin-Rechte |
| **TEACHER** | Blau | Beitraege erstellen, Raeume leiten, Termine und Formulare verwalten |
| **PARENT** | Gruen | Feed lesen, Jobs annehmen, Putz-Termine, Familien verwalten |
| **STUDENT** | Grau | Raum-Inhalte ansehen, eingeschraenkte Teilnahme |

### 2.2 Sonderrollen

Zusaetzlich zu den Hauptrollen gibt es optionale Sonderrollen:

| Sonderrolle | Vergabe durch | Berechtigung |
|-------------|--------------|-------------|
| **ELTERNBEIRAT** | Section Admin | Erweiterte Rechte: Termine, Formulare, Beitraege auf Bereichsebene |
| **PUTZORGA** | Section Admin | Zugriff auf Putz-Konfiguration fuer den zugewiesenen Bereich |

### 2.3 Multi-Rollen-Unterstuetzung

Personen koennen mehrere Rollen gleichzeitig haben (z.B. eine Lehrkraft, die auch Elternteil ist). Das Wechseln zwischen Rollen geschieht ueber das **Rollen-Badge** in der Kopfzeile.

**Wechselbare Rollen:** TEACHER, PARENT, SECTION_ADMIN
**Feste Rollen (kein Wechsel):** SUPERADMIN, STUDENT

Beim Rollenwechsel werden neue Zugangstokens ausgestellt und die Anzeige aktualisiert sich automatisch.

### 2.4 Raum-Rollen

Innerhalb eines Raumes gibt es zusaetzliche Rollen:

| Raum-Rolle | Rechte |
|------------|--------|
| **LEADER** | Alle MEMBER-Rechte + Threads erstellen, Mitglieder verwalten, Beitrittsanfragen bearbeiten, Fotobox konfigurieren |
| **MEMBER** | Inhalte ansehen, Beitraege schreiben, auf Threads antworten, Dateien hochladen, Chat nutzen |

### 2.5 Berechtigungsuebersicht

| Funktion | SUPERADMIN | TEACHER | PARENT | STUDENT | SECTION_ADMIN |
|----------|:----------:|:-------:|:------:|:-------:|:-------------:|
| Dashboard und Feed lesen | x | x | x | x | x |
| Beitraege erstellen | x | x | - | - | x |
| Raeume erstellen | x | x | - | - | x |
| Jobs erstellen | x | x | x | - | x |
| Termine erstellen | x | x | - | - | x |
| Formulare erstellen | x | x | Eingeschraenkt | Eingeschraenkt | x |
| Nachrichten senden | x | x | Regelbasiert | Regelbasiert | x |
| Familienverwaltung | x | - | x | x | - |
| Bereichsverwaltung | x | - | - | - | x |
| Administration | x | - | - | - | - |

---

## 3. Erste Schritte

### 3.1 Anmeldung

1. Oeffnen Sie MonteWeb im Browser (die URL erhalten Sie von Ihrer Schul-IT)
2. Geben Sie Ihre **E-Mail-Adresse** und Ihr **Passwort** ein
3. Klicken Sie auf **Anmelden**

<!-- TODO: Screenshot — Login-Seite (eingeloggt: keiner, zeige Login-Formular) -->

Falls Ihre Schule Single Sign-On (SSO) nutzt, koennen Sie alternativ den Button **Mit SSO anmelden** verwenden.

### 3.2 Registrierung

Falls die Selbstregistrierung aktiviert ist:

1. Klicken Sie auf **Registrieren** unter dem Login-Formular
2. Fuellen Sie die Pflichtfelder aus: E-Mail, Vorname, Nachname, Passwort
3. Beachten Sie die Passwort-Staerkeanzeige
4. Klicken Sie auf **Registrieren**

Nach der Registrierung werden Sie automatisch angemeldet.

### 3.3 Passwort zuruecksetzen

1. Klicken Sie auf **Passwort vergessen?** auf der Login-Seite
2. Geben Sie Ihre E-Mail-Adresse ein
3. Sie erhalten einen Link per E-Mail (sofern das E-Mail-Modul aktiviert ist)
4. Folgen Sie dem Link und vergeben Sie ein neues Passwort

### 3.4 Navigation

#### Desktop (Sidebar)

Die Seitenleiste zeigt alle verfuegbaren Navigationspunkte:

| Menuepunkt | Icon | Sichtbar fuer |
|-----------|------|--------------|
| Dashboard | Haus | Alle |
| Raeume | Kacheln | Alle |
| Familie | Personen | Eltern, Schueler, Superadmin |
| Nachrichten | Sprechblasen | Alle (wenn Modul aktiv) |
| Jobs | Aktentasche | Alle (wenn Modul aktiv) |
| Putzplan | Kalender | Alle (wenn Modul aktiv) |
| Kalender | Kalender+ | Alle (wenn Modul aktiv) |
| Formulare | Checkliste | Alle (wenn Modul aktiv) |
| Bereichsverwaltung | Organigramm | Nur Section Admins |
| Putz-Verwaltung | Schraubenschluessel | Nur PutzOrga (Sonderrolle) |
| Administration | Zahnrad | Nur Superadmin |

<!-- TODO: Screenshot — Sidebar-Navigation (eingeloggt als: admin@monteweb.local) -->

#### Mobil (Bottom Nav)

Auf Mobilgeraeten erscheint eine untere Navigationsleiste mit den wichtigsten Punkten. Weitere Optionen sind ueber den **Mehr**-Button erreichbar.

<!-- TODO: Screenshot — Mobile Bottom Nav mit Mehr-Menue geoeffnet (eingeloggt als: eltern@monteweb.local) -->

#### Kopfzeile

Die Kopfzeile zeigt von links nach rechts:

1. **Schulname/Logo** — Klick fuehrt zum Dashboard
2. **Rollen-Badge** — Zeigt die aktive Rolle (farbcodiert). Bei mehreren Rollen: Klick oeffnet Rollenwechsel
3. **Sprachumschalter** — Deutsch / Englisch
4. **Benachrichtigungsglocke** — Zeigt Anzahl ungelesener Benachrichtigungen
5. **Benutzermenue** — Profil und Abmelden

<!-- TODO: Screenshot — Kopfzeile mit Rollen-Badge und Benachrichtigungsglocke (eingeloggt als: lehrer@monteweb.local) -->

---

## 4. Dashboard

Das Dashboard ist die Startseite nach der Anmeldung. Es zeigt einen personalisierten Ueberblick:

### 4.1 Systembanner

Am oberen Rand erscheinen kontextabhaengige Systembanner, z.B.:
- Putz-Erinnerungen (nur fuer betroffene Eltern)
- Abgesagte Termine
- Wichtige Schulankuendigungen

<!-- TODO: Screenshot — Dashboard mit Systembanner (eingeloggt als: eltern@monteweb.local) -->

### 4.2 Elternstunden-Widget

Fuer Eltern mit Familienverbund zeigt das Widget:
- Aktuelle Elternstunden (Jobboerse)
- Putzstunden (separates Unterkonto)
- Fortschrittsanzeige zum Jahresziel

<!-- TODO: Screenshot — FamilyHoursWidget auf dem Dashboard (eingeloggt als: eltern@monteweb.local) -->

### 4.3 Offene Formulare

Das Formulare-Widget zeigt bis zu 5 unbearbeitete Formulare und Umfragen:
- Titel und Typ (Umfrage / Einwilligung)
- Zielgruppe (Bereich/Raum)
- Frist, falls gesetzt
- Link zur Detail-Ansicht
- **Alle anzeigen** fuehrt zur vollstaendigen Formularliste

<!-- TODO: Screenshot — DashboardFormsWidget mit offenen Formularen (eingeloggt als: eltern@monteweb.local) -->

### 4.4 Beitrags-Verfasser (Composer)

Lehrkraefte und Admins sehen den Post-Composer, um schnell einen neuen Beitrag zu erstellen:
- Optionaler Titel
- Textfeld fuer den Inhalt
- Absenden-Button

<!-- TODO: Screenshot — PostComposer auf dem Dashboard (eingeloggt als: lehrer@monteweb.local) -->

### 4.5 Feed

Unter den Widgets erscheint der Haupt-Feed mit Beitraegen aus allen Raeumen, denen der Nutzer angehoert. Details siehe Kapitel 7: Feed und Beitraege.

---

## 5. Profil und Einstellungen

Erreichbar ueber das Benutzermenue (Klick auf den Nutzernamen) > **Profil**.

### 5.1 Profilbearbeitung

Folgende Daten koennen bearbeitet werden:
- **Avatar** — Profilbild hochladen oder entfernen
- **Vorname** und **Nachname**
- **Telefonnummer** (optional)

<!-- TODO: Screenshot — Profilseite mit Avatar und Formularfeldern (eingeloggt als: lehrer@monteweb.local) -->

### 5.2 Rollenuebersicht

Die Profilseite zeigt alle zugewiesenen Rollen und Sonderrollen als farbcodierte Tags an:
- **Aktive Rolle:** Hervorgehoben
- **Weitere Rollen:** Ausgegraut, per Klick wechselbar

### 5.3 Rollenwechsel

Nutzer mit mehreren zugewiesenen Rollen sehen eine Rollenwechsel-Karte:

1. Die verfuegbaren Rollen werden als Karten angezeigt
2. Klick auf eine andere Rolle wechselt sofort
3. Die Seite aktualisiert sich automatisch — Navigation, Berechtigungen und sichtbare Inhalte passen sich an

**Beispiel:** Eine Person ist sowohl TEACHER als auch PARENT. Als TEACHER sieht sie den Post-Composer und kann Termine erstellen. Nach Wechsel zu PARENT sieht sie stattdessen das Stundenkonto und die Familienverwaltung.

<!-- TODO: Screenshot — Rollenwechsel-Karte auf der Profilseite (eingeloggt als: lehrer@monteweb.local, mit PARENT-Rolle) -->

### 5.4 Push-Benachrichtigungen

Falls vom Browser unterstuetzt, kann der Nutzer Push-Benachrichtigungen aktivieren:
- Toggle-Schalter auf der Profilseite
- Erfordert einmalige Browser-Erlaubnis

### 5.5 DSGVO-Funktionen

Am unteren Rand der Profilseite:
- **Daten exportieren** — Laedt alle persoenlichen Daten als JSON herunter
- **Konto loeschen** — Anonymisiert alle personenbezogenen Daten unwiderruflich

---

## 6. Raeume

Raeume sind das zentrale Organisationskonzept in MonteWeb. Ein Raum kann eine Klasse, eine Arbeitsgruppe oder ein Projekt darstellen.

### 6.1 Meine Raeume

Unter **Raeume** in der Navigation sehen Sie alle Raeume, denen Sie angehoeren, als Kacheln:
- Raum-Avatar
- Raumname
- Raumtyp (Klasse, Gruppe, Projekt, Benutzerdefiniert)

<!-- TODO: Screenshot — Raum-Uebersicht als Kacheln (eingeloggt als: lehrer@monteweb.local) -->

### 6.2 Raeume entdecken

Ueber den Button **Raeume entdecken** gelangen Sie zur Entdeckungsseite:

- **Offene Raeume:** Sofortiger Beitritt mit einem Klick
- **Geschlossene Raeume (Anfrage):** Beitrittsanfrage mit optionaler Nachricht senden
- **Nur auf Einladung:** Kein Beitritt moeglich, nur durch Einladung
- **Suchfeld:** Raeume nach Name filtern

<!-- TODO: Screenshot — Raum-Entdeckungsseite mit offenen und geschlossenen Raeumen (eingeloggt als: eltern@monteweb.local) -->

#### Interessensraum erstellen

Jeder angemeldete Nutzer kann einen **Interessensraum** erstellen:
1. Klick auf **Interessensraum erstellen**
2. Name, Beschreibung und optionale Tags eingeben
3. Der Raum wird erstellt und der Ersteller wird LEADER

### 6.3 Raumdetail-Ansicht

Nach Klick auf einen Raum oeffnet sich die Detailansicht mit mehreren Tabs:

| Tab | Inhalt | Bedingung |
|-----|--------|-----------|
| **Info-Board** | Raum-Feed mit Beitraegen | Immer sichtbar |
| **Mitglieder** | Mitgliederliste und -verwaltung | Immer sichtbar |
| **Diskussionen** | Themen-Threads | Immer sichtbar |
| **Termine** | Raum-Kalender | Wenn Kalender-Modul aktiv |
| **Chat** | Echtzeit-Chat | Wenn Nachrichten-Modul aktiv und Chat aktiviert |
| **Dateien** | Dateiablage | Wenn Dateien-Modul aktiv |
| **Fotobox** | Foto-Galerien | Wenn Fotobox-Modul aktiv |

<!-- TODO: Screenshot — Raumdetail mit Tabs (eingeloggt als: lehrer@monteweb.local, Raum Sonnengruppe) -->

#### Nicht-Mitglieder-Ansicht

Nutzer, die nicht Mitglied sind, sehen:
- Die oeffentliche Raumbeschreibung
- Je nach Beitrittspolitik: **Beitreten**, **Anfrage senden** oder **Nur auf Einladung**

### 6.4 Mitgliederverwaltung

Im Tab **Mitglieder** werden die Raummitglieder gruppiert angezeigt:
- **Leitung:** Alle LEADER-Mitglieder
- **Familien:** Mitglieder nach Familienzugehoerigkeit gruppiert
- **Weitere Mitglieder:** Ohne Familienzuordnung

**Aktionen fuer LEADER / Admin:**
- **Mitglied hinzufuegen:** Nutzersuche mit Rollenfilter (Alle/Lehrkraft/Eltern/Schueler/Familie)
- **Familie hinzufuegen:** Gesamte Familie zum Raum hinzufuegen
- **Beitrittsanfragen:** Eingehende Anfragen genehmigen oder ablehnen

<!-- TODO: Screenshot — Mitglieder-Tab mit Gruppierung und Anfragen-Bereich (eingeloggt als: lehrer@monteweb.local) -->

### 6.5 Raumeinstellungen (LEADER/Admin)

LEADER und Admins koennen:
- **Raum-Avatar** hochladen
- **Oeffentliche Beschreibung** bearbeiten (sichtbar auf der Entdeckungsseite)
- **Raumeinstellungen** anpassen:
  - Beitrittspolitik (Offen / Anfrage / Nur auf Einladung)
  - Diskussionsmodus (Vollstaendig / Nur Ankuendigungen / Deaktiviert)
  - Chat aktivieren/deaktivieren
- **Raum archivieren** (soft-delete, kann rueckgaengig gemacht werden)

---

## 7. Feed und Beitraege

### 7.1 Haupt-Feed

Der Feed auf dem Dashboard zeigt Beitraege aus allen Raeumen, denen der Nutzer angehoert, chronologisch sortiert (neueste zuerst).

Jeder Beitrag zeigt:
- Autor (Name + Avatar)
- Zeitstempel
- Titel (optional)
- Inhalt
- Kommentar-Anzahl
- Angepinnt-Status (falls zutreffend)

<!-- TODO: Screenshot — Feed mit mehreren Beitraegen und einem angepinnten Beitrag (eingeloggt als: eltern@monteweb.local) -->

### 7.2 Raum-Feed (Info-Board)

Im Tab **Info-Board** eines Raumes erscheinen nur Beitraege dieses Raumes.

**Beitrag erstellen** (Lehrkraefte, Admins):
1. Optionalen Titel eingeben
2. Inhalt verfassen
3. Auf **Absenden** klicken

**Gezielte Beitraege (Targeted Posts):**
Beitraege koennen so erstellt werden, dass sie nur fuer bestimmte Nutzer sichtbar sind. Dies wird intern ueber Zielgruppen-IDs gesteuert.

### 7.3 Kommentare

- Klick auf einen Beitrag oeffnet die Kommentare
- Kommentarfeld am unteren Rand
- Kommentare werden chronologisch angezeigt

### 7.4 Beitraege anpinnen

LEADER und Admins koennen Beitraege anpinnen. Angepinnte Beitraege erscheinen immer oben im Feed.

### 7.5 Systembanner

Systembanner erscheinen oberhalb des Feeds und informieren ueber:
- Anstehende Putz-Termine (nur fuer betroffene Eltern)
- Abgesagte Termine
- Systemweite Ankuendigungen

---

## 8. Diskussionen

Diskussions-Threads ermoeglichen themenbezogene Gespraeche innerhalb eines Raumes.

### 8.1 Thread-Uebersicht

Im Tab **Diskussionen** eines Raumes werden alle Threads aufgelistet:
- Thread-Titel
- Autor
- Anzahl der Antworten
- Zeitstempel
- Zielgruppe (falls eingeschraenkt)

<!-- TODO: Screenshot — Diskussions-Tab mit Thread-Liste (eingeloggt als: lehrer@monteweb.local) -->

### 8.2 Thread erstellen (LEADER/Admin)

1. Klick auf **Neues Thema**
2. Titel eingeben
3. Inhalt verfassen
4. **Zielgruppe waehlen:**
   - **Alle** — Sichtbar fuer alle Raummitglieder
   - **Eltern** — Nur fuer Eltern und Lehrkraefte sichtbar
   - **Kinder** — Nur fuer Schueler und Lehrkraefte sichtbar
5. Absenden

### 8.3 Antworten

Alle Raummitglieder (je nach Zielgruppe) koennen auf Threads antworten. Antworten werden chronologisch angezeigt.

### 8.4 Thread verwalten (LEADER/Admin)

- **Archivieren:** Thread wird ausgeblendet, bleibt aber gespeichert
- **Loeschen:** Thread wird endgueltig entfernt

---

## 9. Dateien

Das Dateien-Modul ermoeglicht eine strukturierte Dateiablage innerhalb von Raeumen.

### 9.1 Datei-Uebersicht

Im Tab **Dateien** eines Raumes:
- Ordnerstruktur mit Breadcrumb-Navigation
- Dateien und Ordner als Liste
- Icons je nach Dateityp

<!-- TODO: Screenshot — Dateien-Tab mit Ordnern und Dateien (eingeloggt als: lehrer@monteweb.local) -->

### 9.2 Ordner erstellen

1. Klick auf **Neuer Ordner**
2. Ordnername eingeben
3. **Sichtbarkeit waehlen** (nur fuer Lehrkraefte, LEADER und Admins):
   - **Alle** — Sichtbar fuer alle Raummitglieder
   - **Nur Eltern** — Sichtbar fuer Eltern, Lehrkraefte, LEADER und Admins
   - **Nur Schueler** — Sichtbar fuer Schueler, Lehrkraefte, LEADER und Admins
4. Erstellen

**Hinweis fuer Eltern:** Wenn Eltern einen Ordner erstellen, wird die Sichtbarkeit automatisch auf **Nur Eltern** gesetzt. Das Auswahlfeld erscheint nicht.

<!-- TODO: Screenshot — Ordner-erstellen-Dialog mit Sichtbarkeitsauswahl (eingeloggt als: lehrer@monteweb.local) -->

### 9.3 Dateien hochladen

1. Klick auf **Hochladen**
2. Datei(en) auswaehlen
3. **Sichtbarkeit waehlen** (nur fuer Lehrkraefte/LEADER/Admins):
   - Alle / Nur Eltern / Nur Schueler
4. Upload starten

### 9.4 Sichtbarkeits-Tags

Jeder Ordner und jede Datei zeigt einen farbcodierten Tag:
- **Alle** — Standard (kein Tag)
- **Nur Eltern** — Orange Tag
- **Nur Schueler** — Blauer Tag

Nutzer sehen nur die Inhalte, die fuer ihre Rolle freigegeben sind. LEADER, Lehrkraefte und Admins sehen immer alles.

### 9.5 Dateien herunterladen und loeschen

- **Herunterladen:** Klick auf den Dateinamen
- **Loeschen:** Klick auf das Loeschen-Icon (nur Ersteller, LEADER oder Admin)

---

## 10. Fotobox

Die Fotobox ermoeglicht organisierte Foto-Galerien innerhalb von Raeumen.

### 10.1 Berechtigungsstufen

Die Fotobox hat drei hierarchische Berechtigungsstufen:

| Stufe | Kann |
|-------|------|
| **NUR ANSEHEN** | Foto-Threads und Bilder ansehen, Lightbox nutzen |
| **BILDER POSTEN** | Bilder in bestehende Threads hochladen (max. 20 pro Upload) |
| **THREADS ERSTELLEN** | Neue Foto-Threads erstellen |

**Automatische Zuweisung:**
- SUPERADMIN und Raum-LEADER haben immer THREADS ERSTELLEN
- Andere Mitglieder erhalten die Standardberechtigung des Raumes (konfigurierbar durch LEADER)

### 10.2 Fotobox-Einstellungen (LEADER/Admin)

Im Tab **Fotobox** > Einstellungen:
- **Standardberechtigung** fuer Raummitglieder festlegen

<!-- TODO: Screenshot — Fotobox-Einstellungen (eingeloggt als: lehrer@monteweb.local) -->

### 10.3 Thread erstellen

1. Klick auf **Neues Album** (nur mit THREADS ERSTELLEN-Berechtigung)
2. Titel und optionale Beschreibung eingeben
3. **Sichtbarkeit waehlen** (Lehrkraefte/LEADER/Admins):
   - **Alle** — Fuer alle Raummitglieder
   - **Nur Eltern** — Nur Eltern, Lehrkraefte und Admins
   - **Nur Schueler** — Nur Schueler, Lehrkraefte und Admins
4. Erstellen

**Hinweis fuer Eltern:** Threads, die von Eltern erstellt werden, erhalten automatisch die Sichtbarkeit **Nur Eltern**.

<!-- TODO: Screenshot — Fotobox mit Thread-Liste und Sichtbarkeits-Tags (eingeloggt als: lehrer@monteweb.local) -->

### 10.4 Bilder hochladen

1. Thread oeffnen
2. Klick auf **Bilder hochladen** (nur mit BILDER POSTEN-Berechtigung oder hoeher)
3. Bis zu 20 Bilder pro Upload auswaehlen
4. Thumbnails werden automatisch generiert

### 10.5 Lightbox

Klick auf ein Bild oeffnet die Lightbox-Ansicht:
- Vollbildanzeige
- Vor-/Zurueck-Navigation
- Bildunterschrift (falls vorhanden)

### 10.6 Bilder verwalten

- **Bildunterschrift bearbeiten:** Klick auf das Bearbeiten-Icon
- **Sortierung aendern:** Sortierindex anpassen
- **Loeschen:** Nur Ersteller, LEADER oder Admin

---

## 11. Kalender und Termine

### 11.1 Kalenderuebersicht

Die Kalenderansicht zeigt Termine als chronologische Liste, gegliedert nach Monaten:
- Monatsnavigation (Zurueck / Heute / Vor)
- Terminliste mit: Datum, Uhrzeit, Titel, Umfang (Raum/Bereich/Schule), Ort, RSVP-Zaehler
- Abgesagte Termine mit "Abgesagt"-Tag

<!-- TODO: Screenshot — Kalenderansicht mit Monatsnavigation und Terminliste (eingeloggt als: lehrer@monteweb.local) -->

### 11.2 Termine erstellen

**Berechtigung je nach Umfang:**

| Umfang | Wer darf erstellen |
|--------|-------------------|
| **Raum** | Raum-LEADER, SUPERADMIN, ELTERNBEIRAT |
| **Bereich** | TEACHER, SECTION_ADMIN, SUPERADMIN, ELTERNBEIRAT |
| **Schule** | SUPERADMIN, ELTERNBEIRAT |

**Formularfelder:**
1. Titel
2. Beschreibung (optional)
3. Ort (optional)
4. Ganztaegig (Ja/Nein)
5. Start-Datum und -Uhrzeit
6. End-Datum und -Uhrzeit
7. Umfang: Raum / Bereich / Schule
8. Raum-Auswahl (bei Umfang Raum)
9. Wiederholung: Keine / Taeglich / Woechentlich / Monatlich / Jaehrlich
10. Wiederholungsende (optional)

<!-- TODO: Screenshot — Termin-erstellen-Formular mit Umfangauswahl (eingeloggt als: lehrer@monteweb.local) -->

### 11.3 Termindetail und RSVP

Jeder Nutzer kann auf einen Termin reagieren:
- **Zusagen** (gruen)
- **Vielleicht** (gelb)
- **Absagen** (rot)

Die Zaehler werden live aktualisiert.

<!-- TODO: Screenshot — Termindetail mit RSVP-Buttons und Zaehler (eingeloggt als: eltern@monteweb.local) -->

### 11.4 Jobs mit Terminen verknuepfen

Auf der Termindetail-Seite koennen:
- Bestehende Jobs verknuepft werden
- Neue Jobs direkt fuer diesen Termin erstellt werden

So wird die Freiwilligen-Koordination mit dem Terminplan verbunden.

### 11.5 Termin absagen und loeschen

**Termin absagen** (Ersteller/Admin):
- Alle Nutzer, die den Termin sehen koennen, erhalten einen Feed-Beitrag
- Der Termin bleibt mit "Abgesagt"-Tag sichtbar

**Termin loeschen** (Ersteller/Admin):
- Nur Nutzer, die zugesagt haben, erhalten einen gezielten Feed-Beitrag
- Der Termin wird entfernt

### 11.5 Feiertage und Schulferien

Der Kalender beruecksichtigt:
- **Gesetzliche Feiertage** je nach konfiguriertem Bundesland (Standard: Bayern)
- **Schulferien** (vom Admin konfigurierbar)

In Datumswaehlern (z.B. Putztermine) werden Feiertage rot und Schulferien orange markiert.

---

## 12. Formulare und Umfragen

Das Formular-Modul ermoeglicht Umfragen, Einwilligungserklaerungen und Abfragen mit verschiedenen Fragetypen.

<!-- TODO: Screenshot — FormsView.vue, eingeloggt als: lehrer@monteweb.local -->

### 12.1 Formular-Typen

| Typ | Beschreibung | Kennzeichnung |
|-----|-------------|---------------|
| **SURVEY** | Allgemeine Umfrage | Blau (Info) |
| **CONSENT** | Einwilligungserklaerung | Orange (Warnung) |

### 12.2 Formular erstellen

**Berechtigung:** TEACHER, SECTION_ADMIN, SUPERADMIN

<!-- TODO: Screenshot — FormCreateView.vue, eingeloggt als: lehrer@monteweb.local -->

1. Formulare → **„Erstellen"**
2. Titel und optionale Beschreibung eingeben
3. **Typ** waehlen (Umfrage oder Einwilligung)
4. **Geltungsbereich** festlegen:
   - **Schule**: Alle Nutzer der Schule
   - **Bereich**: Nutzer bestimmter Schulbereiche (Mehrfachauswahl moeglich)
   - **Raum**: Nutzer eines bestimmten Raums
5. Optional: **Deadline** setzen
6. Optional: **Anonym** markieren (Antworten ohne Nutzerzuordnung)

### 12.3 Fragen hinzufuegen

Verfuegbare Fragetypen:

| Typ | Beschreibung |
|-----|-------------|
| **TEXT** | Freitext-Eingabe |
| **SINGLE_CHOICE** | Eine Antwort aus mehreren Optionen |
| **MULTIPLE_CHOICE** | Mehrere Antworten moeglich |
| **RATING** | Bewertungsskala (1-5 oder 1-10) |
| **YES_NO** | Ja/Nein-Auswahl |

- Fragen koennen als **Pflichtfeld** markiert werden
- Reihenfolge per Drag-and-Drop aendern
- Optionen bei Choice-Fragen frei definierbar

### 12.4 Multi-Bereich (Multi-Section)

Formulare mit Geltungsbereich **SECTION** koennen an mehrere Schulbereiche gleichzeitig gerichtet werden:

<!-- TODO: Screenshot — FormCreateView.vue mit Bereichs-Mehrfachauswahl, eingeloggt als: lehrer@monteweb.local -->

- Im Erstell-Dialog erscheint eine **MultiSelect-Komponente** mit allen verfuegbaren Bereichen
- Beispiel: Ein Formular fuer „Grundstufe" UND „Mittelstufe" gleichzeitig
- In der Formularliste werden die Zielbereiche als kommagetrennte Liste angezeigt

### 12.5 Formular-Lebenszyklus

```
DRAFT → PUBLISHED → CLOSED → ARCHIVED
```

| Status | Beschreibung | Aktionen |
|--------|-------------|----------|
| **DRAFT** | Entwurf, noch nicht sichtbar | Bearbeiten, Veroeffentlichen |
| **PUBLISHED** | Aktiv, Antworten moeglich | Schliessen |
| **CLOSED** | Keine neuen Antworten | Archivieren |
| **ARCHIVED** | Nur noch in der Historie | — |

### 12.6 Antworten und Ergebnisse

**Antworten abgeben** (alle Zielgruppen-Nutzer):

<!-- TODO: Screenshot — FormDetailView.vue Antwortformular, eingeloggt als: eltern@monteweb.local -->

- Formular oeffnen → Fragen beantworten → Absenden
- Bei Einwilligungen: Explizite Ja/Nein-Auswahl
- Bereits beantwortete Formulare sind mit gruener Haekchen-Markierung versehen

**Ergebnisse einsehen** (Ersteller/Admin):

<!-- TODO: Screenshot — FormDetailView.vue Ergebnis-Tab, eingeloggt als: lehrer@monteweb.local -->

- Fortschrittsbalken zeigt Ruecklaufquote
- Einzelantworten (sofern nicht anonym)
- Zusammenfassung pro Frage
- **Export**: CSV oder PDF

### 12.7 Dashboard-Widget

Auf dem Dashboard werden offene Formulare als Widget angezeigt:

<!-- TODO: Screenshot — DashboardView.vue mit Formular-Widget, eingeloggt als: eltern@monteweb.local -->

- Maximal 5 unbearbeitete Formulare
- Typ-Kennzeichnung (Info/Warnung)
- Geltungsbereich und Deadline
- Direktlink zum Formular
- „Alle anzeigen" fuehrt zur Formular-Uebersicht

---

## 13. Nachrichten

Das Messaging-Modul bietet Direktnachrichten und Raum-Chat.

<!-- TODO: Screenshot — MessagingView.vue, eingeloggt als: lehrer@monteweb.local -->

### 13.1 Direktnachrichten

**Neue Nachricht senden:**
1. Nachrichten-Seite oeffnen
2. **„Neue Nachricht"** klicken
3. Empfaenger suchen und auswaehlen
4. Nachricht verfassen und senden

<!-- TODO: Screenshot — MessagingView.vue mit Konversation, eingeloggt als: lehrer@monteweb.local -->

**Konversationsliste:**
- Alle laufenden Konversationen auf der linken Seite
- Ungelesene Nachrichten werden hervorgehoben
- Letzte Nachricht als Vorschau
- Zeitstempel der letzten Aktivitaet

### 13.2 Kommunikationsregeln

Die erlaubten Kommunikationswege sind konfigurierbar:

| Von → An | Standard | Konfigurierbar |
|----------|----------|----------------|
| Lehrer ↔ Eltern | Immer erlaubt | Nein |
| Lehrer ↔ Schueler | Erlaubt | Ja |
| Eltern ↔ Eltern | Gesperrt | Ja |
| Schueler ↔ Schueler | Gesperrt | Ja |

Der Admin kann unter **Administration → Einstellungen** die Kommunikationsregeln anpassen.

### 13.3 Raum-Chat

Jeder Raum verfuegt ueber Chat-Kanaele:

| Kanal | Sichtbar fuer | Beschreibung |
|-------|---------------|-------------|
| **MAIN** | Alle Mitglieder | Allgemeiner Kanal |
| **TEACHERS/PARENTS** | Lehrer + Eltern | Fuer Erwachsene |
| **STUDENTS** | Schueler | Nur Schueler |

- Zugang ueber den **Chat-Tab** im Raum
- Echtzeit-Nachrichtenauslieferung via WebSocket
- Nachrichten sind raumgebunden und nicht in der Direktnachrichten-Ansicht sichtbar

---

## 14. Jobboerse und Elternstunden

Das Jobboerse-Modul verwaltet schulische Aufgaben, fuer die Eltern Stunden gutgeschrieben bekommen.

<!-- TODO: Screenshot — JobsView.vue, eingeloggt als: lehrer@monteweb.local -->

### 14.1 Jobs erstellen

**Berechtigung:** TEACHER, SECTION_ADMIN, SUPERADMIN

<!-- TODO: Screenshot — Job-Erstelldialog, eingeloggt als: lehrer@monteweb.local -->

1. Jobboerse → **„Job erstellen"**
2. Titel, Beschreibung eingeben
3. **Stundenwert** festlegen (z.B. 2.0 Stunden)
4. **Maximale Teilnehmer** setzen
5. Optional: Mit Kalender-Event verknuepfen
6. Optional: Deadline fuer Bewerbungen

### 14.2 Auf Jobs bewerben

**Berechtigung:** PARENT

<!-- TODO: Screenshot — JobDetailView.vue, eingeloggt als: eltern@monteweb.local -->

- Job oeffnen → **„Bewerben"** klicken
- Optionale Nachricht an den Ersteller
- Status wird zu „Beworben"
- Ersteller entscheidet ueber Zuweisung

### 14.3 Zuweisung und Abschluss

**Workflow:**

```
Offen → Beworben → Zugewiesen → Gestartet → Abgeschlossen → Bestaetigt
```

| Schritt | Aktion | Durch |
|---------|--------|-------|
| Zuweisen | Bewerber auswaehlen | Ersteller |
| Starten | Arbeit beginnen | Zugewiesener Elternteil |
| Abschliessen | Arbeit erledigt melden | Zugewiesener Elternteil |
| Bestaetigen | Stunden gutschreiben | Ersteller |

### 14.4 Stundenuebersicht

**Familien-Stundenkonto:**

<!-- TODO: Screenshot — FamilyHoursWidget im Dashboard, eingeloggt als: eltern@monteweb.local -->

- Dashboard-Widget zeigt aktuellen Stundenstand
- Getrennte Konten fuer **regulaere Stunden** (Jobboerse) und **Putzstunden**
- Detailansicht unter Familienverbund → Stunden
- **PDF-Export** der Stundenabrechnung (Admin)
- **Report-Export** fuer Auswertungen (Admin)

---

## 15. Putzorganisation

Das Putz-Modul organisiert Reinigungsdienste mit QR-Check-in und Stundengutschrift.

<!-- TODO: Screenshot — CleaningView.vue, eingeloggt als: eltern@monteweb.local -->

### 15.1 Putzplan-Uebersicht

Die Uebersicht zeigt alle verfuegbaren Putztermine:

- **Wiederkehrende Termine**: Woechentlich an bestimmten Tagen (z.B. jeden Mittwoch)
- **Einmalige Putzaktionen**: An einem bestimmten Datum (z.B. Grossputz am 15.03.)
- Freie Plaetze pro Termin
- Bereits registrierte Teilnehmer
- Eigene Registrierungen hervorgehoben

### 15.2 Fuer Putztermin anmelden

**Berechtigung:** PARENT (Opt-in-System)

1. Putzplan → verfuegbaren Termin auswaehlen
2. **„Anmelden"** klicken
3. Registrierung wird bestaetigt
4. Termin erscheint in der eigenen Uebersicht

**Tauschen:**
- Angemeldete koennen einen Tausch anbieten
- Andere Eltern koennen den Tausch annehmen
- Der urspruengliche Teilnehmer wird automatisch abgemeldet

### 15.3 QR-Check-in/-out

Am Putztermin vor Ort:

<!-- TODO: Screenshot — QR-Check-in Seite, eingeloggt als: eltern@monteweb.local -->

1. QR-Code am Putzort scannen (mit Smartphone-Kamera)
2. **Check-in** wird mit Zeitstempel registriert
3. Nach getaner Arbeit erneut QR-Code scannen
4. **Check-out** schliesst den Putztermin ab
5. Putzstunden werden dem Familien-Sonderkonto gutgeschrieben

**QR-Codes verwalten** (Admin):
- Administration → Putzorganisation → QR-Codes
- QR-Codes pro Putzort generieren und drucken

### 15.4 Putztermine konfigurieren

**Berechtigung:** SUPERADMIN, Nutzer mit Rolle PUTZORGA

<!-- TODO: Screenshot — AdminCleaning.vue, eingeloggt als: admin@monteweb.local -->

**Wiederkehrende Termine:**
- Wochentag, Uhrzeit, maximale Teilnehmer, Stundenwert
- Werden automatisch fuer jede Woche generiert

**Einmalige Putzaktionen:**
- Spezifisches Datum waehlen
- Feiertage (rot) und Schulferien (orange) im Datumswaehler sichtbar
- Titel, Beschreibung, Teilnehmerlimit

**Putz-Dashboard** (Admin):
- Uebersicht aller Termine und Registrierungen
- Anwesenheitsstatistik
- Stundenuebersicht pro Familie

### 15.5 Feiertage im Datumswaehler

Bei der Konfiguration von Putzaktionen zeigt der Datumswaehler:
- **Rote Markierung**: Gesetzliche Feiertage (je nach konfiguriertem Bundesland)
- **Orange Markierung**: Schulferien
- Unterstuetzte Bundeslaender: Alle 16 deutschen Bundeslaender
- Standard-Bundesland: Bayern (BY), aenderbar unter Administration → Design & Einstellungen

---

## 16. Familienverbund

Der Familienverbund ist die zentrale Abrechnungseinheit fuer Elternstunden.

<!-- TODO: Screenshot — FamilyView.vue, eingeloggt als: eltern@monteweb.local -->

### 16.1 Familie erstellen

**Berechtigung:** PARENT

1. Unter **Familienverbund** → **„Familie erstellen"**
2. Familienname eingeben
3. Die erstellende Person wird automatisch Familienoberhaupt

**Regeln:**
- Ein Elternteil gehoert zu genau einem Familienverbund
- Kinder koennen mehreren Familien zugeordnet sein (z.B. bei getrennten Eltern)

### 16.2 Familienmitglieder einladen

<!-- TODO: Screenshot — Einladungsdialog, eingeloggt als: eltern@monteweb.local -->

1. Familienverbund → **„Mitglied einladen"**
2. Nutzer suchen (Name oder E-Mail)
3. **Rolle waehlen**: Elternteil (PARENT) oder Kind (CHILD)
4. Einladung senden

**Einladung annehmen/ablehnen:**
- Der Eingeladene erhaelt eine Benachrichtigung
- Ueber die Benachrichtigungsseite: Annehmen oder Ablehnen
- Bei Annahme wird die Person dem Familienverbund hinzugefuegt

### 16.3 Stundenkonto

Jede Familie hat zwei Stundenkonten:

| Konto | Quelle | Beschreibung |
|-------|--------|-------------|
| **Regulaere Stunden** | Jobboerse | Jobs, fuer die Eltern sich beworben und abgeschlossen haben |
| **Putzstunden** | Putzorganisation | Sonderkonto fuer geleistete Putzstunden |

- Stundenstand auf dem Dashboard sichtbar (Widget)
- Detailansicht im Familienverbund
- Admin kann Stunden manuell korrigieren
- PDF-Export der Stundenabrechnung

---

## 17. Benachrichtigungen

Das Benachrichtigungssystem informiert Nutzer ueber relevante Ereignisse.

<!-- TODO: Screenshot — NotificationsView.vue, eingeloggt als: eltern@monteweb.local -->

### 17.1 Benachrichtigungstypen

| Typ | Beschreibung | Empfaenger |
|-----|-------------|-----------|
| Neuer Feed-Beitrag | Post im eigenen Raum | Raum-Mitglieder |
| Kommentar | Antwort auf eigenen Beitrag | Beitragsautor |
| Neue Nachricht | Direktnachricht erhalten | Empfaenger |
| Familien-Einladung | Einladung zum Familienverbund | Eingeladener |
| Beitrittsanfrage | Raumanfrage eingegangen | LEADER des Raums |
| Anfrage genehmigt | Raumanfrage akzeptiert | Antragsteller |
| Job-Bewerbung | Bewerbung auf einen Job | Job-Ersteller |
| Job-Zuweisung | Job wurde zugewiesen | Bewerber |
| Putz-Erinnerung | Putztermin steht an | Registrierte |
| Neues Formular | Formular veroeffentlicht | Zielgruppe |
| Kalender-Event | Neuer/abgesagter Termin | Raum-Mitglieder |

### 17.2 Benachrichtigungskanaele

| Kanal | Beschreibung | Voraussetzung |
|-------|-------------|---------------|
| **In-App** | Glocken-Symbol in der Navigation | Immer aktiv |
| **Push** | Browser-Benachrichtigung | Push-Modul aktiviert + Nutzer-Erlaubnis |
| **WebSocket** | Echtzeit-Updates | Automatisch bei offener App |

### 17.3 Benachrichtigungen verwalten

- **Ungelesene Zaehler**: Rote Zahl am Glocken-Symbol
- Klick auf Benachrichtigung → Weiterleitung zum relevanten Inhalt
- **„Alle als gelesen markieren"** — setzt Zaehler zurueck
- Push-Benachrichtigungen koennen im Profil aktiviert/deaktiviert werden

---

## 18. Bereichsverwaltung (Section Admin)

Section Admins verwalten einen oder mehrere Schulbereiche mit eingeschraenkten Adminrechten. Die Zuweisung erfolgt explizit durch einen Superadmin ueber die Nutzerverwaltung (Administration → Nutzerverwaltung → Nutzer bearbeiten → Bereichsverwaltung zuweisen). Dabei werden `SECTION_ADMIN:<bereichs-id>` Eintraege in den Sonderrollen des Nutzers gespeichert.

<!-- TODO: Screenshot — SectionAdminView.vue, eingeloggt als: sectionadmin@monteweb.local -->

### 18.1 Bereichswechsel

- Oben in der Verwaltungsansicht: **Bereichs-Selektor**
- Wechsel zwischen explizit zugewiesenen Bereichen (z.B. Sonnengruppe ↔ Sternengruppe)
- Alle Ansichten passen sich dem gewaehlten Bereich an

### 18.2 Nutzerverwaltung im Bereich

<!-- TODO: Screenshot — SectionAdminView.vue Nutzer-Tab, eingeloggt als: sectionadmin@monteweb.local -->

- Alle Nutzer des gewaehlten Bereichs auflisten
- Nutzer suchen und filtern (nach Rolle, Name)
- Nutzerdetails einsehen
- Nutzer zu Raeumen des Bereichs zuweisen

### 18.3 Raeume im Bereich

- Alle Raeume des Bereichs anzeigen
- Neue Raeume fuer den Bereich erstellen
- Raummitglieder verwalten
- Raum archivieren

### 18.4 Sonderrollen vergeben

Der Section Admin kann fuer seinen Bereich Sonderrollen vergeben:

| Sonderrolle | Beschreibung |
|-------------|-------------|
| **ELTERNBEIRAT** | Elternvertretung des Bereichs |
| **PUTZORGA** | Zustaendig fuer die Putzorganisation |

- Sonderrollen werden ueber die Nutzerverwaltung des Bereichs zugewiesen
- Ein Nutzer kann mehrere Sonderrollen haben

---

## 19. Administration (SUPERADMIN)

Der Superadmin hat Zugriff auf alle Verwaltungsfunktionen.

<!-- TODO: Screenshot — AdminDashboard.vue, eingeloggt als: admin@monteweb.local -->

### 19.1 Nutzerverwaltung

<!-- TODO: Screenshot — AdminUsers.vue, eingeloggt als: admin@monteweb.local -->

| Funktion | Beschreibung |
|----------|-------------|
| Nutzer auflisten | Alle Nutzer mit Suchfunktion und Rollenfilter |
| Nutzer erstellen | Manuell neue Accounts anlegen |
| Nutzer bearbeiten | Name, E-Mail, Rolle aendern |
| Status aendern | Aktiv / Gesperrt / Geloescht |
| Rolle zuweisen | STUDENT, PARENT, TEACHER, SECTION_ADMIN, SUPERADMIN |
| Sonderrollen | ELTERNBEIRAT, PUTZORGA zuweisen |
| Familie zuordnen | Nutzer einem Familienverbund zuweisen |

### 19.2 Raum-Verwaltung

<!-- TODO: Screenshot — AdminRooms.vue, eingeloggt als: admin@monteweb.local -->

- Alle Raeume auflisten (aktiv und archiviert)
- Neue Raeume erstellen und Bereich zuweisen
- Raum-Einstellungen bearbeiten
- Mitglieder verwalten (LEADER, MEMBER)
- Raeume archivieren/wiederherstellen

### 19.3 Bereichsverwaltung

<!-- TODO: Screenshot — AdminSections.vue, eingeloggt als: admin@monteweb.local -->

- Schulbereiche erstellen und bearbeiten
- Beispiel-Bereiche: Kinderhaus, Grundstufe, Mittelstufe, Oberstufe
- Bereiche haben einen Namen, eine Beschreibung und eine Sortierreihenfolge
- Section Admins werden ueber die Nutzerverwaltung zugewiesen

### 19.4 Modul-Verwaltung

<!-- TODO: Screenshot — AdminModules.vue, eingeloggt als: admin@monteweb.local -->

Optionale Module koennen aktiviert oder deaktiviert werden:

| Modul | Konfigurations-Key |
|-------|-------------------|
| Nachrichten | `monteweb.modules.messaging.enabled` |
| Dateiablage | `monteweb.modules.files.enabled` |
| Jobboerse | `monteweb.modules.jobboard.enabled` |
| Putzorganisation | `monteweb.modules.cleaning.enabled` |
| Kalender | `monteweb.modules.calendar.enabled` |
| Formulare | `monteweb.modules.forms.enabled` |
| Fotobox | `monteweb.modules.fotobox.enabled` |

- Deaktivierte Module sind im Menue nicht sichtbar
- Backend-Endpunkte deaktivierter Module geben 404 zurueck

### 19.5 Design und Einstellungen

<!-- TODO: Screenshot — AdminTheme.vue, eingeloggt als: admin@monteweb.local -->

**Theme-Konfiguration:**
- Primaerfarbe der Anwendung
- Schullogo hochladen (wird in Navigation und Login angezeigt)
- Schulname und Beschreibung

**Regionale Einstellungen:**
- **Bundesland**: Bestimmt die angezeigten Feiertage (alle 16 Bundeslaender verfuegbar)
- **Schulferien**: Manuell konfigurierbar mit Name, Von-Datum, Bis-Datum

**Kommunikationsregeln:**
- Eltern ↔ Eltern Kommunikation erlauben/sperren
- Schueler ↔ Schueler Kommunikation erlauben/sperren

### 19.6 Putzorganisation konfigurieren

<!-- TODO: Screenshot — AdminCleaning.vue, eingeloggt als: admin@monteweb.local -->

- Wiederkehrende Putztermine anlegen (Wochentag, Uhrzeit, Plaetze)
- Einmalige Putzaktionen erstellen (mit Datum)
- Putztermine generieren
- QR-Codes fuer Putzorte erstellen und drucken
- Putz-Dashboard mit Statistiken

### 19.7 Berichte und Exporte

| Export | Format | Beschreibung |
|--------|--------|-------------|
| Elternstunden | PDF | Stundenabrechnung pro Familie |
| Jobbericht | CSV/PDF | Uebersicht aller Jobs und Zuweisungen |
| Formularergebnisse | CSV/PDF | Antworten eines Formulars |
| Putzstatistik | CSV | Anwesenheit und Stunden |

### 19.8 Audit-Log

<!-- TODO: Screenshot — AdminAuditLog.vue, eingeloggt als: admin@monteweb.local -->

- Chronologische Auflistung aller administrativen Aktionen
- Filtermoeglich nach Zeitraum, Nutzer, Aktionstyp
- Erfasste Aktionen: Login, Nutzererstellung, Rollenänderung, Modulaenderungen, Konfigurationsaenderungen
- Dient der Nachvollziehbarkeit und DSGVO-Compliance

---

## 20. Technische Hinweise

### 20.1 Unterstuetzte Browser

| Browser | Mindestversion |
|---------|---------------|
| Chrome | 90+ |
| Firefox | 90+ |
| Safari | 15+ |
| Edge | 90+ |

- Mobile Browser werden vollstaendig unterstuetzt
- Internet Explorer wird **nicht** unterstuetzt

### 20.2 Mobile Nutzung

Die Anwendung ist vollstaendig responsiv:

- **Desktop**: Seitliche Navigation (Sidebar)
- **Mobil**: Untere Navigationsleiste (BottomNav)
- Alle Funktionen sind auf beiden Geraetetypen verfuegbar
- PWA-faehig (Progressive Web App) — kann zum Homescreen hinzugefuegt werden

### 20.3 Sprachen

| Sprache | Code | Status |
|---------|------|--------|
| Deutsch | de | Standard, vollstaendig |
| Englisch | en | Vollstaendig |

- Automatische Erkennung der Browser-Sprache
- Manueller Wechsel ueber das Profil moeglich

### 20.4 Datenschutz (DSGVO)

MonteWeb implementiert folgende Datenschutz-Massnahmen:

- **Datenexport**: Nutzer koennen unter Profil → ihre gespeicherten Daten als JSON exportieren
- **Account-Loeschung**: Nutzer koennen ihren Account vollstaendig loeschen (Profil → Konto loeschen)
- **Anonyme Formulare**: Antworten koennen nicht auf Nutzer zurueckgefuehrt werden
- **Audit-Log**: Alle administrativen Aktionen werden protokolliert
- **Datensparsamkeit**: Nur fuer den Betrieb notwendige Daten werden gespeichert
- **Verschluesselung**: HTTPS fuer alle Verbindungen, Passwoerter BCrypt-gehasht
- **Session-Management**: JWT mit 15-Minuten-Ablauf, Refresh-Token mit 7-Tagen-Ablauf

### 20.5 Sicherheitshinweise

- Passwoerter muessen mindestens 8 Zeichen lang sein
- Nach mehreren fehlgeschlagenen Logins wird der Account voruebergehend gesperrt (Rate-Limiting)
- Alle Datei-Uploads werden auf zulaessige Typen geprueft
- Bildformate werden anhand von Magic Bytes (nicht Dateierweiterung) validiert
- Maximale Upload-Groesse: 20 Dateien pro Vorgang

---

## Anhang A: Tastenkuerzel und Navigation

| Aktion | Desktop | Mobil |
|--------|---------|-------|
| Dashboard | Sidebar → Dashboard | BottomNav → Home |
| Raeume | Sidebar → Raeume | BottomNav → Raeume |
| Feed | Sidebar → Feed | BottomNav → Feed |
| Nachrichten | Sidebar → Nachrichten | BottomNav → Chat |
| Benachrichtigungen | Glocke oben rechts | Glocke oben rechts |
| Profil | Avatar oben rechts | Avatar oben rechts |
| Suche | Suchfeld in der Navigation | Suchfeld in der Navigation |
| Zurueck | Browser-Zurueck oder Breadcrumb | Geraete-Zurueck oder Breadcrumb |

---

## Anhang B: Glossar

| Begriff | Beschreibung |
|---------|-------------|
| **Audience** | Sichtbarkeit/Zielgruppe eines Inhalts (ALL, PARENTS_ONLY, STUDENTS_ONLY) |
| **Bereich / Section** | Organisationseinheit der Schule (z.B. Kinderhaus, Grundstufe, Mittelstufe, Oberstufe) |
| **Einwilligung (Consent)** | Formulartyp, der eine explizite Ja/Nein-Entscheidung erfordert |
| **Elternstunden** | Arbeitsstunden, die Eltern durch Jobs und Putztermine sammeln |
| **Familienverbund** | Abrechnungseinheit fuer Elternstunden; besteht aus Eltern und Kindern |
| **Feed** | Zentraler Nachrichtenstrom mit Beitraegen, Kommentaren und Bannern |
| **Fotobox** | Fotogalerie innerhalb eines Raums, organisiert in Threads |
| **LEADER** | Raumrolle mit erweiterten Rechten (Raum verwalten, Threads erstellen) |
| **MinIO** | Objektspeicher-System fuer Dateien und Bilder |
| **Modul** | Optionale Funktionseinheit, die aktiviert/deaktiviert werden kann |
| **Multi-Section** | Formular, das an mehrere Schulbereiche gleichzeitig gerichtet ist |
| **Putzaktion** | Einmaliger oder wiederkehrender Reinigungstermin |
| **QR-Check-in** | Anwesenheitserfassung via QR-Code-Scan am Putzort |
| **Raum / Room** | Virtuelle Gruppe fuer Klassen oder AGs mit eigenen Tabs (Feed, Dateien, etc.) |
| **RSVP** | Zu-/Absage fuer Kalender-Events |
| **Section Admin** | Administrator mit eingeschraenkten Rechten fuer explizit zugewiesene Schulbereiche |
| **Sonderrolle** | Zusaetzliche Funktion (ELTERNBEIRAT, PUTZORGA) neben der Hauptrolle |
| **Superadmin** | Administrator mit vollen Systemrechten |
| **Targeted Post** | Feed-Beitrag, der nur fuer bestimmte Nutzer sichtbar ist |
| **Thread** | Diskussionsfaden in einem Raum oder Foto-Sammlung in der Fotobox |
| **Thumbnail** | Automatisch generierte Vorschaubilder fuer Fotos |

---

## Anhang C: Rollenmatrix — Detailuebersicht

### C.1 Feed und Beitraege

| Aktion | STUDENT | PARENT | TEACHER | SECTION_ADMIN | SUPERADMIN |
|--------|---------|--------|---------|---------------|------------|
| Feed lesen | Eigene Raeume | Eigene Raeume | Eigene Raeume | Eigene Raeume | Alle |
| Beitrag erstellen | — | — | Ja | Ja | Ja |
| Kommentieren | Ja | Ja | Ja | Ja | Ja |
| Beitrag pinnen | — | — | Ja | Ja | Ja |
| Beitrag loeschen | Eigene | Eigene | Ja (im Raum) | Ja (im Bereich) | Alle |

### C.2 Raeume

| Aktion | STUDENT | PARENT | TEACHER | SECTION_ADMIN | SUPERADMIN |
|--------|---------|--------|---------|---------------|------------|
| Raeume sehen | Eigene | Eigene | Eigene | Bereich | Alle |
| Raum entdecken | Ja | Ja | Ja | Ja | Ja |
| Raum erstellen | — | — | Ja | Ja | Ja |
| Raum-Einstellungen | — | — | Als LEADER | Bereich | Alle |
| Mitglieder verwalten | — | — | Als LEADER | Bereich | Alle |
| Beitrittsanfrage stellen | Ja | Ja | Ja | Ja | — |
| Beitrittsanfrage genehmigen | — | — | Als LEADER | — | Ja |

### C.3 Diskussionen

| Aktion | STUDENT | PARENT | TEACHER | SECTION_ADMIN | SUPERADMIN |
|--------|---------|--------|---------|---------------|------------|
| Threads lesen | Je nach Audience | Je nach Audience | Alle | Alle | Alle |
| Thread erstellen | — | — | Als LEADER | — | Ja |
| Antworten | Ja (sichtbare) | Ja (sichtbare) | Ja | Ja | Ja |
| Thread archivieren | — | — | Als LEADER | — | Ja |

### C.4 Dateien

| Aktion | STUDENT | PARENT | TEACHER | SECTION_ADMIN | SUPERADMIN |
|--------|---------|--------|---------|---------------|------------|
| Dateien sehen | Je nach Audience | Je nach Audience | Alle | Alle | Alle |
| Datei hochladen | Ja | Ja | Ja | Ja | Ja |
| Ordner erstellen | Ja (auto STUDENTS_ONLY) | Ja (auto PARENTS_ONLY) | Ja + Audience-Wahl | Ja + Audience-Wahl | Ja + Audience-Wahl |
| Datei loeschen | Eigene | Eigene | Ja (im Raum) | Ja (im Bereich) | Alle |

### C.5 Fotobox

| Aktion | STUDENT | PARENT | TEACHER | SECTION_ADMIN | SUPERADMIN |
|--------|---------|--------|---------|---------------|------------|
| Fotos ansehen | Je nach Audience | Je nach Audience | Alle | Alle | Alle |
| Fotos hochladen | POST_IMAGES+ | POST_IMAGES+ | Ja | Ja | Ja |
| Thread erstellen | — | — | Als LEADER + Audience-Wahl | — | Ja + Audience-Wahl |
| Fotobox-Einstellungen | — | — | Als LEADER | — | Ja |

### C.6 Kalender

| Aktion | STUDENT | PARENT | TEACHER | SECTION_ADMIN | SUPERADMIN |
|--------|---------|--------|---------|---------------|------------|
| Termine sehen | Eigene Raeume | Eigene Raeume | Eigene Raeume | Bereich | Alle |
| Raum-Event erstellen | — | — | Als LEADER | — | Ja |
| Bereichs-Event erstellen | — | — | Ja | Ja | Ja |
| Schul-Event erstellen | — | — | — | — | Ja |
| RSVP (Zu-/Absagen) | Ja | Ja | Ja | Ja | Ja |
| Event absagen | — | — | Ersteller | Ersteller | Ja |

### C.7 Formulare

| Aktion | STUDENT | PARENT | TEACHER | SECTION_ADMIN | SUPERADMIN |
|--------|---------|--------|---------|---------------|------------|
| Formulare sehen | Zielgruppe | Zielgruppe | Alle eigenen | Bereich | Alle |
| Formular erstellen | — | — | Ja | Ja | Ja |
| Formular beantworten | Wenn Zielgruppe | Wenn Zielgruppe | Wenn Zielgruppe | Wenn Zielgruppe | Wenn Zielgruppe |
| Ergebnisse sehen | — | — | Eigene Formulare | Bereich | Alle |
| Export (CSV/PDF) | — | — | Eigene Formulare | Bereich | Alle |

### C.8 Jobboerse

| Aktion | STUDENT | PARENT | TEACHER | SECTION_ADMIN | SUPERADMIN |
|--------|---------|--------|---------|---------------|------------|
| Jobs sehen | — | Ja | Ja | Ja | Ja |
| Job erstellen | — | — | Ja | Ja | Ja |
| Bewerben | — | Ja | — | — | — |
| Zuweisen | — | — | Eigene Jobs | Eigene Jobs | Alle |
| Stunden bestaetigen | — | — | Eigene Jobs | Eigene Jobs | Alle |

### C.9 Putzorganisation

| Aktion | STUDENT | PARENT | TEACHER | SECTION_ADMIN | SUPERADMIN |
|--------|---------|--------|---------|---------------|------------|
| Putzplan sehen | — | Ja | Ja | Ja | Ja |
| Anmelden | — | Ja | — | — | — |
| QR-Check-in | — | Ja | — | — | — |
| Konfigurieren | — | PUTZORGA | — | — | Ja |
| QR-Codes verwalten | — | — | — | — | Ja |

### C.10 Administration

| Aktion | STUDENT | PARENT | TEACHER | SECTION_ADMIN | SUPERADMIN |
|--------|---------|--------|---------|---------------|------------|
| Nutzer verwalten | — | — | — | Bereich | Alle |
| Raeume verwalten | — | — | — | Bereich | Alle |
| Bereiche verwalten | — | — | — | — | Alle |
| Module konfigurieren | — | — | — | — | Ja |
| Theme/Design | — | — | — | — | Ja |
| Kommunikationsregeln | — | — | — | — | Ja |
| Audit-Log | — | — | — | — | Ja |
| Schulferien/Feiertage | — | — | — | — | Ja |

---

*MonteWeb Produkthandbuch — Version 1.0*
*Erstellt: Februar 2026*
*Fuer MonteWeb v1.0 (Flyway V001–V046)*
