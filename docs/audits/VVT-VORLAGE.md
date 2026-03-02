# Verzeichnis der Verarbeitungstätigkeiten (VVT)
## nach Art. 30 DSGVO

**Verantwortlicher:** [Schulname eintragen]
**Anschrift:** [Schuladresse eintragen]
**Datenschutzbeauftragter (DSB):** [Name, E-Mail eintragen – falls benannt]
**System:** MonteWeb – Schulintranet
**Fassung:** 1.0 | **Erstellt:** 2026-02-28
**Nächste Überprüfung:** [Datum eintragen – empfohlen: jährlich]

---

> **Hinweis:** Diese Vorlage ist im Rahmen einer technischen Prüfung entstanden und muss durch den
> Datenschutzbeauftragten bzw. die verantwortliche Stelle rechtlich geprüft und angepasst werden.
> Insbesondere Rechtsgrundlagen und Löschfristen sind standortspezifisch zu verifizieren.

---

## 1. Nutzerregistrierung und Kontoverwaltung

| Feld | Inhalt |
|------|--------|
| **Bezeichnung** | Registrierung und Verwaltung von Nutzerkonten |
| **Zweck** | Identifikation, Authentifizierung, Zugriffsverwaltung im Schulintranet |
| **Rechtsgrundlage** | Art. 6 Abs. 1 lit. b DSGVO (Vertragserfüllung / schulisches Rechtsverhältnis); ggf. Art. 6 Abs. 1 lit. c (rechtliche Verpflichtung) |
| **Betroffene Personen** | Lehrkräfte, Eltern, Schüler (ab 16 J.), pädagogisches Personal, Verwaltung |
| **Datenkategorien** | Vor- und Nachname, E-Mail-Adresse, Passwort-Hash, Rolle, Profilbild (optional), Telefon (optional), TOTP-Secret (bei aktivierter 2FA) |
| **Empfänger** | Interne Nutzung; kein Drittlandsexport |
| **Löschfrist** | Auf Anfrage sofort (Art. 17); automatisch nach 14-tägiger Abklingfrist bei Löschanfrage; Anonymisierung statt Löschung wo Referenzintegrität erforderlich |
| **Technische Maßnahmen** | Passwort-Hashing (bcrypt), JWT-Authentifizierung (15 min Access Token), verschlüsselte Übertragung (TLS), 2FA optional/erzwingbar |

---

## 2. Familienverwaltung

| Feld | Inhalt |
|------|--------|
| **Bezeichnung** | Verwaltung von Familienverbünden und Eltern-Kind-Zuordnungen |
| **Zweck** | Abrechnung von Elternstunden, Zuweisung von Schulbereichen, Kommunikationsregeln |
| **Rechtsgrundlage** | Art. 6 Abs. 1 lit. b DSGVO |
| **Betroffene Personen** | Eltern, Kinder (Schüler) |
| **Datenkategorien** | Familienname, Mitglieder (Eltern + Kinder), Rollenzuordnung, Stundenkonto, Befreiungsstatus |
| **Empfänger** | Lehrkräfte, Schuladministration |
| **Löschfrist** | Gemeinsam mit Nutzer-Löschung; Stundenkonto: Aufbewahrung gemäß Schulrecht (empfohlen: 3 Jahre nach Schulaustritt) |
| **Technische Maßnahmen** | Rollenbasierter Zugriff; Familiendaten nur für zugehörige Eltern und Admins sichtbar |

---

## 3. Direktnachrichten und Gruppen-Chat

