# MonteWeb Produkthandbuch

**Schul-Intranet fuer Montessori-Schulkomplexe**

Version 1.0 -- Stand: Februar 2026

---

## Inhaltsverzeichnis

1. [Einleitung](#1-einleitung)
   - 1.1 [Zweck des Handbuchs](#11-zweck-des-handbuchs)
   - 1.2 [Systemvoraussetzungen](#12-systemvoraussetzungen)
   - 1.3 [Anmeldung am System](#13-anmeldung-am-system)
   - 1.4 [Registrierung](#14-registrierung)
   - 1.5 [Passwort vergessen](#15-passwort-vergessen)
   - 1.6 [Anmeldung per SSO (Single Sign-On)](#16-anmeldung-per-sso-single-sign-on)
   - 1.7 [Sprachumschaltung](#17-sprachumschaltung)
   - 1.8 [Installation als App (PWA)](#18-installation-als-app-pwa)
   - 1.9 [Aufbau der Oberflaeche](#19-aufbau-der-oberflaeche)
2. [Rollenuebersicht](#2-rollenuebersicht)
   - 2.1 [Systemrollen](#21-systemrollen)
   - 2.2 [Sonderrollen](#22-sonderrollen)
   - 2.3 [Raum-Rollen](#23-raum-rollen)
   - 2.4 [Familien-Rollen](#24-familien-rollen)
   - 2.5 [Berechtigungsmatrix auf einen Blick](#25-berechtigungsmatrix-auf-einen-blick)
3. [Rolle: SUPERADMIN (Schulleitung / IT-Administration)](#3-rolle-superadmin)
   - 3.1 [Profil und Verifizierung](#31-profil-und-verifizierung)
   - 3.2 [Funktionsuebersicht](#32-funktionsuebersicht)
   - 3.3 [Menuepunkte im Detail](#33-menuepunkte-im-detail)
   - 3.4 [Navigation](#34-navigation)
   - 3.5 [Kommunikation](#35-kommunikation)
   - 3.6 [Einschraenkungen](#36-einschraenkungen)
   - 3.7 [Beziehungen zu anderen Rollen](#37-beziehungen-zu-anderen-rollen)
4. [Rolle: SECTION_ADMIN (Bereichsleitung)](#4-rolle-section_admin)
   - 4.1 [Profil und Verifizierung](#41-profil-und-verifizierung)
   - 4.2 [Funktionsuebersicht](#42-funktionsuebersicht)
   - 4.3 [Menuepunkte im Detail](#43-menuepunkte-im-detail)
   - 4.4 [Navigation](#44-navigation)
   - 4.5 [Kommunikation](#45-kommunikation)
   - 4.6 [Einschraenkungen](#46-einschraenkungen)
   - 4.7 [Beziehungen zu anderen Rollen](#47-beziehungen-zu-anderen-rollen)
5. [Rolle: TEACHER (Lehrkraefte / Erzieher)](#5-rolle-teacher)
   - 5.1 [Profil und Verifizierung](#51-profil-und-verifizierung)
   - 5.2 [Funktionsuebersicht](#52-funktionsuebersicht)
   - 5.3 [Menuepunkte im Detail](#53-menuepunkte-im-detail)
   - 5.4 [Navigation](#54-navigation)
   - 5.5 [Kommunikation](#55-kommunikation)
   - 5.6 [Einschraenkungen](#56-einschraenkungen)
   - 5.7 [Beziehungen zu anderen Rollen](#57-beziehungen-zu-anderen-rollen)
6. [Rolle: PARENT (Eltern)](#6-rolle-parent)
   - 6.1 [Profil und Verifizierung](#61-profil-und-verifizierung)
   - 6.2 [Funktionsuebersicht](#62-funktionsuebersicht)
   - 6.3 [Menuepunkte im Detail](#63-menuepunkte-im-detail)
   - 6.4 [Navigation](#64-navigation)
   - 6.5 [Kommunikation](#65-kommunikation)
   - 6.6 [Einschraenkungen](#66-einschraenkungen)
   - 6.7 [Beziehungen zu anderen Rollen](#67-beziehungen-zu-anderen-rollen)
7. [Rolle: STUDENT (Schueler)](#7-rolle-student)
   - 7.1 [Profil und Verifizierung](#71-profil-und-verifizierung)
   - 7.2 [Funktionsuebersicht](#72-funktionsuebersicht)
   - 7.3 [Menuepunkte im Detail](#73-menuepunkte-im-detail)
   - 7.4 [Navigation](#74-navigation)
   - 7.5 [Kommunikation](#75-kommunikation)
   - 7.6 [Einschraenkungen](#76-einschraenkungen)
   - 7.7 [Beziehungen zu anderen Rollen](#77-beziehungen-zu-anderen-rollen)
8. [Sonderrolle: ELTERNBEIRAT](#8-sonderrolle-elternbeirat)
   - 8.1 [Was ist die Sonderrolle?](#81-was-ist-die-sonderrolle)
   - 8.2 [Erweiterte Berechtigungen](#82-erweiterte-berechtigungen)
   - 8.3 [Typische Arbeitsablaeufe](#83-typische-arbeitsablaeufe)
9. [Sonderrolle: PUTZORGA](#9-sonderrolle-putzorga)
   - 9.1 [Was ist die Sonderrolle?](#91-was-ist-die-sonderrolle)
   - 9.2 [Erweiterte Berechtigungen](#92-erweiterte-berechtigungen)
   - 9.3 [Typische Arbeitsablaeufe](#93-typische-arbeitsablaeufe)
10. [Module im Detail](#10-module-im-detail)
    - 10.1 [Dashboard und Feed](#101-dashboard-und-feed)
    - 10.2 [Raeume](#102-raeume)
    - 10.3 [Familie und Stundenkonto](#103-familie-und-stundenkonto)
    - 10.4 [Nachrichten (Direktkommunikation)](#104-nachrichten-direktkommunikation)
    - 10.5 [Jobboerse (Elternstunden)](#105-jobboerse-elternstunden)
    - 10.6 [Putz-Organisation](#106-putz-organisation)
    - 10.7 [Kalender und Termine](#107-kalender-und-termine)
    - 10.8 [Formulare und Umfragen](#108-formulare-und-umfragen)
    - 10.9 [Fotobox](#109-fotobox)
    - 10.10 [Benachrichtigungen](#1010-benachrichtigungen)
    - 10.11 [Profil und Datenschutz](#1011-profil-und-datenschutz)
11. [Kommunikationsmatrix](#11-kommunikationsmatrix)
12. [Glossar](#12-glossar)
13. [Haeufig gestellte Fragen (FAQ)](#13-haeufig-gestellte-fragen-faq)

---

# 1. Einleitung

## 1.1 Zweck des Handbuchs

Dieses Handbuch richtet sich an alle Nutzerinnen und Nutzer des MonteWeb-Schulintranets. Es beschreibt saemtliche Funktionen, die Ihnen je nach Ihrer Rolle im System zur Verfuegung stehen.

MonteWeb ist das zentrale Kommunikations- und Organisationswerkzeug Ihres Montessori-Schulkomplexes. Es verbindet Schulleitung, Lehrkraefte, Erzieher, Eltern und Schueler auf einer gemeinsamen Plattform. Ueber MonteWeb koennen Sie:

- Neuigkeiten aus Ihren Klassen und Gruppen im Feed verfolgen
- Direkt mit Lehrkraeften, Eltern oder der Schulleitung kommunizieren
- Sich fuer freiwillige Aufgaben (Elternstunden) in der Jobboerse anmelden
- Putztermine organisieren und per QR-Code einchecken
- Termine und Veranstaltungen im Kalender einsehen und Ihre Teilnahme bestaetigen
- Umfragen und Einverstaendniserklaerungen ausfuellen
- Fotos aus dem Schulalltag in der Fotobox teilen
- Dateien innerhalb Ihrer Raeume austauschen

Dieses Handbuch erklaert Schritt fuer Schritt, wie Sie jede dieser Funktionen nutzen. Es ist nach Rollen gegliedert, sodass Sie direkt zu dem Kapitel springen koennen, das fuer Sie relevant ist.

## 1.2 Systemvoraussetzungen

MonteWeb laeuft vollstaendig im Webbrowser. Sie benoetigen:

- **Einen aktuellen Webbrowser**: Google Chrome, Mozilla Firefox, Microsoft Edge oder Apple Safari in einer aktuellen Version
- **Internetverbindung**: Eine stabile Internetverbindung (WLAN oder mobile Daten)
- **Geraet**: Computer, Laptop, Tablet oder Smartphone -- MonteWeb passt sich automatisch an die Bildschirmgroesse an

Es muss keine zusaetzliche Software installiert werden. Optional koennen Sie MonteWeb als App auf Ihrem Geraet installieren (siehe Abschnitt 1.8).

## 1.3 Anmeldung am System

So melden Sie sich bei MonteWeb an:

1. Oeffnen Sie Ihren Webbrowser und geben Sie die Adresse Ihres MonteWeb-Systems ein (diese erhalten Sie von Ihrer Schulleitung oder IT-Abteilung).
2. Sie sehen die Anmeldeseite mit dem Titel "MonteWeb".
3. Geben Sie Ihre **E-Mail-Adresse** in das Feld "E-Mail" ein.
4. Geben Sie Ihr **Passwort** in das Feld "Passwort" ein.
5. Klicken Sie auf die Schaltflaeche **"Anmelden"**.
6. Nach erfolgreicher Anmeldung werden Sie automatisch auf das Dashboard (Ihre Startseite) weitergeleitet.

Falls Ihre Anmeldedaten nicht korrekt sind, erscheint eine Fehlermeldung: "Anmeldung fehlgeschlagen. Bitte ueberpruefen Sie Ihre Zugangsdaten." Pruefen Sie in diesem Fall Ihre E-Mail-Adresse und Ihr Passwort auf Tippfehler.

## 1.4 Registrierung

Falls Sie noch kein Konto besitzen, koennen Sie sich selbst registrieren:

1. Klicken Sie auf der Anmeldeseite auf den Link **"Registrieren"** (unterhalb der Anmeldung).
2. Fuellen Sie das Formular aus:
   - **Vorname**: Ihr Vorname
   - **Nachname**: Ihr Nachname
   - **E-Mail**: Ihre E-Mail-Adresse (wird fuer die Anmeldung verwendet)
   - **Passwort**: Waehlen Sie ein sicheres Passwort
   - **Telefon** (optional): Ihre Telefonnummer
3. Klicken Sie auf **"Registrieren"**.
4. Nach erfolgreicher Registrierung werden Sie automatisch angemeldet.

Hinweis: Nach der Registrierung muss Ihre Schulleitung (SUPERADMIN) Ihnen moeglicherweise die korrekte Rolle zuweisen, damit Sie alle fuer Sie vorgesehenen Funktionen nutzen koennen.

## 1.5 Passwort vergessen

Falls Sie Ihr Passwort vergessen haben:

1. Klicken Sie auf der Anmeldeseite auf **"Passwort vergessen?"**.
2. Geben Sie Ihre registrierte E-Mail-Adresse ein.
3. Sie erhalten eine E-Mail mit einem Link zum Zuruecksetzen Ihres Passworts (sofern der E-Mail-Versand an Ihrer Schule aktiviert ist).
4. Klicken Sie auf den Link in der E-Mail und vergeben Sie ein neues Passwort.

Falls Sie keine E-Mail erhalten, wenden Sie sich bitte an Ihre Schulleitung oder IT-Abteilung.

## 1.6 Anmeldung per SSO (Single Sign-On)

Falls Ihre Schule eine zentrale Anmeldung (SSO) eingerichtet hat, sehen Sie auf der Anmeldeseite zusaetzlich die Schaltflaeche **"Mit SSO anmelden"**.

1. Klicken Sie auf **"Mit SSO anmelden"**.
2. Sie werden auf die Anmeldeseite Ihres Schulnetzwerks weitergeleitet.
3. Melden Sie sich dort mit Ihren ueblichen Zugangsdaten an.
4. Nach erfolgreicher Anmeldung werden Sie automatisch zurueck zu MonteWeb geleitet.

Hinweis: Diese Option ist nur verfuegbar, wenn Ihre Schule SSO aktiviert hat.

## 1.7 Sprachumschaltung

MonteWeb steht in zwei Sprachen zur Verfuegung: **Deutsch** und **Englisch**.

So wechseln Sie die Sprache:

1. Klicken Sie oben rechts in der Kopfzeile auf das Sprachsymbol (Flagge oder Sprachkuerzel).
2. Waehlen Sie die gewuenschte Sprache aus.
3. Die Oberflaeche wird sofort in der gewaehlten Sprache angezeigt.

MonteWeb erkennt beim ersten Besuch automatisch die Spracheinstellung Ihres Browsers und waehlt die passende Sprache.

## 1.8 Installation als App (PWA)

MonteWeb kann auf Ihrem Smartphone oder Computer wie eine normale App installiert werden. Vorteile:

- Schnellerer Zugriff ueber ein Symbol auf dem Startbildschirm
- Vollbildmodus ohne Browserleisten
- Funktioniert auch bei schlechter Internetverbindung teilweise offline

**Auf dem Smartphone (Android/Chrome):**
1. Oeffnen Sie MonteWeb im Chrome-Browser.
2. Tippen Sie auf das Menue (drei Punkte oben rechts).
3. Waehlen Sie **"Zum Startbildschirm hinzufuegen"** oder **"App installieren"**.
4. Bestaetigen Sie die Installation.

**Auf dem iPhone (Safari):**
1. Oeffnen Sie MonteWeb in Safari.
2. Tippen Sie auf das Teilen-Symbol (Quadrat mit Pfeil nach oben).
3. Waehlen Sie **"Zum Home-Bildschirm"**.
4. Bestaetigen Sie mit **"Hinzufuegen"**.

**Auf dem Computer (Chrome/Edge):**
1. Oeffnen Sie MonteWeb im Browser.
2. Klicken Sie auf das Installations-Symbol in der Adressleiste (kleines Monitor-Symbol mit Pfeil).
3. Bestaetigen Sie die Installation.

## 1.9 Aufbau der Oberflaeche

Die MonteWeb-Oberflaeche besteht aus drei Hauptbereichen:

### Kopfzeile (Header)

Am oberen Bildschirmrand finden Sie:
- **Schulname oder Logo**: Links oben, zeigt den Namen oder das Logo Ihrer Schule
- **Benachrichtigungsglocke**: Zeigt die Anzahl ungelesener Benachrichtigungen. Klicken Sie darauf, um Ihre Benachrichtigungen zu sehen.
- **Sprachumschalter**: Zum Wechseln zwischen Deutsch und Englisch
- **Profilbereich**: Ihr Name oder Avatar. Klicken Sie darauf, um zu Ihrem Profil zu gelangen oder sich abzumelden.

### Seitenleiste (Navigation)

Auf Computern und Tablets sehen Sie links eine Seitenleiste mit den Hauptmenuepunkten:
- **Dashboard**: Ihre Startseite mit dem Feed
- **Raeume**: Ihre Klassen, Gruppen und Projekte
- **Familie**: Ihr Familienverbund und Stundenkonto
- **Nachrichten**: Direktnachrichten (sofern aktiviert)
- **Jobboerse**: Freiwillige Aufgaben fuer Elternstunden (sofern aktiviert)
- **Putz-Orga**: Putztermine (sofern aktiviert)
- **Kalender**: Termine und Veranstaltungen (sofern aktiviert)
- **Formulare**: Umfragen und Einverstaendniserklaerungen (sofern aktiviert)
- **Verwaltung**: Nur fuer die Schulleitung (SUPERADMIN) sichtbar

Welche Menuepunkte Sie sehen, haengt davon ab, welche Module an Ihrer Schule aktiviert sind und welche Rolle Sie haben.

### Untere Navigation (Smartphone)

Auf Smartphones wird die Seitenleiste durch eine untere Navigationsleiste ersetzt. Diese zeigt die wichtigsten Menuepunkte als Symbole am unteren Bildschirmrand. Ueber den Punkt "Mehr" erreichen Sie weitere Menuepunkte.

### Inhaltsbereich

Der grosse Bereich in der Mitte zeigt den jeweiligen Inhalt der ausgewaehlten Seite -- Ihren Feed, die Raumdetails, Nachrichten usw.

---

# 2. Rollenuebersicht

MonteWeb verwendet ein Rollensystem, das bestimmt, welche Funktionen Ihnen zur Verfuegung stehen. Jede Person erhaelt genau eine Systemrolle, kann aber zusaetzliche Sonderrollen und innerhalb von Raeumen weitere Rollen haben.

## 2.1 Systemrollen

Jeder Nutzer hat genau eine der folgenden Systemrollen:

| Rolle | Typischer Personenkreis | Kurzbeschreibung |
|-------|------------------------|------------------|
| **SUPERADMIN** | Schulleitung, IT-Administration | Vollzugriff auf alle Funktionen und die Verwaltungsoberflaeche. Kann das gesamte System konfigurieren. |
| **SECTION_ADMIN** | Bereichsleitung (z.B. Leitung Grundschule) | Verwaltet den eigenen Schulbereich. Kann Jobs und Putztermine fuer den eigenen Bereich verwalten. |
| **TEACHER** | Lehrkraefte, Erzieher | Paedagogisches Personal. Erhaelt automatisch die Leitungsrolle in Klassenraeumen. Kann Diskussionen, Termine und Formulare erstellen. |
| **PARENT** | Eltern, Erziehungsberechtigte | Kann Familienverbund verwalten, sich fuer Aufgaben und Putztermine anmelden. Die meisten Funktionen sind auf diese Rolle zugeschnitten. |
| **STUDENT** | Schueler (ab Mittelschule) | Eingeschraenkter Zugang. Kann Rauminhalte sehen und an erlaubten Diskussionen teilnehmen. |

## 2.2 Sonderrollen

Zusaetzlich zur Systemrolle koennen einzelne Personen Sonderrollen erhalten. Diese werden von der Schulleitung (SUPERADMIN) vergeben und erweitern die Berechtigungen:

| Sonderrolle | Beschreibung |
|-------------|-------------|
| **ELTERNBEIRAT** | Kann schulweite und bereichsweite Termine, Formulare und Feed-Beitraege erstellen. Kann ausserdem die Putz-Organisation verwalten. Kann global oder fuer einen bestimmten Schulbereich vergeben werden. |
| **PUTZORGA** | Kann Putztermine fuer den zugewiesenen Schulbereich verwalten (Termine erstellen, QR-Codes generieren, Dashboard einsehen). Immer an einen bestimmten Schulbereich gebunden. |

## 2.3 Raum-Rollen

Innerhalb jedes Raumes (Klasse, Gruppe, Projekt) hat jedes Mitglied eine Raum-Rolle:

| Raum-Rolle | Beschreibung |
|------------|-------------|
| **Leitung** (LEADER) | Verantwortlich fuer den Raum. Kann den Raum bearbeiten, Mitglieder verwalten, Diskussionen moderieren, Beitrittsanfragen bearbeiten und die Fotobox konfigurieren. |
| **Mitglied** (MEMBER) | Standard-Mitglied, typischerweise Schueler. Hat Zugang zu allen Rauminhalten und zum Chat. |
| **Eltern-Mitglied** (PARENT_MEMBER) | Elternteil als Raummitglied. Hat Zugang zu Rauminhalten und zum separaten Eltern-Chatkanal. |
| **Gast** (GUEST) | Eingeschraenkter Zugang, nur Leseberechtigung. |

Wichtig: Lehrkraefte (TEACHER) erhalten automatisch die Rolle "Leitung", wenn sie einem Klassenraum hinzugefuegt werden. Die Schulleitung (SUPERADMIN) hat auf alle Raeume Leitungsrechte, auch ohne Mitglied zu sein.

## 2.4 Familien-Rollen

Innerhalb des Familienverbundes gibt es zwei Rollen:

| Familien-Rolle | Beschreibung |
|---------------|-------------|
| **Elternteil** (PARENT) | Kann den Familienverbund verwalten, Einladungscodes erzeugen und Mitglieder einladen. |
| **Kind** (CHILD) | Ist dem Familienverbund zugeordnet. Ein Kind kann mehreren Familien angehoeren (z.B. bei getrennt lebenden Eltern). |

## 2.5 Berechtigungsmatrix auf einen Blick

Die folgende Tabelle zeigt die wichtigsten Funktionen und welche Rollen sie nutzen koennen:

| Funktion | SUPERADMIN | SECTION_ADMIN | TEACHER | PARENT | STUDENT |
|----------|:----------:|:-------------:|:-------:|:------:|:-------:|
| Verwaltungsoberflaeche | Ja | -- | -- | -- | -- |
| Module ein-/ausschalten | Ja | -- | -- | -- | -- |
| Schulbereiche verwalten | Ja | -- | -- | -- | -- |
| Benutzer verwalten | Ja | -- | -- | -- | -- |
| Raeume erstellen | Ja | Ja | Ja | Ja | Ja |
| Raeume archivieren/loeschen | Ja | -- | -- | -- | -- |
| Schulweite Beitraege | Ja | Ja | Ja | -- | -- |
| Bereichsweite Beitraege | Ja | Ja | Ja | -- | -- |
| Schulweite Termine | Ja | -- | -- | -- | -- |
| Bereichsweite Termine | Ja | Ja | Ja | -- | -- |
| Raum-Termine (als Leitung) | Ja | -- | Ja | Ja | -- |
| Formulare erstellen | Ja | Ja | Ja | Als Leitung | -- |
| Jobs erstellen | -- | Ja | -- | Ja | -- |
| Jobs annehmen | -- | -- | -- | Ja | -- |
| Putztermine wahrnehmen | -- | -- | -- | Ja | -- |
| Putz-Verwaltung | Ja | Ja | -- | -- | -- |
| Familie erstellen | Ja | -- | -- | Ja | -- |
| Nachrichten an alle Rollen | Ja | Ja | Ja | -- | -- |
| Nachrichten an Eltern | Ja | Ja | Ja | Konfig. | -- |
| Nachrichten an Schueler | Ja | Ja | Ja | -- | Konfig. |
| Push-Benachrichtigungen | Ja | Ja | Ja | Ja | Ja |
| DSGVO-Datenexport | Ja | Ja | Ja | Ja | Ja |

"Konfig." bedeutet: Diese Funktion kann von der Schulleitung ein- oder ausgeschaltet werden.

---

# 3. Rolle: SUPERADMIN

**(Schulleitung / IT-Administration)**

Der SUPERADMIN ist die hoechste Berechtigungsstufe im System. Diese Rolle ist fuer die Schulleitung und die IT-Administration vorgesehen. Der SUPERADMIN hat vollstaendigen Zugriff auf alle Funktionen und kann das gesamte System konfigurieren.

## 3.1 Profil und Verifizierung

Als SUPERADMIN wird Ihr Konto bei der Ersteinrichtung des Systems angelegt. Sie koennen in Ihrem Profil folgende Angaben pflegen:

- **Vorname und Nachname**: Werden ueberall im System als Ihr Anzeigename verwendet
- **E-Mail-Adresse**: Dient zur Anmeldung (nicht aenderbar)
- **Telefonnummer** (optional): Fuer interne Kontaktzwecke
- **Profilbild (Avatar)**: Laden Sie ein Bild hoch, das neben Ihrem Namen angezeigt wird

So bearbeiten Sie Ihr Profil:
1. Klicken Sie oben rechts auf Ihren Namen oder Avatar.
2. Waehlen Sie **"Profil"**.
3. Aendern Sie die gewuenschten Felder.
4. Klicken Sie auf **"Speichern"**.

Zusaetzlich koennen Sie auf der Profilseite:
- **Push-Benachrichtigungen** aktivieren oder deaktivieren, um Echtzeit-Meldungen auf Ihrem Geraet zu erhalten

## 3.2 Funktionsuebersicht

Als SUPERADMIN stehen Ihnen folgende Hauptfunktionen zur Verfuegung:

**Verwaltung (nur SUPERADMIN):**
- Benutzer verwalten (Rollen zuweisen, Sonderrollen vergeben, Profile bearbeiten)
- Schulbereiche anlegen und verwalten (z.B. Krippe, Kindergarten, Grundschule, Mittelschule, Oberstufe)
- Raeume erstellen, bearbeiten, archivieren und loeschen
- Module aktivieren und deaktivieren (Nachrichten, Dateien, Jobboerse, Putz-Orga, Kalender, Formulare, Fotobox)
- Design und Branding anpassen (Farben, Logo)
- Kommunikationsregeln festlegen (Eltern-Eltern-Nachrichten, Schueler-Schueler-Nachrichten)
- Stundenregelung konfigurieren (Sollstunden pro Familie, davon Putzstunden)
- Stundenbericht einsehen und exportieren (PDF, CSV)
- Putz-Verwaltung (Konfigurationen, Termine, QR-Codes)

**Allgemeine Funktionen:**
- Dashboard mit Feed einsehen
- Alle Raeume einsehen (auch archivierte)
- In allen Raeumen die Leitungsrolle ausueben
- Schulweite, bereichsweite und raumbezogene Beitraege erstellen
- Schulweite, bereichsweite und raumbezogene Termine erstellen
- Schulweite, bereichsweite und raumbezogene Formulare erstellen
- Direkte Nachrichten an alle Rollen senden
- Diskussions-Threads erstellen und moderieren
- Benachrichtigungen empfangen

## 3.3 Menuepunkte im Detail

### Dashboard

Das Dashboard zeigt Ihren persoenlichen Feed. Hier sehen Sie Beitraege aus allen Raeumen, denen Sie zugeordnet sind, sowie schulweite und bereichsweite Beitraege. Angeheftete Beitraege werden oben angezeigt.

Als SUPERADMIN koennen Sie:
- Beitraege verfassen (schulweit, bereichsweit oder fuer einzelne Raeume)
- Beitraege kommentieren
- Eigene Beitraege bearbeiten und loeschen
- Fremde Beitraege loeschen
- Beitraege anheften

### Raeume

Unter "Raeume" sehen Sie alle Raeume, denen Sie zugeordnet sind. Ueber den Link "Entdecken" koennen Sie alle im System vorhandenen Raeume durchsuchen.

Als SUPERADMIN:
- Sehen Sie alle Raeume, auch archivierte
- Koennen Sie in jedem Raum wie ein Leiter agieren (Mitglieder verwalten, Diskussionen moderieren, Einstellungen aendern)
- Koennen Sie neue Raeume erstellen
- Koennen Sie die oeffentliche Beschreibung bearbeiten, die Nicht-Mitglieder sehen
- Koennen Sie Beitrittsanfragen genehmigen oder ablehnen
- Koennen Sie Familien als Ganzes einem Raum hinzufuegen

In der Raumdetailansicht stehen Ihnen folgende Registerkarten (Tabs) zur Verfuegung:
- **Info-Board**: Feed-Beitraege speziell fuer diesen Raum
- **Mitglieder**: Liste aller Mitglieder mit ihren Raum-Rollen, offene Beitrittsanfragen
- **Diskussionen**: Strukturierte Diskussions-Threads
- **Chat**: Echtzeit-Chat mit verschiedenen Kanaelen (Alle, Eltern, Schueler)
- **Dateien**: Dateien hochladen und herunterladen
- **Kalender**: Termine speziell fuer diesen Raum
- **Fotobox**: Foto-Threads und Bildergalerien

### Familie

Die Seite "Familie" zeigt Ihren Familienverbund an (sofern Sie einem angehoeren). Als SUPERADMIN koennen Sie Familien erstellen, auch wenn dies in der Regel von Eltern uebernommen wird.

### Nachrichten

Im Bereich "Nachrichten" koennen Sie Direktnachrichten an jeden Benutzer im System senden. Sie koennen:
- Neue Konversationen starten (Einzel- oder Gruppengespraeche)
- Empfaenger ueber eine Suchfunktion finden
- Nachrichten in Echtzeit senden und empfangen

### Jobboerse

Als SUPERADMIN sehen Sie die Jobboerse, koennen aber selbst keine Jobs annehmen und keine Elternstunden leisten. Sie sehen die offenen Jobs und koennen den Stundenbericht ueber die Verwaltung einsehen.

### Putz-Orga

Sie sehen die Putztermine, koennen sich aber nicht selbst dafuer anmelden. Die Verwaltung der Putzkonfigurationen erfolgt ueber den Verwaltungsbereich.

### Kalender

Der Kalender zeigt alle Termine an, die fuer Sie relevant sind -- schulweite, bereichsweite und raumbezogene Termine. Sie koennen:
- Neue Termine erstellen (alle Ebenen: schulweit, bereichsweit, raumbezogen)
- Termine bearbeiten und absagen
- Ihre Teilnahme bestaetigen ("Zusage", "Vielleicht" oder "Absage")
- Durch die Monate navigieren

### Formulare

Unter "Formulare" koennen Sie:
- Neue Umfragen oder Einverstaendniserklaerungen erstellen
- Formulare fuer die gesamte Schule, einen Bereich oder einen Raum veroeffentlichen
- Ergebnisse einsehen und als CSV oder PDF exportieren
- Einzelantworten einsehen (bei nicht-anonymen Formularen)

### Verwaltung

Der Verwaltungsbereich ist nur fuer Sie als SUPERADMIN sichtbar. Er umfasst folgende Unterbereiche:

**Verwaltungs-Dashboard:**
Eine Uebersicht mit Schnellzugriffen auf alle Verwaltungsbereiche.

**Benutzer:**
- Alle Benutzer auflisten und durchsuchen
- Benutzerrollen aendern (z.B. von PARENT zu TEACHER)
- Sonderrollen zuweisen (ELTERNBEIRAT, PUTZORGA)
- Benutzerprofile bearbeiten
- Raum-Mitgliedschaften eines Benutzers einsehen
- Familien-Mitgliedschaften eines Benutzers einsehen
- Benutzer zu Raeumen oder Familien hinzufuegen

**Raeume:**
- Alle Raeume auflisten (nach Bereich und Typ filterbar)
- Raeume erstellen, bearbeiten, deaktivieren, reaktivieren oder endgueltig loeschen
- Raum-Bereich zuordnen
- Mitglieder verwalten (hinzufuegen, entfernen, Rollen aendern)
- Mitglieder zwischen Raeumen verschieben oder kopieren

**Schulbereiche:**
- Schulbereiche anlegen (z.B. "Krippe", "Kindergarten", "Grundschule", "Mittelschule", "Oberstufe")
- Bereiche bearbeiten (Name, Beschreibung, Reihenfolge)
- Bereiche deaktivieren
- Bereichsleitungen (SECTION_ADMIN) zuweisen

**Module:**
- Module ein- und ausschalten: Nachrichten, Dateien, Jobboerse, Putz-Orga, Kalender, Formulare, Fotobox
- Wenn ein Modul deaktiviert wird, verschwindet der zugehoerige Menuepunkt fuer alle Benutzer

**Stundenbericht:**
- Uebersicht aller Familien mit ihren geleisteten und ausstehenden Stunden
- Ampelsystem: Gruen (Soll erreicht), Gelb (unterwegs), Rot (deutlich unter Soll)
- Getrennte Anzeige von Elternstunden und Putzstunden
- Export als CSV oder PDF

**Putz-Verwaltung:**
- Putzkonfigurationen erstellen (Schulbereich, Wochentag, Uhrzeit, Teilnehmerzahl, Stundengutschrift)
- Putztermine fuer einen Zeitraum generieren
- QR-Codes als PDF exportieren (zum Aushang am Putzort)
- PutzOrga-Rolle an Eltern vergeben

**Design und Branding:**
- Primaerfarbe und weitere Farben des Systems aendern
- Logo hochladen
- Schulnamen konfigurieren
- Vorschau der Farbeinstellungen

**Stundenregelung:**
- Gesamtstunden-Sollwert pro Familie und Jahr festlegen
- Anteil der Putzstunden am Sollwert festlegen

## 3.4 Navigation

Als SUPERADMIN sehen Sie in der Seitenleiste folgende Menuepunkte:
1. Dashboard
2. Raeume
3. Familie
4. Nachrichten (wenn Modul aktiviert)
5. Jobboerse (wenn Modul aktiviert)
6. Putz-Orga (wenn Modul aktiviert)
7. Kalender (wenn Modul aktiviert)
8. Formulare (wenn Modul aktiviert)
9. **Verwaltung** (nur fuer SUPERADMIN sichtbar)

Der aktuell aktive Menuepunkt wird farblich hervorgehoben.

## 3.5 Kommunikation

Als SUPERADMIN koennen Sie:
- **Direktnachrichten** an jede Person im System senden (alle Rollen)
- **Schulweite Beitraege** im Feed veroeffentlichen
- **Bereichsweite Beitraege** im Feed veroeffentlichen
- **Raumbeitraege** in jedem Raum veroeffentlichen
- **Diskussions-Threads** in jedem Raum erstellen, archivieren und loeschen
- **Benachrichtigungen** in Echtzeit empfangen (In-App und optional per Push)

Sie koennen ausserdem die **Kommunikationsregeln** konfigurieren:
- Ob Eltern untereinander Nachrichten senden duerfen (standardmaessig deaktiviert)
- Ob Schueler untereinander Nachrichten senden duerfen (standardmaessig deaktiviert)

## 3.6 Einschraenkungen

Trotz des Vollzugriffs gibt es einige Funktionen, die fuer den SUPERADMIN nicht vorgesehen sind:
- **Keine Jobs annehmen**: Sie koennen keine Elternstunden leisten, da Sie kein Elternteil im Sinne des Stundensystems sind
- **Keine Putztermine wahrnehmen**: Sie koennen sich nicht fuer Putztermine anmelden
- **Keine Stundengutschrift**: Da Sie keinem Familienverbund als Elternteil zugeordnet sind, erhalten Sie keine Stundengutschriften

## 3.7 Beziehungen zu anderen Rollen

| Beziehung | Beschreibung |
|-----------|-------------|
| Zu SECTION_ADMIN | Sie weisen Personen die Bereichsleitung zu und definieren die Schulbereiche |
| Zu TEACHER | Sie weisen die Lehrkraft-Rolle zu und koennen Lehrkraefte Raeumen zuordnen |
| Zu PARENT | Sie weisen die Eltern-Rolle zu und koennen Familienzuordnungen einsehen |
| Zu STUDENT | Sie weisen die Schueler-Rolle zu |
| Zu ELTERNBEIRAT | Sie vergeben die Sonderrolle ELTERNBEIRAT an ausgewaehlte Eltern |
| Zu PUTZORGA | Sie vergeben die Sonderrolle PUTZORGA an ausgewaehlte Eltern fuer bestimmte Bereiche |

---

# 4. Rolle: SECTION_ADMIN

**(Bereichsleitung)**

Der SECTION_ADMIN ist fuer die Verwaltung eines bestimmten Schulbereichs zustaendig, beispielsweise die Grundschule oder den Kindergarten. Diese Rolle wird von der Schulleitung (SUPERADMIN) vergeben.

## 4.1 Profil und Verifizierung

Ihr Profil verwalten Sie genau wie unter Abschnitt 3.1 beschrieben. Sie koennen Ihren Namen, Ihre Telefonnummer und Ihr Profilbild aendern sowie Push-Benachrichtigungen aktivieren.

## 4.2 Funktionsuebersicht

Als Bereichsleitung stehen Ihnen folgende Hauptfunktionen zur Verfuegung:

- Dashboard mit Feed einsehen
- Raeume erstellen und Raeume, in denen Sie Leitung sind, verwalten
- Bereichsweite Feed-Beitraege erstellen
- Bereichsweite Termine erstellen
- Bereichsweite Formulare erstellen
- Jobs erstellen und verwalten (inkl. fremder Jobs)
- Putztermine fuer den eigenen Bereich verwalten (sofern von SUPERADMIN freigegeben)
- Direkte Nachrichten an alle Rollen senden
- Diskussions-Threads erstellen (auch ohne Leitungsrolle im Raum)
- Benachrichtigungen empfangen

## 4.3 Menuepunkte im Detail

### Dashboard

Das Dashboard zeigt Ihren persoenlichen Feed mit Beitraegen aus Ihren Raeumen und bereichsweiten Beitraegen. Sie koennen:
- Beitraege verfassen (bereichsweit oder fuer Raeume)
- Beitraege kommentieren
- Eigene Beitraege bearbeiten und loeschen

### Raeume

Sie sehen Ihre eigenen Raeume und koennen ueber "Entdecken" weitere Raeume durchsuchen. In Raeumen, in denen Sie die Leitungsrolle haben, koennen Sie:
- Mitglieder verwalten
- Beitrittsanfragen genehmigen oder ablehnen
- Diskussionen moderieren
- Raum-Einstellungen aendern

### Familie

Falls Sie privat einem Familienverbund angehoeren, sehen Sie diesen hier.

### Nachrichten

Sie koennen Nachrichten an alle Rollen senden: SUPERADMIN, andere SECTION_ADMINs, TEACHER, PARENT und STUDENT.

### Jobboerse

Sie koennen:
- Neue Jobs erstellen
- Eigene Jobs bearbeiten
- Beliebige Jobs loeschen (auch von anderen Nutzern erstellt)
- Die Jobliste einsehen

Allerdings koennen Sie sich **nicht** selbst fuer Jobs bewerben und leisten keine Elternstunden.

### Putz-Orga

Sie sehen die Putztermine, koennen sich aber **nicht** selbst anmelden. Fuer die Verwaltung der Putztermine Ihres Bereichs wenden Sie sich an den SUPERADMIN oder nutzen die Ihnen zugewiesenen Verwaltungsfunktionen.

### Kalender

Sie koennen:
- Termine fuer Ihren Schulbereich erstellen
- An Terminen teilnehmen (Zusage, Vielleicht, Absage)
- Termine in Raeumen erstellen, in denen Sie Leitung sind

### Formulare

Sie koennen:
- Formulare fuer Ihren Schulbereich erstellen
- Formulare fuer Raeume erstellen, in denen Sie Leitung sind
- Ergebnisse Ihrer Formulare einsehen

## 4.4 Navigation

In der Seitenleiste sehen Sie:
1. Dashboard
2. Raeume
3. Familie
4. Nachrichten (wenn Modul aktiviert)
5. Jobboerse (wenn Modul aktiviert)
6. Putz-Orga (wenn Modul aktiviert)
7. Kalender (wenn Modul aktiviert)
8. Formulare (wenn Modul aktiviert)

Der Verwaltungsbereich ist fuer Sie **nicht** sichtbar.

## 4.5 Kommunikation

Als Bereichsleitung koennen Sie:
- **Direktnachrichten** an alle Rollen senden (SUPERADMIN, SECTION_ADMIN, TEACHER, PARENT, STUDENT)
- **Bereichsweite Beitraege** im Feed veroeffentlichen
- **Diskussions-Threads** in Raeumen erstellen und moderieren (wie ein TEACHER)
- **Benachrichtigungen** empfangen (In-App und optional Push)

## 4.6 Einschraenkungen

- **Kein Zugriff auf die Verwaltungsoberflaeche**: Sie koennen keine Benutzer verwalten, Module konfigurieren oder das Design aendern
- **Keine Jobs annehmen**: Sie koennen sich nicht fuer Elternstunden bewerben
- **Keine Putztermine wahrnehmen**: Sie koennen sich nicht fuer Putztermine anmelden
- **Keine schulweiten Beitraege/Termine**: Schulweite Aktionen sind dem SUPERADMIN vorbehalten
- **Keine Raeume archivieren oder loeschen**: Das ist nur dem SUPERADMIN moeglich

## 4.7 Beziehungen zu anderen Rollen

| Beziehung | Beschreibung |
|-----------|-------------|
| Zu SUPERADMIN | Die Schulleitung weist Ihnen die Bereichsleitung zu und definiert Ihren Bereich |
| Zu TEACHER | Sie arbeiten eng mit den Lehrkraeften Ihres Bereichs zusammen |
| Zu PARENT | Eltern in Ihrem Bereich koennen Ihnen Nachrichten senden |
| Zu STUDENT | Schueler in Ihrem Bereich koennen Ihnen Nachrichten senden |

---

# 5. Rolle: TEACHER

**(Lehrkraefte / Erzieher)**

Die Rolle TEACHER ist fuer das paedagogische Personal vorgesehen -- Lehrkraefte, Erzieher und paedagogische Fachkraefte. Lehrkraefte erhalten automatisch die Leitungsrolle, wenn sie einem Klassenraum zugewiesen werden.

## 5.1 Profil und Verifizierung

Ihr Profil verwalten Sie wie unter Abschnitt 3.1 beschrieben. Auf der Profilseite koennen Sie ausserdem Push-Benachrichtigungen aktivieren.

## 5.2 Funktionsuebersicht

Als Lehrkraft stehen Ihnen folgende Hauptfunktionen zur Verfuegung:

- Dashboard mit Feed einsehen
- Raeume erstellen und verwalten (als Leitung)
- Bereichsweite und raumbezogene Feed-Beitraege erstellen
- Diskussions-Threads erstellen und moderieren (auch ohne Leitungsrolle im Raum)
- Bereichsweite Termine erstellen
- Bereichsweite Formulare erstellen
- Raumbezogene Termine und Formulare erstellen (als Leitung)
- Direkte Nachrichten an alle Rollen senden
- Benachrichtigungen empfangen
- An allen Chatkanälen in Ihren Raeumen teilnehmen

## 5.3 Menuepunkte im Detail

### Dashboard

Das Dashboard zeigt Ihren Feed. Als Lehrkraft koennen Sie:
- Beitraege fuer Ihre Raeume, Ihren Schulbereich oder schulweit (wenn berechtigt) verfassen
- Beitraege kommentieren
- Eigene Beitraege bearbeiten und loeschen

### Raeume

Unter "Raeume" sehen Sie alle Raeume, denen Sie zugeordnet sind. Typischerweise sind das Ihre Klassen.

**Automatische Leitungsrolle**: Wenn Sie einem Raum vom Typ "Klasse" hinzugefuegt werden, erhalten Sie automatisch die Leitungsrolle (LEADER). Das bedeutet, Sie koennen:
- Den Raum bearbeiten (Name, Beschreibung, Einstellungen)
- Oeffentliche Beschreibung fuer Nicht-Mitglieder verfassen
- Mitglieder hinzufuegen und entfernen
- Beitrittsanfragen genehmigen oder ablehnen
- Familien als Ganzes hinzufuegen
- Diskussionsmodus festlegen (Voll, Nur Ankuendigungen, Deaktiviert)
- Festlegen, ob Eltern Diskussions-Threads erstellen duerfen
- Chat-Kanaele verwalten
- Fotobox-Einstellungen aendern

**Diskussions-Threads**: Als Lehrkraft koennen Sie in jedem Raum, in dem Sie Mitglied sind, Diskussions-Threads erstellen -- auch wenn Sie dort nicht die Leitungsrolle haben. Sie koennen:
- Neue Threads mit Titel und Beschreibung erstellen
- Die Zielgruppe festlegen (Alle, nur Eltern, nur Kinder)
- Threads archivieren (dann sind keine Antworten mehr moeglich)
- Threads loeschen

**Raumdetail-Tabs**: Sie sehen alle Registerkarten (Info-Board, Mitglieder, Diskussionen, Chat, Dateien, Kalender, Fotobox) und koennen in jedem Bereich aktiv werden.

**Raeume entdecken**: Ueber "Entdecken" koennen Sie alle Raeume durchsuchen und bei offenen Raeumen direkt beitreten oder bei Raeumen mit Beitrittskontrolle eine Anfrage senden.

### Familie

Falls Sie privat einem Familienverbund angehoeren, wird dieser hier angezeigt.

### Nachrichten

Sie koennen Nachrichten an alle Rollen senden und empfangen. Die Suchfunktion hilft Ihnen, bestimmte Personen schnell zu finden.

### Jobboerse

Sie sehen die offenen Jobs, koennen aber **weder Jobs erstellen noch sich bewerben**. Die Jobboerse ist primaer fuer Eltern gedacht.

### Putz-Orga

Sie sehen die Putztermine, koennen sich aber **nicht** dafuer anmelden.

### Kalender

Sie koennen:
- Termine fuer Ihren Schulbereich erstellen
- Termine fuer Raeume erstellen, in denen Sie Leitung sind
- An Terminen teilnehmen (Zusage, Vielleicht, Absage)
- Durch die Monate navigieren

### Formulare

Sie koennen:
- Formulare und Umfragen fuer Ihren Schulbereich erstellen
- Formulare fuer Raeume erstellen, in denen Sie Leitung sind
- Verschiedene Fragetypen verwenden (Freitext, Einfachauswahl, Mehrfachauswahl, Bewertung, Ja/Nein)
- Formulare als Entwurf speichern, veroeffentlichen und schliessen
- Ergebnisse einsehen und exportieren

## 5.4 Navigation

In der Seitenleiste sehen Sie:
1. Dashboard
2. Raeume
3. Familie
4. Nachrichten (wenn Modul aktiviert)
5. Jobboerse (wenn Modul aktiviert)
6. Putz-Orga (wenn Modul aktiviert)
7. Kalender (wenn Modul aktiviert)
8. Formulare (wenn Modul aktiviert)

## 5.5 Kommunikation

Als Lehrkraft koennen Sie:
- **Direktnachrichten** an alle Rollen senden (SUPERADMIN, SECTION_ADMIN, TEACHER, PARENT, STUDENT)
- **Bereichsweite Beitraege** im Feed veroeffentlichen
- **Raum-Beitraege** in Ihren Raeumen veroeffentlichen
- **Diskussions-Threads** erstellen und moderieren
- Im **Raum-Chat** in allen Kanaelen schreiben (Alle, Eltern, Schueler)
- **Benachrichtigungen** empfangen (In-App und optional Push)

## 5.6 Einschraenkungen

- **Kein Zugriff auf die Verwaltungsoberflaeche**
- **Keine Jobs erstellen oder annehmen**: Die Jobboerse steht Ihnen nur zur Ansicht zur Verfuegung
- **Keine Putztermine wahrnehmen**: Sie koennen sich nicht anmelden
- **Keine schulweiten Termine oder Formulare erstellen**: Das ist dem SUPERADMIN vorbehalten (es sei denn, Sie haben zusaetzlich die Sonderrolle ELTERNBEIRAT)
- **Keine Raeume archivieren oder loeschen**: Nur der SUPERADMIN kann das
- **Keine Benutzerrollen aendern**

## 5.7 Beziehungen zu anderen Rollen

| Beziehung | Beschreibung |
|-----------|-------------|
| Zu SUPERADMIN | Die Schulleitung weist Ihnen die Lehrkraft-Rolle zu und kann Sie Raeumen zuordnen |
| Zu SECTION_ADMIN | Ihre Bereichsleitung koordiniert den Schulbereich |
| Zu PARENT | Eltern koennen Ihnen jederzeit Nachrichten senden; Sie kommunizieren ueber Raum-Beitraege, Diskussionen und Chat |
| Zu STUDENT | Schueler in Ihren Raeumen; Sie moderieren Diskussionen und Chats |

---

# 6. Rolle: PARENT

**(Eltern / Erziehungsberechtigte)**

Die Rolle PARENT ist die zentrale Rolle fuer Eltern und Erziehungsberechtigte. Die meisten Funktionen von MonteWeb -- Familienverbund, Jobboerse, Putz-Organisation, Stundenkonto -- sind auf diese Rolle zugeschnitten.

## 6.1 Profil und Verifizierung

Ihr Profil verwalten Sie wie unter Abschnitt 3.1 beschrieben. Besonders wichtig fuer Sie:
- Halten Sie Ihren **Namen** aktuell, da er in Nachrichten und bei der Jobboerse angezeigt wird
- Hinterlegen Sie optional Ihre **Telefonnummer** fuer die Kontaktaufnahme bei Jobs
- Aktivieren Sie **Push-Benachrichtigungen**, um keine wichtigen Mitteilungen zu verpassen

## 6.2 Funktionsuebersicht

Als Elternteil stehen Ihnen folgende Hauptfunktionen zur Verfuegung:

**Familie und Stundenkonto:**
- Familienverbund erstellen oder beitreten
- Familienmitglieder einladen (per Code oder per Suche)
- Kinder dem Familienverbund zuordnen
- Stundenkonto einsehen (Elternstunden und Putzstunden)

**Raeume:**
- Raeume erstellen (z.B. Elterninitiativen, Projekte)
- Raeume entdecken und Beitritt anfragen
- In Raeumen Beitraege verfassen und kommentieren
- An Diskussionen teilnehmen (und Threads erstellen, wenn erlaubt)
- Im Chat kommunizieren (Hauptkanal und Elternkanal)
- Dateien hochladen und herunterladen
- Fotos in der Fotobox teilen

**Jobboerse (Elternstunden):**
- Offene Jobs einsehen
- Sich fuer Jobs anmelden
- Aufgaben starten und abschliessen
- Geleistete Stunden werden dem Familienverbund gutgeschrieben

**Putz-Organisation:**
- Putztermine einsehen
- Sich fuer Putztermine anmelden und abmelden
- Per QR-Code einchecken und auschecken
- Putzstunden werden dem Familienverbund gutgeschrieben (eigenes Unterkonto)

**Kalender:**
- Termine einsehen
- Teilnahme bestaetigen (Zusage, Vielleicht, Absage)
- Raum-Termine erstellen (als Raumleitung)

**Formulare:**
- Umfragen und Einverstaendniserklaerungen ausfuellen
- Raum-Formulare erstellen (als Raumleitung)

**Kommunikation:**
- Nachrichten an Lehrkraefte, Bereichsleitung und Schulleitung senden
- Nachrichten an andere Eltern senden (sofern konfiguriert)

## 6.3 Menuepunkte im Detail

### Dashboard

Das Dashboard ist Ihre Startseite. Hier sehen Sie alle aktuellen Beitraege:
- Beitraege aus Ihren Raeumen (z.B. Neuigkeiten aus der Klasse Ihres Kindes)
- Bereichsweite Beitraege (z.B. Informationen aus der Grundschule)
- Schulweite Beitraege (z.B. Ankuendigungen der Schulleitung)
- System-Banner (z.B. Hinweise auf anstehende Putztermine)

Sie koennen:
- Beitraege kommentieren
- Eigene Raum-Beitraege verfassen
- Durch aeltere Beitraege scrollen und weitere laden

### Raeume

Unter "Raeume" sehen Sie alle Raeume, denen Sie zugeordnet sind. Typisch sind:
- Der Klassenraum Ihres Kindes (Sie sind dort als "Eltern-Mitglied")
- Projektgruppen oder Arbeitskreise
- Der Elternbeirats-Raum (falls vorhanden)

**Raeume entdecken**: Klicken Sie auf "Entdecken", um alle verfuegbaren Raeume zu durchsuchen. Sie koennen nach Namen suchen und sehen die oeffentliche Beschreibung der Raeume.

**Einem Raum beitreten:**
- **Offene Raeume**: Klicken Sie auf "Beitreten" -- Sie sind sofort Mitglied.
- **Raeume mit Beitrittskontrolle**: Klicken Sie auf "Beitritt anfragen" und verfassen Sie optional eine Nachricht an die Raumleitung. Die Leitung entscheidet, ob Sie aufgenommen werden. Sie erhalten eine Benachrichtigung ueber die Entscheidung.
- **Raeume nur auf Einladung**: Diese Raeume koennen Sie nicht selbst betreten. Die Raumleitung muss Sie hinzufuegen.

**In der Raumdetailansicht:**

*Info-Board:*
Hier sehen Sie die Beitraege aus diesem Raum und koennen selbst Beitraege verfassen und kommentieren.

Sie koennen den Feed eines Raums **stummschalten**, wenn Sie weniger Benachrichtigungen erhalten moechten. Klicken Sie dazu auf "Feed stummschalten".

*Mitglieder:*
Hier sehen Sie alle Mitglieder des Raums mit ihren Rollen (Leitung, Mitglied, Eltern-Mitglied, Gast).

Falls Sie die Leitungsrolle haben, koennen Sie hier:
- Familien hinzufuegen
- Beitrittsanfragen genehmigen oder ablehnen

*Diskussionen:*
Strukturierte Gespraeche zu bestimmten Themen. Wenn die Raumeinstellungen es erlauben, koennen Sie als Elternteil neue Diskussions-Threads erstellen.

Jeder Thread hat:
- Einen Titel und optional eine Beschreibung
- Eine Zielgruppe: "Alle" (fuer alle Mitglieder), "Eltern" (nur fuer Elternteile und Lehrkraefte) oder "Kinder" (nur fuer Schueler und Lehrkraefte)
- Antworten von anderen Mitgliedern

Sie koennen Threads lesen und Antworten verfassen, solange der Thread aktiv ist. Archivierte Threads koennen nur noch gelesen werden.

*Chat:*
Der Echtzeit-Chat bietet verschiedene Kanaele:
- **Alle**: Alle Raummitglieder koennen hier schreiben
- **Eltern**: Nur Eltern-Mitglieder und die Raumleitung koennen hier schreiben

Nachrichten werden in Echtzeit zugestellt.

*Dateien:*
Laden Sie Dateien hoch (z.B. Dokumente, Fotos) oder laden Sie Dateien herunter, die andere Mitglieder bereitgestellt haben. Sie koennen Ordner erstellen, um Dateien zu organisieren.

*Kalender:*
Zeigt Termine an, die speziell fuer diesen Raum angelegt wurden (z.B. Elternabend, Klassenfest).

*Fotobox:*
Hier koennen Fotos aus dem Schulalltag in thematischen Threads (Alben) geteilt werden. Je nach Raum-Einstellung koennen Sie:
- Foto-Threads ansehen und Bilder in der Lightbox betrachten
- Eigene Bilder in bestehende Threads hochladen
- Neue Foto-Threads erstellen

### Familie

Die Familienseite ist ein zentraler Bereich fuer Sie als Elternteil.

**Familienverbund erstellen:**
1. Klicken Sie auf **"Familienverbund erstellen"**.
2. Geben Sie einen Familiennamen ein (z.B. "Familie Mueller").
3. Klicken Sie auf **"Erstellen"**.

**Einem Familienverbund beitreten:**
1. Klicken Sie auf **"Beitreten"**.
2. Geben Sie den Einladungscode ein, den Sie von einem Familienmitglied erhalten haben.
3. Klicken Sie auf **"Beitreten"**.

**Einladungscode generieren:**
Um ein anderes Familienmitglied einzuladen:
1. Klicken Sie auf **"Code generieren"**.
2. Ein Code wird angezeigt.
3. Klicken Sie auf das Kopier-Symbol und teilen Sie den Code mit dem neuen Mitglied.

**Mitglied per Suche einladen:**
1. Klicken Sie auf **"Mitglied einladen"**.
2. Suchen Sie die Person ueber die Namenssuche.
3. Waehlen Sie die Rolle (Elternteil oder Kind).
4. Klicken Sie auf **"Einladung senden"**.
5. Die eingeladene Person erhaelt eine Benachrichtigung und kann die Einladung annehmen oder ablehnen.

**Eingehende Einladungen:**
Wenn Sie von einer anderen Familie eingeladen wurden, erscheint oben auf der Familienseite eine Einladung mit dem Familiennamen und der vorgesehenen Rolle. Sie koennen die Einladung **annehmen** oder **ablehnen**.

**Stundenkonto:**
Auf der Familienseite sehen Sie Ihr Stundenkonto:
- **Elternstunden**: Stunden aus der Jobboerse (bestaetigte und ausstehende)
- **Putzstunden**: Stunden aus der Putz-Organisation (eigenes Unterkonto)
- **Ampelfarbe**: Gruen (Soll erreicht oder fast erreicht), Gelb (auf dem Weg), Rot (deutlich unter dem Soll)
- **Verbleibend**: Wie viele Stunden Sie noch leisten muessen

Wichtig: Das Stundenkonto bezieht sich immer auf den gesamten Familienverbund, nicht auf einzelne Personen. Wenn ein Elternteil Stunden leistet, werden diese der Familie gutgeschrieben.

### Nachrichten

Im Bereich "Nachrichten" koennen Sie:
- Neue Konversationen starten: Klicken Sie auf **"Neue Nachricht"** und suchen Sie den Empfaenger
- Nachrichten in bestehenden Konversationen lesen und antworten
- Gruppengespraeche fuehren

**Wer kann wem schreiben?**
- Sie koennen **immer** an Lehrkraefte, Bereichsleitungen und die Schulleitung schreiben
- An andere **Eltern** koennen Sie nur schreiben, wenn die Schulleitung dies erlaubt hat
- An **Schueler** koennen Sie **keine** Nachrichten senden

Falls Sie versuchen, eine nicht erlaubte Nachricht zu senden, erhalten Sie den Hinweis: "Kommunikation zwischen diesen Nutzern ist nicht erlaubt."

### Jobboerse

Die Jobboerse ist das Herzstuck der Elternstunden. Hier finden Sie freiwillige Aufgaben, fuer die Sie Stunden gutgeschrieben bekommen.

**Jobs suchen und ansehen:**
- Auf der Uebersichtsseite sehen Sie zwei Bereiche: "Offene Jobs" und "Meine Aufgaben"
- Sie koennen nach Kategorien filtern (z.B. Garten, Reparatur, Veranstaltung)
- Jeder Job zeigt: Titel, Kategorie, Ort, geschaetzte Stunden, Datum/Uhrzeit und die Anzahl der benoetigten Helfer

**Fuer einen Job anmelden:**
1. Oeffnen Sie die Job-Detailseite.
2. Klicken Sie auf **"Anmelden"**.
3. Der Job erscheint nun unter "Meine Aufgaben".

**Aufgabe durchfuehren und abschliessen:**
1. Oeffnen Sie die Aufgabe unter "Meine Aufgaben".
2. Klicken Sie auf **"Starten"**, wenn Sie beginnen.
3. Nach Abschluss klicken Sie auf **"Abschliessen"**.
4. Geben Sie die tatsaechlich geleisteten Stunden ein und optional eine Anmerkung.
5. Die Stunden muessen noch vom Job-Ersteller bestaetigt werden.

**Job erstellen:**
Als Elternteil koennen Sie auch selbst Jobs erstellen:
1. Klicken Sie auf **"Job erstellen"**.
2. Fuellen Sie das Formular aus: Titel, Beschreibung, Kategorie, Ort, geschaetzte Stunden, maximale Helferanzahl, Datum und Uhrzeit.
3. Optional: Verknuepfen Sie den Job mit einem Kalendertermin.
4. Klicken Sie auf **"Erstellen"**.

**Stunden bestaetigen (als Job-Ersteller):**
Wenn jemand eine Aufgabe abschliesst, die Sie erstellt haben, muessen Sie die Stunden bestaetigen. Oeffnen Sie den Job und klicken Sie bei der jeweiligen Zuweisung auf **"Bestaetigen"**.

### Putz-Orga

Die Putz-Organisation ermoeglicht es Ihnen, Putztermine wahrzunehmen und dafuer Putzstunden gutgeschrieben zu bekommen.

**Putztermine einsehen:**
- Unter "Anstehende Termine" sehen Sie alle verfuegbaren Putztermine
- "Meine Termine" zeigt die Termine, fuer die Sie sich eingetragen haben
- Jeder Termin zeigt: Datum, Uhrzeit, Schulbereich, Anzahl der Teilnehmer und ob noch Plaetze frei sind

**Fuer einen Putztermin anmelden:**
1. Oeffnen Sie einen Putztermin.
2. Klicken Sie auf **"Eintragen"**.
3. Der Termin erscheint nun unter "Meine Termine".

**Abmelden:**
Falls Sie doch nicht teilnehmen koennen:
1. Oeffnen Sie den Termin unter "Meine Termine".
2. Klicken Sie auf **"Austragen"**.

**Tausch anbieten:**
Falls Sie nicht teilnehmen koennen, aber keinen Ersatz finden:
1. Klicken Sie auf **"Tausch anbieten"**.
2. Andere Eltern sehen, dass ein Tauschplatz verfuegbar ist.

**QR-Check-in am Putzort:**
Am Putzort haengt ein QR-Code aus.
1. Scannen Sie den QR-Code mit der Kamera Ihres Smartphones.
2. Alternativ geben Sie den Code manuell ein.
3. Klicken Sie auf **"Einchecken"**.
4. Nach dem Putzen klicken Sie auf **"Auschecken"**.
5. Die Putzstunden werden Ihrem Familien-Putzstundenkonto gutgeschrieben.

### Kalender

Der Kalender zeigt alle fuer Sie relevanten Termine:
- Raum-Termine (z.B. Elternabend der Klasse Ihres Kindes)
- Bereichs-Termine (z.B. Tag der offenen Tuer der Grundschule)
- Schulweite Termine (z.B. Schulfest)

**Termine ansehen:**
- Navigieren Sie mit den Pfeilen durch die Monate
- Klicken Sie auf "Heute", um zum aktuellen Monat zurueckzukehren
- Klicken Sie auf einen Termin, um die Details zu sehen

**Teilnahme bestaetigen (RSVP):**
1. Oeffnen Sie einen Termin.
2. Waehlen Sie: **"Zusage"**, **"Vielleicht"** oder **"Absage"**.
3. Sie sehen, wie viele andere Personen zugesagt haben.

**Raum-Termine erstellen (als Leitung):**
Falls Sie in einem Raum die Leitungsrolle haben, koennen Sie Termine fuer diesen Raum erstellen.

### Formulare

Unter "Formulare" finden Sie:
- **Verfuegbare Formulare**: Umfragen und Einverstaendniserklaerungen, die fuer Sie bestimmt sind
- **Meine Formulare**: Formulare, die Sie selbst erstellt haben (als Raumleitung)

**Formular ausfuellen:**
1. Oeffnen Sie ein veroeffentlichtes Formular.
2. Beantworten Sie die Fragen (Freitext, Auswahl, Bewertung oder Ja/Nein).
3. Klicken Sie auf **"Antwort absenden"**.
4. Bereits beantwortete Formulare sind mit einem gruenen Haekchen markiert.

**Einverstaendniserklaerungen:**
Bei Formularen vom Typ "Einverstaendnis" muessen Sie mit "Ja" oder "Nein" antworten. Diese sind immer nicht-anonym.

## 6.4 Navigation

In der Seitenleiste sehen Sie:
1. Dashboard
2. Raeume
3. Familie
4. Nachrichten (wenn Modul aktiviert)
5. Jobboerse (wenn Modul aktiviert)
6. Putz-Orga (wenn Modul aktiviert)
7. Kalender (wenn Modul aktiviert)
8. Formulare (wenn Modul aktiviert)

## 6.5 Kommunikation

Als Elternteil koennen Sie:
- **Direktnachrichten** an Lehrkraefte, Bereichsleitungen und Schulleitung senden (immer moeglich)
- **Direktnachrichten** an andere Eltern senden (nur wenn von der Schulleitung erlaubt)
- **Keine Direktnachrichten** an Schueler senden
- **Raum-Beitraege** in Ihren Raeumen verfassen und kommentieren
- **Diskussions-Threads** erstellen (wenn die Raumeinstellung es erlaubt)
- Im **Raum-Chat** schreiben (Hauptkanal und Elternkanal)
- **Benachrichtigungen** empfangen (In-App und optional Push)

## 6.6 Einschraenkungen

- **Kein Zugriff auf die Verwaltungsoberflaeche**
- **Keine schulweiten oder bereichsweiten Beitraege, Termine oder Formulare erstellen** (es sei denn, Sie haben die Sonderrolle ELTERNBEIRAT)
- **Keine Nachrichten an Schueler** senden
- **Keine Raeume archivieren oder loeschen**
- **Nachrichten an andere Eltern** nur moeglich, wenn die Schulleitung es erlaubt hat

## 6.7 Beziehungen zu anderen Rollen

| Beziehung | Beschreibung |
|-----------|-------------|
| Zu SUPERADMIN | Die Schulleitung kann Ihnen Sonderrollen geben und Ihre Rolle aendern |
| Zu SECTION_ADMIN | Ihre Bereichsleitung; Sie koennen Nachrichten austauschen |
| Zu TEACHER | Ihre Hauptansprechpartner; Sie koennen jederzeit Nachrichten senden |
| Zu STUDENT | Ihr Kind ist moeglicherweise Mitglied in denselben Raeumen; direkte Nachrichten sind nicht moeglich |
| Zu Familienverbund | Stunden werden dem Familienverbund gutgeschrieben; Sie verwalten den Verbund gemeinsam mit anderen Elternteilen |

---

# 7. Rolle: STUDENT

**(Schueler)**

Die Rolle STUDENT ist fuer Schuelerinnen und Schueler vorgesehen, typischerweise ab der Mittelschule. Schueler haben einen eingeschraenkten Zugang, der dem Alter und der paedagogischen Situation angemessen ist.

## 7.1 Profil und Verifizierung

Das Profil koennen Sie wie unter Abschnitt 3.1 beschrieben verwalten. Als Schueler koennen Sie:
- Ihren Namen aendern
- Ihre Telefonnummer hinterlegen
- Ein Profilbild hochladen
- Push-Benachrichtigungen aktivieren

## 7.2 Funktionsuebersicht

Als Schueler stehen Ihnen folgende Funktionen zur Verfuegung:

- Dashboard mit Feed einsehen
- Raeume ansehen und in Raeumen Beitraege lesen und kommentieren
- Raeume entdecken und beitreten (wenn erlaubt)
- An erlaubten Diskussionen teilnehmen
- Im Raum-Chat schreiben (Hauptkanal und Schuelerkanal)
- Dateien in Raeumen hochladen und herunterladen
- Fotos in der Fotobox ansehen und teilen (wenn erlaubt)
- Termine im Kalender einsehen und Teilnahme bestaetigen
- Formulare ausfuellen
- Nachrichten an Lehrkraefte und Schulleitung senden
- Benachrichtigungen empfangen

## 7.3 Menuepunkte im Detail

### Dashboard

Das Dashboard zeigt Ihren Feed mit Beitraegen aus Ihren Raeumen und schulweiten Ankuendigungen. Sie koennen:
- Beitraege lesen und kommentieren
- Raum-Beitraege verfassen

### Raeume

Unter "Raeume" sehen Sie die Raeume, denen Sie zugeordnet sind (z.B. Ihre Klasse, Projektgruppen).

In der Raumdetailansicht:
- **Info-Board**: Beitraege lesen und kommentieren, eigene Beitraege verfassen
- **Mitglieder**: Sehen, wer im Raum ist
- **Diskussionen**: Threads mit der Zielgruppe "Alle" sehen; Threads mit der Zielgruppe "Kinder" sehen (wenn vom Raum aktiviert). Threads mit der Zielgruppe "Eltern" koennen Sie **nicht** sehen. Sie koennen auf aktive Threads antworten, aber keine eigenen Threads erstellen.
- **Chat**: Im Hauptkanal und im Schuelerkanal schreiben. Den Elternkanal koennen Sie **nicht** sehen.
- **Dateien**: Dateien hochladen und herunterladen
- **Kalender**: Raum-Termine einsehen
- **Fotobox**: Fotos ansehen; je nach Einstellung auch Bilder hochladen oder Threads erstellen

**Raeume entdecken**: Sie koennen ueber "Entdecken" Raeume suchen und bei offenen Raeumen beitreten oder Beitrittsanfragen senden.

### Familie

Sie koennen keinen Familienverbund erstellen. Falls Sie als Kind einem Familienverbund zugeordnet sind, sehen Sie die Familienseite mit den Mitgliedern.

### Nachrichten

Sie koennen Nachrichten an **Lehrkraefte**, **Bereichsleitungen** und die **Schulleitung** senden. Ob Sie Nachrichten an andere Schueler senden koennen, haengt von der Konfiguration Ihrer Schule ab (standardmaessig deaktiviert).

An **Eltern** koennen Sie **keine** Nachrichten senden.

### Jobboerse

Die Jobboerse steht Ihnen **nicht** zur Verfuegung. Sie koennen weder Jobs einsehen noch sich bewerben.

### Putz-Orga

Die Putz-Organisation steht Ihnen **nicht** zur Verfuegung. Sie koennen sich nicht fuer Putztermine anmelden.

### Kalender

Sie sehen alle fuer Sie relevanten Termine und koennen Ihre Teilnahme bestaetigen (Zusage, Vielleicht, Absage).

### Formulare

Sie koennen veroeffentlichte Formulare und Umfragen ausfuellen. Sie koennen **keine** eigenen Formulare erstellen.

## 7.4 Navigation

In der Seitenleiste sehen Sie:
1. Dashboard
2. Raeume
3. Familie
4. Nachrichten (wenn Modul aktiviert)
5. Kalender (wenn Modul aktiviert)
6. Formulare (wenn Modul aktiviert)

Die Menuepunkte "Jobboerse" und "Putz-Orga" sind fuer Sie sichtbar, aber die Funktionen sind fuer Ihre Rolle eingeschraenkt.

## 7.5 Kommunikation

Als Schueler koennen Sie:
- **Direktnachrichten** an Lehrkraefte, Bereichsleitungen und Schulleitung senden
- **Direktnachrichten** an andere Schueler senden (nur wenn von der Schulleitung erlaubt)
- **Keine Direktnachrichten** an Eltern senden
- **Raum-Beitraege** in Ihren Raeumen verfassen und kommentieren
- Im **Raum-Chat** schreiben (Hauptkanal und Schuelerkanal)
- An **Diskussionen** antworten (aber keine eigenen Threads erstellen)
- **Benachrichtigungen** empfangen

## 7.6 Einschraenkungen

- **Kein Zugriff auf die Verwaltungsoberflaeche**
- **Keine Jobboerse-Teilnahme**: Sie koennen keine Jobs erstellen oder annehmen
- **Keine Putztermin-Teilnahme**: Sie koennen sich nicht fuer Putztermine anmelden
- **Keine Familie erstellen**: Der Familienverbund wird von Eltern verwaltet
- **Keine Diskussions-Threads erstellen**: Sie koennen nur auf bestehende Threads antworten
- **Keine Nachrichten an Eltern**: Direktnachrichten an Eltern sind nicht moeglich
- **Nachrichten an Schueler eingeschraenkt**: Nur moeglich, wenn von der Schulleitung erlaubt
- **Eltern-Diskussionen nicht sichtbar**: Threads mit der Zielgruppe "Eltern" koennen Sie nicht sehen
- **Eltern-Chatkanal nicht sichtbar**: Der Eltern-Chatkanal ist fuer Sie nicht sichtbar
- **Keine schulweiten oder bereichsweiten Beitraege, Termine oder Formulare erstellen**

## 7.7 Beziehungen zu anderen Rollen

| Beziehung | Beschreibung |
|-----------|-------------|
| Zu SUPERADMIN | Die Schulleitung verwaltet Ihr Konto |
| Zu SECTION_ADMIN | Ihre Bereichsleitung; Sie koennen Nachrichten senden |
| Zu TEACHER | Ihre Lehrkraefte; Hauptansprechpartner fuer Nachrichten |
| Zu PARENT | Ihre Eltern sind moeglicherweise im selben Raum, aber direkte Nachrichten sind nicht moeglich |
| Zu anderen STUDENT | Nachrichten nur moeglich, wenn von der Schule erlaubt |

---

# 8. Sonderrolle: ELTERNBEIRAT

## 8.1 Was ist die Sonderrolle?

Die Sonderrolle ELTERNBEIRAT wird von der Schulleitung (SUPERADMIN) zusaetzlich zur normalen Systemrolle vergeben. Sie kann einem beliebigen Elternteil zugewiesen werden -- entweder schulweit oder fuer einen bestimmten Schulbereich.

Die Sonderrolle erweitert Ihre Berechtigungen, ohne Ihre Grundrolle zu aendern. Sie bleiben beispielsweise PARENT mit allen Elternfunktionen, erhalten aber zusaetzliche Moeglichkeiten.

## 8.2 Erweiterte Berechtigungen

Als ELTERNBEIRAT koennen Sie zusaetzlich zu Ihren normalen Elternrechten:

**Kalender/Termine:**
- Termine fuer alle Ebenen erstellen: schulweit, bereichsweit und raumbezogen
- Termine bearbeiten und absagen

**Feed-Beitraege:**
- Schulweite Beitraege im Feed veroeffentlichen
- Bereichsweite Beitraege im Feed veroeffentlichen

**Formulare:**
- Formulare und Umfragen fuer alle Ebenen erstellen: schulweit, bereichsweit und raumbezogen
- Ergebnisse Ihrer Formulare einsehen und exportieren

**Putz-Organisation:**
- Putz-Verwaltung: Konfigurationen erstellen und bearbeiten
- Putztermine generieren
- Slots verwalten
- QR-Codes als PDF exportieren
- Putz-Dashboard einsehen

## 8.3 Typische Arbeitsablaeufe

**Elternabend ankuendigen:**
1. Oeffnen Sie den Kalender.
2. Erstellen Sie einen neuen Termin.
3. Waehlen Sie die Ebene (z.B. "Schulbereich: Grundschule").
4. Geben Sie Titel, Datum, Uhrzeit und Ort ein.
5. Veroeffentlichen Sie den Termin.
6. Alle betroffenen Eltern und Lehrkraefte erhalten eine Benachrichtigung.

**Elternumfrage erstellen:**
1. Oeffnen Sie "Formulare".
2. Klicken Sie auf "Formular erstellen".
3. Waehlen Sie Typ "Umfrage" und Ebene (z.B. "Schulweit").
4. Fuegen Sie Fragen hinzu (Freitext, Auswahl, Bewertung, Ja/Nein).
5. Veroeffentlichen Sie das Formular.
6. Alle betroffenen Nutzer erhalten eine Benachrichtigung.

**Schulweite Nachricht veroeffentlichen:**
1. Oeffnen Sie das Dashboard.
2. Verfassen Sie einen Beitrag mit dem Scope "Schulweit".
3. Alle Benutzer sehen den Beitrag in ihrem Feed.

---

# 9. Sonderrolle: PUTZORGA

## 9.1 Was ist die Sonderrolle?

Die Sonderrolle PUTZORGA wird von der Schulleitung (SUPERADMIN) einem Elternteil fuer einen bestimmten Schulbereich zugewiesen. Sie ermoeglicht die Verwaltung der Putz-Organisation fuer diesen Bereich.

In der Seitenleiste sehen Personen mit der PUTZORGA-Rolle einen zusaetzlichen Menuepunkt: **"PutzOrga-Verwaltung"**.

## 9.2 Erweiterte Berechtigungen

Als PUTZORGA koennen Sie fuer Ihren zugewiesenen Schulbereich:

- **Putzkonfigurationen verwalten**: Neue Konfigurationen erstellen (Wochentag, Uhrzeit, Teilnehmerzahl, Stundengutschrift)
- **Putztermine generieren**: Fuer einen bestimmten Zeitraum automatisch Termine aus den Konfigurationen erzeugen
- **Slots verwalten**: Einzelne Putztermine bearbeiten oder absagen
- **QR-Codes als PDF exportieren**: QR-Code-Seiten zum Aushang am Putzort erstellen
- **Putz-Dashboard einsehen**: Statistiken ueber abgeschlossene Termine, Ausfaelle und Auslastung

Gleichzeitig koennen Sie weiterhin alle normalen Elternfunktionen nutzen (sich fuer Putztermine anmelden, Jobs annehmen usw.).

## 9.3 Typische Arbeitsablaeufe

**Putztermine fuer das neue Halbjahr erstellen:**
1. Oeffnen Sie die "PutzOrga-Verwaltung".
2. Erstellen Sie ggf. eine neue Konfiguration oder waehlen Sie eine bestehende.
3. Klicken Sie auf **"Termine generieren"**.
4. Waehlen Sie den Zeitraum (z.B. von 01.02. bis 31.07.).
5. Das System erzeugt automatisch alle Putztermine basierend auf dem konfigurierten Wochentag.

**QR-Codes fuer den Putzort drucken:**
1. Waehlen Sie die Konfiguration aus.
2. Klicken Sie auf **"QR-Codes PDF"**.
3. Waehlen Sie den Zeitraum.
4. Eine PDF-Datei wird heruntergeladen.
5. Drucken Sie die QR-Codes aus und haengen Sie sie am Putzort auf.

**Putz-Dashboard pruefen:**
Das Dashboard zeigt Ihnen:
- Wie viele Putztermine insgesamt stattgefunden haben
- Wie viele Termine abgeschlossen wurden
- Wie viele Ausfaelle (No-Shows) es gab
- Wie viele Termine noch Teilnehmer benoetigen

---

# 10. Module im Detail

MonteWeb ist modular aufgebaut. Die Schulleitung kann einzelne Module ein- oder ausschalten. Wenn ein Modul deaktiviert ist, verschwindet der zugehoerige Menuepunkt fuer alle Benutzer.

## 10.1 Dashboard und Feed

Das Dashboard ist die Startseite nach der Anmeldung. Es zeigt den persoenlichen Feed -- eine chronologisch sortierte Liste von Beitraegen.

**Beitragsquellen:**
- **Raum-Beitraege**: Posts aus Raeumen, denen Sie angehoeren
- **Bereichs-Beitraege**: Posts, die fuer Ihren gesamten Schulbereich bestimmt sind
- **Schulweite Beitraege**: Posts fuer die gesamte Schule
- **System-Banner**: Wichtige Hinweise (z.B. anstehende Putztermine)

**Beitrag verfassen:**
1. Klicken Sie in das Eingabefeld "Was gibt es Neues?"
2. Geben Sie optional einen Titel ein
3. Schreiben Sie Ihren Beitrag
4. Klicken Sie auf **"Veroeffentlichen"**

**Beitrag kommentieren:**
1. Klicken Sie unter einem Beitrag auf "Kommentieren"
2. Schreiben Sie Ihren Kommentar
3. Senden Sie den Kommentar ab

**Angeheftete Beitraege:**
Beitraege koennen von berechtigten Personen angeheftet werden. Angeheftete Beitraege erscheinen immer oben im Feed, gekennzeichnet mit dem Hinweis "Angeheftet".

**Aeltere Beitraege laden:**
Scrollen Sie ans Ende der Beitragsliste und klicken Sie auf **"Weitere laden"**, um aeltere Beitraege anzuzeigen.

## 10.2 Raeume

Raeume sind das zentrale Organisationselement in MonteWeb. Sie repraesentieren Klassen, Gruppen, Projekte oder andere Gemeinschaften.

### Raumtypen

| Typ | Beschreibung | Beispiel |
|-----|-------------|---------|
| **Klasse** | Regulaerer Unterrichtsraum; Lehrkraefte werden automatisch als Leitung eingetragen | "Klasse 3a", "Schmetterlingsgruppe" |
| **Gruppe** | Allgemeine Gruppe fuer verschiedene Zwecke | "Elternbeirat", "Schulchor" |
| **Projekt** | Zeitlich begrenztes Projekt | "Schulfest 2026", "Renovierung Pausenhof" |
| **Interessengruppe** | Freiwillige Gruppe zu einem Thema; kann Tags und ein Ablaufdatum haben | "Yoga-AG", "Leseclub" |
| **Sonstige** | Frei definierbar | -- |

### Beitrittspolitik

Jeder Raum hat eine Beitrittspolitik:
- **Offen**: Jeder kann direkt beitreten
- **Auf Anfrage**: Interessierte senden eine Anfrage; die Raumleitung entscheidet
- **Nur auf Einladung**: Nur die Raumleitung kann Mitglieder hinzufuegen

### Raum-Einstellungen (fuer die Raumleitung)

Die Raumleitung kann folgende Einstellungen aendern:
- **Chat aktivieren/deaktivieren**: Ob der Echtzeit-Chat im Raum verfuegbar ist
- **Dateien aktivieren/deaktivieren**: Ob Dateien hochgeladen werden koennen
- **Elternbereich aktivieren**: Ob ein separater Bereich fuer Eltern existiert
- **Sichtbarkeit**: "Nur Mitglieder", "Schulbereich" oder "Alle"
- **Diskussionsmodus**: "Voll" (alle koennen antworten), "Nur Ankuendigungen" (nur Leitung und Lehrkraefte erstellen Threads), "Deaktiviert"
- **Mitglieder-Threads erlauben**: Ob Eltern eigene Diskussions-Threads erstellen duerfen
- **Kinder-Diskussionen**: Ob Schueler Threads mit der Zielgruppe "Kinder" sehen koennen

### Raum-Tabs im Detail

**Info-Board:**
Beitraege speziell fuer diesen Raum. Lehrkraefte und die Raumleitung koennen hier Neuigkeiten posten. Alle Mitglieder koennen kommentieren.

**Mitglieder:**
Liste aller Raummitglieder mit ihren Rollen. Die Raumleitung kann hier:
- Familien als Ganzes hinzufuegen (alle Familienmitglieder werden dem Raum zugeordnet)
- Offene Beitrittsanfragen bearbeiten

**Diskussionen:**
Strukturierte Gespraeche in Threads. Jeder Thread hat:
- Einen Titel und eine optionale Beschreibung
- Eine Zielgruppe (Alle, Eltern, Kinder)
- Antworten von Mitgliedern

Archivierte Threads sind schreibgeschuetzt und mit dem Hinweis "Archiviert" gekennzeichnet.

**Chat:**
Echtzeit-Kommunikation in Kanaelen:
- **Alle**: Fuer alle Raummitglieder
- **Eltern**: Nur fuer Eltern-Mitglieder und die Raumleitung
- **Schueler**: Nur fuer Schueler-Mitglieder und die Raumleitung

**Dateien:**
Dateien hochladen, herunterladen und in Ordnern organisieren. Jedes Mitglied (ausser Gaeste) kann Dateien hochladen.

**Kalender:**
Raum-spezifische Termine (z.B. Elternabend, Klassenausflug). Die Raumleitung kann Termine erstellen.

**Fotobox:**
Bilder in thematischen Threads (Alben) teilen. Die Raumleitung kann die Berechtigungen konfigurieren:
- **Nur ansehen**: Mitglieder koennen Bilder ansehen, aber nicht hochladen
- **Bilder posten**: Mitglieder koennen Bilder in bestehende Threads hochladen
- **Threads erstellen**: Mitglieder koennen neue Foto-Threads anlegen

Bilder koennen in einer Lightbox-Ansicht vergroessert betrachtet werden.

## 10.3 Familie und Stundenkonto

Der Familienverbund ist die zentrale Einheit fuer das Stundensystem in MonteWeb.

**Wichtige Regeln:**
- Ein Elternteil gehoert zu genau einem Familienverbund
- Ein Kind kann mehreren Familienverbuenden angehoeren (z.B. bei getrennt lebenden Eltern)
- Alle Elternstunden und Putzstunden werden dem Familienverbund gutgeschrieben, nicht einzelnen Personen
- Das Putzstundenkonto ist ein eigenes Unterkonto innerhalb des Stundensystems

**Stundenkonto-Anzeige:**
Das Stundenkonto zeigt:
- **Bestaetigte Elternstunden**: Bereits vom Job-Ersteller bestaetigte Stunden
- **Ausstehende Elternstunden**: Noch nicht bestaetigte Stunden
- **Putzstunden**: Geleistete Putzstunden (eigenes Unterkonto)
- **Verbleibende Stunden**: Differenz zum Sollwert
- **Ampelfarbe**: Gruen / Gelb / Rot je nach Fortschritt

**Familienavatar:**
Sie koennen ein Familienbild hochladen, das neben dem Familiennamen angezeigt wird.

## 10.4 Nachrichten (Direktkommunikation)

Das Nachrichtenmodul ermoeglicht direkte Kommunikation zwischen einzelnen Personen oder Gruppen.

**Neue Konversation starten:**
1. Klicken Sie auf **"Neue Nachricht"**.
2. Suchen Sie den Empfaenger ueber die Suchfunktion (Name oder E-Mail).
3. Waehlen Sie, ob es eine Einzel- oder Gruppenkonversation sein soll.
4. Schreiben Sie Ihre Nachricht und senden Sie sie ab.

**Konversationen verwalten:**
- Links sehen Sie die Liste Ihrer Konversationen, sortiert nach der letzten Nachricht
- Ungelesene Nachrichten werden hervorgehoben
- Klicken Sie auf eine Konversation, um den Nachrichtenverlauf zu sehen

**Echtzeit-Zustellung:**
Neue Nachrichten werden in Echtzeit zugestellt. Wenn Ihr Gespraechspartner online ist, sieht er die Nachricht sofort.

**Kommunikationsregeln:**
Nicht alle Rollenkombinationen sind fuer Nachrichten erlaubt. Wenn Sie versuchen, eine Nachricht zu senden, die nicht erlaubt ist, erhalten Sie einen entsprechenden Hinweis.

## 10.5 Jobboerse (Elternstunden)

Die Jobboerse ist das zentrale Werkzeug fuer die Organisation von Elternstunden.

**Jobkategorien:**
Jobs sind in Kategorien organisiert (z.B. Garten, Reparatur, Veranstaltung, Buero). Sie koennen die Liste nach Kategorien filtern.

**Job-Lebenszyklus:**
1. **Offen**: Der Job wurde erstellt und sucht Helfer
2. **Vergeben**: Genuegend Helfer haben sich angemeldet
3. **In Arbeit**: Die Aufgabe wird gerade erledigt
4. **Abgeschlossen**: Die Stunden wurden bestaetigt
5. **Abgesagt**: Der Job wurde storniert

**Verknuepfung mit Kalendertermin:**
Jobs koennen mit Kalenderterminen verknuepft werden. So sehen Sie in der Terminansicht, welche Jobs mit dem Termin zusammenhaengen, und koennen direkt zur Anmeldung gelangen.

## 10.6 Putz-Organisation

Die Putz-Organisation verwaltet die Putztermine der Schule. Eltern melden sich freiwillig an (Opt-in-System, keine Rotationspflicht).

**Termin-Status:**
- **Offen**: Der Termin ist verfuegbar und hat noch freie Plaetze
- **Voll**: Alle Plaetze sind belegt
- **Laufend**: Der Termin findet gerade statt
- **Abgeschlossen**: Der Termin ist beendet, Stunden wurden gutgeschrieben
- **Abgesagt**: Der Termin wurde storniert

**Putzstunden und Familienkonto:**
Putzstunden werden separat vom allgemeinen Elternstundenkonto gefuehrt. Es gibt einen eigenen Sollwert fuer Putzstunden, der von der Schulleitung festgelegt wird.

## 10.7 Kalender und Termine

Der Kalender zeigt alle fuer Sie relevanten Termine in einer Monatsansicht (Agenda).

**Terminebenen:**
- **Raum**: Nur fuer Mitglieder eines bestimmten Raumes sichtbar (z.B. Elternabend der Klasse 3a)
- **Schulbereich**: Fuer alle Personen eines Schulbereichs sichtbar (z.B. Grundschul-Sportfest)
- **Schulweit**: Fuer alle sichtbar (z.B. Sommerfest)

**Wiederkehrende Termine:**
Termine koennen als wiederkehrend eingestellt werden:
- Taeglich
- Woechentlich
- Monatlich
- Jaehrlich

Bei wiederkehrenden Terminen kann ein Enddatum festgelegt werden.

**RSVP (Teilnahmebestaetigung):**
Zu jedem Termin koennen Sie Ihre Teilnahme bestaetigen:
- **Zusage**: Sie nehmen teil
- **Vielleicht**: Sie sind noch unsicher
- **Absage**: Sie koennen nicht teilnehmen

Die Anzahl der Zu- und Absagen wird auf der Terminseite angezeigt.

**Termin absagen:**
Der Ersteller eines Termins oder berechtigte Personen koennen einen Termin absagen. Alle Personen, die bereits eine RSVP abgegeben haben, werden benachrichtigt.

## 10.8 Formulare und Umfragen

Das Formularmodul ermoeglicht es, Umfragen und Einverstaendniserklaerungen zu erstellen und zu verwalten.

**Formulartypen:**
- **Umfrage**: Enthaelt verschiedene Fragetypen; kann anonym sein
- **Einverstaendnis**: Eine Ja/Nein-Abfrage; ist immer nicht-anonym

**Fragetypen:**
- **Freitext**: Offene Textantwort
- **Einfachauswahl**: Eine Antwort aus mehreren Optionen
- **Mehrfachauswahl**: Mehrere Antworten aus Optionen
- **Bewertung**: Zahlenbewertung (z.B. 1-5)
- **Ja/Nein**: Einfache Ja/Nein-Frage

**Formular-Lebenszyklus:**
1. **Entwurf**: Das Formular wird erstellt und kann bearbeitet werden
2. **Veroeffentlicht**: Das Formular ist fuer die Zielgruppe sichtbar und kann ausgefuellt werden
3. **Geschlossen**: Keine neuen Antworten moeglich
4. **Archiviert**: Nur noch zur Ansicht

**Ergebnisse:**
Ersteller und die Schulleitung koennen die Ergebnisse einsehen:
- Zusammenfassung mit Grafiken (Verteilungen, Durchschnittswerte)
- Ruecklaufquote (Fortschrittsbalken)
- Bei nicht-anonymen Formularen: Einzelantworten pro Person
- Export als CSV oder PDF

## 10.9 Fotobox

Die Fotobox ermoeglicht das Teilen von Fotos aus dem Schulalltag innerhalb von Raeumen.

**Foto-Threads:**
Fotos werden in thematischen Threads (Alben) organisiert, z.B. "Schulfest 2026" oder "Projektwoche".

**Bilder hochladen:**
1. Oeffnen Sie einen Foto-Thread.
2. Klicken Sie auf **"Bilder hochladen"**.
3. Waehlen Sie Bilder von Ihrem Geraet aus oder ziehen Sie sie in den Upload-Bereich.
4. Fuegen Sie optional eine Bildunterschrift hinzu.
5. Erlaubte Formate: JPEG, PNG, WebP, GIF.

**Lightbox:**
Klicken Sie auf ein Bild, um es in der Lightbox vergroessert anzuzeigen. Blaettern Sie mit den Pfeiltasten durch die Bilder.

**Berechtigungsstufen:**
Die Raumleitung legt fest, was Mitglieder in der Fotobox tun koennen:
- **Nur ansehen**: Bilder betrachten, aber nicht hochladen
- **Bilder posten**: Bilder in bestehende Threads hochladen
- **Threads erstellen**: Neue Foto-Threads anlegen und Bilder hochladen

Die Raumleitung und die Schulleitung (SUPERADMIN) haben immer volle Rechte in der Fotobox.

## 10.10 Benachrichtigungen

MonteWeb benachrichtigt Sie automatisch ueber wichtige Ereignisse.

**Benachrichtigungstypen:**
- Neuer Beitrag in einem Ihrer Raeume
- Neuer Kommentar zu Ihrem Beitrag
- Neue Direktnachricht
- Neuer Diskussions-Thread in einem Ihrer Raeume
- Neue Antwort in einer Diskussion, an der Sie teilnehmen
- Neuer Termin erstellt
- Termin geaendert oder abgesagt
- Neues Formular veroeffentlicht
- Einverstaendniserklaerung erfordert Ihre Antwort
- Beitrittsanfrage fuer Ihren Raum (als Leitung)
- Beitrittsanfrage genehmigt oder abgelehnt
- Familien-Einladung erhalten
- Familien-Einladung angenommen
- Elternstunden bestaetigt
- Putzstunden bestaetigt

**Zustellwege:**
1. **In-App**: Die Benachrichtigungsglocke in der Kopfzeile zeigt die Anzahl ungelesener Meldungen. Klicken Sie darauf, um alle Benachrichtigungen zu sehen.
2. **Push-Benachrichtigungen**: Wenn in Ihrem Profil aktiviert, erhalten Sie Meldungen direkt auf Ihrem Geraet -- auch wenn MonteWeb nicht geoeffnet ist.
3. **E-Mail** (optional): Falls von der Schule konfiguriert, erhalten Sie wichtige Benachrichtigungen per E-Mail.

**Benachrichtigungen verwalten:**
- Klicken Sie auf eine Benachrichtigung, um direkt zum betreffenden Inhalt zu springen
- Markieren Sie einzelne Benachrichtigungen als gelesen
- Klicken Sie auf **"Alle gelesen"**, um alle Benachrichtigungen auf einmal als gelesen zu markieren

## 10.11 Profil und Datenschutz

### Profil bearbeiten

Auf der Profilseite koennen Sie:
- **Vorname und Nachname** aendern
- **Telefonnummer** hinzufuegen oder aendern
- **Profilbild (Avatar)** hochladen oder entfernen
- **Push-Benachrichtigungen** ein- oder ausschalten

Die E-Mail-Adresse kann nicht geaendert werden, da sie zur Anmeldung verwendet wird.

### Datenschutz (DSGVO)

MonteWeb respektiert Ihre Datenschutzrechte gemaess der Datenschutz-Grundverordnung (DSGVO):

**Datenexport:**
Sie koennen jederzeit alle ueber Sie gespeicherten Daten als Datei herunterladen. Nutzen Sie dafuer die Exportfunktion in Ihrem Profil.

**Konto loeschen:**
Sie koennen Ihr Konto loeschen lassen. Dabei werden Ihre persoenlichen Daten anonymisiert:
- Ihr Name, Ihre E-Mail und Ihre Telefonnummer werden entfernt
- Ihre Beitraege und Nachrichten bleiben erhalten, werden aber einem anonymisierten Konto zugeordnet

Bitte beachten Sie: Die Kontoloesung kann nicht rueckgaengig gemacht werden. Wenden Sie sich bei Fragen an die Schulleitung.

---

# 11. Kommunikationsmatrix

Die folgende Tabelle zeigt, wer wem Direktnachrichten senden kann:

| Absender / Empfaenger | SUPERADMIN | SECTION_ADMIN | TEACHER | PARENT | STUDENT |
|------------------------|:----------:|:-------------:|:-------:|:------:|:-------:|
| **SUPERADMIN** | Ja | Ja | Ja | Ja | Ja |
| **SECTION_ADMIN** | Ja | Ja | Ja | Ja | Ja |
| **TEACHER** | Ja | Ja | Ja | Ja | Ja |
| **PARENT** | Ja | Ja | Ja | Konfig.* | Nein |
| **STUDENT** | Ja | Ja | Ja | Nein | Konfig.** |

\* **Eltern-Eltern-Nachrichten**: Standardmaessig deaktiviert. Die Schulleitung kann diese Funktion einschalten.

\** **Schueler-Schueler-Nachrichten**: Standardmaessig deaktiviert. Die Schulleitung kann diese Funktion einschalten.

**Grundsaetze:**
- Schulleitung, Bereichsleitungen und Lehrkraefte koennen immer an alle Rollen Nachrichten senden.
- Eltern koennen an keine Schueler Nachrichten senden.
- Schueler koennen an keine Eltern Nachrichten senden.
- Eltern untereinander und Schueler untereinander: nur wenn von der Schulleitung erlaubt.

---

# 12. Glossar

| Begriff | Erklaerung |
|---------|-----------|
| **Avatar** | Ihr Profilbild, das neben Ihrem Namen angezeigt wird |
| **Bereichsleitung** | Eine Person mit der Rolle SECTION_ADMIN, die einen Schulbereich verwaltet |
| **Beitrittsanfrage** | Eine Anfrage, um einem geschlossenen Raum beizutreten; muss von der Raumleitung genehmigt werden |
| **Dashboard** | Die Startseite nach der Anmeldung, die den persoenlichen Feed anzeigt |
| **Diskussions-Thread** | Ein strukturiertes Gespraech zu einem bestimmten Thema innerhalb eines Raumes |
| **DSGVO** | Datenschutz-Grundverordnung; regelt den Schutz Ihrer persoenlichen Daten |
| **Einladungscode** | Ein Code, mit dem eine Person einem Familienverbund beitreten kann |
| **Einverstaendniserklaerung** | Ein Formular, bei dem Sie mit Ja oder Nein antworten (z.B. Erlaubnis fuer Fotoaufnahmen) |
| **Elternstunden** | Freiwillige Arbeitsstunden, die Eltern fuer die Schule leisten (ueber die Jobboerse) |
| **Familienverbund** | Die Familie als organisatorische Einheit; alle Stunden werden dem Verbund gutgeschrieben |
| **Feed** | Die chronologische Liste von Beitraegen auf dem Dashboard oder im Info-Board eines Raumes |
| **Fotobox** | Bildergalerie innerhalb eines Raumes mit thematischen Foto-Threads |
| **Formulare** | Umfragen und Einverstaendniserklaerungen, die von berechtigten Personen erstellt werden |
| **Interessengruppe** | Ein Raumtyp fuer freiwillige Gruppen zu einem bestimmten Thema |
| **Jobboerse** | Uebersicht ueber freiwillige Aufgaben, fuer die Elternstunden gutgeschrieben werden |
| **Leitung (LEADER)** | Die Raum-Rolle mit den meisten Rechten; verantwortlich fuer den Raum |
| **Lightbox** | Vergroesserte Bildansicht in der Fotobox |
| **Modul** | Ein optionaler Funktionsbereich, der von der Schulleitung ein- oder ausgeschaltet werden kann |
| **Push-Benachrichtigung** | Eine Meldung, die direkt auf Ihrem Geraet erscheint, auch wenn MonteWeb nicht geoeffnet ist |
| **Putz-Orga** | Die Organisation der Putztermine an der Schule |
| **Putzstunden** | Stunden, die durch die Teilnahme an Putzterminen gesammelt werden (eigenes Unterkonto) |
| **PWA** | Progressive Web App; ermoeglicht die Installation von MonteWeb als App auf Ihrem Geraet |
| **QR-Code** | Ein quadratischer Barcode, der am Putzort aushaengt und zum Ein-/Auschecken gescannt wird |
| **Raum** | Ein digitaler Raum fuer eine Klasse, Gruppe, ein Projekt oder eine Interessengemeinschaft |
| **RSVP** | Teilnahmebestaetigung fuer einen Termin (Zusage, Vielleicht, Absage) |
| **Schulbereich** | Ein Abschnitt der Schule (z.B. Krippe, Kindergarten, Grundschule, Mittelschule, Oberstufe) |
| **Sonderrolle** | Eine zusaetzliche Rolle (ELTERNBEIRAT oder PUTZORGA), die ueber die normale Systemrolle hinausgeht |
| **SSO** | Single Sign-On; Anmeldung ueber ein zentrales Schulnetzwerk-Konto |
| **Stundenkonto** | Uebersicht ueber geleistete und ausstehende Stunden des Familienverbundes |
| **SUPERADMIN** | Die hoechste Berechtigungsstufe; typischerweise Schulleitung oder IT-Administration |
| **Umfrage** | Ein Formular mit verschiedenen Fragetypen zur Meinungserhebung |
| **Zielgruppe** | Bei Diskussions-Threads: Wer den Thread sehen kann (Alle, Eltern, Kinder) |

---

# 13. Haeufig gestellte Fragen (FAQ)

### Anmeldung und Zugang

**Ich kann mich nicht anmelden. Was tun?**
Pruefen Sie zunaechst, ob Ihre E-Mail-Adresse und Ihr Passwort korrekt eingegeben sind. Achten Sie auf Gross-/Kleinschreibung beim Passwort. Falls Sie Ihr Passwort vergessen haben, nutzen Sie die "Passwort vergessen"-Funktion. Wenn das Problem weiterhin besteht, wenden Sie sich an die Schulleitung oder IT-Abteilung.

**Wie aendere ich mein Passwort?**
Nutzen Sie die "Passwort vergessen"-Funktion auf der Anmeldeseite. Sie erhalten eine E-Mail mit einem Link zum Zuruecksetzen.

**Kann ich die Sprache aendern?**
Ja, klicken Sie in der Kopfzeile oben rechts auf den Sprachumschalter. Sie koennen zwischen Deutsch und Englisch wechseln.

### Raeume

**Wie trete ich einem Raum bei?**
Gehen Sie zu "Raeume" und klicken Sie auf "Entdecken". Suchen Sie den gewuenschten Raum. Bei offenen Raeumen koennen Sie direkt beitreten. Bei Raeumen mit Beitrittskontrolle senden Sie eine Anfrage, die von der Raumleitung genehmigt werden muss.

**Ich sehe bestimmte Inhalte in einem Raum nicht. Warum?**
Manche Inhalte sind nur fuer bestimmte Raum-Rollen sichtbar. Eltern-Diskussionen sind z.B. nur fuer Eltern-Mitglieder und die Raumleitung sichtbar. Der Eltern-Chatkanal ist nur fuer Eltern-Mitglieder zugaenglich.

**Wie kann ich einen Raum stumm schalten?**
Oeffnen Sie die Detailseite des Raums und klicken Sie auf "Feed stummschalten". Sie erhalten dann keine Feed-Benachrichtigungen mehr fuer diesen Raum.

### Familie und Stunden

**Wie erstelle ich einen Familienverbund?**
Gehen Sie zu "Familie" und klicken Sie auf "Familienverbund erstellen". Geben Sie einen Familiennamen ein und klicken Sie auf "Erstellen".

**Wie lade ich jemanden in meine Familie ein?**
Es gibt zwei Moeglichkeiten: (1) Generieren Sie einen Einladungscode und teilen Sie ihn mit der Person. (2) Klicken Sie auf "Mitglied einladen", suchen Sie die Person und senden Sie eine Einladung.

**Mein Kind hat getrennt lebende Eltern. Wie wird das abgebildet?**
Ein Kind kann mehreren Familienverbuenden zugeordnet werden. Jeder Elternteil erstellt einen eigenen Familienverbund und fuegt das Kind hinzu.

**Werden meine Stunden dem Familienverbund gutgeschrieben?**
Ja, alle Elternstunden und Putzstunden werden dem Familienverbund gutgeschrieben, nicht einzelnen Personen. So zaehlen die Stunden beider Elternteile zusammen.

**Was bedeutet die Ampelfarbe?**
Gruen: Sie haben genuegend Stunden geleistet oder sind auf gutem Weg. Gelb: Sie sollten sich um weitere Stunden bemuehen. Rot: Es fehlen noch deutlich Stunden.

### Jobboerse

**Wie melde ich mich fuer einen Job an?**
Oeffnen Sie die Jobboerse, waehlen Sie einen Job aus und klicken Sie auf "Anmelden". Der Job erscheint dann unter "Meine Aufgaben".

**Wie schliesse ich eine Aufgabe ab?**
Oeffnen Sie die Aufgabe unter "Meine Aufgaben", klicken Sie auf "Starten" und nach Abschluss auf "Abschliessen". Geben Sie die tatsaechlich geleisteten Stunden ein.

**Warum kann ich keine Jobs annehmen?**
Nur Eltern (Rolle PARENT) koennen sich fuer Jobs bewerben. Schulleitung, Bereichsleitungen und Lehrkraefte leisten keine Elternstunden.

### Putz-Organisation

**Wie melde ich mich fuer einen Putztermin an?**
Gehen Sie zu "Putz-Orga", waehlen Sie einen Termin und klicken Sie auf "Eintragen".

**Wie funktioniert der QR-Check-in?**
Am Putzort haengt ein QR-Code. Scannen Sie diesen mit der Kamera Ihres Smartphones oder geben Sie den Code manuell ein. Klicken Sie auf "Einchecken". Nach dem Putzen klicken Sie auf "Auschecken".

**Was passiert, wenn ich nicht erscheine?**
Wenn Sie sich angemeldet haben, aber nicht erscheinen (kein Check-in), wird dies als "Nicht erschienen" vermerkt.

### Kalender und Termine

**Wie bestaetigt ich meine Teilnahme an einem Termin?**
Oeffnen Sie den Termin im Kalender und waehlen Sie "Zusage", "Vielleicht" oder "Absage".

**Kann ich wiederkehrende Termine erstellen?**
Ja, beim Erstellen eines Termins koennen Sie eine Wiederholung einstellen (taeglich, woechentlich, monatlich, jaehrlich) und ein Enddatum festlegen.

### Formulare

**Wie fuege ich ein Formular aus?**
Gehen Sie zu "Formulare", oeffnen Sie das Formular und beantworten Sie die Fragen. Klicken Sie auf "Antwort absenden".

**Kann ich meine Antwort nochmals aendern?**
Nein, jedes Formular kann nur einmal ausgefuellt werden. Pruefen Sie Ihre Antworten vor dem Absenden.

**Sind meine Antworten anonym?**
Das haengt vom Formular ab. Wenn es als "Anonym" gekennzeichnet ist, kann der Ersteller Ihre Antworten nicht Ihrem Namen zuordnen.

### Benachrichtigungen

**Wie aktiviere ich Push-Benachrichtigungen?**
Gehen Sie zu Ihrem Profil und schalten Sie den Schalter fuer Push-Benachrichtigungen ein. Erlauben Sie die Benachrichtigungen in Ihrem Browser, wenn Sie dazu aufgefordert werden.

**Ich erhalte keine Push-Benachrichtigungen. Was tun?**
Pruefen Sie, ob Push-Benachrichtigungen in Ihrem Browserprofil aktiviert sind. Falls der Browser die Benachrichtigungen blockiert hat, muessen Sie dies in den Browser-Einstellungen aendern. Auf der Profilseite wird ein Hinweis angezeigt, wenn Benachrichtigungen blockiert sind.

### Datenschutz

**Wie kann ich meine gespeicherten Daten einsehen?**
Nutzen Sie die DSGVO-Datenexport-Funktion in Ihrem Profil. Sie erhalten eine Datei mit allen ueber Sie gespeicherten Daten.

**Wie kann ich mein Konto loeschen?**
Kontaktieren Sie die Schulleitung oder nutzen Sie die Loesch-Funktion in Ihrem Profil. Ihre Daten werden anonymisiert. Bitte beachten Sie, dass dieser Vorgang nicht rueckgaengig gemacht werden kann.

---

*Dieses Handbuch wurde fuer MonteWeb -- Schul-Intranet fuer Montessori-Schulkomplexe erstellt. Bei Fragen oder Problemen wenden Sie sich bitte an Ihre Schulleitung oder IT-Abteilung.*
