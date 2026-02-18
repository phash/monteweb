# MonteWeb Cheat-Sheet: Superadmin (SUPERADMIN)

**Version 1.0 | Februar 2026**

---

## Wichtigste Funktionen

| Funktion | Wo zu finden | Beschreibung |
|----------|-------------|-------------|
| Benutzerverwaltung | Verwaltung > Benutzer | Nutzer, Rollen, Status |
| Raumverwaltung | Verwaltung > Raeume | Erstellen, bearbeiten, loeschen |
| Schulbereiche | Verwaltung > Schulbereiche | Bereiche anlegen/bearbeiten |
| Familien | Verwaltung > Familien | Verbuende, Stunden, Befreiung |
| Module | Verwaltung > Module | Funktionen an-/ausschalten |
| Putz-Orga | Verwaltung > Putz-Orga | Aktionen, QR-Codes, PutzOrgas |
| Stundenbericht | Verwaltung > Stundenbericht | Familien-Ampel, CSV/PDF |
| Jahresabrechnung | Verwaltung > Jahresabrechnung | Perioden, Abschluss |
| Design & Branding | Verwaltung > Design | Logo, Farben, Ferien, Stunden |
| Fehlermeldungen | Verwaltung > Fehlermeldungen | Automatische Fehlererfassung |

---

## Top 5 Aktionen

| # | Aktion | Schritte |
|---|--------|----------|
| 1 | **Nutzer freischalten** | Verwaltung > Benutzer > "Neue Benutzer" > "Freischalten" |
| 2 | **Rolle zuweisen** | Verwaltung > Benutzer > Nutzer klicken > Rollen waehlen > Speichern |
| 3 | **Modul umschalten** | Verwaltung > Module > Toggle an/aus > Speichern |
| 4 | **Putzaktion erstellen** | Verwaltung > Putz-Orga > "Neue Putzaktion" > Daten eingeben |
| 5 | **Schuljahr abschliessen** | Verwaltung > Jahresabrechnung > "Abrechnung abschliessen" |

---

## Typische Workflows

### Ersteinrichtung
```
1. Schulbereiche anlegen (Kinderhaus, GS, MS, OS)
2. Design: Logo + Farben + Schulname
3. Bundesland + Schulferien eintragen
4. Module aktivieren/deaktivieren
5. Stundenregelung festlegen
6. Raeume erstellen + Bereichen zuordnen
7. Nutzer anlegen/freischalten
8. Section Admins zuweisen
9. Putzaktionen konfigurieren
10. Jahresabrechnung starten
```

### Neues Schuljahr
```
Jahresabrechnung abschliessen > Schulferien aktualisieren
> Neue Raeume erstellen > Nutzer/Familien pruefen
> Putzaktionen neu generieren
```

### Nutzer mit Multi-Rollen
```
Verwaltung > Benutzer > Nutzer bearbeiten
> Zugewiesene Rollen: TEACHER + PARENT waehlen > Speichern
```

### Section Admin einrichten
```
Verwaltung > Benutzer > Nutzer bearbeiten
> Rolle SECTION_ADMIN hinzufuegen > Zustaendige Bereiche waehlen > Speichern
```

---

## Modul-Uebersicht

| Modul | Key | Standard |
|-------|-----|:--------:|
| Nachrichten | messaging | Aktiv |
| Dateiablage | files | Aktiv |
| Jobbboerse | jobboard | Aktiv |
| Putz-Orga | cleaning | Aktiv |
| Kalender | calendar | Aktiv |
| Formulare | forms | Aktiv |
| Fotobox | fotobox | Aktiv |
| Fundgrube | fundgrube | Aktiv |

---

## Stundenverwaltung

| Einstellung | Ort | Beschreibung |
|-------------|-----|-------------|
| Gesamtstunden/Jahr | Design & Branding | Jahresziel pro Familie |
| Putzstunden-Anteil | Design & Branding | Davon Putzstunden |
| Familien-Befreiung | Verwaltung > Familien | Toggle pro Familie |
| Stundenbericht | Verwaltung > Stundenbericht | Ampel + CSV/PDF |
| Jahresabrechnung | Verwaltung > Jahresabrechnung | Perioden verwalten |

**Ampel-System im Bericht:**
- Gruen = Auf Kurs oder Ziel erreicht
- Gelb = Nachholbedarf
- Rot = Deutlich hinter dem Ziel

---

## Berechtigungsuebersicht

| Berechtigung | Status |
|-------------|:------:|
| Alle TEACHER-Rechte | JA |
| Alle SECTION_ADMIN-Rechte | JA |
| Nutzer anlegen/bearbeiten/loeschen | JA |
| Alle Rollen zuweisen | JA |
| Schulbereiche verwalten | JA |
| Familien verwalten + befreien | JA |
| Module aktivieren/deaktivieren | JA |
| Design/Theme/Logo | JA |
| Bundesland + Schulferien | JA |
| Kommunikationsregeln | JA |
| Stundenregelung | JA |
| Jahresabrechnung | JA |
| Putz-Konfiguration + QR-Codes | JA |
| Fehlermeldungen + GitHub Issues | JA |
| Audit-Log | JA |
| Stundenbericht (CSV/PDF) | JA |
| Schulweite Termine | JA |

---

## Kommunikationsregeln

| Regel | Standard | Aenderbar |
|-------|:--------:|:---------:|
| Lehrer ↔ Eltern | Erlaubt | Nein |
| Lehrer ↔ Schueler | Erlaubt | Ja |
| Eltern ↔ Eltern | Gesperrt | Ja |
| Schueler ↔ Schueler | Gesperrt | Ja |

Ort: Verwaltung > Design & Branding

---

## Sicherheit

| Massnahme | Details |
|-----------|---------|
| JWT | 15min Access + 7d Refresh |
| Rate-Limiting | Auth-Endpunkte geschuetzt |
| Passwoerter | BCrypt, mind. 8 Zeichen |
| Audit-Log | Alle Admin-Aktionen protokolliert |
| DSGVO | Datenexport, Konto-Loeschung, Anonymisierung |

---

## Wichtig zu wissen

- Sie haben **Vollzugriff** auf alle Funktionen
- **Minimalprinzip**: Nur so viele SUPERADMINs wie noetig
- **Delegieren**: Section Admins fuer Bereiche, PutzOrga fuer Putzplaene
- **Audit-Log** regelmaessig pruefen
- **Schulferien** jaehrlich aktualisieren
- Bei **Modul-Deaktivierung**: Daten bleiben erhalten

---

*MonteWeb Cheat-Sheet Superadmin v1.0*