| Feld | Inhalt |
|------|--------|
| **Bezeichnung** | Interne Kommunikation via Direktnachrichten und Raum-Chat |
| **Zweck** | Kommunikation zwischen Lehrkräften, Eltern, Schülern im schulischen Kontext |
| **Rechtsgrundlage** | Art. 6 Abs. 1 lit. b DSGVO |
| **Betroffene Personen** | Alle angemeldeten Nutzer |
| **Datenkategorien** | Nachrichteninhalt (Text, Bilder), Zeitstempel, Absender-ID, Empfänger-ID, Lesebestätigung, Reply-Referenzen |
| **Empfänger** | Ausschließlich direkte Gesprächsteilnehmer; Admins haben technischen Zugriff (zu dokumentieren) |
| **Löschfrist** | Chat-Bilder: 90 Tage (automatisch); Nachrichten: auf Nutzeranfrage; nach Nutzerlöschung: Anonymisierung |
| **Technische Maßnahmen** | Nur authentifizierte Nutzer; Kommunikationsregeln konfigurierbar (Eltern↔Eltern opt-in); MinIO für Bilder ohne öffentlichen Zugriff |

---

## 4. Newsfeed und Beiträge

| Feld | Inhalt |
|------|--------|
| **Bezeichnung** | Schul-Newsfeed, Kommentare, Datei-Anhänge |
| **Zweck** | Schulweite und bereichsspezifische Informationsvermittlung |
| **Rechtsgrundlage** | Art. 6 Abs. 1 lit. b DSGVO |
| **Betroffene Personen** | Alle Nutzer |
| **Datenkategorien** | Beitragsinhalt, Bilder/Anhänge, Kommentare, Reaktionen (Emoji), Autor-ID, Zeitstempel, Zielgruppen-Filter |
| **Empfänger** | Je nach Sichtbarkeit: alle Nutzer oder spezifische Gruppen |
| **Löschfrist** | Durch Autor oder Admin löschbar; nach Nutzerlöschung: Anonymisierung des Autors |
| **Technische Maßnahmen** | Targeted Posts (nur für bestimmte User-IDs sichtbar), rollenbasierter Zugriff |

---

## 5. Kalender und Veranstaltungen

| Feld | Inhalt |
|------|--------|
| **Bezeichnung** | Schulkalender, Raumkalender, Zusagen (RSVP), iCal-Export |
| **Zweck** | Terminplanung, Verfügbarkeits- und Teilnahmeerfassung |
| **Rechtsgrundlage** | Art. 6 Abs. 1 lit. b DSGVO |
| **Betroffene Personen** | Alle Nutzer; insb. Teilnehmer von Veranstaltungen |
| **Datenkategorien** | Veranstaltungsdaten, RSVP-Status (Zusage/Absage), Teilnehmer-IDs, iCal-Abonnements |
| **Empfänger** | Schulintern; iCal-Export: unter Nutzerhoheit |
| **Löschfrist** | Vergangene Events: 1 Jahr nach Datum; RSVP-Daten: gemeinsam mit Event |
| **Technische Maßnahmen** | Berechtigungskonzept (ROOM/SECTION/SCHOOL), Löschung löst Feed-Benachrichtigung aus |

---

## 6. Jobboard und Elternstunden

| Feld | Inhalt |
|------|--------|
| **Bezeichnung** | Elternstunden-Jobboard, Stunden-Abrechnung, Jahresabrechnung |
| **Zweck** | Dokumentation und Abrechnung der schulischen Elternmitwirkungspflicht |
| **Rechtsgrundlage** | Art. 6 Abs. 1 lit. b und c DSGVO (schulische Pflicht); ggf. Art. 6 Abs. 1 lit. f |
| **Betroffene Personen** | Eltern |
| **Datenkategorien** | Job-Ausschreibungen, Bewerbungen, Zuweisungen, Stunden-Buchungen, Familien-Stundenkonto, Jahresabrechnung (PDF) |
| **Empfänger** | Schuladministration, betroffene Lehrkräfte |
| **Löschfrist** | Stunden-Daten: 3 Jahre nach Schuljahresende (Aufbewahrungspflicht); Jobs: 1 Jahr nach Abschluss |
| **Technische Maßnahmen** | PDF-Export nur für Admins; rollenbasierter Zugriff |

---

