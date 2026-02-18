# MonteWeb Handbuch fuer Lehrkraefte

**Version:** 1.0 | **Stand:** Februar 2026 | **Rolle:** TEACHER (Lehrkraft)

---

## Inhaltsverzeichnis

1. [Ueberblick ueber die Lehrerrolle](#1-ueberblick-ueber-die-lehrerrolle)
2. [Erste Schritte und Navigation](#2-erste-schritte-und-navigation)
3. [Dashboard](#3-dashboard)
4. [Raeume erstellen und verwalten](#4-raeume-erstellen-und-verwalten)
5. [Feed und Beitraege](#5-feed-und-beitraege)
6. [Diskussionen](#6-diskussionen)
7. [Dateien verwalten](#7-dateien-verwalten)
8. [Fotobox verwalten](#8-fotobox-verwalten)
9. [Kalender und Termine](#9-kalender-und-termine)
10. [Formulare und Umfragen](#10-formulare-und-umfragen)
11. [Jobboerse -- Jobs erstellen](#11-jobboerse--jobs-erstellen)
12. [Nachrichten](#12-nachrichten)
13. [Putz-Orga und Fundgrube](#13-putz-orga-und-fundgrube)
14. [Profil und Rollenwechsel](#14-profil-und-rollenwechsel)
15. [Haeufige Fragen (FAQ)](#15-haeufige-fragen-faq)

---

## 1. Ueberblick ueber die Lehrerrolle

Als Lehrkraft sind Sie ein zentraler Akteur in MonteWeb. Sie koennen:

| Funktion | Berechtigung |
|----------|:------------:|
| Beitraege im Feed erstellen | Ja |
| Raeume erstellen und leiten | Ja |
| Diskussions-Threads erstellen | Ja (als Raum-LEADER) |
| Mitglieder verwalten | Ja (als Raum-LEADER) |
| Dateien und Ordner verwalten | Ja (mit Sichtbarkeitssteuerung) |
| Fotobox konfigurieren | Ja (als Raum-LEADER) |
| Termine erstellen | Ja (Raum- und Bereichsebene) |
| Formulare erstellen | Ja |
| Jobs erstellen (Jobboerse) | Ja |
| Nachrichten senden | An alle Rollen |
| Familienverwaltung | Nein (nur als PARENT) |
| Administration | Nein (nur als SUPERADMIN) |

### Raum-Rollen

Als Lehrkraft koennen Sie in Raeumen die Rolle **LEADER** (Leitung) haben. Das gibt Ihnen zusaetzliche Rechte:

| LEADER-Recht | Beschreibung |
|-------------|-------------|
| Mitglieder verwalten | Hinzufuegen, entfernen, Rollen aendern |
| Beitrittsanfragen | Genehmigen oder ablehnen |
| Raumeinstellungen | Beitrittspolitik, Diskussionsmodus, Chat |
| Threads erstellen | Diskussionen starten |
| Fotobox konfigurieren | Berechtigungsstufen festlegen |
| Raum archivieren | Soft-delete des Raums |

---

## 2. Erste Schritte und Navigation

### 2.1 Anmeldung

1. MonteWeb im Browser oeffnen
2. E-Mail und Passwort eingeben
3. **"Anmelden"** klicken

### 2.2 Navigation

**Seitenleiste (Desktop):**
- Dashboard, Raeume, Nachrichten, Jobbboerse, Putz-Orga, Kalender, Formulare, Fundgrube, Profil

**Kopfzeile:**
- Schulname/Logo → Dashboard
- **Rollen-Badge** (blau: "Lehrkraft") -- Bei mehreren Rollen: Klick zum Wechseln
- Benachrichtigungsglocke
- Benutzermenue

**Mobil:**
- Untere Navigationsleiste mit "Mehr"-Button fuer weitere Optionen

---

## 3. Dashboard

Als Lehrkraft sehen Sie auf dem Dashboard:

### 3.1 Post-Composer

Direkt oben: Ein Textfeld zum schnellen Erstellen neuer Feed-Beitraege.
1. Optionalen Titel eingeben
2. Inhalt verfassen
3. **"Veroeffentlichen"** klicken
4. Der Beitrag erscheint im Feed

### 3.2 Offene Formulare

Widget mit Formularen, die Sie noch nicht beantwortet haben.

### 3.3 Feed

Beitraege aus allen Raeumen, denen Sie angehoeren, chronologisch sortiert.

---

## 4. Raeume erstellen und verwalten

### 4.1 Raum erstellen

1. Gehen Sie zu **Raeume** > **"Raeume entdecken"**
2. Klicken Sie auf **"Raum erstellen"**
3. Fuellen Sie aus:
   - **Name** (z.B. "Sonnengruppe")
   - **Beschreibung**
   - **Typ**: Klasse, Gruppe, Projekt, Interessengruppe, Sonstige
   - **Schulbereich** (z.B. Grundstufe)
4. Sie werden automatisch LEADER des neuen Raums

### 4.2 Raumeinstellungen aendern

Als LEADER koennen Sie:

1. Im Raum oben rechts auf **Einstellungen** (Zahnrad) klicken
2. Folgende Optionen anpassen:

| Einstellung | Optionen | Bedeutung |
|-------------|----------|-----------|
| **Beitrittspolitik** | Offen / Auf Anfrage / Nur auf Einladung | Wer kann dem Raum beitreten? |
| **Diskussionsmodus** | Voll / Nur Ankuendigungen / Deaktiviert | Wer kann auf Threads antworten? |
| **Chat** | Aktiviert / Deaktiviert | Echtzeit-Chat im Raum |

3. **Oeffentliche Beschreibung** bearbeiten (sichtbar auf der Entdeckungsseite)
4. **Raum-Avatar** hochladen

### 4.3 Mitglieder verwalten

Im Tab **Mitglieder** als LEADER:

**Person hinzufuegen:**
1. Klicken Sie auf **"Mitglied hinzufuegen"** oder **"Lehrkraft hinzufuegen"**
2. Name oder E-Mail eingeben
3. Person auswaehlen
4. Die Person wird als MEMBER hinzugefuegt

**Familie hinzufuegen:**
1. Klicken Sie auf **"Familie aufnehmen"**
2. Familie aus der Liste auswaehlen
3. Alle Familienmitglieder (Eltern + Kinder) werden hinzugefuegt

**Rollen aendern:**
- Klicken Sie auf das Rollen-Dropdown neben einem Mitglied
- Aendern Sie die Rolle (LEADER, MEMBER, PARENT_MEMBER, GUEST)

**Mitglied entfernen:**
- Klicken Sie auf das Entfernen-Icon neben dem Mitglied
- Bestaetigen Sie die Aktion

### 4.4 Beitrittsanfragen bearbeiten

Wenn die Beitrittspolitik "Auf Anfrage" ist:

1. Im Mitglieder-Tab erscheint der Bereich **"Offene Anfragen"**
2. Sie sehen den Antragsteller und dessen Nachricht
3. Klicken Sie auf **"Annehmen"** oder **"Ablehnen"**
4. Bei Annahme wird die Person automatisch MEMBER

### 4.5 Raum archivieren

1. In den Raumeinstellungen auf **"Raum archivieren"** klicken
2. Der Raum wird fuer Nutzer unsichtbar
3. Kann vom Admin wiederhergestellt werden

---

## 5. Feed und Beitraege

### 5.1 Beitrag erstellen

**Auf dem Dashboard:**
1. Post-Composer nutzen (Titel optional, Inhalt eingeben)
2. **"Veroeffentlichen"** klicken

**Im Raum (Info-Board):**
1. Im Tab "Info-Board" den Post-Composer nutzen
2. Der Beitrag erscheint nur im Raum-Feed

### 5.2 Beitrag anpinnen

Wichtige Beitraege koennen angepinnt werden:
1. Am Beitrag auf das **Pin-Symbol** klicken
2. Der Beitrag erscheint immer oben im Feed
3. Erneut klicken zum Losloeosen

### 5.3 Beitrag loeschen

- Eigene Beitraege: Klick auf Loeschen-Icon am Beitrag
- Als LEADER: Auch Beitraege anderer Mitglieder im Raum

### 5.4 Gezielte Beitraege (Targeted Posts)

Beitraege koennen so erstellt werden, dass sie nur fuer bestimmte Nutzer sichtbar sind. Dies wird intern ueber Zielgruppen-IDs gesteuert.

---

## 6. Diskussionen

### 6.1 Thread erstellen (als LEADER)

Im Tab **Diskussionen** eines Raumes:

1. Klicken Sie auf **"Neue Diskussion"**
2. **Titel** eingeben
3. **Inhalt** verfassen (optional)
4. **Zielgruppe waehlen:**

| Zielgruppe | Wer sieht den Thread |
|------------|---------------------|
| **Alle** | Alle Raummitglieder |
| **Eltern** | Nur Eltern und Lehrkraefte |
| **Kinder** | Nur Schueler und Lehrkraefte |

5. Absenden

### 6.2 Threads verwalten

Als LEADER koennen Sie:
- **Archivieren** -- Thread wird ausgeblendet, bleibt gespeichert
- **Loeschen** -- Thread wird endgueltig entfernt

### 6.3 Antworten

Alle Mitglieder (je nach Zielgruppe) koennen auf Threads antworten.

---

## 7. Dateien verwalten

### 7.1 Ordner erstellen mit Sichtbarkeit

Im Tab **Dateien** eines Raumes:

1. Klicken Sie auf **"Neuer Ordner"**
2. Ordnername eingeben
3. **Sichtbarkeit waehlen:**

| Sichtbarkeit | Wer sieht den Ordner |
|-------------|---------------------|
| **Alle** | Alle Raummitglieder |
| **Nur Eltern** | Eltern, Lehrkraefte, Admins |
| **Nur Schueler** | Schueler, Lehrkraefte, Admins |

4. Erstellen

### 7.2 Dateien hochladen

1. **"Hochladen"** klicken
2. Datei(en) auswaehlen
3. Upload starten

### 7.3 Sichtbarkeits-Tags

- Ordner und Dateien zeigen farbige Tags fuer die Sichtbarkeit
- **Nur Eltern** = Orange Tag
- **Nur Schueler** = Blauer Tag
- Als Lehrkraft sehen Sie immer alle Inhalte, unabhaengig von der Sichtbarkeit

---

## 8. Fotobox verwalten

### 8.1 Fotobox-Einstellungen (als LEADER)

Im Tab **Fotobox** > Einstellungen:

**Standardberechtigung fuer Raummitglieder:**

| Stufe | Berechtigung |
|-------|-------------|
| **Nur ansehen** | Fotos anschauen, Lightbox nutzen |
| **Bilder posten** | Bilder in bestehende Threads hochladen |
| **Threads erstellen** | Neue Foto-Threads erstellen |

> **Hinweis:** Als LEADER haben Sie immer die hoechste Stufe (Threads erstellen).

### 8.2 Thread erstellen

1. **"Neuer Thread"** klicken
2. Titel und Beschreibung eingeben
3. **Sichtbarkeit waehlen:** Alle / Nur Eltern / Nur Schueler
4. Erstellen

### 8.3 Bilder hochladen

1. Thread oeffnen
2. **"Bilder hochladen"** klicken
3. Bis zu 20 Bilder pro Upload auswaehlen
4. Thumbnails werden automatisch generiert

### 8.4 Bilder verwalten

- Bildunterschrift bearbeiten
- Bilder loeschen (eigene oder als LEADER alle)
- Lightbox: Klick auf ein Bild fuer Vollbildansicht

---

## 9. Kalender und Termine

### 9.1 Termine erstellen

Als Lehrkraft koennen Sie Termine auf zwei Ebenen erstellen:

| Ebene | Sichtbar fuer | Wie |
|-------|--------------|-----|
| **Raum** | Alle Mitglieder des Raums | Nur als LEADER des Raums |
| **Bereich** | Alle Nutzer des Schulbereichs | Immer als TEACHER |

1. Gehen Sie zu **Kalender** > **"Termin erstellen"**
2. Fuellen Sie aus:
   - Titel (z.B. "Elternabend", "Schulfest")
   - Beschreibung (optional)
   - Ort (optional)
   - Datum und Uhrzeit (oder "Ganztaegig")
   - **Sichtbarkeit**: Raum oder Schulbereich
   - Bei Raum: Raum auswaehlen
   - Bei Bereich: Bereich auswaehlen
3. Optional: **Wiederholung** einstellen (taeglich, woechentlich, monatlich, jaehrlich)
4. **"Erstellen"** klicken

### 9.2 Termin bearbeiten

Nur als Ersteller:
1. Termin oeffnen
2. **"Bearbeiten"** klicken
3. Aenderungen vornehmen
4. Speichern

### 9.3 Termin absagen

1. Termin oeffnen
2. **"Termin absagen"** klicken
3. Alle betroffenen Nutzer erhalten einen Feed-Beitrag
4. Der Termin bleibt mit "Abgesagt"-Markierung sichtbar

### 9.4 Termin loeschen

1. Termin oeffnen
2. **"Loeschen"** klicken
3. Nur Nutzer, die zugesagt hatten, werden informiert

### 9.5 Jobs mit Terminen verknuepfen

Auf der Termindetail-Seite koennen Sie:
- **Bestehende Jobs verknuepfen** -- Vorhandenen Job aus der Jobboerse zuordnen
- **Neuen Job erstellen** -- Direkt einen Job fuer diesen Termin anlegen

So koennen Eltern sich fuer Helfertaetigkeiten bei Veranstaltungen anmelden.

---

## 10. Formulare und Umfragen

### 10.1 Formular erstellen

1. Gehen Sie zu **Formulare** > **"Formular erstellen"**
2. Fuellen Sie die Grunddaten aus:

| Feld | Beschreibung |
|------|-------------|
| **Titel** | z.B. "Elternumfrage", "Einverstaendniserklaerung" |
| **Beschreibung** | Optionale Erlaeuterung |
| **Typ** | Umfrage oder Einwilligung |
| **Geltungsbereich** | Schule / Bereich / Raum |
| **Frist** | Optional: Deadline fuer Antworten |
| **Anonym** | Antworten ohne Nutzerzuordnung |

3. Bei **Bereich**: Einen oder mehrere Schulbereiche auswaehlen (Multi-Section)
4. Bei **Raum**: Einen Raum auswaehlen

### 10.2 Fragen hinzufuegen

Klicken Sie auf **"Frage hinzufuegen"** und waehlen Sie den Fragetyp:

| Fragetyp | Beschreibung |
|----------|-------------|
| **Freitext** | Offenes Textfeld |
| **Einfachauswahl** | Eine Option aus mehreren |
| **Mehrfachauswahl** | Mehrere Optionen moeglich |
| **Bewertung** | Sterne-Skala |
| **Ja/Nein** | Einfache Ja/Nein-Auswahl |

Fuer jede Frage:
- Fragetext eingeben
- Beschreibung hinzufuegen (optional)
- Als **Pflichtfeld** markieren (optional)
- Bei Choice-Fragen: Antwortoptionen definieren

### 10.3 Formular-Lebenszyklus

```
Entwurf → Veroeffentlicht → Geschlossen → Archiviert
```

| Aktion | Effekt |
|--------|--------|
| **Als Entwurf speichern** | Formular ist noch nicht sichtbar |
| **Veroeffentlichen** | Formular wird fuer die Zielgruppe sichtbar |
| **Schliessen** | Keine neuen Antworten mehr moeglich |

### 10.4 Ergebnisse einsehen

1. Formular oeffnen > **"Ergebnisse anzeigen"**
2. Sie sehen:
   - Ruecklaufquote (Fortschrittsbalken)
   - Zusammenfassung pro Frage
   - Einzelantworten (sofern nicht anonym)
3. **Export:**
   - **CSV Export** -- Tabellarische Daten
   - **PDF Export** -- Formatierter Bericht

---

## 11. Jobboerse -- Jobs erstellen

### 11.1 Job erstellen

1. Gehen Sie zu **Jobbboerse** > **"Job erstellen"**
2. Fuellen Sie aus:

| Feld | Beschreibung |
|------|-------------|
| **Titel** | z.B. "Schulhof kehren" |
| **Beschreibung** | Was ist zu tun? |
| **Kategorie** | Normal oder Reinigung |
| **Geschaetzte Stunden** | z.B. 2.0 Stunden |
| **Ort** | z.B. "Schulhof" |
| **Datum** | Wann soll die Aufgabe erledigt werden? |
| **Uhrzeit** | z.B. "14:00 - 16:00" |
| **Max. Helfer** | Wie viele Eltern werden benoetigt? |
| **Kontakt** | Ansprechperson |

3. Optional: Mit einem Kalender-Termin verknuepfen

> **Hinweis:** Jobs mit Kategorie "Reinigung" werden dem Putzstundenkonto der Familie gutgeschrieben, "Normal" dem regulaeren Elternstundenkonto.

### 11.2 Bewerbungen verwalten

Wenn Eltern sich auf Ihren Job bewerben:

1. Job oeffnen
2. Unter **"Helfer"** sehen Sie die Bewerber
3. Bewerber werden automatisch zugewiesen (First-Come-First-Served)

### 11.3 Stunden bestaetigen

Nach Abschluss eines Jobs durch ein Elternteil:

1. Job oeffnen
2. Die tatsaechlichen Stunden werden angezeigt
3. Sie koennen den Abschluss bestaetigen
4. Die Stunden werden dem Familienkonto gutgeschrieben

### 11.4 Job bearbeiten und loeschen

- **Bearbeiten**: Klicken Sie auf "Job bearbeiten" auf der Detailseite
- **Loeschen**: Klicken Sie auf "Loeschen" und bestaetigen Sie

---

## 12. Nachrichten

### 12.1 Kommunikationsmoeglichkeiten

Als Lehrkraft koennen Sie mit allen Rollen kommunizieren:
- **Eltern** -- Immer erlaubt
- **Andere Lehrkraefte** -- Immer erlaubt
- **Schueler** -- Erlaubt (je nach Einstellung)
- **Admins** -- Immer erlaubt

### 12.2 Bilder und Antworten

- **Bilder senden**: Klick auf Bild-Symbol, Bild auswaehlen (max. 10 MB)
- **Auf Nachricht antworten**: Hover ueber Nachricht > "Antworten"

### 12.3 Raum-Chat

Im Tab **Chat** eines Raumes:
- **MAIN** -- Allgemeiner Kanal (alle Mitglieder)
- **Eltern-Lehrer Chat** -- Nur Lehrer und Eltern
- **Schueler-Lehrer Chat** -- Nur Schueler und Lehrer

---

## 13. Putz-Orga und Fundgrube

### 13.1 Putz-Orga

Als Lehrkraft koennen Sie die Putztermine einsehen, sich aber nicht anmelden (das ist fuer Eltern). Sie sehen:
- Anstehende Termine und Belegung
- Welche Familien sich eingetragen haben

### 13.2 Fundgrube

Die schulweite Fundgrube steht allen Nutzern zur Verfuegung:
- Verlorene Gegenstaende melden (mit Foto)
- Gegenstaende beanspruchen ("Das gehoert mir!")

---

## 14. Profil und Rollenwechsel

### 14.1 Multi-Rollen

Viele Lehrkraefte sind auch Eltern. In diesem Fall haben Sie zwei Rollen:

- **TEACHER** -- Beitraege erstellen, Raeume leiten, Jobs erstellen
- **PARENT** -- Familienverwaltung, Jobs annehmen, Putztermine belegen

**Rollenwechsel:**
1. Klicken Sie auf den **Rollen-Badge** in der Kopfzeile (oder auf der Profilseite)
2. Waehlen Sie die gewuenschte Rolle
3. Die Anzeige passt sich automatisch an

### 14.2 Profil bearbeiten

- Avatar hochladen/aendern
- Name und Telefonnummer aendern
- Push-Benachrichtigungen aktivieren

---

## 15. Haeufige Fragen (FAQ)

**Wie erstelle ich einen Raum fuer meine Klasse?**
1. Raeume > Entdecken > "Raum erstellen"
2. Typ "Klasse" waehlen, Schulbereich zuweisen, Name eingeben

**Wie fuege ich alle Familien zu meinem Raum hinzu?**
- Im Mitglieder-Tab: "Familie aufnehmen" -- so kommen alle Familienmitglieder auf einmal

**Kann ich sehen, wer ein Formular noch nicht beantwortet hat?**
- In den Ergebnissen sehen Sie die Ruecklaufquote und die Einzelantworten (bei nicht-anonymen Formularen)

**Wie erstelle ich einen Termin fuer eine Schulveranstaltung?**
- Kalender > "Termin erstellen" > Sichtbarkeit "Schulbereich" waehlen
- Fuer schulweite Events brauchen Sie SUPERADMIN-Rechte

**Wie kann ich verhindern, dass Schueler in Diskussionen schreiben?**
- Raumeinstellungen > Diskussionsmodus auf "Nur Ankuendigungen" stellen

**Wie loese ich eine Beitrittsanfrage?**
- Im Mitglieder-Tab unter "Offene Anfragen" sehen Sie eingehende Anfragen
- "Annehmen" oder "Ablehnen" klicken

**Ich bin TEACHER und PARENT -- wie wechsle ich die Rolle?**
- Klicken Sie auf den Rollen-Badge oben oder auf der Profilseite
- Je nach aktiver Rolle sehen Sie unterschiedliche Funktionen

---

*MonteWeb Lehrer-Handbuch -- Version 1.0 -- Februar 2026*