## 7. Putz-Organisation (QR-Check-in)

| Feld | Inhalt |
|------|--------|
| **Bezeichnung** | Freiwillige Putz-Dienste, QR-Code-Check-in, Stunden-Erfassung |
| **Zweck** | Organisation und Nachweis von Reinigungsarbeiten durch Eltern |
| **Rechtsgrundlage** | Art. 6 Abs. 1 lit. b DSGVO (vertragliche Verpflichtung/schulische Ordnung) |
| **Betroffene Personen** | Eltern (freiwillige Anmeldung) |
| **Datenkategorien** | Anmeldungen, Check-in/Check-out-Zeitstempel, Raumbezug, Familien-Stundenkonto (Putz-Unterkonto) |
| **Empfänger** | Schuladministration |
| **Löschfrist** | 3 Jahre nach Schuljahresende |
| **Technische Maßnahmen** | QR-Code enthält keine PII; Check-in erfordert Authentifizierung |

---

## 8. Formulare und Umfragen

| Feld | Inhalt |
|------|--------|
| **Bezeichnung** | Einwilligungs-/Consent-Formulare, Schulumfragen |
| **Zweck** | Einholung von Einwilligungen, schulische Befragungen |
| **Rechtsgrundlage** | Art. 6 Abs. 1 lit. a DSGVO (Einwilligung) oder Art. 6 Abs. 1 lit. b/c |
| **Betroffene Personen** | Eltern, Lehrkräfte, ggf. Schüler |
| **Datenkategorien** | Formularantworten, Zeitstempel, Nutzer-ID des Ausfüllers, Zielgruppen-Konfiguration |
| **Empfänger** | Formular-Ersteller, Schuladministration |
| **Löschfrist** | Einwilligungs-Formulare: Bis Einwilligung widerrufen oder Schulaustritt; Umfragen: 2 Jahre nach Abschluss |
| **Technische Maßnahmen** | Anonymisierter Ergebnisexport möglich; Multi-Section-Targeting |

---

## 9. Fotobox

| Feld | Inhalt |
|------|--------|
| **Bezeichnung** | Schulinterne Foto-Threads und Bildergalerien |
| **Zweck** | Schulinterne Dokumentation und Kommunikation mit Bildern |
| **Rechtsgrundlage** | Art. 6 Abs. 1 lit. a DSGVO (Einwilligung, insb. für Aufnahmen identifizierbarer Personen); KUG §22 |
| **Betroffene Personen** | Alle auf Bildern abgebildeten Personen; Upload-Nutzer |
| **Datenkategorien** | Fotos (mit Personenbezug möglich), Zeitstempel, Upload-Nutzer, Thumbnail |
| **Empfänger** | Abhängig von Thread-Audience (ALL / PARENTS_ONLY / STUDENTS_ONLY) |
| **Löschfrist** | Auf Anfrage sofort; empfohlen: Schuljahresende + 1 Jahr |
| **Technische Maßnahmen** | Audience-Filter, JWT-geschützte Bild-URLs, MinIO ohne öffentlichen Zugriff, Thumbnails; **Einwilligungs-Flow ausstehend (siehe DSGVO-H-04)** |

---

## 10. Fundgrube (Lost & Found)

| Feld | Inhalt |
|------|--------|
| **Bezeichnung** | Schulweites Fundsachen-Verzeichnis mit Fotos |
| **Zweck** | Rückführung verlorener Gegenstände |
| **Rechtsgrundlage** | Art. 6 Abs. 1 lit. f DSGVO (berechtigtes Interesse der Schulgemeinschaft) |
| **Betroffene Personen** | Finder, Einreicher, Abholer |
| **Datenkategorien** | Beschreibung, Fotos (ggf. mit Personenbezug auf dem Gegenstand), Zeitstempel, Claim-Status, Nutzer-IDs |
| **Empfänger** | Alle authentifizierten Nutzer (Beschreibung); vollständige Daten nur für Admin und Einreicher |
| **Löschfrist** | Automatisch nach 24h bei abgeholten Items; nicht abgeholte: 30 Tage |
| **Technische Maßnahmen** | Claim-Workflow mit Ablauf, MinIO-Bilder über Backend-Proxy, Bereichsfilter |

---

## 11. Wiki

| Feld | Inhalt |
|------|--------|
| **Bezeichnung** | Raum-Wiki, Versionsverwaltung |
| **Zweck** | Interne Wissenssammlung und Dokumentation je Raum |
| **Rechtsgrundlage** | Art. 6 Abs. 1 lit. b DSGVO |
| **Betroffene Personen** | Autoren (Nutzer-ID in Versionshistorie) |
| **Datenkategorien** | Wiki-Inhalt (Markdown), Versionshistorie, Autor-ID, Zeitstempel |
| **Empfänger** | Raummitglieder |
| **Löschfrist** | Gemeinsam mit Raum-Löschung oder auf Anfrage; Versionshistorie: 2 Jahre |
| **Technische Maßnahmen** | Zugriff nur für authentifizierte Raummitglieder |

---

## 12. Aufgabenverwaltung (Kanban)

| Feld | Inhalt |
|------|--------|
| **Bezeichnung** | Kanban-Board pro Raum |
| **Zweck** | Aufgabenverteilung und -tracking im schulischen Kontext |
| **Rechtsgrundlage** | Art. 6 Abs. 1 lit. b DSGVO |
| **Betroffene Personen** | Nutzer denen Aufgaben zugewiesen sind |
| **Datenkategorien** | Aufgabenbezeichnung, Beschreibung, Zuweisung (Nutzer-ID), Checklisten, Fälligkeitsdatum |
| **Empfänger** | Raummitglieder |
| **Löschfrist** | Gemeinsam mit Raum oder auf Anfrage |
| **Technische Maßnahmen** | Zugriff nur für Raummitglieder |

---

## 13. Push-Benachrichtigungen

| Feld | Inhalt |
|------|--------|
| **Bezeichnung** | Web-Push-Benachrichtigungen (VAPID) |
| **Zweck** | Zeitnahe Benachrichtigung über neue Nachrichten, Ereignisse |
| **Rechtsgrundlage** | Art. 6 Abs. 1 lit. a DSGVO (Einwilligung via Browser-API) |
| **Betroffene Personen** | Nutzer mit aktivierten Push-Benachrichtigungen |
| **Datenkategorien** | Browser-Endpunkt-URL (Push-Subscription), VAPID-Keys, Nutzer-ID |
| **Empfänger** | Browser-Anbieter (indirekt via Push-Service) |
| **Drittlandübermittlung** | Abhängig vom Browser-Push-Service (Google FCM, Mozilla, Apple) |
| **Löschfrist** | Sofort bei Deaktivierung durch Nutzer |
| **Technische Maßnahmen** | Opt-in; keine Nachrichteninhalte in Push-Payload; VAPID-Verschlüsselung |

---

## 14. Authentifizierung und Sicherheit

| Feld | Inhalt |
|------|--------|
| **Bezeichnung** | Login, JWT-Token-Verwaltung, 2FA, Passwort-Reset |
| **Zweck** | Sicherstellung der Systemzugangssteuerung |
| **Rechtsgrundlage** | Art. 6 Abs. 1 lit. b und f DSGVO |
| **Betroffene Personen** | Alle angemeldeten Nutzer |
| **Datenkategorien** | Login-Zeitstempel, IP-Adresse (nginx-Log), JWT-Token (nicht persistent), 2FA-Secret, Recovery-Codes, Passwort-Reset-Token |
| **Empfänger** | Nur technisches System |
| **Löschfrist** | JWT: 15 min (Access) / 7 Tage (Refresh); Reset-Token: 1h; nginx-Logs: gemäß Konfiguration |
| **Technische Maßnahmen** | bcrypt-Passwort-Hashing, Rate-Limiting auf Auth-Endpunkten, HTTPS, TOTP-2FA |

---

## 15. Fehlermeldungen und Error-Reporting

| Feld | Inhalt |
|------|--------|
| **Bezeichnung** | Automatisches Frontend-Fehler-Reporting an Admins |
| **Zweck** | Systemstabilität und Fehlerbehebung |
| **Rechtsgrundlage** | Art. 6 Abs. 1 lit. f DSGVO (berechtigtes Interesse: Systemsicherheit) |
| **Betroffene Personen** | Nutzer die Fehler auslösen (ggf. Nutzer-ID) |
| **Datenkategorien** | Fehlermeldung, Stack-Trace, URL-Pfad (ohne Query-Parameter), User-Agent, Nutzer-ID (optional), Fingerprint |
| **Empfänger** | Schuladministration; optional GitHub (bei Issue-Erstellung: kein Personenbezug übermittelt) |
| **Löschfrist** | Empfohlen: 90 Tage für RESOLVED/IGNORED (ausstehend – DSGVO-M-03) |
| **Technische Maßnahmen** | Fingerprint-basierte Deduplizierung; Query-Parameter werden vor Speicherung entfernt; GitHub-Issues ohne PII |

---

## 16. Audit-Log und Datenzugriff-Protokoll

| Feld | Inhalt |
|------|--------|
| **Bezeichnung** | Administrativer Audit-Trail und DSGVO-Datenzugriff-Log |
| **Zweck** | Nachweisbarkeit administrativer Aktionen; Art. 15-Auskunft |
| **Rechtsgrundlage** | Art. 6 Abs. 1 lit. c DSGVO (rechtliche Verpflichtung zur Nachweisführung) |
| **Betroffene Personen** | Nutzer deren Daten abgerufen/geändert werden; Admins die Aktionen durchführen |
| **Datenkategorien** | Aktion, Zeitstempel, handelnde Nutzer-ID, Ziel-Nutzer-ID, Kontext-Beschreibung |
| **Empfänger** | Datenschutzbeauftragter, betroffene Person auf Anfrage (Art. 15) |
| **Löschfrist** | 3 Jahre (konfigurierbar via `data_retention_days_audit`, Standard: 1095 Tage) |
| **Technische Maßnahmen** | Nur für SUPERADMIN einsehbar; unveränderliche Protokollierung |

---

## Technische und organisatorische Maßnahmen (TOMs) – Überblick

| Maßnahme | Implementierung |
|----------|----------------|
| Pseudonymisierung | UUID als Primärschlüssel statt E-Mail; Löschung anonymisiert Nutzer |
| Verschlüsselung (Transit) | TLS via Caddy (Auto-SSL) |
| Verschlüsselung (Ruhe) | PostgreSQL: Dateisystem-Ebene empfohlen; MinIO: Disk-Encryption empfohlen |
| Zugriffskontrolle | Rollenbasiert (SUPERADMIN, TEACHER, PARENT, STUDENT, SECTION_ADMIN), JWT |
| Authentifizierung | Passwort-Hash (bcrypt), 2FA (TOTP), optionaler OIDC/SSO |
| Protokollierung | Audit-Log, DataAccessLog, Spring Boot Logs |
| Datensparsamkeit | Pflichtfelder minimiert; optionale Felder explizit markiert; URL-Sanitisierung |
| Löschkonzept | Geplante Deletion nach 14 Tagen Abklingfrist; Anonymisierung |
| Datensicherung | Optional: Backup-Profil (daily pg_dump + MinIO mirror) |
| Zugriffsbeschränkung | Docker-Netzwerke isoliert; Ports nur intern erreichbar; nginx blockiert Actuator |

---

*Ende der VVT-Vorlage. Diese Datei ist als lebendes Dokument zu verstehen und muss bei Systemänderungen aktualisiert werden.*
