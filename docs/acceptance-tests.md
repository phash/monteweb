# MonteWeb — Akzeptanztest-Suite

**Erstellt:** 2026-03-02
**Scope:** Alle 20+ Module, alle 5 Rollen (SUPERADMIN, SECTION_ADMIN, TEACHER, PARENT, STUDENT)
**User Stories:** 296 | **Module:** 23

**Testkonten:**
| Rolle | E-Mail | Passwort |
|-------|--------|----------|
| SUPERADMIN | admin@monteweb.local | admin123 |
| SECTION_ADMIN | sectionadmin@monteweb.local | test1234 |
| TEACHER | lehrer@monteweb.local | test1234 |
| PARENT | eltern@monteweb.local | test1234 |
| STUDENT | schueler@monteweb.local | test1234 |

---



**Module:** Auth, Profil & Einstellungen, Dashboard & Navigation, Raeume, Feed
**Rollen:** SUPERADMIN (SA), SECTION_ADMIN (SECADMIN), TEACHER (T), PARENT (P), STUDENT (S)
**Testkonten:**

| Rolle | E-Mail | Passwort |
|-------|--------|----------|
| SA | admin@monteweb.local | admin123 |
| SECADMIN | sectionadmin@monteweb.local | test1234 |
| T | lehrer@monteweb.local | test1234 |
| P | eltern@monteweb.local | test1234 |
| S | schueler@monteweb.local | test1234 |

---

## Modul: Auth

### US-001: Login mit gueltigen Zugangsdaten
**Als** Benutzer (alle Rollen) **moechte ich** mich mit E-Mail und Passwort anmelden, **damit** ich auf das Intranet zugreifen kann.

**Vorbedingungen:** Benutzer ist nicht eingeloggt; System ist erreichbar unter http://localhost.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | http://localhost im Browser oeffnen | LoginView wird angezeigt mit Titel "MonteWeb" und Untertitel "Anmelden" |
| 2 | Feld "E-Mail" mit `lehrer@monteweb.local` fuellen | Eingabe wird angenommen |
| 3 | Feld "Passwort" mit `test1234` fuellen | Eingabe wird maskiert angezeigt |
| 4 | Button "Anmelden" klicken | Weiterleitung zum Dashboard; Begruessung "Willkommen, [Vorname]!" wird angezeigt |

**Akzeptanzkriterien:**
- [ ] Login funktioniert fuer alle 5 Testkonten (SA, SECADMIN, T, P, S)
- [ ] Nach erfolgreichem Login wird das Dashboard angezeigt
- [ ] JWT-Token wird gesetzt (kein Token in sessionStorage sichtbar)

---

### US-002: Login mit falschen Zugangsdaten
**Als** Benutzer **moechte ich** eine verstaendliche Fehlermeldung erhalten, **damit** ich weiss, dass meine Zugangsdaten falsch sind.

**Vorbedingungen:** Benutzer ist nicht eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Login-Seite oeffnen | LoginView wird angezeigt |
| 2 | E-Mail `lehrer@monteweb.local` und Passwort `falschesPasswort` eingeben | Felder werden befuellt |
| 3 | Button "Anmelden" klicken | Fehlermeldung: "Anmeldung fehlgeschlagen. Bitte ueberpruefen Sie Ihre Zugangsdaten." |
| 4 | E-Mail `nichtvorhanden@monteweb.local` und Passwort `test1234` eingeben | Felder werden befuellt |
| 5 | Button "Anmelden" klicken | Gleiche Fehlermeldung erscheint (kein Hinweis ob E-Mail existiert) |

**Akzeptanzkriterien:**
- [ ] Fehlermeldung gibt keinen Hinweis, ob die E-Mail existiert
- [ ] Benutzer bleibt auf der Login-Seite
- [ ] Formularfelder bleiben befuellt

---

### US-003: Login mit leerem Formular
**Als** Benutzer **moechte ich** auf Pflichtfelder hingewiesen werden, **damit** ich das Formular korrekt ausfuellen kann.

**Vorbedingungen:** Benutzer ist nicht eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Login-Seite oeffnen | LoginView wird angezeigt |
| 2 | Ohne Eingabe Button "Anmelden" klicken | Browser-Validierung verhindert Absenden; Pflichtfeld-Hinweis bei E-Mail |
| 3 | Nur E-Mail eingeben, Passwort leer lassen | Browser-Validierung verhindert Absenden; Pflichtfeld-Hinweis bei Passwort |

**Akzeptanzkriterien:**
- [ ] HTML5-Validierung greift bei leeren Pflichtfeldern
- [ ] Kein API-Request wird gesendet

---

### US-004: Registrierung mit gueltigen Daten
**Als** neuer Benutzer **moechte ich** mich registrieren koennen, **damit** ich Zugang zum Schulintranet erhalte.

**Vorbedingungen:** Benutzer ist nicht eingeloggt; Admin-Einstellung "Neue Benutzer muessen freigeschaltet werden" ist aktiv.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Login-Seite oeffnen | LoginView wird angezeigt |
| 2 | Link "Noch kein Konto? Registrieren" klicken | Registrierungsformular wird angezeigt mit Feldern: Vorname, Nachname, E-Mail, Passwort, Telefon |
| 3 | Vorname "Test", Nachname "Benutzer", E-Mail "test.neu@monteweb.local", Passwort "Sicher123!" eingeben | Felder werden befuellt; Passwort-Staerke-Anzeige erscheint |
| 4 | Checkbox "Ich akzeptiere die Nutzungsbedingungen" anklicken | Checkbox wird aktiviert |
| 5 | Button "Registrieren" klicken | Erfolgsmeldung: "Registrierung erfolgreich! Ihr Konto muss zunaechst von einem Administrator freigeschaltet werden." |
| 6 | Link "Zurueck zur Anmeldung" klicken | Login-Formular wird wieder angezeigt |

**Akzeptanzkriterien:**
- [ ] Alle Pflichtfelder (Vorname, Nachname, E-Mail, Passwort) sind mit Stern markiert
- [ ] Telefon ist optional
- [ ] Nutzungsbedingungen-Link oeffnet /terms in neuem Tab
- [ ] Nach Registrierung erscheint Freischaltungs-Hinweis

---

### US-005: Registrierung ohne Nutzungsbedingungen
**Als** neuer Benutzer **moechte ich** darauf hingewiesen werden, dass ich die Nutzungsbedingungen akzeptieren muss.

**Vorbedingungen:** Benutzer ist nicht eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Zum Registrierungsformular wechseln | Formular wird angezeigt |
| 2 | Alle Pflichtfelder ausfuellen, Checkbox "Ich akzeptiere die Nutzungsbedingungen" NICHT anklicken | Felder befuellt, Checkbox leer |
| 3 | Button "Registrieren" klicken | Fehlermeldung: "Sie muessen den Nutzungsbedingungen zustimmen." |

**Akzeptanzkriterien:**
- [ ] Registrierung wird clientseitig blockiert
- [ ] Kein API-Request wird gesendet
- [ ] Fehlermeldung wird in rot angezeigt

---

### US-006: Registrierung mit bereits vorhandener E-Mail
**Als** neuer Benutzer **moechte ich** informiert werden, wenn meine E-Mail bereits registriert ist.

**Vorbedingungen:** Benutzer ist nicht eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Registrierungsformular oeffnen | Formular wird angezeigt |
| 2 | E-Mail `lehrer@monteweb.local` eingeben, restliche Felder ausfuellen, Nutzungsbedingungen akzeptieren | Felder befuellt |
| 3 | Button "Registrieren" klicken | Fehlermeldung: "Registrierung fehlgeschlagen." oder spezifische Fehlermeldung vom Backend |

**Akzeptanzkriterien:**
- [ ] System verhindert Doppelregistrierung
- [ ] Benutzer bleibt auf der Registrierungsseite

---

### US-007: Benutzer-Freischaltung durch Admin
**Als** SUPERADMIN **moechte ich** neue Benutzer freischalten, **damit** nur berechtigte Personen Zugang erhalten.

**Vorbedingungen:** Ein neuer Benutzer hat sich registriert und wartet auf Freischaltung.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als SA einloggen (admin@monteweb.local / admin123) | Dashboard wird angezeigt |
| 2 | Navigation "Verwaltung" klicken | Admin-Dashboard wird angezeigt |
| 3 | Karte "Benutzer" klicken | Benutzerliste wird angezeigt |
| 4 | Tab/Filter "Neue Benutzer" waehlen | Liste der wartenden Benutzer wird angezeigt |
| 5 | Button "Freischalten" beim neuen Benutzer klicken | Toast: "Benutzer freigeschaltet" |
| 6 | Abmelden, als neuer Benutzer einloggen | Login erfolgreich, Dashboard wird angezeigt |

**Akzeptanzkriterien:**
- [ ] Nur SA kann Benutzer freischalten
- [ ] Freigeschalteter Benutzer kann sich sofort einloggen
- [ ] Toast-Benachrichtigung bestaetigt Freischaltung

---

### US-008: Login eines nicht freigeschalteten Benutzers
**Als** nicht freigeschalteter Benutzer **moechte ich** einen klaren Hinweis erhalten, **damit** ich weiss, dass mein Konto noch nicht aktiv ist.

**Vorbedingungen:** Benutzer hat sich registriert, wurde aber noch nicht freigeschaltet.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Login-Seite oeffnen | LoginView wird angezeigt |
| 2 | Mit den Zugangsdaten des nicht freigeschalteten Benutzers einloggen | Fehlermeldung: "Ihr Konto wartet auf Freischaltung durch einen Administrator." |

**Akzeptanzkriterien:**
- [ ] Benutzer kann sich nicht einloggen
- [ ] Fehlermeldung ist verstaendlich und hilfreich
- [ ] Benutzer bleibt auf der Login-Seite

---

### US-009: Passwort-Zuruecksetzen anfordern
**Als** Benutzer **moechte ich** mein Passwort zuruecksetzen koennen, **damit** ich wieder Zugang zu meinem Konto erhalte.

**Vorbedingungen:** Benutzer ist nicht eingeloggt; E-Mail-Versand ist konfiguriert (`monteweb.email.enabled`).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Login-Seite oeffnen | LoginView wird angezeigt |
| 2 | Link "Passwort vergessen?" klicken | Passwort-Reset-Formular oder Dialog wird angezeigt |
| 3 | E-Mail `lehrer@monteweb.local` eingeben | Feld wird befuellt |
| 4 | Absenden-Button klicken | Bestaetigung: "Falls die E-Mail existiert, wurde ein Reset-Link gesendet" (neutrale Formulierung) |
| 5 | Gleichen Vorgang mit nicht existierender E-Mail wiederholen | Gleiche neutrale Bestaetigung (kein Hinweis ob E-Mail existiert) |

**Akzeptanzkriterien:**
- [ ] API-Endpunkt POST /api/v1/auth/password-reset wird aufgerufen
- [ ] Antwort verraet nicht, ob die E-Mail existiert
- [ ] Bei konfiguriertem E-Mail-Dienst wird eine Reset-Mail verschickt

---

### US-010: Passwort-Zuruecksetzen mit Token
**Als** Benutzer **moechte ich** ueber einen Reset-Link ein neues Passwort setzen koennen.

**Vorbedingungen:** Gueltigem Reset-Token wurde generiert (API: POST /api/v1/auth/password-reset).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | POST /api/v1/auth/password-reset/confirm mit gueltigem Token und neuem Passwort aufrufen | Antwort: "Password has been reset" |
| 2 | Mit dem neuen Passwort einloggen | Login erfolgreich |
| 3 | POST /api/v1/auth/password-reset/confirm mit abgelaufenem/ungueltigem Token aufrufen | Fehler 400/404 |

**Akzeptanzkriterien:**
- [ ] Neues Passwort wird gesetzt
- [ ] Token ist danach ungueltig (einmalige Verwendung)
- [ ] Abgelaufene Tokens werden per Scheduler bereinigt (taeglich um 03:00)

---

### US-011: 2FA (TOTP) aktivieren
**Als** Benutzer **moechte ich** Zwei-Faktor-Authentifizierung einrichten, **damit** mein Konto sicherer ist.

**Vorbedingungen:** Benutzer ist eingeloggt; 2FA-Modus ist OPTIONAL oder MANDATORY.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als T einloggen (lehrer@monteweb.local / test1234) | Dashboard wird angezeigt |
| 2 | Profil-Seite oeffnen (Avatar klicken > "Profil") | Profilseite mit Abschnitt "Zwei-Faktor-Authentifizierung" |
| 3 | Button "2FA aktivieren" klicken | Dialog oeffnet sich mit QR-Code und Secret-Key |
| 4 | QR-Code scannen oder Secret manuell in Authenticator-App eingeben | 6-stelliger TOTP-Code wird generiert |
| 5 | TOTP-Code in Feld "Code eingeben" eintragen | Feld wird befuellt |
| 6 | Button "Bestaetigen" klicken | Wiederherstellungscodes werden angezeigt; Hinweis: "Speichern Sie diese Codes sicher. Jeder Code kann nur einmal verwendet werden." |
| 7 | Button "Ich habe die Codes gespeichert" klicken | Dialog schliesst sich; Status zeigt "2FA aktiv" |

**Akzeptanzkriterien:**
- [ ] QR-Code ist scannbar und enthaelt korrektes TOTP-URI
- [ ] Recovery-Codes werden einmalig angezeigt
- [ ] Nach Aktivierung wird bei jedem Login 2FA abgefragt
- [ ] Status "2FA aktiv" wird im Profil angezeigt

---

### US-012: Login mit aktiviertem 2FA
**Als** Benutzer mit aktiviertem 2FA **moechte ich** nach Eingabe meines Passworts einen TOTP-Code eingeben, **damit** der Zugang doppelt gesichert ist.

**Vorbedingungen:** 2FA ist fuer den Benutzer aktiviert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Login-Seite oeffnen | LoginView wird angezeigt |
| 2 | E-Mail und Passwort eingeben, "Anmelden" klicken | Anzeige wechselt zu "2FA-Code erforderlich" mit Eingabefeld |
| 3 | Gueltigen 6-stelligen TOTP-Code eingeben | Feld akzeptiert Eingabe |
| 4 | Button "Bestaetigen" klicken | Login erfolgreich, Weiterleitung zum Dashboard |

**Akzeptanzkriterien:**
- [ ] Nach korrektem Passwort wird 2FA-Schritt angezeigt
- [ ] Link "Zurueck" fuehrt zum Login-Formular
- [ ] Eingabefeld akzeptiert maximal 8 Zeichen (auch Recovery-Codes)

---

### US-013: Login mit falschem 2FA-Code
**Als** Benutzer mit aktiviertem 2FA **moechte ich** bei falschem Code informiert werden.

**Vorbedingungen:** 2FA ist fuer den Benutzer aktiviert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Login mit korrektem Passwort durchfuehren | 2FA-Code-Eingabe wird angezeigt |
| 2 | Falschen Code "000000" eingeben | Fehlermeldung: "Ungueltiger Code" |
| 3 | Erneut falschen Code eingeben | Gleiche Fehlermeldung |

**Akzeptanzkriterien:**
- [ ] Fehlermeldung wird angezeigt
- [ ] Benutzer kann erneut versuchen
- [ ] Kein Login ohne gueltigen Code moeglich

---

### US-014: Login mit Recovery-Code
**Als** Benutzer **moechte ich** einen Wiederherstellungscode verwenden koennen, **damit** ich mich einloggen kann, wenn mein Authenticator nicht verfuegbar ist.

**Vorbedingungen:** 2FA ist aktiviert; Benutzer hat Recovery-Codes gespeichert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Login mit korrektem Passwort durchfuehren | 2FA-Code-Eingabe wird angezeigt |
| 2 | Recovery-Code (8 Zeichen) statt TOTP-Code eingeben | Feld akzeptiert Eingabe (Hinweis unter Feld vorhanden) |
| 3 | Button "Bestaetigen" klicken | Login erfolgreich, Weiterleitung zum Dashboard |
| 4 | Abmelden und gleichen Recovery-Code erneut verwenden | Fehlermeldung: "Ungueltiger Code" (Code wurde verbraucht) |

**Akzeptanzkriterien:**
- [ ] Recovery-Code funktioniert einmalig
- [ ] Hinweis unter dem Eingabefeld informiert ueber Recovery-Codes
- [ ] Jeder Recovery-Code kann nur einmal verwendet werden

---

### US-015: 2FA deaktivieren
**Als** Benutzer **moechte ich** 2FA deaktivieren koennen, **damit** ich den Login vereinfachen kann.

**Vorbedingungen:** 2FA ist aktiviert; 2FA-Modus ist nicht MANDATORY.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Profil-Seite oeffnen | Abschnitt "Zwei-Faktor-Authentifizierung" zeigt "2FA aktiv" |
| 2 | Button "2FA deaktivieren" klicken | Dialog mit Passwort-Abfrage: "Zum Deaktivieren Ihr Passwort eingeben" |
| 3 | Passwort eingeben und bestaetigen | Toast: "2FA wurde deaktiviert"; Status wechselt |
| 4 | Abmelden und erneut einloggen | Login erfolgt direkt ohne 2FA-Abfrage |

**Akzeptanzkriterien:**
- [ ] Deaktivierung erfordert Passworteingabe
- [ ] Bei MANDATORY-Modus ist Deaktivierung nicht moeglich
- [ ] Nach Deaktivierung kein 2FA-Schritt beim Login

---

### US-016: 2FA MANDATORY-Modus (Admin-Erzwingung)
**Als** SUPERADMIN **moechte ich** 2FA fuer alle Benutzer erzwingen, **damit** die Sicherheit des Systems gewaehrleistet ist.

**Vorbedingungen:** SA ist eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als SA: Verwaltung > Einstellungen > Sicherheit oeffnen | 2FA-Konfiguration wird angezeigt |
| 2 | "2FA-Modus" auf "Pflicht" setzen | Modus wird geaendert; Hinweis: "Bei Pflicht haben Nutzer 7 Tage Zeit zur Einrichtung" |
| 3 | Einstellungen speichern | Toast: "Einstellungen gespeichert" |
| 4 | Abmelden, als T (ohne 2FA) einloggen | Nach Passworteingabe: "2FA-Einrichtung erforderlich" mit Hinweis und Frist |
| 5 | Code eingeben (aus frisch eingerichtetem Authenticator) | Login erfolgreich oder Weiterleitung zur Profilseite fuer Einrichtung |

**Akzeptanzkriterien:**
- [ ] Grace Period von 7 Tagen wird angezeigt
- [ ] Benutzer ohne 2FA sieht Setup-Aufforderung beim Login
- [ ] Nach Ablauf der Frist ist Login ohne 2FA nicht mehr moeglich
- [ ] SA kann den Modus zwischen DISABLED, OPTIONAL und MANDATORY wechseln

---

### US-017: SSO/OIDC-Login
**Als** Benutzer **moechte ich** mich per SSO anmelden, **damit** ich meine bestehenden Schul-Zugangsdaten nutzen kann.

**Vorbedingungen:** OIDC ist konfiguriert (`monteweb.oidc.enabled=true`); OIDC-Provider ist erreichbar.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Login-Seite oeffnen | Unterhalb des Login-Formulars erscheint Trennlinie "oder" und Button "Mit SSO anmelden" |
| 2 | Button "Mit SSO anmelden" klicken | Weiterleitung zum konfigurierten OIDC-Provider |
| 3 | Am OIDC-Provider authentifizieren | Rueckleitung zu MonteWeb; Login erfolgreich, Dashboard wird angezeigt |

**Akzeptanzkriterien:**
- [ ] SSO-Button wird nur angezeigt wenn OIDC aktiviert ist
- [ ] Bei deaktiviertem OIDC ist kein SSO-Button sichtbar
- [ ] OIDC-Flow funktioniert mit Authorization Code

---

### US-018: SSO-Button bei deaktiviertem OIDC
**Als** Benutzer **moechte ich** keinen SSO-Button sehen, wenn SSO nicht konfiguriert ist.

**Vorbedingungen:** OIDC ist nicht konfiguriert (`monteweb.oidc.enabled=false` oder nicht gesetzt).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Login-Seite oeffnen | LoginView wird angezeigt |
| 2 | Pruefen ob SSO-Button sichtbar ist | Kein Button "Mit SSO anmelden" sichtbar; keine Trennlinie "oder" |

**Akzeptanzkriterien:**
- [ ] SSO-Button und Divider sind nicht sichtbar
- [ ] Login-Formular funktioniert normal

---

### US-019: LDAP-Authentifizierung
**Als** Benutzer **moechte ich** mich mit meinen LDAP/Active-Directory-Zugangsdaten anmelden.

**Vorbedingungen:** LDAP ist aktiviert (in `tenant_config.modules` JSONB); LDAP-Server ist erreichbar.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Login-Seite oeffnen | Normales Login-Formular |
| 2 | LDAP-Benutzername als E-Mail und LDAP-Passwort eingeben | Felder werden befuellt |
| 3 | Button "Anmelden" klicken | Login erfolgreich; bei erstem Login wird Benutzer automatisch erstellt |

**Akzeptanzkriterien:**
- [ ] LDAP-Benutzer wird bei erstem Login automatisch im System angelegt
- [ ] Standard-Rolle wird gemaess LDAP-Konfiguration zugewiesen
- [ ] Lokale Anmeldung bleibt als Fallback aktiv
- [ ] Bei LDAP-Server-Ausfall greift Fallback auf lokale Authentifizierung

---

### US-020: LDAP-Konfiguration durch Admin
**Als** SUPERADMIN **moechte ich** LDAP konfigurieren koennen, **damit** Benutzer sich ueber den Verzeichnisdienst anmelden koennen.

**Vorbedingungen:** SA ist eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Verwaltung > Einstellungen > Integrationen > "LDAP / Active Directory" oeffnen | LDAP-Konfigurationsformular wird angezeigt |
| 2 | Toggle "LDAP-Anmeldung aktivieren" einschalten | Formularfelder werden sichtbar |
| 3 | Server-URL (z.B. `ldap://ldap.example.com:389`), Base DN, Bind DN, Bind-Passwort eingeben | Felder werden befuellt |
| 4 | Benutzer-Suchfilter, Attribut-Zuordnungen (E-Mail, Vorname, Nachname) und Standard-Rolle konfigurieren | Felder befuellt |
| 5 | Button "Verbindung testen" klicken | Bei erreichbarem Server: "LDAP-Verbindung erfolgreich"; bei Fehler: "LDAP-Verbindung fehlgeschlagen" |
| 6 | "Speichern" klicken | Toast: "LDAP-Einstellungen gespeichert" |

**Akzeptanzkriterien:**
- [ ] Bind-Passwort wird verschluesselt gespeichert und nicht angezeigt ("Gespeichert (wird nicht angezeigt)")
- [ ] SSL-Option ist konfigurierbar
- [ ] Verbindungstest gibt klare Rueckmeldung
- [ ] Standard-Rolle kann gewaehlt werden (TEACHER, PARENT, STUDENT)

---

### US-021: Terms of Service bei Registrierung
**Als** neuer Benutzer **moechte ich** die Nutzungsbedingungen vor der Registrierung einsehen koennen.

**Vorbedingungen:** Nutzungsbedingungen sind unter `tenant_config.terms_text` konfiguriert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Registrierungsformular oeffnen | Checkbox "Ich akzeptiere die Nutzungsbedingungen" mit Link sichtbar |
| 2 | Link "Nutzungsbedingungen" klicken | /terms oeffnet sich in neuem Tab mit dem konfigurierten Text |
| 3 | Tab schliessen, zurueck zum Registrierungsformular | Formular ist unveraendert |
| 4 | Checkbox anklicken und Formular absenden | Registrierung wird durchgefuehrt |

**Akzeptanzkriterien:**
- [ ] Link oeffnet in neuem Tab (`target="_blank"`)
- [ ] Nutzungsbedingungen-Text kommt aus der Datenbank
- [ ] Checkbox ist Pflicht fuer Registrierung

---

### US-022: Logout
**Als** eingeloggter Benutzer **moechte ich** mich abmelden koennen, **damit** mein Konto geschuetzt ist.

**Vorbedingungen:** Benutzer ist eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Navigationspunkt "Abmelden" klicken | Benutzer wird abgemeldet; Weiterleitung zur Login-Seite |
| 2 | Browser-Zurueck-Button druecken | Login-Seite bleibt; kein Zugriff auf geschuetzte Seiten |
| 3 | Direkt /dashboard im Browser aufrufen | Weiterleitung zur Login-Seite |

**Akzeptanzkriterien:**
- [ ] Session wird serverseitig invalidiert
- [ ] Kein Zugriff auf geschuetzte Seiten nach Logout
- [ ] Weiterleitung zur Login-Seite

---

### US-023: Automatische Weiterleitung nach Login
**Als** Benutzer **moechte ich** nach dem Login zur urspruenglich angefragten Seite weitergeleitet werden.

**Vorbedingungen:** Benutzer ist nicht eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | URL /rooms direkt aufrufen (nicht eingeloggt) | Weiterleitung zur Login-Seite mit ?redirect=/rooms |
| 2 | Einloggen | Nach erfolgreichem Login: Weiterleitung zu /rooms (nicht zum Dashboard) |

**Akzeptanzkriterien:**
- [ ] Redirect-Parameter wird sicher verarbeitet (kein Open Redirect)
- [ ] Nur relative Pfade werden akzeptiert (kein //, kein ://)
- [ ] Ohne Redirect-Parameter wird zum Dashboard weitergeleitet

---

## Modul: Profil & Einstellungen

### US-024: Profil anzeigen
**Als** eingeloggter Benutzer **moechte ich** mein Profil einsehen, **damit** ich meine Daten ueberpruefen kann.

**Vorbedingungen:** Benutzer ist eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Navigation: auf eigenen Avatar oder "Profil" klicken | Profilseite wird angezeigt mit Titel "Mein Profil" |
| 2 | Folgende Bereiche pruefen | Sichtbar: Profilbild/Avatar, Vorname, Nachname, Telefon, Rollen, Erscheinungsbild, Sprache |

**Akzeptanzkriterien:**
- [ ] Profilseite zeigt alle persoenlichen Informationen
- [ ] Aktive Rolle wird angezeigt
- [ ] Zugewiesene Rollen werden als Tags dargestellt

---

### US-025: Profil bearbeiten
**Als** eingeloggter Benutzer **moechte ich** meinen Vornamen, Nachnamen und Telefonnummer aendern.

**Vorbedingungen:** Benutzer ist eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Profilseite oeffnen | Formular mit Feldern Vorname, Nachname, Telefon |
| 2 | Vorname von "Lehrer" auf "Lehrkraft" aendern | Feld wird aktualisiert |
| 3 | Button "Speichern" klicken | Toast: "Profil gespeichert"; Name im Header aktualisiert |
| 4 | Seite neu laden | Geaenderter Name bleibt bestehen |

**Akzeptanzkriterien:**
- [ ] Aenderungen werden sofort wirksam
- [ ] Name im Navigationsheader aktualisiert sich
- [ ] Vorname und Nachname sind Pflichtfelder

---

### US-026: Avatar hochladen
**Als** eingeloggter Benutzer **moechte ich** ein Profilbild hochladen, **damit** ich visuell erkennbar bin.

**Vorbedingungen:** Benutzer ist eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Profilseite oeffnen | Avatar-Bereich wird angezeigt (Platzhalter oder vorhandenes Bild) |
| 2 | "Profilbild aendern" / Upload-Bereich klicken | Datei-Dialog oeffnet sich |
| 3 | Bilddatei (JPG/PNG) auswaehlen | Bild wird hochgeladen; Toast: "Avatar hochgeladen" |
| 4 | Bild wird im Avatar-Bereich und im Header angezeigt | Neues Profilbild ist sichtbar |

**Akzeptanzkriterien:**
- [ ] Upload akzeptiert gaengige Bildformate (JPEG, PNG)
- [ ] Profilbild wird in Header-Navigation aktualisiert
- [ ] Zu grosse Dateien werden abgelehnt

---

### US-027: Avatar entfernen
**Als** eingeloggter Benutzer **moechte ich** mein Profilbild entfernen koennen.

**Vorbedingungen:** Benutzer hat ein Profilbild hochgeladen.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Profilseite oeffnen | Vorhandenes Profilbild wird angezeigt |
| 2 | Button "Avatar entfernen" klicken | Toast: "Avatar entfernt"; Platzhalter-Avatar wird angezeigt |

**Akzeptanzkriterien:**
- [ ] Profilbild wird aus MinIO geloescht
- [ ] Platzhalter wird an allen Stellen angezeigt

---

### US-028: Dark Mode umschalten
**Als** eingeloggter Benutzer **moechte ich** zwischen hellem und dunklem Design wechseln.

**Vorbedingungen:** Benutzer ist eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Profilseite oeffnen | Abschnitt "Erscheinungsbild" mit Dropdown sichtbar |
| 2 | Dropdown oeffnen | Optionen: "Automatisch (System)", "Hell", "Dunkel" |
| 3 | "Dunkel" auswaehlen | Gesamte Seite wechselt sofort zu dunklem Design; Toast: "Erscheinungsbild gespeichert" |
| 4 | "Hell" auswaehlen | Seite wechselt zu hellem Design |
| 5 | "Automatisch (System)" auswaehlen | Design richtet sich nach Betriebssystem-Einstellung |
| 6 | Seite neu laden | Einstellung bleibt bestehen |

**Akzeptanzkriterien:**
- [ ] Drei Modi: SYSTEM, LIGHT, DARK
- [ ] Einstellung wird in `users.dark_mode` gespeichert
- [ ] CSS Custom Properties `--mw-*` wechseln korrekt
- [ ] Dark Mode ist auch auf der Login-Seite waehlbar

---

### US-029: Sprachauswahl
**Als** eingeloggter Benutzer **moechte ich** die Sprache der Oberflaeche wechseln.

**Vorbedingungen:** Benutzer ist eingeloggt; Mehrsprachigkeit ist aktiviert (`available_languages` enthaelt > 1 Sprache).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Profilseite oeffnen | Abschnitt "Sprache" mit LanguageSwitcher-Komponente sichtbar |
| 2 | Von "Deutsch" auf "English" wechseln | Alle UI-Texte wechseln auf Englisch (z.B. "Dashboard", "Rooms", "Profile") |
| 3 | Von "English" zurueck auf "Deutsch" wechseln | UI-Texte wechseln zurueck auf Deutsch |

**Akzeptanzkriterien:**
- [ ] Sprachwechsler ist nur sichtbar wenn mehr als eine Sprache aktiviert ist
- [ ] Sprachwechsler ist auch auf der Login-Seite verfuegbar
- [ ] Einstellung wird beibehalten nach Neuladen
- [ ] Alle UI-Elemente werden korrekt uebersetzt

---

### US-030: Sprachauswahl nicht sichtbar bei nur einer Sprache
**Als** Benutzer **moechte ich** keinen Sprachwechsler sehen, wenn nur eine Sprache konfiguriert ist.

**Vorbedingungen:** `available_languages` enthaelt nur `{de}`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Profilseite oeffnen | Kein Sprachwechsler sichtbar |
| 2 | Login-Seite oeffnen (ausgeloggt) | Kein Sprachwechsler sichtbar |

**Akzeptanzkriterien:**
- [ ] LanguageSwitcher-Komponente wird ausgeblendet
- [ ] System nutzt die Standardsprache

---

### US-031: Benutzerdefinierte Profilfelder anzeigen und ausfuellen
**Als** eingeloggter Benutzer **moechte ich** zusaetzliche Profilfelder ausfuellen, die der Admin definiert hat.

**Vorbedingungen:** Modul `profilefields` ist aktiviert; Admin hat Profilfelder definiert (z.B. "Geburtstag" als DATE, "Hobby" als TEXT).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Profilseite oeffnen | Abschnitt "Zusaetzliche Profilfelder" mit Untertitel "Benutzerdefinierte Felder fuer dein Profil" |
| 2 | Textfeld "Hobby" mit "Gartenarbeit" fuellen | Eingabe wird angenommen |
| 3 | Datumsfeld "Geburtstag" mit gueltigem Datum fuellen | DatePicker oeffnet sich |
| 4 | Button "Speichern" unter den Profilfeldern klicken | Toast: "Profilfelder gespeichert" |
| 5 | Seite neu laden | Gespeicherte Werte bleiben erhalten |

**Akzeptanzkriterien:**
- [ ] Verschiedene Feldtypen werden korrekt dargestellt (TEXT, DATE, SELECT, BOOLEAN)
- [ ] Pflichtfelder sind als solche gekennzeichnet
- [ ] Werte werden in `profile_field_values` gespeichert

---

### US-032: Profilfelder als Admin verwalten
**Als** SUPERADMIN **moechte ich** benutzerdefinierte Profilfelder erstellen und verwalten.

**Vorbedingungen:** SA ist eingeloggt; Modul `profilefields` ist aktiviert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Verwaltung > "Profilfelder" klicken | Seite "Profilfelder verwalten" mit Untertitel "Benutzerdefinierte Felder fuer Benutzerprofile erstellen und verwalten" |
| 2 | Button "Neues Feld" klicken | Dialog oeffnet sich mit Feldern: Feldschluessel, Bezeichnung (DE), Bezeichnung (EN), Feldtyp, Pflichtfeld, Position, Aktiv |
| 3 | Feldschluessel "hobby", Bezeichnung DE "Hobby", Typ "Text" eingeben | Felder werden befuellt |
| 4 | Speichern | Toast: "Profilfeld erstellt"; Feld erscheint in der Liste |
| 5 | Feld bearbeiten: Typ auf "Auswahl" aendern, Optionen "Lesen, Sport, Musik" eingeben | Optionen-Feld wird sichtbar |
| 6 | Speichern | Toast: "Profilfeld aktualisiert" |
| 7 | Feld loeschen mit Bestaetigung | Toast: "Profilfeld geloescht"; Warnung: "Alle Benutzerdaten fuer dieses Feld werden ebenfalls geloescht." |

**Akzeptanzkriterien:**
- [ ] Feldschluessel akzeptiert nur Kleinbuchstaben, Zahlen und Unterstriche
- [ ] Vier Feldtypen: Text, Datum, Auswahl, Ja/Nein
- [ ] Optionen nur bei Typ "Auswahl" sichtbar (kommagetrennt)
- [ ] Position bestimmt Reihenfolge der Anzeige
- [ ] Loeschung entfernt auch alle zugehoerigen Werte

---

### US-033: Rollenwechsel im Profil
**Als** Benutzer mit mehreren Rollen **moechte ich** zwischen meinen Rollen wechseln koennen.

**Vorbedingungen:** Benutzer hat mehrere zugewiesene Rollen (z.B. TEACHER und PARENT).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Profilseite oeffnen | Abschnitt "Meine Rollen" zeigt alle zugewiesenen Rollen; "Aktive Rolle" ist markiert |
| 2 | Button "Rolle wechseln" bei einer anderen Rolle klicken | Toast: "Rolle gewechselt zu [Rollenname]" |
| 3 | Navigation pruefen | Menuepunkte passen sich der neuen Rolle an (z.B. Admin-Menue nur fuer SA) |

**Akzeptanzkriterien:**
- [ ] Rollenwechsel aktualisiert die Sichtbarkeit von Modulen und Inhalten
- [ ] Aktive Rolle wird visuell hervorgehoben
- [ ] Rollenwechsel erfordert keinen Neulogin

---

### US-034: Push-Benachrichtigungen aktivieren
**Als** eingeloggter Benutzer **moechte ich** Push-Benachrichtigungen aktivieren, **damit** ich ueber Neuigkeiten informiert werde.

**Vorbedingungen:** Benutzer ist eingeloggt; Push ist konfiguriert (`monteweb.push.enabled`); Browser unterstuetzt Push.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Profilseite oeffnen | Abschnitt "Push-Benachrichtigungen" mit Toggle "Push-Benachrichtigungen aktivieren" |
| 2 | Toggle einschalten | Browser fragt nach Berechtigung; bei Genehmigung wird Push aktiviert |
| 3 | Toggle ausschalten | Push-Benachrichtigungen werden deaktiviert |

**Akzeptanzkriterien:**
- [ ] Bei blockierter Browser-Berechtigung: Hinweis "Push-Benachrichtigungen wurden im Browser blockiert. Bitte erlauben Sie diese in den Browser-Einstellungen."
- [ ] Push funktioniert mit VAPID-Schluessel

---

### US-035: Stummgeschaltete Chats verwalten
**Als** eingeloggter Benutzer **moechte ich** meine stummgeschalteten Chats sehen und die Stummschaltung aufheben.

**Vorbedingungen:** Benutzer hat mindestens einen Chat stummgeschaltet.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Profilseite oeffnen | Abschnitt "Stummgeschaltete Chats" zeigt Liste der stummgeschalteten Konversationen |
| 2 | Button "Stummschaltung aufheben" bei einem Chat klicken | Chat wird aus der Liste entfernt; Toast: "Stummschaltung aufgehoben" |
| 3 | Wenn keine Chats stummgeschaltet sind | Hinweis: "Keine stummgeschalteten Chats" |

**Akzeptanzkriterien:**
- [ ] Alle stummgeschalteten Chats werden aufgelistet
- [ ] Stummschaltung kann einzeln aufgehoben werden
- [ ] Aktualisierung erfolgt sofort

---

### US-036: E-Mail-Zusammenfassung konfigurieren
**Als** eingeloggter Benutzer **moechte ich** die Haeufigkeit meiner E-Mail-Zusammenfassung einstellen.

**Vorbedingungen:** Benutzer ist eingeloggt; E-Mail ist konfiguriert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Profilseite oeffnen | Abschnitt "E-Mail-Zusammenfassung" mit Dropdown "Haeufigkeit" |
| 2 | Dropdown oeffnen | Optionen: Deaktiviert, Taeglich, Woechentlich, Alle 2 Wochen, Monatlich |
| 3 | "Woechentlich" auswaehlen | Toast: "E-Mail-Einstellung gespeichert" |

**Akzeptanzkriterien:**
- [ ] Fuenf Frequenz-Optionen verfuegbar
- [ ] Einstellung wird persistiert
- [ ] Hinweis erklaert den Zweck der Zusammenfassung

---

## Modul: Dashboard & Navigation

### US-037: Dashboard nach Login
**Als** eingeloggter Benutzer **moechte ich** auf dem Dashboard eine Uebersicht ueber relevante Inhalte sehen.

**Vorbedingungen:** Benutzer ist eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Nach Login wird Dashboard angezeigt | Begruessung "Willkommen, [Vorname]!" als Untertitel |
| 2 | System-Banner pruefen | Falls vorhanden: Banner fuer anstehende Putzaktionen oder andere Hinweise |
| 3 | Feed-Bereich pruefen | Beitraege aus Raeumen des Benutzers werden angezeigt |

**Akzeptanzkriterien:**
- [ ] Begruessung zeigt den Vornamen des Benutzers
- [ ] Feed zeigt nur Inhalte aus Raeumen, in denen der Benutzer Mitglied ist
- [ ] Banner werden kontextabhaengig angezeigt (Putz-Banner nur fuer betroffene Eltern)

---

### US-038: Dashboard-Widget offene Formulare
**Als** eingeloggter Benutzer **moechte ich** offene Formulare direkt auf dem Dashboard sehen.

**Vorbedingungen:** Modul `forms` ist aktiviert; es gibt veroeffentlichte Formulare fuer den Benutzer.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Dashboard oeffnen | Widget "Offene Formulare" zeigt unbeantwortete Formulare |
| 2 | Auf ein Formular im Widget klicken | Weiterleitung zur Formular-Detail-Seite |
| 3 | Alle Formulare beantwortet | Widget zeigt keine Eintraege oder wird ausgeblendet |

**Akzeptanzkriterien:**
- [ ] Widget wird nur angezeigt wenn Modul `forms` aktiviert ist
- [ ] Nur offene, noch nicht beantwortete Formulare werden angezeigt
- [ ] Link "Alle anzeigen" fuehrt zur Formulare-Uebersicht

---

### US-039: PostComposer auf Dashboard (nur T/SA)
**Als** Lehrkraft oder SUPERADMIN **moechte ich** direkt auf dem Dashboard Beitraege erstellen koennen.

**Vorbedingungen:** Benutzer ist als T oder SA eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Dashboard oeffnen als T | PostComposer mit Feldern "Titel (optional)" und "Was gibt es Neues?" ist sichtbar |
| 2 | Dashboard oeffnen als P | PostComposer ist NICHT sichtbar |
| 3 | Dashboard oeffnen als S | PostComposer ist NICHT sichtbar |

**Akzeptanzkriterien:**
- [ ] PostComposer nur fuer TEACHER und SUPERADMIN sichtbar
- [ ] PARENT und STUDENT sehen keinen PostComposer auf dem Dashboard

---

### US-040: Hauptnavigation -- Menuepunkte nach Rolle
**Als** eingeloggter Benutzer **moechte ich** nur die fuer meine Rolle relevanten Navigationspunkte sehen.

**Vorbedingungen:** Benutzer ist eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als SA einloggen | Navigation enthaelt: Dashboard, Raeume, Familie, Nachrichten, Kalender, Formulare, Entdecken, Verwaltung, Profil, Abmelden (+ optionale Module) |
| 2 | Als T einloggen | Navigation enthaelt: Dashboard, Raeume, Nachrichten, Kalender, Entdecken, Profil, Abmelden (kein "Verwaltung") |
| 3 | Als P einloggen | Navigation enthaelt: Dashboard, Raeume, Familie, Nachrichten, Kalender, Profil, Abmelden |
| 4 | Als S einloggen | Navigation enthaelt: Dashboard, Raeume, Nachrichten, Kalender, Profil, Abmelden (kein "Familie") |
| 5 | Als SECADMIN einloggen | Navigation enthaelt zusaetzlich "Bereichsverwaltung" |

**Akzeptanzkriterien:**
- [ ] "Verwaltung" nur fuer SA sichtbar
- [ ] "Bereichsverwaltung" nur fuer SECADMIN sichtbar
- [ ] "Familie" nicht fuer STUDENT sichtbar (da nicht selbst erstellbar)
- [ ] Optionale Module (Jobboerse, Putz-Orga, Fundgrube etc.) nur sichtbar wenn aktiviert

---

### US-041: Navigation -- optionale Module
**Als** eingeloggter Benutzer **moechte ich** nur Navigationspunkte fuer aktivierte Module sehen.

**Vorbedingungen:** Benutzer ist eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als SA: Verwaltung > Module oeffnen | Liste aller Module mit Toggle-Schaltern |
| 2 | Modul "Jobboerse" deaktivieren | Toggle aus; Toast: "Modul-Konfiguration gespeichert" |
| 3 | Navigation pruefen | Menuepunkt "Jobboerse" ist nicht mehr sichtbar |
| 4 | Modul "Jobboerse" wieder aktivieren | Menuepunkt "Jobboerse" erscheint wieder |

**Akzeptanzkriterien:**
- [ ] Deaktivierte Module verschwinden aus der Navigation fuer ALLE Rollen
- [ ] Backend-Endpunkte deaktivierter Module geben 404 zurueck
- [ ] Module: messaging, files, jobboard, cleaning, calendar, forms, fotobox, fundgrube, bookmarks, tasks, wiki, profilefields

---

### US-042: Navigation -- Benachrichtigungsglocke
**Als** eingeloggter Benutzer **moechte ich** ungelesene Benachrichtigungen in der Navigation sehen.

**Vorbedingungen:** Benutzer ist eingeloggt; es gibt ungelesene Benachrichtigungen.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Navigation pruefen | Glocken-Icon mit Badge-Zaehler fuer ungelesene Benachrichtigungen |
| 2 | Glocke klicken | Dropdown/Overlay mit Benachrichtigungsliste |
| 3 | "Alle gelesen" klicken | Zaehler verschwindet; alle Benachrichtigungen als gelesen markiert |

**Akzeptanzkriterien:**
- [ ] Badge zeigt korrekte Anzahl ungelesener Benachrichtigungen
- [ ] Benachrichtigungen zeigen Zeitangaben (Gerade eben, vor Xm, vor Xh, vor Xd)
- [ ] "Alle gelesen" markiert alle als gelesen
- [ ] Bei keinen Benachrichtigungen: "Keine Benachrichtigungen"

---

### US-043: 404-Seite
**Als** Benutzer **moechte ich** bei einer ungueltige URL eine hilfreiche Fehlerseite sehen.

**Vorbedingungen:** Benutzer ist eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | URL /nichtexistent im Browser aufrufen | 404-Seite: "404 - Seite nicht gefunden" mit Meldung "Die angeforderte Seite existiert nicht oder wurde verschoben." |
| 2 | Button "Zurueck zum Dashboard" klicken | Weiterleitung zum Dashboard |

**Akzeptanzkriterien:**
- [ ] 404-Seite zeigt klare Fehlermeldung auf Deutsch
- [ ] Link zurueck zum Dashboard funktioniert
- [ ] Catch-All Route im Router faengt alle ungultigen Pfade ab

---

### US-044: Wartungsmodus
**Als** SUPERADMIN **moechte ich** das System in den Wartungsmodus versetzen koennen, **damit** ich Wartungsarbeiten durchfuehren kann.

**Vorbedingungen:** SA ist eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Verwaltung > Module > "Wartungsmodus" aktivieren | Toggle einschalten |
| 2 | Optional: Wartungsnachricht eingeben (z.B. "Systemwartung bis ca. 18:00 Uhr") | Feld wird befuellt |
| 3 | Speichern | Toast: "Modul-Konfiguration gespeichert" |
| 4 | Als T einloggen (in anderem Browser) | Wartungsseite wird angezeigt: "Wartungsarbeiten" mit konfigurierter Nachricht |
| 5 | Link "Als Admin anmelden" auf Wartungsseite klicken | Login-Seite wird angezeigt |
| 6 | Als SA einloggen | Normaler Zugang (Wartungsmodus hat keinen Effekt fuer Admins) |
| 7 | Wartungsmodus deaktivieren | System ist wieder fuer alle zugaenglich |

**Akzeptanzkriterien:**
- [ ] Nur SA kann auf das System zugreifen waehrend Wartungsmodus aktiv
- [ ] Alle anderen Rollen sehen Wartungsseite
- [ ] Wartungsnachricht wird angezeigt (Fallback: "Das System wird gerade gewartet. Bitte versuchen Sie es spaeter erneut.")
- [ ] Link "Als Admin anmelden" ist auf Wartungsseite verfuegbar

---

### US-045: Globale Suche (Ctrl+K)
**Als** eingeloggter Benutzer **moechte ich** ueber eine globale Suche schnell Inhalte finden.

**Vorbedingungen:** Benutzer ist eingeloggt; Solr ist konfiguriert (optional, Fallback auf DB-Suche).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Tastenkombination Ctrl+K druecken | Such-Dialog oeffnet sich |
| 2 | Suchbegriff eingeben (z.B. "Sonnengruppe") | Suchergebnisse werden angezeigt, gruppiert nach Typ (Benutzer, Raeume, Beitraege, Termine, Dateien, Wiki, Aufgaben) |
| 3 | Auf ein Ergebnis klicken | Navigation zum entsprechenden Inhalt |

**Akzeptanzkriterien:**
- [ ] Suche durchsucht alle relevanten Dokumenttypen
- [ ] Ergebnisse sind nach Relevanz sortiert
- [ ] Bei deaktiviertem Solr greift Fallback auf DB-Suche
- [ ] Suchfeld ist im Dialog fokussiert

---

### US-046: PWA-Installation
**Als** Benutzer **moechte ich** MonteWeb als App auf meinem Geraet installieren koennen.

**Vorbedingungen:** Browser unterstuetzt PWA; Benutzer hat die Installation nicht innerhalb der letzten 7 Tage abgelehnt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | MonteWeb im Browser oeffnen | Nach kurzer Zeit erscheint Install-Banner: "MonteWeb installieren" mit Text "App auf dem Startbildschirm hinzufuegen fuer schnelleren Zugriff" |
| 2 | Button "Installieren" klicken | Browser-Install-Dialog erscheint; bei Bestaetigung wird App installiert |
| 3 | Install-Banner schliessen | Banner verschwindet; wird fuer 7 Tage nicht mehr angezeigt |

**Akzeptanzkriterien:**
- [ ] Install-Banner hat 7-Tage Dismiss-Delay
- [ ] Service Worker mit NetworkFirst-Caching fuer API-Calls
- [ ] Icons in verschiedenen Groessen vorhanden

---

## Modul: Raeume

### US-047: Meine Raeume anzeigen
**Als** eingeloggter Benutzer **moechte ich** eine Uebersicht meiner Raeume sehen.

**Vorbedingungen:** Benutzer ist eingeloggt und Mitglied in mindestens einem Raum.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Navigation "Raeume" klicken | Seite "Meine Raeume" wird angezeigt |
| 2 | Raumliste pruefen | Alle Raeume, in denen der Benutzer Mitglied ist, werden aufgelistet |
| 3 | Raum-Karten pruefen | Jede Karte zeigt: Name, Typ-Tag (Klasse/Gruppe/Projekt/...), Mitgliederanzahl |

**Akzeptanzkriterien:**
- [ ] Nur eigene Raeume werden angezeigt
- [ ] Raumtypen werden als farbige Tags dargestellt (Klasse, Gruppe, Projekt, Interessengruppe, Sonstige)
- [ ] Bei keinen Raeumen: "Sie sind noch keinem Raum zugeordnet."

---

### US-048: Raeume entdecken
**Als** eingeloggter Benutzer **moechte ich** verfuegbare Raeume durchsuchen und ihnen beitreten.

**Vorbedingungen:** Benutzer ist eingeloggt; es gibt oeffentliche Raeume.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Navigation "Entdecken" klicken | Seite "Raeume entdecken" mit Suchfeld und Filtern |
| 2 | Filter "Bereich" oeffnen | Dropdown mit allen Schulbereichen und Option "Alle Bereiche" |
| 3 | Filter "Typ" oeffnen | Dropdown mit Raumtypen: Alle Typen, Klasse, Gruppe, Projekt, Interessengruppe, Sonstige |
| 4 | Suchbegriff eingeben | Raeume werden nach Name und Beschreibung gefiltert |
| 5 | Ergebnisliste pruefen | Raeume nach Schulbereich gruppiert, mit Mitgliederanzahl und Beitrittsoption |

**Akzeptanzkriterien:**
- [ ] Suchfeld: "Nach Raeumen suchen..."
- [ ] Filter kombinierbar (Bereich + Typ + Suchtext)
- [ ] Raeume zeigen oeffentliche Beschreibung
- [ ] Bereits beigetretene Raeume sind markiert

---

### US-049: Raum erstellen (T/SA)
**Als** Lehrkraft oder SUPERADMIN **moechte ich** einen neuen Raum erstellen.

**Vorbedingungen:** Benutzer ist als T oder SA eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Seite "Raeume entdecken" oeffnen | Button "Raum erstellen" ist sichtbar |
| 2 | Button "Raum erstellen" klicken | Dialog oeffnet sich mit Feldern: Name, Beschreibung, Tags |
| 3 | Name "Testprojekt" eingeben, Beschreibung hinzufuegen | Felder werden befuellt |
| 4 | Optional: Tags eingeben (Enter-Taste bestaetigt jeden Tag) | Tags werden als Chips angezeigt |
| 5 | Button "Erstellen" klicken | Toast: "Raum erstellt"; Weiterleitung zum neuen Raum |

**Akzeptanzkriterien:**
- [ ] Name ist Pflichtfeld
- [ ] Beschreibung und Tags sind optional
- [ ] Ersteller wird automatisch LEADER des Raumes
- [ ] Bei KLASSE-Typ wird automatisch ein Standard-Ordner im Files-Modul erstellt

---

### US-050: Raum erstellen -- nicht erlaubt fuer P/S
**Als** Parent oder Student **moechte ich** keinen Raum erstellen koennen.

**Vorbedingungen:** Benutzer ist als P oder S eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Seite "Raeume entdecken" oeffnen | Button "Raum erstellen" ist NICHT sichtbar fuer P und S |
| 2 | API-Aufruf POST /api/v1/rooms als P | HTTP 403 Forbidden |

**Akzeptanzkriterien:**
- [ ] Kein "Raum erstellen"-Button fuer PARENT und STUDENT
- [ ] Backend blockiert unbefugte Erstellung

---

### US-051: Raum beitreten (offene Raeume)
**Als** eingeloggter Benutzer **moechte ich** einem offenen Raum beitreten.

**Vorbedingungen:** Es gibt einen Raum mit Join-Policy "Offen" (OPEN).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Seite "Raeume entdecken" oeffnen | Offener Raum wird mit Button "Beitreten" angezeigt |
| 2 | Button "Beitreten" klicken | Toast: "Erfolgreich beigetreten"; Raum erscheint in "Meine Raeume" |
| 3 | Erneut "Beitreten" versuchen | Button nicht mehr sichtbar (bereits Mitglied) |

**Akzeptanzkriterien:**
- [ ] Beitritt zu offenen Raeumen ist sofort wirksam
- [ ] Benutzer wird als MEMBER hinzugefuegt
- [ ] Raum erscheint sofort in "Meine Raeume"

---

### US-052: Beitrittsanfrage (Anfrage-Raeume)
**Als** eingeloggter Benutzer **moechte ich** eine Beitrittsanfrage fuer geschuetzte Raeume stellen.

**Vorbedingungen:** Es gibt einen Raum mit Join-Policy "Auf Anfrage" (REQUEST).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Raum mit "Auf Anfrage" auf der Entdecken-Seite finden | Button "Beitritt anfragen" wird angezeigt |
| 2 | Button "Beitritt anfragen" klicken | Dialog: "Beitrittsanfrage fuer [Raumname]" mit optionalem Nachrichtenfeld |
| 3 | Optional Nachricht eingeben: "Moechte gerne teilnehmen" | Feld befuellt |
| 4 | Anfrage absenden | Toast: "Anfrage gesendet"; Button wechselt zu "Anfrage gesendet" (deaktiviert) |

**Akzeptanzkriterien:**
- [ ] Nachricht an Leitung ist optional
- [ ] Platzhalter: "Nachricht an die Leitung (optional)..."
- [ ] Nach Absenden kann keine zweite Anfrage gestellt werden
- [ ] LEADER des Raumes erhaelt Benachrichtigung

---

### US-053: Beitrittsanfrage genehmigen/ablehnen (LEADER)
**Als** Raum-LEADER **moechte ich** Beitrittsanfragen bearbeiten koennen.

**Vorbedingungen:** Es gibt eine offene Beitrittsanfrage fuer den Raum.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Raum-Detail-Seite oeffnen als LEADER | Abschnitt "Offene Anfragen" zeigt die Beitrittsanfrage(n) |
| 2 | Button "Annehmen" bei einer Anfrage klicken | Toast: "Anfrage angenommen"; Benutzer wird als MEMBER hinzugefuegt |
| 3 | Bei einer anderen Anfrage: Button "Ablehnen" klicken | Toast: "Anfrage abgelehnt"; Anfrage wird entfernt |

**Akzeptanzkriterien:**
- [ ] Nur LEADER und SUPERADMIN koennen Anfragen bearbeiten
- [ ] Angenommene Benutzer werden automatisch MEMBER
- [ ] Anfragender Benutzer wird per Benachrichtigung informiert

---

### US-054: Raum-Nur-Einladung
**Als** Benutzer **moechte ich** keinem Raum beitreten koennen, der auf "Nur auf Einladung" steht.

**Vorbedingungen:** Es gibt einen Raum mit Join-Policy "Nur auf Einladung" (INVITE_ONLY).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Raum auf der Entdecken-Seite finden | Kein "Beitreten"- oder "Anfragen"-Button sichtbar |
| 2 | Raum-Detail-Seite als Nicht-Mitglied aufrufen | Hinweis "Kein Mitglied" und "Beitreten um Inhalte zu sehen" oder nur oeffentliche Beschreibung sichtbar |

**Akzeptanzkriterien:**
- [ ] Keine Moeglichkeit zum Beitritt ohne Einladung
- [ ] Inhalte des Raumes nicht sichtbar fuer Nicht-Mitglieder
- [ ] Nur oeffentliche Beschreibung ist einsehbar

---

### US-055: Raum-Detail-Seite -- Tabs
**Als** Raum-Mitglied **moechte ich** die verschiedenen Bereiche eines Raumes ueber Tabs erreichen.

**Vorbedingungen:** Benutzer ist Mitglied eines Raumes; diverse Module sind aktiviert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Raum-Detail-Seite oeffnen | Tabs werden angezeigt (abhaengig von Raum-Einstellungen und aktivierten Modulen) |
| 2 | Tab "Info-Board" (Standard-Tab) | Feed-Beitraege des Raumes werden angezeigt |
| 3 | Tab "Diskussionen" (wenn aktiviert) | Diskussions-Threads des Raumes |
| 4 | Tab "Chat" (wenn aktiviert) | Echtzeit-Chat mit Nachrichten |
| 5 | Tab "Dateien" (wenn aktiviert) | Dateiablage mit Ordnern |
| 6 | Tab "Fotobox" (wenn aktiviert) | Foto-Threads |
| 7 | Tab "Aufgaben" (wenn aktiviert) | Kanban-Board |
| 8 | Tab "Wiki" (wenn aktiviert) | Wiki-Seiten |
| 9 | Tab "Einstellungen" (nur LEADER/SA) | Raum-Konfiguration |

**Akzeptanzkriterien:**
- [ ] Nur aktivierte Module zeigen Tabs
- [ ] Tab "Einstellungen" nur fuer LEADER und SUPERADMIN sichtbar
- [ ] Standard-Tab ist "Info-Board"

---

### US-056: Raum-Mitglieder verwalten (LEADER)
**Als** Raum-LEADER **moechte ich** Mitglieder hinzufuegen und entfernen koennen.

**Vorbedingungen:** Benutzer ist LEADER eines Raumes.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Raum-Detail-Seite oeffnen | Mitgliederliste sichtbar (Lehrkraefte, Schueler, Familien, Weitere Mitglieder) |
| 2 | Button "Mitglied hinzufuegen" klicken | Suchfeld: "Name oder E-Mail eingeben..." |
| 3 | Name oder E-Mail eingeben, Person auswaehlen | Person wird gefunden und ausgewaehlt |
| 4 | Hinzufuegen bestaetigen | Toast: "Mitglied hinzugefuegt" |
| 5 | Rolle eines Mitglieds aendern | Dropdown mit Rollen: Leitung, Mitglied, Eltern-Mitglied, Gast |
| 6 | Toast bestaetigt Rollenaenderung | "Rolle geaendert" |
| 7 | Mitglied entfernen mit Bestaetigung | Dialog: "Mitglied wirklich entfernen?"; nach Bestaetigung: "Mitglied entfernt" |

**Akzeptanzkriterien:**
- [ ] Suche nach Name oder E-Mail funktioniert
- [ ] Bereits vorhandene Mitglieder zeigen Hinweis "Ist bereits Mitglied"
- [ ] Rollen: LEADER, MEMBER, PARENT_MEMBER, GUEST
- [ ] Entfernung erfordert Bestaetigung

---

### US-057: Familie einem Raum hinzufuegen (LEADER)
**Als** Raum-LEADER **moechte ich** eine ganze Familie einem Raum hinzufuegen.

**Vorbedingungen:** Benutzer ist LEADER; es gibt Familienverbuende im System.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Raum-Detail-Seite oeffnen | Button "Familie aufnehmen" sichtbar |
| 2 | Button "Familie aufnehmen" klicken | Dialog: "Familie auswaehlen" mit Suchfeld |
| 3 | Familie auswaehlen und hinzufuegen | Toast: "Familienmitglieder hinzugefuegt"; alle Familienmitglieder sind nun Mitglieder |

**Akzeptanzkriterien:**
- [ ] Eltern werden als PARENT_MEMBER hinzugefuegt
- [ ] Kinder werden als MEMBER hinzugefuegt
- [ ] Bei KLASSE-Raeumen: separate Darstellung von Lehrkraeften, Schuelern, Familien

---

### US-058: Raum-Einstellungen (LEADER)
**Als** Raum-LEADER **moechte ich** die Raum-Einstellungen konfigurieren.

**Vorbedingungen:** Benutzer ist LEADER eines Raumes.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Tab "Einstellungen" oeffnen | Raum-Einstellungen werden angezeigt |
| 2 | Module pruefen: Chat, Dateien aktivieren/deaktivieren | Toggles: "Aktiviert den Chat-Bereich in diesem Raum" / "Aktiviert die Dateiablage in diesem Raum" |
| 3 | Sichtbarkeit aendern: "Nur Mitglieder" / "Schulbereich" / "Alle" | Dropdown wird aktualisiert |
| 4 | Diskussionsmodus aendern: "Vollstaendig" / "Nur Ankuendigungen" / "Deaktiviert" | Dropdown wird aktualisiert |
| 5 | Toggle "Mitglieder duerfen Themen erstellen" | An/Aus mit Hinweis: "Wenn deaktiviert, koennen nur Leitung und Admins Diskussionsthemen erstellen" |
| 6 | "Speichern" klicken | Toast: "Einstellungen gespeichert" |

**Akzeptanzkriterien:**
- [ ] Nur LEADER und SA sehen den Einstellungen-Tab
- [ ] Modul-Toggles aktivieren/deaktivieren entsprechende Tabs
- [ ] Sichtbarkeit beeinflusst, wer den Raum auf der Entdecken-Seite sieht
- [ ] Diskussionsmodus steuert, wer Diskussionen erstellen kann

---

### US-059: Raum archivieren (SA)
**Als** SUPERADMIN **moechte ich** einen Raum archivieren koennen, **damit** er nicht mehr aktiv genutzt wird.

**Vorbedingungen:** SA ist eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Verwaltung > Raeume oeffnen | Raumliste mit allen Raeumen |
| 2 | Raum "Testprojekt" finden, Button "Raum deaktivieren" klicken | Dialog: "Raum <<Testprojekt>> wirklich deaktivieren? Er wird fuer Nutzer nicht mehr sichtbar sein." |
| 3 | Bestaetigen | Toast: "Raum deaktiviert"; Raum zeigt Tag "Deaktiviert" |
| 4 | Button "Raum reaktivieren" klicken | Dialog: "Raum <<Testprojekt>> wirklich reaktivieren?" |
| 5 | Bestaetigen | Toast: "Raum reaktiviert"; Raum ist wieder sichtbar |

**Akzeptanzkriterien:**
- [ ] Deaktivierter Raum ist fuer Nutzer nicht mehr sichtbar
- [ ] Deaktivierung ist reversibel (Reaktivierung moeglich)
- [ ] Nur SA kann Raeume deaktivieren/reaktivieren
- [ ] Endgueltiges Loeschen mit Warnung: "Alle Mitgliedschaften, Diskussionen, Dateien und Chat-Nachrichten werden unwiderruflich geloescht."

---

### US-060: Diskussions-Thread erstellen
**Als** Raum-Mitglied **moechte ich** eine Diskussion im Raum starten.

**Vorbedingungen:** Diskussionsmodus ist "Vollstaendig" oder "Nur Ankuendigungen" (fuer LEADER); Einstellung "Mitglieder duerfen Themen erstellen" ist an.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Tab "Diskussionen" im Raum oeffnen | Seite zeigt bestehende Diskussionen oder "Noch keine Diskussionen vorhanden" |
| 2 | Button "Neue Diskussion" klicken | Formular: "Diskussion erstellen" mit Feldern Titel und Beschreibung |
| 3 | Titel "Wandertag-Planung" eingeben | Feld wird befuellt; Platzhalter: "Thema der Diskussion..." |
| 4 | Beschreibung eingeben (optional) | Platzhalter: "Worum geht es in dieser Diskussion?" |
| 5 | Zielgruppe waehlen: "Alle" / "Eltern" / "Kinder" | Dropdown mit Optionen |
| 6 | Absenden | Diskussion erscheint in der Liste mit Antworten-Zaehler |

**Akzeptanzkriterien:**
- [ ] Titel ist Pflichtfeld
- [ ] Beschreibung ist optional
- [ ] Zielgruppe (Audience): Alle, Eltern, Kinder
- [ ] Bei deaktivierter Mitglieder-Erstellung: nur LEADER/SA koennen Diskussionen erstellen
- [ ] Antworten-Zaehler zeigt "{n} Antworten"

---

### US-061: Diskussions-Thread beantworten
**Als** Raum-Mitglied **moechte ich** auf eine Diskussion antworten.

**Vorbedingungen:** Es gibt eine offene Diskussion im Raum.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Diskussion anklicken | Detail-Ansicht mit allen Antworten |
| 2 | Antwortfeld "Antwort schreiben..." fuellen | Text wird eingegeben |
| 3 | Absenden | Antwort erscheint in der Liste; Antworten-Zaehler erhoet sich |

**Akzeptanzkriterien:**
- [ ] Antworten werden chronologisch angezeigt
- [ ] Bei "Nur Ankuendigungen" koennen nur LEADER/SA Diskussionen erstellen, aber alle antworten
- [ ] Bei deaktiviertem Diskussionsmodus: Tab "Diskussionen" nicht sichtbar

---

### US-062: Diskussion archivieren (LEADER)
**Als** Raum-LEADER **moechte ich** eine Diskussion archivieren, **damit** keine weiteren Antworten moeglich sind.

**Vorbedingungen:** Benutzer ist LEADER; es gibt eine aktive Diskussion.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Diskussion oeffnen | Detail-Ansicht mit Button "Archivieren" |
| 2 | Button "Archivieren" klicken | Diskussion zeigt Tag "Archiviert" |
| 3 | Antwortfeld pruefen | Hinweis: "Diese Diskussion ist archiviert. Antworten sind nicht mehr moeglich." |

**Akzeptanzkriterien:**
- [ ] Archivierte Diskussionen zeigen Hinweis
- [ ] Antwortfeld ist deaktiviert/ausgeblendet
- [ ] Nur LEADER/SA koennen archivieren

---

### US-063: Raum-Chat (Echtzeit-Nachrichten)
**Als** Raum-Mitglied **moechte ich** im Raum-Chat in Echtzeit kommunizieren.

**Vorbedingungen:** Chat ist im Raum aktiviert (Raum-Einstellungen).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Tab "Chat" im Raum oeffnen | Chat-Bereich mit Nachrichten oder "Noch keine Nachrichten in diesem Kanal." |
| 2 | Kanaele pruefen | Kanaele: "Alle", "Eltern-Lehrer Chat", "Schueler-Lehrer Chat" |
| 3 | Nachricht "Hallo zusammen!" in Textfeld eingeben | Platzhalter: "Nachricht schreiben..." |
| 4 | Nachricht senden (Enter oder Button) | Nachricht erscheint sofort im Chat |
| 5 | Bild anhaengen (Button "Bild anhaengen") | Bild wird hochgeladen und als Thumbnail angezeigt |
| 6 | Auf eine Nachricht antworten (Button "Antworten") | Antwort wird als Reply dargestellt mit Referenz zur Originalnachricht |

**Akzeptanzkriterien:**
- [ ] WebSocket-Verbindung fuer Echtzeit-Updates (/ws/messages)
- [ ] Drei Kanaele: MAIN (Alle), PARENTS (Eltern-Lehrer), STUDENTS (Schueler-Lehrer)
- [ ] Bilder: JPEG, PNG, WebP, GIF; max. 10 MB
- [ ] Reply-Threading mit reply_to_id
- [ ] Nachrichten zeigen Datum-Separatoren (Heute, Gestern)

---

### US-064: Raum-Chat stummschalten
**Als** Raum-Mitglied **moechte ich** den Chat stummschalten, **damit** ich keine Benachrichtigungen erhalte.

**Vorbedingungen:** Chat ist im Raum aktiviert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Tab "Chat" oeffnen | Button "Stummschalten" im Chat-Header sichtbar |
| 2 | Button "Stummschalten" klicken | Icon wechselt; Label zeigt "Stumm"; Toast: "Stummschaltung aufgehoben" bei erneutem Klick |
| 3 | Neue Nachricht im Chat pruefen | Keine Push-Benachrichtigung fuer stummgeschalteten Chat |

**Akzeptanzkriterien:**
- [ ] Mute-Toggle in Chat-Header
- [ ] Stummgeschalteter Chat erscheint in Profil unter "Stummgeschaltete Chats"
- [ ] Persistiert in `conversation_participants.muted`

---

### US-065: Jitsi-Videokonferenz im Raum starten
**Als** Raum-Mitglied **moechte ich** einen Videochat starten, **damit** wir uns virtuell treffen koennen.

**Vorbedingungen:** Jitsi-Modul ist aktiviert (in modules JSONB); Jitsi-Server-URL ist konfiguriert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Raum-Detail-Seite oeffnen | Button "Videochat starten" sichtbar (oder im Chat-Header) |
| 2 | Button klicken | Neuer Tab oeffnet sich mit Jitsi-Konferenz auf konfiguriertem Server |

**Akzeptanzkriterien:**
- [ ] Jitsi-Button nur sichtbar wenn Modul aktiviert
- [ ] Jitsi-Server-URL aus Tenant-Konfiguration (Standard: meet.jit.si)
- [ ] Raum-spezifischer Konferenzname

---

### US-066: Raum-Feed stummschalten
**Als** Raum-Mitglied **moechte ich** den Feed eines Raumes stummschalten.

**Vorbedingungen:** Benutzer ist Mitglied eines Raumes.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Raum-Detail-Seite oeffnen | Button/Option "Feed stummschalten" sichtbar |
| 2 | "Feed stummschalten" klicken | Toast: "Feed stummgeschaltet" |
| 3 | Neue Beitraege im Raum pruefen | Beitraege erscheinen nicht mehr im Dashboard-Feed |
| 4 | "Stummschaltung aufheben" klicken | Toast: "Stummschaltung aufgehoben" |

**Akzeptanzkriterien:**
- [ ] Stummgeschaltete Raum-Feeds erscheinen nicht im globalen Feed
- [ ] Beitraege sind weiterhin im Raum selbst sichtbar
- [ ] Toggle-Funktion (Stummschalten / Aufheben)

---

### US-067: Raum-Zugriff als Nicht-Mitglied
**Als** Nicht-Mitglied **moechte ich** keinen Zugriff auf Raum-Inhalte haben.

**Vorbedingungen:** Benutzer ist kein Mitglied des Raumes; Raum hat Sichtbarkeit "Nur Mitglieder".

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | URL /rooms/[raum-id] direkt aufrufen | Seite zeigt nur oeffentliche Beschreibung; Hinweis "Kein Mitglied" / "Beitreten um Inhalte zu sehen" |
| 2 | API-Aufruf GET /api/v1/rooms/[raum-id]/files als Nicht-Mitglied | HTTP 403 Forbidden |

**Akzeptanzkriterien:**
- [ ] Keine Inhalte (Posts, Dateien, Chat) fuer Nicht-Mitglieder sichtbar
- [ ] Nur oeffentliche Beschreibung wird angezeigt
- [ ] Backend-Endpunkte pruefen Mitgliedschaft

---

### US-068: Klassenwechsel / Migration (LEADER)
**Als** Raum-LEADER eines KLASSE-Raumes **moechte ich** Schueler in eine andere Klasse verschieben oder die Schule verlassen lassen.

**Vorbedingungen:** Benutzer ist LEADER eines KLASSE-Raumes mit Schuelern.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Raum-Detail-Seite oeffnen | Button "Klassenwechsel" sichtbar (nur bei KLASSE-Raeumen) |
| 2 | Button "Klassenwechsel" klicken | Dialog: "Klassenwechsel / Migration" |
| 3 | Kinder auswaehlen (Checkboxen) | Anzeige: "{count} Kind(er) ausgewaehlt" |
| 4 | Aktion "In andere Klasse verschieben" waehlen | Dropdown "Zielklasse auswaehlen" erscheint |
| 5 | Zielklasse waehlen, "Ausfuehren" klicken | Toast: "{count} Kind(er) erfolgreich verschoben."; Eltern werden automatisch in Zielklasse aufgenommen |
| 6 | Alternativ: Aktion "Schule verlassen" waehlen | Warnung: "Diese Aktion entfernt die ausgewaehlten Kinder und deren Familien aus allen Raeumen und deaktiviert die Familien." |
| 7 | Bestaetigen | Toast: "{count} Kind(er) haben die Schule verlassen."; Familien werden deaktiviert |

**Akzeptanzkriterien:**
- [ ] Nur bei KLASSE-Raeumen verfuegbar
- [ ] "Verschieben" fuegt Kinder + Eltern zur Zielklasse hinzu
- [ ] "Schule verlassen" entfernt aus allen Raeumen und deaktiviert Familien
- [ ] Warnung bei irreversibler Aktion

---

## Modul: Feed

### US-069: Feed-Beitraege anzeigen
**Als** eingeloggter Benutzer **moechte ich** den Feed mit Beitraegen aus meinen Raeumen sehen.

**Vorbedingungen:** Benutzer ist eingeloggt und Mitglied in Raeumen mit Beitraegen.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Dashboard oeffnen | Feed-Liste zeigt Beitraege chronologisch sortiert |
| 2 | Beitrag pruefen | Jeder Beitrag zeigt: Autor, Quelle (Raum/Schulbereich/Schulweit), Inhalt, Erstellungsdatum, Kommentar-Zaehler, Reaktionen |
| 3 | Quell-Labels pruefen | Labels: "Raum", "Schulbereich", "Schulweit", "Pinnwand", "System" |
| 4 | "Weitere laden" am Ende der Liste klicken | Naechste Seite der Beitraege wird geladen (Pagination: 20 pro Seite) |

**Akzeptanzkriterien:**
- [ ] Beitraege sind chronologisch sortiert (neueste zuerst)
- [ ] Angeheftete Beitraege stehen oben
- [ ] Pagination mit "Weitere laden"-Button
- [ ] Bei keinen Beitraegen: "Noch keine Beitraege vorhanden."

---

### US-070: Beitrag erstellen (T/SA im Raum)
**Als** Lehrkraft oder SUPERADMIN **moechte ich** einen Beitrag in einem Raum veroeffentlichen.

**Vorbedingungen:** Benutzer ist LEADER oder MEMBER des Raumes und hat Rolle T oder SA.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Raum-Detail-Seite oeffnen | PostComposer ist sichtbar mit Feldern "Titel (optional)" und "Was gibt es Neues?" |
| 2 | Titel "Elternabend" eingeben | Titelfeld wird befuellt |
| 3 | Inhalt eingeben: "Am Freitag findet der Elternabend statt." | Inhaltsfeld wird befuellt |
| 4 | Button "Veroeffentlichen" klicken | Toast: "Beitrag veroeffentlicht"; Beitrag erscheint im Raum-Feed |

**Akzeptanzkriterien:**
- [ ] Titel ist optional
- [ ] Inhalt ist Pflichtfeld (oder alternativ Poll/Dateianhang)
- [ ] Beitrag erscheint sofort im Feed des Raumes
- [ ] Beitrag erscheint auch im Dashboard-Feed der Raum-Mitglieder

---

### US-071: Beitrag erstellen -- nicht erlaubt fuer P/S
**Als** Parent oder Student **moechte ich** keinen Beitrag im Raum erstellen koennen (ausser in Diskussionen).

**Vorbedingungen:** Benutzer ist als P oder S eingeloggt und Mitglied eines Raumes.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Raum-Detail-Seite oeffnen als P | PostComposer ist NICHT sichtbar auf dem Info-Board |
| 2 | Raum-Detail-Seite oeffnen als S | PostComposer ist NICHT sichtbar auf dem Info-Board |

**Akzeptanzkriterien:**
- [ ] PARENT und STUDENT koennen keine Feed-Beitraege erstellen
- [ ] PostComposer auf Dashboard und Raum-Detail nur fuer T/SA sichtbar

---

### US-072: Beitrag mit Datei-Anhaengen erstellen
**Als** Lehrkraft **moechte ich** Dateien an einen Beitrag anhaengen.

**Vorbedingungen:** Benutzer ist T oder SA; Raum ist geoeffnet.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | PostComposer oeffnen | Button "Dateien anhaengen" (Bueroklammer-Icon) sichtbar |
| 2 | Button klicken | Datei-Dialog oeffnet sich |
| 3 | 2 Dateien auswaehlen (z.B. PDF und Bild) | Anzeige: "2 Datei(en) ausgewaehlt" mit Dateiliste (Name, Groesse, Icon) |
| 4 | Einzelne Datei entfernen (X-Button) | Datei wird aus der Liste entfernt |
| 5 | Beitrag veroeffentlichen | Toast: "Beitrag veroeffentlicht"; Anhaenge werden als Download-Links dargestellt |

**Akzeptanzkriterien:**
- [ ] Maximal 10 Dateien pro Beitrag
- [ ] Maximale Dateigroesse: 50 MB pro Datei
- [ ] Bei zu grosser Datei: "Datei zu gross (max. 50 MB)"
- [ ] Bei zu vielen Dateien: "Maximal 10 Dateien erlaubt"
- [ ] Zwei-Schritt-Prozess: Post erstellen, dann Dateien hochladen (transparent fuer Benutzer)
- [ ] Verschiedene Datei-Icons (Bild, PDF, Video, Audio, Datei)

---

### US-073: Beitrag loeschen
**Als** Autor eines Beitrags oder SUPERADMIN **moechte ich** einen Beitrag loeschen koennen.

**Vorbedingungen:** Beitrag existiert; Benutzer ist Autor oder SA.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Bei eigenem Beitrag: Menue/Button "Loeschen" klicken | Bestaetigungsdialog: "Moechten Sie diesen Beitrag wirklich loeschen? Dies kann nicht rueckgaengig gemacht werden." |
| 2 | "Loeschen" bestaetigen | Beitrag wird entfernt; er verschwindet aus dem Feed |
| 3 | Als SA: Beitrag eines anderen Benutzers loeschen | Gleicher Bestaetigungsdialog; Loeschung funktioniert |
| 4 | Als T: Beitrag eines anderen T loeschen versuchen | Loeschen-Option ist NICHT sichtbar |

**Akzeptanzkriterien:**
- [ ] Nur Autor und SA koennen Beitraege loeschen
- [ ] Bestaetigungsdialog mit Titel "Beitrag loeschen"
- [ ] Loeschung ist endgueltig
- [ ] Zugehoerige Kommentare und Anhaenge werden mitgeloescht

---

### US-074: Kommentar verfassen
**Als** eingeloggter Benutzer **moechte ich** einen Beitrag kommentieren.

**Vorbedingungen:** Benutzer ist eingeloggt und kann den Beitrag sehen.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Beitrag im Feed oeffnen/erweitern | Kommentarfeld sichtbar: "Kommentar schreiben..." |
| 2 | Kommentar eingeben: "Gute Idee!" | Text wird eingegeben |
| 3 | Button "Kommentieren" klicken | Kommentar erscheint unter dem Beitrag; Kommentar-Zaehler erhoet sich |
| 4 | Leeren Kommentar absenden versuchen | Button ist deaktiviert |

**Akzeptanzkriterien:**
- [ ] Kommentare werden chronologisch unter dem Beitrag angezeigt
- [ ] Kommentar-Zaehler wird aktualisiert
- [ ] Leere Kommentare werden verhindert
- [ ] Jeder Kommentar zeigt: Autorname, Inhalt, Zeitstempel

---

### US-075: Beitrag anheften/loslösen (T/SA)
**Als** Lehrkraft oder SUPERADMIN **moechte ich** einen Beitrag oben anheften.

**Vorbedingungen:** Benutzer ist T oder SA.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Bei einem Beitrag: Option "Anheften" klicken | Beitrag wird oben fixiert; Label "Angeheftet" erscheint |
| 2 | Feed pruefen | Angehefteter Beitrag steht an erster Stelle |
| 3 | Option "Losloesen" klicken | Label "Angeheftet" verschwindet; Beitrag rueckt an chronologische Position |

**Akzeptanzkriterien:**
- [ ] Pin-Toggle: "Anheften" / "Losloesen"
- [ ] Angeheftete Beitraege zeigen Tag "Angeheftet"
- [ ] Nur T und SA koennen pinnen
- [ ] P und S sehen keine Pin-Option

---

### US-076: Reaktionen auf Beitraege
**Als** eingeloggter Benutzer **moechte ich** auf Beitraege mit Emojis reagieren.

**Vorbedingungen:** Benutzer ist eingeloggt; Beitraege sind sichtbar.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Unter einem Beitrag: Reaktions-Button klicken | Emoji-Auswahl wird angezeigt |
| 2 | Emoji auswaehlen (z.B. Daumen hoch) | Reaktion erscheint unter dem Beitrag mit Zaehler |
| 3 | Gleiche Reaktion erneut klicken | Reaktion wird zurueckgenommen (Toggle-Verhalten) |
| 4 | Andere Benutzer reagieren auf gleichen Beitrag | Zaehler erhoeht sich; eigene Reaktion ist hervorgehoben |

**Akzeptanzkriterien:**
- [ ] Toggle-Pattern: Klick setzt/entfernt Reaktion
- [ ] Zaehler zeigt Anzahl pro Emoji
- [ ] Eigene Reaktionen sind visuell markiert (userReacted: true)
- [ ] Reaktionen auch auf Kommentare moeglich

---

### US-077: Reaktionen auf Kommentare
**Als** eingeloggter Benutzer **moechte ich** auf Kommentare mit Emojis reagieren.

**Vorbedingungen:** Benutzer ist eingeloggt; Kommentare sind sichtbar.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Bei einem Kommentar: Reaktions-Button klicken | Emoji-Auswahl wird angezeigt |
| 2 | Emoji auswaehlen | Reaktion erscheint am Kommentar mit Zaehler |
| 3 | Gleiche Reaktion erneut klicken | Reaktion wird zurueckgenommen |

**Akzeptanzkriterien:**
- [ ] Selbes Toggle-Verhalten wie bei Post-Reaktionen
- [ ] API: POST /api/v1/feed/comments/{id}/reactions

---

### US-078: Beitrag mit Umfrage (Poll)
**Als** Lehrkraft **moechte ich** eine Umfrage in einem Beitrag erstellen.

**Vorbedingungen:** Benutzer ist T oder SA.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | PostComposer oeffnen | Button/Icon fuer Umfrage erstellen sichtbar |
| 2 | Umfrage-Modus aktivieren | Felder: Frage, Optionen (min. 2), Mehrfachauswahl-Toggle |
| 3 | Frage "Wann passt der Elternabend?" eingeben | Platzhalter: "Frage eingeben..." |
| 4 | Optionen hinzufuegen: "Montag", "Dienstag", "Mittwoch" | Optionen mit Platzhalter "Option {n}" |
| 5 | "Veroeffentlichen" klicken | Umfrage erscheint im Feed mit Abstimmungsmoeglichkeit |
| 6 | Als P: Auf Option "Montag" klicken | Button "Abstimmen"; Ergebnis zeigt Zaehler |
| 7 | Als T: "Umfrage beenden" klicken | Umfrage zeigt "Beendet"; keine weiteren Abstimmungen moeglich |

**Akzeptanzkriterien:**
- [ ] Mindestens 2 Optionen erforderlich
- [ ] Einfach- oder Mehrfachauswahl konfigurierbar
- [ ] Ergebnis zeigt Stimmenanzahl: "{count} Stimme(n)"
- [ ] Ersteller kann Umfrage beenden
- [ ] Beendete Umfrage zeigt "Beendet"

---

### US-079: Parent-Only Beitraege
**Als** Lehrkraft **moechte ich** Beitraege erstellen, die nur fuer Eltern sichtbar sind.

**Vorbedingungen:** Benutzer ist T oder SA; Raum hat Eltern-Mitglieder.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Beitrag im Raum erstellen mit parentOnly-Flag | Beitrag wird mit Kennzeichnung "Nur fuer Eltern" erstellt |
| 2 | Als P den Raum-Feed oeffnen | Beitrag ist sichtbar |
| 3 | Als S den Raum-Feed oeffnen | Beitrag ist NICHT sichtbar |

**Akzeptanzkriterien:**
- [ ] FeedPost.parentOnly = true filtert fuer Schueler
- [ ] Nur T/SA koennen Parent-Only-Beitraege erstellen
- [ ] Eltern und Lehrkraefte sehen den Beitrag

---

### US-080: Targeted Posts (gezielte Beitraege)
**Als** Lehrkraft **moechte ich** Beitraege fuer bestimmte Benutzer sichtbar machen.

**Vorbedingungen:** Benutzer ist T oder SA.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Beitrag erstellen und target_user_ids setzen (API) | Beitrag wird erstellt mit eingeschraenkter Sichtbarkeit |
| 2 | Als gezielter Benutzer Feed oeffnen | Beitrag ist sichtbar |
| 3 | Als nicht gezielter Benutzer Feed oeffnen | Beitrag ist NICHT sichtbar |

**Akzeptanzkriterien:**
- [ ] `feed_posts.target_user_ids`: NULL = alle sichtbar, gefuellt = nur gelistete User
- [ ] Gezielte Beitraege werden nur den ausgewaehlten Benutzern angezeigt
- [ ] PostgreSQL UUID-Array fuer target_user_ids

---

### US-081: System-Banner auf Dashboard
**Als** eingeloggter Benutzer **moechte ich** kontextabhaengige Banner sehen.

**Vorbedingungen:** Benutzer ist eingeloggt; es gibt aktive Banner (z.B. Putz-Banner fuer betroffene Eltern).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Dashboard oeffnen | System-Banner werden oberhalb des Feeds angezeigt |
| 2 | Banner pruefen | Titel, Inhalt, optionaler Link und Ablaufdatum |
| 3 | Als P mit anstehendem Putztermin | Putz-Banner wird angezeigt |
| 4 | Als P ohne Putztermin | Putz-Banner wird NICHT angezeigt |

**Akzeptanzkriterien:**
- [ ] Banner sind kontextabhaengig (z.B. Putz-Banner nur fuer betroffene Eltern)
- [ ] Banner haben optionales Ablaufdatum (expiresAt)
- [ ] Banner koennen einen Link enthalten

---

### US-082: Feed im Raum (Raum-spezifische Beitraege)
**Als** Raum-Mitglied **moechte ich** nur Beitraege des aktuellen Raumes sehen.

**Vorbedingungen:** Benutzer ist Mitglied eines Raumes mit Beitraegen.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Raum-Detail-Seite oeffnen | Tab "Info-Board" zeigt nur Beitraege dieses Raumes |
| 2 | Beitraege vergleichen mit Dashboard-Feed | Raum-Feed zeigt nur raum-spezifische Beitraege; Dashboard zeigt alle |
| 3 | Leerer Raum-Feed | Hinweis: "Noch keine Beitraege in diesem Raum" |

**Akzeptanzkriterien:**
- [ ] API: GET /api/v1/feed/rooms/{roomId}/posts
- [ ] Nur Beitraege des spezifischen Raumes werden angezeigt
- [ ] Pagination funktioniert auch im Raum-Feed

---

### US-083: Beitrag-Quell-Labels
**Als** Benutzer **moechte ich** erkennen koennen, woher ein Beitrag stammt.

**Vorbedingungen:** Feed zeigt Beitraege aus verschiedenen Quellen.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Dashboard-Feed oeffnen | Beitraege zeigen Quell-Labels |
| 2 | Raum-Beitrag pruefen | Label: "Raum" mit Raumnamen |
| 3 | Schulbereichs-Beitrag pruefen | Label: "Schulbereich" |
| 4 | Schulweiter Beitrag pruefen | Label: "Schulweit" |

**Akzeptanzkriterien:**
- [ ] Quell-Labels: Raum, Schulbereich, Schulweit, Pinnwand, System
- [ ] Label ist farblich unterscheidbar
- [ ] Klick auf Raum-Label navigiert zum Raum

---

### US-084: Link-Vorschau in Beitraegen
**Als** Benutzer **moechte ich** bei Links im Beitrag eine Vorschau sehen.

**Vorbedingungen:** Beitrag enthaelt eine URL.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Beitrag mit URL erstellen | Link-Vorschau wird generiert (Titel, Beschreibung, Bild, Seitenname) |
| 2 | Vorschau pruefen | API: GET /api/v1/feed/link-preview?url=... |

**Akzeptanzkriterien:**
- [ ] Link-Vorschau zeigt: Titel, Beschreibung, Bild (optional), Seitenname
- [ ] Label "Link-Vorschau" wird angezeigt
- [ ] Fallback bei nicht erreichbaren URLs

---

### US-085: Anhang herunterladen
**Als** Benutzer **moechte ich** Datei-Anhaenge von Beitraegen herunterladen.

**Vorbedingungen:** Beitrag hat Datei-Anhaenge.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Beitrag mit Anhaengen im Feed finden | Anhaenge werden mit Dateiname, Groesse und Icon dargestellt |
| 2 | Auf einen Anhang klicken | Datei wird heruntergeladen |

**Akzeptanzkriterien:**
- [ ] Download-URL: /api/v1/feed/attachments/{attachmentId}/download
- [ ] Dateiname, Dateityp und Groesse werden angezeigt
- [ ] Icons fuer verschiedene Dateitypen (Bild, PDF, Video, Audio)

---

### US-086: Feed -- kein Beitrag ohne Inhalt
**Als** Benutzer **moechte ich** keine leeren Beitraege erstellen koennen.

**Vorbedingungen:** PostComposer ist sichtbar.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | PostComposer oeffnen, keinen Text eingeben | Button "Veroeffentlichen" ist deaktiviert |
| 2 | Nur Titel eingeben, aber keinen Inhalt | Button bleibt deaktiviert |
| 3 | Inhalt eingeben ODER Datei anhaengen ODER Umfrage erstellen | Button wird aktiv |

**Akzeptanzkriterien:**
- [ ] Mindestens einer von: Inhalt, Datei-Anhang, Umfrage muss vorhanden sein
- [ ] Button "Veroeffentlichen" ist deaktiviert solange kein Inhalt vorhanden
- [ ] Titel allein reicht nicht aus

---

### US-087: Beitrag schulweit erstellen (nur SA)
**Als** SUPERADMIN **moechte ich** schulweite Beitraege im Dashboard erstellen.

**Vorbedingungen:** Benutzer ist SA.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Dashboard oeffnen als SA | PostComposer mit sourceType SCHOOL ist verfuegbar |
| 2 | Beitrag erstellen: "Schulweite Ankuendigung" | Beitrag wird mit Label "Schulweit" erstellt |
| 3 | Als P Dashboard oeffnen | Schulweiter Beitrag ist sichtbar |
| 4 | Als S Dashboard oeffnen | Schulweiter Beitrag ist sichtbar |

**Akzeptanzkriterien:**
- [ ] Schulweite Beitraege sind fuer alle Benutzer sichtbar
- [ ] SourceType SCHOOL
- [ ] PostComposer auf Dashboard erstellt schulweite Beitraege

---

### US-088: Video-Embed in Beitraegen
**Als** Benutzer **moechte ich** eingebettete Videos in Beitraegen sehen.

**Vorbedingungen:** Beitrag enthaelt einen Video-Link (z.B. YouTube).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Beitrag mit YouTube-Link erstellen | Video wird als Embed angezeigt |
| 2 | Video-Label pruefen | Label "Video" wird angezeigt |

**Akzeptanzkriterien:**
- [ ] YouTube- und aehnliche Video-Links werden eingebettet
- [ ] Fallback: normaler Link wenn Embed nicht moeglich

---

### US-089: Impersonation (SA)
**Als** SUPERADMIN **moechte ich** mich als anderer Benutzer anmelden koennen, **damit** ich Probleme nachstellen kann.

**Vorbedingungen:** SA ist eingeloggt; Impersonation ist aktiviert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als SA: Impersonation-Funktion nutzen | Banner: "Angemeldet als [Name] (Impersonation)" |
| 2 | System aus Sicht des anderen Benutzers testen | Alle Inhalte werden gemaess der Rolle des impersonierten Benutzers angezeigt |
| 3 | Button "Zurueck zum Admin" klicken | Zurueck zur SA-Session |

**Akzeptanzkriterien:**
- [ ] Banner "Angemeldet als {name} (Impersonation)" wird angezeigt
- [ ] Hinweis: "Nur fuer Test- und Supportzwecke"
- [ ] "Zurueck zum Admin" beendet die Impersonation
- [ ] Sicherheitsrelevante Funktion -- nur wenn aktiviert

---

### US-090: Zugriffsschutz -- unautorisierter API-Zugriff
**Als** nicht authentifizierter Benutzer **moechte ich** keinen Zugriff auf geschuetzte API-Endpunkte haben.

**Vorbedingungen:** Kein gueltiger JWT-Token.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | GET /api/v1/feed ohne Token aufrufen | HTTP 401 Unauthorized |
| 2 | GET /api/v1/rooms/mine ohne Token aufrufen | HTTP 401 Unauthorized |
| 3 | GET /api/v1/users/me ohne Token aufrufen | HTTP 401 Unauthorized |
| 4 | POST /api/v1/auth/login (oeffentlicher Endpunkt) | HTTP 200 (mit gueltigen Daten) |

**Akzeptanzkriterien:**
- [ ] Alle geschuetzten Endpunkte geben 401 ohne Token zurueck
- [ ] Oeffentliche Endpunkte (login, register, password-reset) sind ohne Token erreichbar
- [ ] JWT-Token hat 15 Minuten Gueltigkeit
- [ ] Refresh-Token hat 7 Tage Gueltigkeit (httpOnly Cookie)

---


**Module:** Kalender, Nachrichten/Messaging, Dateien, Formulare
**Rollen:** SUPERADMIN (SA), SECTION_ADMIN (SECADMIN), TEACHER (T), PARENT (P), STUDENT (S)

**Testkonten:**
| Konto | Rolle | Passwort |
|-------|-------|----------|
| admin@monteweb.local | SA | admin123 |
| sectionadmin@monteweb.local | SECADMIN | test1234 |
| lehrer@monteweb.local | T | test1234 |
| eltern@monteweb.local | P | test1234 |
| schueler@monteweb.local | S | test1234 |

---

## Modul: Kalender

### US-100: Raum-Termin erstellen (LEADER)
**Als** Raumleiter (LEADER) **moechte ich** einen Termin fuer meinen Raum erstellen, **damit** alle Raummitglieder ueber anstehende Veranstaltungen informiert werden.

**Vorbedingungen:** Benutzer ist als LEADER in mindestens einem Raum eingetragen. Login als `lehrer@monteweb.local`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Im Hauptmenue auf "Kalender" klicken | Kalenderansicht wird angezeigt |
| 2 | Button "Termin erstellen" klicken | Formular "Termin erstellen" oeffnet sich |
| 3 | Feld "Titel" ausfuellen: "Elternabend Sonnengruppe" | Titel wird angenommen |
| 4 | Feld "Beschreibung" ausfuellen: "Thema: Jahresplanung" | Beschreibung wird angenommen |
| 5 | Feld "Ort" ausfuellen: "Klassenzimmer 3" | Ort wird angenommen |
| 6 | "Sichtbarkeit" auf "Raum" setzen | Dropdown "Raum auswaehlen" erscheint |
| 7 | Raum "Sonnengruppe" auswaehlen | Raum ist selektiert |
| 8 | "Ganztaegig" deaktiviert lassen, Startdatum/-zeit und Enddatum/-zeit setzen | Datums- und Zeitfelder sind befuellt |
| 9 | "Wiederholung" auf "Keine" belassen | Keine Wiederholung eingestellt |
| 10 | Formular absenden | Toast "Termin erstellt" erscheint, Termin ist im Kalender sichtbar |

**Akzeptanzkriterien:**
- [ ] Termin erscheint in der Kalenderansicht am richtigen Datum
- [ ] Termin ist nur fuer Mitglieder der Sonnengruppe sichtbar
- [ ] Termin zeigt Titel, Beschreibung, Ort und Zeitraum korrekt an
- [ ] Ersteller wird als Autor des Termins angezeigt

---

### US-101: Bereichs-Termin erstellen (TEACHER)
**Als** Lehrkraft **moechte ich** einen Termin fuer einen Schulbereich erstellen, **damit** alle Benutzer des Bereichs informiert werden.

**Vorbedingungen:** Login als `lehrer@monteweb.local` (TEACHER).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Im Hauptmenue auf "Kalender" klicken | Kalenderansicht wird angezeigt |
| 2 | Button "Termin erstellen" klicken | Formular oeffnet sich |
| 3 | Feld "Titel" ausfuellen: "Bereichsversammlung Kinderhaus" | Titel wird angenommen |
| 4 | "Sichtbarkeit" auf "Schulbereich" setzen | Dropdown "Schulbereich auswaehlen" erscheint |
| 5 | Schulbereich auswaehlen (z.B. Kinderhaus) | Bereich ist selektiert |
| 6 | Startdatum, Startzeit, Enddatum und Endzeit setzen | Felder sind befuellt |
| 7 | Formular absenden | Toast "Termin erstellt" erscheint |

**Akzeptanzkriterien:**
- [ ] Termin ist fuer alle Benutzer des gewaehlten Schulbereichs sichtbar
- [ ] Termin ist NICHT fuer Benutzer anderer Bereiche sichtbar
- [ ] SECTION_ADMIN kann ebenfalls Bereichs-Termine erstellen

---

### US-102: Schulweiten Termin erstellen (SUPERADMIN)
**Als** Superadmin **moechte ich** einen schulweiten Termin erstellen, **damit** alle Benutzer der Schule informiert werden.

**Vorbedingungen:** Login als `admin@monteweb.local` (SUPERADMIN).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Im Hauptmenue auf "Kalender" klicken | Kalenderansicht wird angezeigt |
| 2 | Button "Termin erstellen" klicken | Formular oeffnet sich |
| 3 | Feld "Titel" ausfuellen: "Schulfest 2026" | Titel wird angenommen |
| 4 | Feld "Beschreibung" ausfuellen: "Grosses Sommerfest" | Beschreibung wird angenommen |
| 5 | Feld "Ort" ausfuellen: "Schulhof" | Ort wird angenommen |
| 6 | "Sichtbarkeit" auf "Schulweit" setzen | Kein weiteres Dropdown noetig |
| 7 | "Ganztaegig" aktivieren | Start- und Endzeitfelder verschwinden |
| 8 | Startdatum und Enddatum setzen | Felder sind befuellt |
| 9 | Formular absenden | Toast "Termin erstellt" erscheint |

**Akzeptanzkriterien:**
- [ ] Termin ist fuer ALLE Benutzer der Schule sichtbar (SA, SECADMIN, T, P, S)
- [ ] Nur SUPERADMIN kann schulweite Termine erstellen
- [ ] Ganztaegiger Termin zeigt keine Uhrzeit an

---

### US-103: Berechtigungspruefung — Eltern koennen keine Termine erstellen
**Als** System **moechte ich** verhindern, dass Eltern oder Schueler Termine erstellen, **damit** nur autorisierte Rollen Termine anlegen koennen.

**Vorbedingungen:** Login als `eltern@monteweb.local` (PARENT).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Im Hauptmenue auf "Kalender" klicken | Kalenderansicht wird angezeigt |
| 2 | Pruefen, ob Button "Termin erstellen" sichtbar ist | Button ist NICHT sichtbar fuer PARENT |
| 3 | Logout, Login als `schueler@monteweb.local` (STUDENT) | Erfolgreich angemeldet |
| 4 | Im Hauptmenue auf "Kalender" klicken | Kalenderansicht wird angezeigt |
| 5 | Pruefen, ob Button "Termin erstellen" sichtbar ist | Button ist NICHT sichtbar fuer STUDENT |
| 6 | API-Aufruf `POST /api/v1/calendar/events` als PARENT | HTTP 400 oder 403 — Zugriff verweigert |

**Akzeptanzkriterien:**
- [ ] PARENT sieht keinen "Termin erstellen"-Button
- [ ] STUDENT sieht keinen "Termin erstellen"-Button
- [ ] Direkter API-Zugriff wird abgelehnt
- [ ] Nur LEADER (Raum), TEACHER/SECADMIN (Bereich) und SUPERADMIN (Schulweit) koennen erstellen

---

### US-104: RSVP — Teilnahme zusagen/absagen
**Als** Benutzer **moechte ich** meine Teilnahme an einem Termin zu- oder absagen, **damit** der Organisator die Teilnehmerzahl planen kann.

**Vorbedingungen:** Ein Raum-Termin existiert, Benutzer ist Mitglied dieses Raums. Login als `eltern@monteweb.local`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Im Kalender auf einen bestehenden Termin klicken | Termindetails werden angezeigt |
| 2 | RSVP-Button "Zusage" klicken | Status wechselt auf "Zusage", Zaehler "Zusagen" erhoet sich um 1 |
| 3 | RSVP-Button "Vielleicht" klicken | Status wechselt auf "Vielleicht", Zaehler aktualisiert sich |
| 4 | RSVP-Button "Absage" klicken | Status wechselt auf "Absage", Zaehler aktualisiert sich |
| 5 | Erneut "Zusage" klicken | Status wechselt zurueck auf "Zusage" |

**Akzeptanzkriterien:**
- [ ] Jeder Benutzer kann genau einen RSVP-Status pro Termin setzen (Zusage/Vielleicht/Absage)
- [ ] Zaehler fuer Zusagen, Vielleicht und Absagen werden korrekt angezeigt
- [ ] RSVP-Status ist nach Seitenneuladung persistent
- [ ] Alle Rollen (SA, SECADMIN, T, P, S) koennen RSVP abgeben

---

### US-105: Termin absagen mit Feed-Benachrichtigung
**Als** Terminersteller **moechte ich** einen Termin absagen, **damit** alle Teilnehmer automatisch informiert werden.

**Vorbedingungen:** Ein Termin existiert mit mindestens einer RSVP-Zusage. Login als Ersteller des Termins.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Termindetails des eigenen Termins oeffnen | Termin wird angezeigt, Button "Termin absagen" ist sichtbar |
| 2 | Button "Termin absagen" klicken | Bestaetigungsdialog erscheint: "Termin wirklich absagen? Alle Teilnehmer werden benachrichtigt." |
| 3 | Absage bestaetigen | Termin wird als "Abgesagt" markiert |
| 4 | Kalenderansicht pruefen | Termin zeigt Label "Abgesagt" |
| 5 | Feed pruefen (alle betroffenen Benutzer) | Absage-Beitrag erscheint im Feed fuer ALLE Benutzer im Scope |

**Akzeptanzkriterien:**
- [ ] Abgesagter Termin zeigt deutlich "Abgesagt" im Kalender
- [ ] Feed-Post ueber Absage wird fuer alle Benutzer im Termin-Scope erstellt
- [ ] RSVP-Buttons sind nach Absage deaktiviert
- [ ] Nur Ersteller oder SUPERADMIN kann einen Termin absagen

---

### US-106: Termin loeschen — Feed nur fuer Zusager
**Als** Terminersteller **moechte ich** einen Termin loeschen, **damit** er aus dem Kalender entfernt wird und nur Zusager informiert werden.

**Vorbedingungen:** Ein Termin existiert mit RSVP-Zusagen. Login als Ersteller.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Termindetails oeffnen | Termin wird angezeigt |
| 2 | Termin-Loeschen-Button klicken | Bestaetigungsdialog erscheint: "Termin wirklich loeschen?" |
| 3 | Loeschung bestaetigen | Termin wird entfernt, Toast bestaetigt Loeschung |
| 4 | Kalenderansicht pruefen | Termin ist nicht mehr sichtbar |
| 5 | Feed pruefen als Benutzer mit Zusage | Hinweis ueber geloeschten Termin erscheint im Feed |
| 6 | Feed pruefen als Benutzer OHNE Zusage | Kein Feed-Post ueber den geloeschten Termin |

**Akzeptanzkriterien:**
- [ ] Geloeschter Termin ist aus der Kalenderansicht entfernt
- [ ] Feed-Benachrichtigung nur fuer Benutzer mit RSVP-Status "Zusage"
- [ ] Benutzer ohne Zusage erhalten KEINE Feed-Benachrichtigung
- [ ] Nur Ersteller oder SUPERADMIN kann loeschen

---

### US-107: Termin bearbeiten
**Als** Terminersteller **moechte ich** einen bestehenden Termin bearbeiten, **damit** ich Aenderungen an Titel, Beschreibung, Ort oder Zeit vornehmen kann.

**Vorbedingungen:** Ein eigener Termin existiert. Login als Ersteller.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Termindetails oeffnen | Termin wird mit "Termin bearbeiten"-Button angezeigt |
| 2 | Button "Termin bearbeiten" klicken | Bearbeitungsformular oeffnet sich mit vorausgefuellten Feldern |
| 3 | Titel aendern auf "Elternabend (aktualisiert)" | Neuer Titel wird angenommen |
| 4 | Enddatum anpassen | Neues Enddatum wird angenommen |
| 5 | Aenderungen speichern | Toast "Termin aktualisiert" erscheint |
| 6 | Termindetails erneut oeffnen | Aktualisierter Titel und Datum werden angezeigt |

**Akzeptanzkriterien:**
- [ ] Alle Felder (Titel, Beschreibung, Ort, Datum, Zeit, Farbe) sind editierbar
- [ ] Scope (Raum/Bereich/Schule) kann NICHT nachtraeglich geaendert werden
- [ ] Nur Ersteller oder SUPERADMIN kann bearbeiten
- [ ] RSVP-Status der Teilnehmer bleibt nach Bearbeitung erhalten

---

### US-108: Wiederkehrende Termine erstellen
**Als** Raumleiter **moechte ich** einen wiederkehrenden Termin erstellen, **damit** regelmaessige Veranstaltungen automatisch im Kalender erscheinen.

**Vorbedingungen:** Login als `lehrer@monteweb.local` (LEADER in Sonnengruppe).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | "Termin erstellen" klicken | Formular oeffnet sich |
| 2 | Titel "Woechentlicher Morgenkreis" eintragen | Titel wird angenommen |
| 3 | Sichtbarkeit "Raum" waehlen, Raum "Sonnengruppe" | Raum ist selektiert |
| 4 | Startdatum und -zeit setzen (z.B. naechster Montag 08:00) | Felder befuellt |
| 5 | "Wiederholung" auf "Woechentlich" setzen | Feld "Wiederholung bis" erscheint |
| 6 | "Wiederholung bis" auf Datum in 3 Monaten setzen | Enddatum fuer Serie ist gesetzt |
| 7 | Formular absenden | Toast "Termin erstellt" erscheint |
| 8 | Kalenderansicht auf 3-Monats-Ansicht wechseln | Termin erscheint jede Woche bis zum Enddatum |

**Akzeptanzkriterien:**
- [ ] Wiederholungsoptionen: Keine, Taeglich, Woechentlich, Monatlich, Jaehrlich
- [ ] Feld "Wiederholung bis" erscheint nur bei Wiederholung != "Keine"
- [ ] Termine werden korrekt in der Kalenderansicht angezeigt
- [ ] Einzelne Wiederholungen koennen individuell bearbeitet/geloescht werden

---

### US-109: Kalenderansichten wechseln
**Als** Benutzer **moechte ich** zwischen verschiedenen Kalenderansichten wechseln, **damit** ich Termine in unterschiedlichen Zeitraeumen ueberblicken kann.

**Vorbedingungen:** Mehrere Termine existieren in verschiedenen Monaten. Login als beliebiger Benutzer.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Kalender oeffnen | Standard-Ansicht wird angezeigt |
| 2 | Auf "1 Monat" klicken | Monatsansicht wird angezeigt |
| 3 | Auf "3 Monate" klicken | 3-Monats-Ansicht wird angezeigt |
| 4 | Auf "Schuljahr" klicken | Jahresansicht wird angezeigt |
| 5 | Auf "Liste" klicken | Listenansicht mit Terminen wird angezeigt |
| 6 | Button "Heute" klicken | Ansicht springt auf das aktuelle Datum |

**Akzeptanzkriterien:**
- [ ] Vier Ansichten verfuegbar: Liste, 1 Monat, 3 Monate, Schuljahr
- [ ] "Heute"-Button navigiert zum aktuellen Datum
- [ ] Feiertage werden rot markiert angezeigt
- [ ] Schulferien werden orange markiert angezeigt

---

### US-110: Termin als .ics exportieren
**Als** Benutzer **moechte ich** einen einzelnen Termin als .ics-Datei exportieren, **damit** ich ihn in meinen persoenlichen Kalender importieren kann.

**Vorbedingungen:** Ein Termin existiert, auf den der Benutzer Zugriff hat. Login als beliebiger Benutzer.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Termindetails eines bestehenden Termins oeffnen | Termindetails werden angezeigt |
| 2 | Button "Als .ics exportieren" klicken | .ics-Datei wird heruntergeladen |
| 3 | Heruntergeladene Datei pruefen | Datei hat Endung `.ics` und enthaelt korrektes iCalendar-Format |
| 4 | Datei in externem Kalender (z.B. Outlook, Google Calendar) importieren | Termin wird korrekt angezeigt mit Titel, Ort, Datum/Zeit |

**Akzeptanzkriterien:**
- [ ] Export-Button ist fuer alle Benutzer sichtbar, die den Termin sehen koennen
- [ ] .ics-Datei enthaelt VEVENT mit SUMMARY, DTSTART, DTEND, LOCATION, DESCRIPTION
- [ ] Content-Type der Antwort ist `text/calendar`
- [ ] Content-Disposition enthaelt `attachment; filename="event.ics"`

---

### US-111: iCal-Abonnement erstellen (SUPERADMIN)
**Als** Superadmin **moechte ich** ein externes iCal-Abonnement hinzufuegen, **damit** externe Kalender automatisch in den Schulkalender importiert werden.

**Vorbedingungen:** Login als `admin@monteweb.local` (SUPERADMIN).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Admin-Bereich oeffnen, "iCal-Abonnements" navigieren | Seite "iCal-Abonnements" wird angezeigt |
| 2 | Button "Abonnement hinzufuegen" klicken | Formular oeffnet sich |
| 3 | Feld "Name" ausfuellen: "Schulferien Bayern" | Name wird angenommen |
| 4 | Feld "URL" ausfuellen mit gueltiger .ics-URL | URL wird angenommen |
| 5 | Optional: Farbe auswaehlen | Farbfeld wird gesetzt |
| 6 | Formular absenden | Toast "Abonnement erstellt" erscheint, Abonnement in Liste sichtbar |
| 7 | Button "Jetzt synchronisieren" klicken | Toast "Synchronisierung gestartet" erscheint |
| 8 | Kalenderansicht oeffnen, "Importierte Termine anzeigen" aktivieren | Importierte Termine werden in der gewaehlten Farbe angezeigt |

**Akzeptanzkriterien:**
- [ ] iCal-Abonnements sind nur fuer SUPERADMIN verwaltbar
- [ ] Importierte Termine werden mit eigener Farbe dargestellt
- [ ] Letzte Synchronisierung wird mit Zeitstempel angezeigt
- [ ] Abonnement kann geloescht werden (Bestaetigungsdialog)

---

### US-112: iCal-Abonnement loeschen
**Als** Superadmin **moechte ich** ein iCal-Abonnement loeschen, **damit** nicht mehr benoetigte externe Kalender entfernt werden.

**Vorbedingungen:** Mindestens ein iCal-Abonnement existiert. Login als `admin@monteweb.local`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Admin-Bereich oeffnen, "iCal-Abonnements" navigieren | Liste der Abonnements wird angezeigt |
| 2 | Loeschen-Button neben einem Abonnement klicken | Bestaetigungsdialog: "Abonnement wirklich loeschen?" |
| 3 | Loeschung bestaetigen | Toast "Abonnement geloescht", Eintrag verschwindet aus der Liste |
| 4 | Kalenderansicht pruefen | Importierte Termine des geloeschten Abonnements sind entfernt |

**Akzeptanzkriterien:**
- [ ] Bestaetigungsdialog vor Loeschung
- [ ] Zugehoerige importierte Termine werden mitgeloescht
- [ ] Nur SUPERADMIN kann Abonnements loeschen

---

### US-113: Jitsi-Videokonferenz zu Termin hinzufuegen
**Als** Terminersteller **moechte ich** eine Jitsi-Videokonferenz zu meinem Termin hinzufuegen, **damit** Teilnehmer online beitreten koennen.

**Vorbedingungen:** Jitsi-Modul ist aktiviert (Admin > Module > Jitsi). Ein eigener Termin existiert. Login als Ersteller.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Termindetails des eigenen Termins oeffnen | Termindetails werden angezeigt |
| 2 | Button "Videokonferenz hinzufuegen" klicken | Toast "Link wird erstellt...", Jitsi-Link wird generiert |
| 3 | Termindetails nach Generierung pruefen | "Videokonferenz"-Bereich mit Link "Besprechung beitreten" wird angezeigt |
| 4 | Link "Besprechung beitreten" klicken | Neues Fenster/Tab oeffnet sich mit Jitsi-Meeting |
| 5 | Button "Videokonferenz entfernen" klicken | Jitsi-Link wird entfernt, Button aendert sich zurueck zu "Videokonferenz hinzufuegen" |

**Akzeptanzkriterien:**
- [ ] Jitsi-Button nur sichtbar, wenn Jitsi-Modul aktiviert ist
- [ ] Generierter Link ist ein gueltiger Jitsi-Raum-Name
- [ ] Nur Terminersteller oder SUPERADMIN kann Videokonferenz hinzufuegen/entfernen
- [ ] Alle Teilnehmer sehen den Jitsi-Link in den Termindetails

---

### US-114: Jitsi-Modul deaktiviert — kein Videokonferenz-Button
**Als** System **moechte ich** den Videokonferenz-Button ausblenden, wenn Jitsi deaktiviert ist, **damit** keine nicht verfuegbaren Funktionen angezeigt werden.

**Vorbedingungen:** Jitsi-Modul ist deaktiviert. Login als `admin@monteweb.local`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Admin > Module > Jitsi deaktivieren | Modul wird deaktiviert |
| 2 | Kalender oeffnen, Termindetails eines Termins oeffnen | Termindetails werden angezeigt |
| 3 | Pruefen, ob "Videokonferenz hinzufuegen"-Button sichtbar ist | Button ist NICHT sichtbar |
| 4 | API-Aufruf `POST /api/v1/calendar/events/{id}/jitsi` | Fehlermeldung — Modul deaktiviert |

**Akzeptanzkriterien:**
- [ ] Kein Jitsi-UI-Element sichtbar, wenn Modul deaktiviert
- [ ] API lehnt Jitsi-Anfragen ab, wenn Modul deaktiviert
- [ ] Bereits vorhandene Jitsi-Links bleiben sichtbar, aber "Entfernen"-Button entfaellt

---

### US-115: Farbmarkierung fuer Termine
**Als** Terminersteller **moechte ich** meinem Termin eine Farbe zuweisen, **damit** er im Kalender visuell hervorgehoben wird.

**Vorbedingungen:** Login als Benutzer mit Terminberechtigungen.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | "Termin erstellen" klicken | Formular oeffnet sich |
| 2 | Titel und Pflichtfelder ausfuellen | Felder sind befuellt |
| 3 | Feld "Farbe" — vordefinierte Farbe auswaehlen | Farbe ist selektiert |
| 4 | Alternativ: "Eigene Farbe" waehlen und Hex-Wert eingeben | Eigene Farbe ist gesetzt |
| 5 | Termin speichern | Termin wird erstellt |
| 6 | Kalenderansicht pruefen | Termin wird in der gewaehlten Farbe dargestellt |

**Akzeptanzkriterien:**
- [ ] Vordefinierte Farbauswahl vorhanden
- [ ] Eigene Farbe per Farbwaehler moeglich
- [ ] Farbe wird in allen Kalenderansichten korrekt dargestellt
- [ ] Farbe kann nachtraeglich beim Bearbeiten geaendert werden

---

### US-116: Kalender mit Putzaktionen und Jobs
**Als** Benutzer **moechte ich** Putzaktionen und offene Jobs im Kalender ein-/ausblenden, **damit** ich eine vollstaendige Uebersicht ueber alle Termine habe.

**Vorbedingungen:** Putzaktionen und Jobs existieren. Login als beliebiger Benutzer.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Kalender oeffnen | Kalenderansicht wird angezeigt |
| 2 | Toggle "Putzaktionen anzeigen" aktivieren | Putzaktionstermine werden im Kalender eingeblendet |
| 3 | Toggle "Putzaktionen anzeigen" deaktivieren | Putzaktionstermine verschwinden |
| 4 | Toggle "Offene Jobs anzeigen" aktivieren | Jobs werden im Kalender eingeblendet |
| 5 | Toggle "Offene Jobs anzeigen" deaktivieren | Jobs verschwinden |

**Akzeptanzkriterien:**
- [ ] Putzaktionen werden mit speziellem Label "Putzaktion" dargestellt
- [ ] Jobs werden mit Label im Kalender dargestellt
- [ ] Toggles sind persistent (bleiben nach Seitenneuladen erhalten)
- [ ] Putzaktionen und Jobs sind visuell von normalen Terminen unterscheidbar

---

### US-117: Gesamtkalender exportieren
**Als** Benutzer **moechte ich** meinen gesamten Kalender als .ics-Datei exportieren, **damit** ich alle Termine in einem externen Kalender abonnieren kann.

**Vorbedingungen:** Mehrere Termine existieren. Login als beliebiger Benutzer.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Kalender oeffnen | Kalenderansicht wird angezeigt |
| 2 | Button "Exportieren" klicken | .ics-Datei mit allen sichtbaren Terminen wird heruntergeladen |
| 3 | Datei pruefen | Datei enthaelt VCALENDAR mit allen Terminen als VEVENT-Eintraege |

**Akzeptanzkriterien:**
- [ ] Export enthaelt nur Termine, die der Benutzer sehen darf
- [ ] Alle Termin-Details (Titel, Beschreibung, Ort, Datum) sind in der Exportdatei
- [ ] Dateiformat ist valides iCalendar (RFC 5545)

---

## Modul: Nachrichten/Messaging

### US-120: Direktnachricht starten
**Als** Benutzer **moechte ich** eine Direktnachricht an einen anderen Benutzer senden, **damit** ich privat kommunizieren kann.

**Vorbedingungen:** Messaging-Modul ist aktiviert. Login als `lehrer@monteweb.local` (TEACHER).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Im Hauptmenue auf "Nachrichten" klicken | Nachrichtenansicht wird angezeigt |
| 2 | Button "Neue Konversation" klicken | Dialog oeffnet sich |
| 3 | Im Feld "Empfaenger suchen" den Namen "eltern" eingeben | Suchergebnisse zeigen passende Benutzer |
| 4 | Benutzer `eltern@monteweb.local` auswaehlen | Benutzer ist als Empfaenger markiert |
| 5 | Button "Konversation starten" klicken | Konversation wird geoeffnet |
| 6 | Im Feld "Nachricht schreiben..." Text eingeben: "Hallo!" | Text ist im Eingabefeld |
| 7 | Senden-Button klicken oder Enter druecken | Nachricht erscheint im Chatverlauf |

**Akzeptanzkriterien:**
- [ ] Konversation erscheint in der Konversationsliste beider Teilnehmer
- [ ] Nachricht zeigt Absendername, Inhalt und Zeitstempel
- [ ] Ungelesene Nachrichten werden mit Badge/Zaehler angezeigt
- [ ] Lehrer kann an Eltern schreiben (immer erlaubt)

---

### US-121: Gruppenchat erstellen
**Als** Benutzer **moechte ich** einen Gruppenchat mit mehreren Teilnehmern erstellen, **damit** wir gemeinsam kommunizieren koennen.

**Vorbedingungen:** Login als `lehrer@monteweb.local`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | "Nachrichten" oeffnen, "Neue Konversation" klicken | Dialog oeffnet sich |
| 2 | Checkbox "Gruppennachricht" aktivieren | Feld "Gruppenname" erscheint |
| 3 | "Gruppenname" ausfuellen: "Lehrerteam Kinderhaus" | Name wird angenommen |
| 4 | Mehrere Empfaenger suchen und hinzufuegen (mind. 2) | Empfaenger werden in der Liste angezeigt |
| 5 | "Konversation starten" klicken | Gruppenchat wird geoeffnet |
| 6 | Nachricht senden: "Willkommen in der Gruppe!" | Nachricht erscheint fuer alle Teilnehmer |

**Akzeptanzkriterien:**
- [ ] Gruppenname wird in der Konversationsliste angezeigt
- [ ] Alle Teilnehmer sehen die Konversation und empfangen Nachrichten
- [ ] Gruppenchat ist als Gruppe markiert (isGroup = true)
- [ ] Mindestens 2 Empfaenger muessen ausgewaehlt werden fuer Gruppenkonversation

---

### US-122: Bild in Nachricht senden
**Als** Benutzer **moechte ich** ein Bild in einer Nachricht senden, **damit** ich visuelle Inhalte teilen kann.

**Vorbedingungen:** Eine Konversation existiert. Login als beliebiger Benutzer.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Bestehende Konversation oeffnen | Chatverlauf wird angezeigt |
| 2 | Button "Bild anhaengen" klicken | Dateiauswahl-Dialog oeffnet sich |
| 3 | Ein Bild auswaehlen (JPEG, PNG, WebP oder GIF) | Bild wird als Vorschau angezeigt |
| 4 | Optional: Text dazu schreiben | Text ist im Eingabefeld |
| 5 | Nachricht senden | Nachricht mit Bild erscheint im Chatverlauf |
| 6 | Auf das Bild klicken | Bild wird in voller Groesse angezeigt |

**Akzeptanzkriterien:**
- [ ] Unterstuetzte Formate: JPEG, PNG, WebP, GIF
- [ ] Maximale Dateigroesse: 10 MB
- [ ] Thumbnail wird im Chat angezeigt, Klick zeigt Originalbild
- [ ] Nachricht kann nur Bild (ohne Text) enthalten
- [ ] Fehlermeldung bei ungueltigem Format: "Ungueltiges Bildformat. Erlaubt: JPEG, PNG, WebP, GIF"
- [ ] Fehlermeldung bei zu grosser Datei: "Datei ist zu gross. Maximal 10 MB erlaubt."

---

### US-123: Auf Nachricht antworten (Reply-Threading)
**Als** Benutzer **moechte ich** auf eine bestimmte Nachricht antworten, **damit** der Kontext der Antwort klar ist.

**Vorbedingungen:** Eine Konversation mit mehreren Nachrichten existiert. Login als beliebiger Benutzer.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Konversation oeffnen | Chatverlauf wird angezeigt |
| 2 | Auf "Antworten" bei einer bestimmten Nachricht klicken | Reply-Vorschau erscheint ueber dem Eingabefeld mit Absender und Textvorschau |
| 3 | Antworttext eingeben: "Stimme zu!" | Text ist im Eingabefeld |
| 4 | Nachricht senden | Antwort erscheint mit Verweis auf die Originalnachricht |
| 5 | Originalnachricht loeschen (falls moeglich) | reply_to wird auf NULL gesetzt, Antwort bleibt erhalten |

**Akzeptanzkriterien:**
- [ ] Reply-Vorschau zeigt Absendername und gekuerzten Inhalt der Originalnachricht
- [ ] Antwort-Nachricht zeigt visuell den Bezug zur Originalnachricht
- [ ] Bei Antwort auf Bild-Nachricht wird "hat ein Bild" als Vorschau angezeigt
- [ ] Loeschung der Originalnachricht entfernt den Verweis (ON DELETE SET NULL), Antwort bleibt

---

### US-124: Reaktionen auf Nachrichten
**Als** Benutzer **moechte ich** auf eine Nachricht mit einem Emoji reagieren, **damit** ich schnell Feedback geben kann.

**Vorbedingungen:** Eine Konversation mit Nachrichten existiert. Login als beliebiger Benutzer.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Konversation oeffnen | Chatverlauf wird angezeigt |
| 2 | Auf eine Nachricht hovern/lange druecken | Reaktions-Optionen erscheinen |
| 3 | Ein Emoji auswaehlen (z.B. Daumen hoch) | Reaktion wird unter der Nachricht angezeigt mit Zaehler "1" |
| 4 | Gleiche Reaktion nochmals klicken (eigene) | Reaktion wird entfernt (Toggle-Verhalten) |
| 5 | Reaktion erneut hinzufuegen, anderer Benutzer reagiert ebenfalls | Zaehler zeigt "2", eigene Reaktion ist hervorgehoben |

**Akzeptanzkriterien:**
- [ ] Mehrere Emoji-Reaktionen pro Nachricht moeglich
- [ ] Jeder Benutzer kann pro Emoji nur einmal reagieren (Toggle)
- [ ] Zaehler zeigt Gesamtanzahl der Reaktionen pro Emoji
- [ ] Eigene Reaktionen sind visuell hervorgehoben (userReacted)

---

### US-125: Konversation stummschalten
**Als** Benutzer **moechte ich** eine Konversation stummschalten, **damit** ich keine Benachrichtigungen mehr erhalte.

**Vorbedingungen:** Mindestens eine Konversation existiert. Login als beliebiger Benutzer.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Konversation oeffnen | Chatverlauf wird angezeigt |
| 2 | Button "Stummschalten" klicken | Toast "Stumm" erscheint, Icon zeigt Stummschaltung an |
| 3 | Neue Nachricht in dieser Konversation erhalten (durch anderen Benutzer) | Keine Push-Benachrichtigung, aber Nachricht erscheint im Chat |
| 4 | Button "Stummschaltung aufheben" klicken | Toast "Stummschaltung aufgehoben", Icon verschwindet |
| 5 | Profilseite oeffnen | Liste stummgeschalteter Chats zeigt ggf. stummgeschaltete Konversationen |
| 6 | "Stummschaltung aufheben"-Button auf der Profilseite klicken | Stummschaltung wird aufgehoben |

**Akzeptanzkriterien:**
- [ ] Stummgeschaltete Konversation zeigt "Stumm"-Indikator in der Liste
- [ ] Keine Push-Benachrichtigungen fuer stummgeschaltete Konversationen
- [ ] Nachrichten werden weiterhin empfangen und im Chat angezeigt
- [ ] Profilseite zeigt alle stummgeschalteten Chats mit Unmute-Buttons
- [ ] Stummschaltung ist fuer DMs und Gruppenchats moeglich

---

### US-126: Kommunikationsregeln — Lehrer-Eltern immer erlaubt
**Als** System **moechte ich** sicherstellen, dass Lehrer und Eltern immer kommunizieren duerfen, **damit** die paedagogische Kommunikation nicht eingeschraenkt wird.

**Vorbedingungen:** Login als `lehrer@monteweb.local` (TEACHER).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | "Nachrichten" oeffnen, "Neue Konversation" klicken | Dialog oeffnet sich |
| 2 | Elternbenutzer suchen und auswaehlen | Benutzer ist selektiert |
| 3 | "Konversation starten" klicken | Konversation wird erfolgreich erstellt |
| 4 | Nachricht senden | Nachricht wird zugestellt |
| 5 | Logout, Login als `eltern@monteweb.local` | Erfolgreich angemeldet |
| 6 | "Nachrichten" oeffnen, "Neue Konversation" zu einem Lehrer starten | Konversation wird erfolgreich erstellt |

**Akzeptanzkriterien:**
- [ ] Lehrer kann immer an Eltern schreiben (unabhaengig von Kommunikationsregeln)
- [ ] Eltern kann immer an Lehrer schreiben
- [ ] SUPERADMIN, SECTION_ADMIN und TEACHER gelten als "Staff" und koennen an alle schreiben
- [ ] Diese Regel kann NICHT deaktiviert werden

---

### US-127: Kommunikationsregeln — Eltern-zu-Eltern konfigurierbar
**Als** Superadmin **moechte ich** die Eltern-zu-Eltern-Kommunikation ein-/ausschalten, **damit** ich die Kommunikationsregeln der Schule steuern kann.

**Vorbedingungen:** Login als `admin@monteweb.local` (SUPERADMIN).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Admin > Einstellungen > Kommunikation oeffnen | Einstellungsbereich wird angezeigt |
| 2 | Toggle "Nachrichten zwischen Eltern" DEAKTIVIEREN | Toggle ist aus |
| 3 | Einstellungen speichern | Toast "Einstellungen gespeichert" |
| 4 | Logout, Login als `eltern@monteweb.local` | Erfolgreich angemeldet |
| 5 | "Neue Konversation" klicken, anderen Elternbenutzer suchen und auswaehlen | Benutzer gefunden |
| 6 | "Konversation starten" klicken | Fehlermeldung: "Kommunikation zwischen diesen Nutzern ist nicht erlaubt" |
| 7 | Logout, Login als Admin, Toggle AKTIVIEREN, speichern | Einstellung gespeichert |
| 8 | Logout, Login als Eltern, erneut Konversation mit anderem Eltern starten | Konversation wird erfolgreich erstellt |

**Akzeptanzkriterien:**
- [ ] Standard: Eltern-zu-Eltern-Kommunikation ist DEAKTIVIERT
- [ ] Admin kann die Einstellung jederzeit aendern
- [ ] Bei deaktivierter Einstellung: Fehlermeldung fuer Eltern beim Versuch
- [ ] Bestehende Konversationen werden durch Aenderung NICHT betroffen

---

### US-128: Kommunikationsregeln — Schueler-zu-Schueler konfigurierbar
**Als** Superadmin **moechte ich** die Schueler-zu-Schueler-Kommunikation ein-/ausschalten, **damit** ich altersgerechte Kommunikation sicherstellen kann.

**Vorbedingungen:** Login als `admin@monteweb.local`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Admin > Einstellungen > Kommunikation oeffnen | Einstellungsbereich wird angezeigt |
| 2 | Toggle "Nachrichten zwischen Schueler:innen" DEAKTIVIEREN | Toggle ist aus |
| 3 | Einstellungen speichern | Toast "Einstellungen gespeichert" |
| 4 | Logout, Login als `schueler@monteweb.local` | Erfolgreich angemeldet |
| 5 | "Neue Konversation" zu anderem Schueler starten | Fehlermeldung: Kommunikation nicht erlaubt |
| 6 | Konversation zu Lehrer starten | Konversation wird erfolgreich erstellt (Schueler-Lehrer immer erlaubt) |

**Akzeptanzkriterien:**
- [ ] Standard: Schueler-zu-Schueler-Kommunikation ist konfigurierbar
- [ ] Schueler koennen immer an Lehrkraefte schreiben
- [ ] Eltern-Schueler-Kommunikation (ausserhalb Staff) ist standardmaessig gesperrt

---

### US-129: Ungelesene Nachrichten und Zaehler
**Als** Benutzer **moechte ich** die Anzahl ungelesener Nachrichten sehen, **damit** ich weiss, ob neue Nachrichten vorliegen.

**Vorbedingungen:** Login als Benutzer mit mindestens einer Konversation.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Ein anderer Benutzer sendet eine Nachricht | Ungelesene-Zaehler im Navigationsmenue erhoet sich |
| 2 | Konversationsliste oeffnen | Konversation mit ungelesener Nachricht zeigt Badge mit Anzahl |
| 3 | Konversation oeffnen und Nachricht lesen | Badge der Konversation verschwindet |
| 4 | Gesamt-Zaehler im Menue pruefen | Gesamt-Zaehler hat sich verringert |

**Akzeptanzkriterien:**
- [ ] Ungelesen-Badge in der Hauptnavigation zeigt Gesamtanzahl
- [ ] Jede Konversation zeigt individuellen Ungelesen-Zaehler
- [ ] Oeffnen einer Konversation markiert alle Nachrichten als gelesen
- [ ] Zaehler aktualisiert sich in Echtzeit (WebSocket)

---

### US-130: WebSocket-Echtzeitnachrichten
**Als** Benutzer **moechte ich** neue Nachrichten in Echtzeit empfangen, **damit** ich ohne Seitenneuladen kommunizieren kann.

**Vorbedingungen:** Zwei Benutzer sind gleichzeitig eingeloggt und haben eine gemeinsame Konversation.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Benutzer A oeffnet Konversation mit Benutzer B | Chatverlauf wird angezeigt |
| 2 | Benutzer B oeffnet dieselbe Konversation | Chatverlauf wird angezeigt |
| 3 | Benutzer A sendet Nachricht: "Hallo B!" | Nachricht erscheint sofort bei Benutzer A |
| 4 | Benutzer B pruefen | Nachricht "Hallo B!" erscheint sofort bei B ohne Seitenneuladen |
| 5 | Benutzer B antwortet: "Hallo A!" | Nachricht erscheint sofort bei beiden Benutzern |

**Akzeptanzkriterien:**
- [ ] Nachrichten erscheinen in Echtzeit ohne Seitenneuladen
- [ ] WebSocket-Verbindung ueber `/ws/messages`
- [ ] Bei Verbindungsabbruch: automatische Wiederverbindung
- [ ] Neue Nachrichten in nicht-geoeffneten Konversationen aktualisieren den Ungelesen-Zaehler

---

### US-131: Konversation loeschen
**Als** Benutzer **moechte ich** eine Konversation aus meiner Liste entfernen, **damit** ich meine Nachrichtenueebersicht aufgeraeumt halten kann.

**Vorbedingungen:** Mindestens eine Konversation existiert. Login als beliebiger Benutzer.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Konversationsliste oeffnen | Konversationen werden angezeigt |
| 2 | Loeschen-Option fuer eine Konversation waehlen | Bestaetigungsdialog: "Moechten Sie diese Konversation wirklich loeschen? Sie wird aus Ihrer Liste entfernt." |
| 3 | Loeschung bestaetigen | Konversation verschwindet aus der eigenen Liste |
| 4 | Pruefen: anderer Teilnehmer sieht Konversation weiterhin | Konversation ist beim anderen Teilnehmer noch sichtbar |

**Akzeptanzkriterien:**
- [ ] Loeschung entfernt Konversation nur fuer den aktuellen Benutzer
- [ ] Andere Teilnehmer behalten die Konversation
- [ ] Bestaetigungsdialog vor Loeschung
- [ ] Geloeschte Konversation kann nicht wiederhergestellt werden

---

### US-132: Datei-Anhaenge in Nachrichten
**Als** Benutzer **moechte ich** PDF-Dateien an Nachrichten anhaengen oder Raum-Dateien verlinken, **damit** ich Dokumente teilen kann.

**Vorbedingungen:** Eine Konversation existiert. Login als beliebiger Benutzer.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Konversation oeffnen | Chatverlauf wird angezeigt |
| 2 | Button "PDF anhaengen" klicken | Dateiauswahl-Dialog oeffnet sich |
| 3 | PDF-Datei auswaehlen | Datei wird als Anhang markiert |
| 4 | Nachricht senden | Nachricht mit Datei-Anhang erscheint im Chat |
| 5 | Auf den Anhang klicken | Datei wird heruntergeladen |
| 6 | Button "Datei verlinken" klicken | Dialog "Raum auswaehlen" oeffnet sich |
| 7 | Raum und Datei auswaehlen | Datei-Link wird in Nachricht eingebettet |

**Akzeptanzkriterien:**
- [ ] PDF-Dateien koennen direkt angehangen werden
- [ ] Raum-Dateien koennen verlinkt werden (FILE_LINK-Typ)
- [ ] Anhang zeigt Dateiname, Dateityp und Groesse
- [ ] Download des Anhangs funktioniert fuer alle Konversationsteilnehmer

---

### US-133: Messaging-Modul deaktiviert
**Als** System **moechte ich** das Messaging-Modul komplett ausblenden koennen, **damit** die Schule das Feature bei Bedarf abschalten kann.

**Vorbedingungen:** Login als `admin@monteweb.local`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Admin > Module > Messaging deaktivieren | Modul wird deaktiviert |
| 2 | Logout, Login als `lehrer@monteweb.local` | Erfolgreich angemeldet |
| 3 | Hauptnavigation pruefen | Menue-Punkt "Nachrichten" ist NICHT sichtbar |
| 4 | URL `/nachrichten` direkt aufrufen | Weiterleitung auf Dashboard oder 404-Seite |
| 5 | API-Aufruf `GET /api/v1/messages/conversations` | HTTP 404 oder 503 |

**Akzeptanzkriterien:**
- [ ] Bei deaktiviertem Modul: kein Menue-Eintrag "Nachrichten"
- [ ] Direkter URL-Zugriff wird abgefangen
- [ ] API-Endpunkte sind nicht erreichbar
- [ ] Reaktivierung stellt alle Konversationen wieder her

---

### US-134: Umfrage in Nachricht erstellen (Poll)
**Als** Benutzer **moechte ich** eine Umfrage in einer Konversation erstellen, **damit** ich schnell Meinungen einholen kann.

**Vorbedingungen:** Eine Konversation existiert. Login als beliebiger Benutzer.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Konversation oeffnen | Chatverlauf wird angezeigt |
| 2 | Umfrage-Erstellungsoption waehlen | Umfrage-Formular oeffnet sich |
| 3 | Frage eingeben: "Wann soll das Treffen stattfinden?" | Frage wird angenommen |
| 4 | Optionen hinzufuegen: "Montag", "Dienstag", "Mittwoch" | Optionen werden angezeigt |
| 5 | Optional: Mehrfachauswahl aktivieren | Checkbox "Mehrfachauswahl" ist aktiv |
| 6 | Umfrage senden | Umfrage-Nachricht erscheint im Chat mit abstimmbaren Optionen |
| 7 | Auf "Montag" abstimmen | Stimme wird gezaehlt, Balken aktualisiert sich |
| 8 | Ersteller schliesst die Umfrage | Umfrage wird als "geschlossen" markiert, keine weiteren Stimmen moeglich |

**Akzeptanzkriterien:**
- [ ] Frage und mindestens 2 Optionen sind Pflicht
- [ ] Mehrfachauswahl kann aktiviert/deaktiviert werden
- [ ] Jeder Teilnehmer kann abstimmen (erneute Abstimmung aendert die Stimme)
- [ ] Stimmenanzahl und prozentuale Verteilung werden angezeigt
- [ ] Nur Ersteller kann die Umfrage schliessen

---

## Modul: Dateien

### US-140: Datei in Raum hochladen
**Als** Raummitglied **moechte ich** eine Datei in den Raum hochladen, **damit** ich Materialien mit anderen Mitgliedern teilen kann.

**Vorbedingungen:** Benutzer ist Mitglied eines Raums. Login als `lehrer@monteweb.local`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Raumdetails oeffnen | Raumansicht wird angezeigt |
| 2 | Tab/Bereich "Dateien" oeffnen | Dateibereich zeigt "Noch keine Dateien vorhanden" oder vorhandene Dateien |
| 3 | Button "Hochladen" klicken | Dateiauswahl-Dialog oeffnet sich |
| 4 | Eine Datei auswaehlen (z.B. PDF, DOCX, Bild) | Datei wird hochgeladen |
| 5 | Upload-Fortschritt beobachten | Fortschrittsanzeige oder sofortige Rueckmeldung |
| 6 | Upload abgeschlossen | Datei erscheint in der Dateiliste mit Name, Groesse, Hochlade-Datum |

**Akzeptanzkriterien:**
- [ ] Datei wird in MinIO gespeichert
- [ ] Dateiliste zeigt: Dateiname, Dateityp (Content-Type), Groesse, Hochgeladen von, Datum
- [ ] Alle Raummitglieder koennen die Datei sehen (bei Audience "Alle")
- [ ] Maximale Dateigroesse wird eingehalten (konfigurierbar in tenant_config.max_upload_size_mb, Standard 50 MB)

---

### US-141: Datei herunterladen
**Als** Raummitglied **moechte ich** eine hochgeladene Datei herunterladen, **damit** ich auf geteilte Materialien zugreifen kann.

**Vorbedingungen:** Mindestens eine Datei ist im Raum hochgeladen. Login als beliebiges Raummitglied.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Raumdetails oeffnen, Tab "Dateien" | Dateiliste wird angezeigt |
| 2 | Auf eine Datei klicken oder Download-Button druecken | Datei wird heruntergeladen |
| 3 | Heruntergeladene Datei pruefen | Datei ist intakt und hat den korrekten Inhalt |

**Akzeptanzkriterien:**
- [ ] Download liefert die Originaldatei mit korrektem Content-Type
- [ ] Dateiname im Download entspricht dem Originalnamen
- [ ] Nur Raummitglieder koennen Dateien herunterladen
- [ ] Nicht-Mitglieder erhalten HTTP 403

---

### US-142: Ordner erstellen und verwalten
**Als** Raummitglied **moechte ich** Ordner erstellen, **damit** ich Dateien organisieren kann.

**Vorbedingungen:** Benutzer ist Mitglied eines Raums. Login als `lehrer@monteweb.local`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Raumdetails oeffnen, Tab "Dateien" | Dateibereich wird angezeigt |
| 2 | Button "Neuer Ordner" klicken | Dialog mit Feld "Ordnername" und "Sichtbarkeit" oeffnet sich |
| 3 | Ordnername eingeben: "Elternbriefe" | Name wird angenommen |
| 4 | Sichtbarkeit auf "Alle" belassen | Sichtbarkeit ist "Alle" |
| 5 | Ordner erstellen | Ordner erscheint in der Dateistruktur |
| 6 | In den Ordner navigieren | Leerer Ordner wird angezeigt |
| 7 | Datei in den Ordner hochladen | Datei erscheint im Ordner |

**Akzeptanzkriterien:**
- [ ] Ordner koennen in der Root-Ebene und als Unterordner erstellt werden
- [ ] Ordnername ist Pflichtfeld
- [ ] Ordner zeigt Sichtbarkeit (Alle/Nur Eltern/Nur Schueler)
- [ ] KLASSE-Raeume erhalten automatisch einen Standard-Ordner bei Erstellung

---

### US-143: Ordner-Audience — Nur Eltern
**Als** Lehrkraft **moechte ich** einen Ordner erstellen, der nur fuer Eltern sichtbar ist, **damit** vertrauliche Elterninfos nicht fuer Schueler zugaenglich sind.

**Vorbedingungen:** Login als `lehrer@monteweb.local` (LEADER in Raum mit Eltern und Schuelern).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Tab "Dateien" im Raum oeffnen | Dateibereich wird angezeigt |
| 2 | "Neuer Ordner" klicken | Dialog oeffnet sich |
| 3 | Ordnername: "Vertraulich — Eltern", Sichtbarkeit: "Nur Eltern" | Felder befuellt |
| 4 | Ordner erstellen | Ordner mit Label "Nur Eltern" erscheint |
| 5 | Datei in diesen Ordner hochladen | Datei wird hochgeladen |
| 6 | Logout, Login als `schueler@monteweb.local` | Erfolgreich angemeldet |
| 7 | Denselben Raum oeffnen, Tab "Dateien" | Ordner "Vertraulich — Eltern" ist NICHT sichtbar |
| 8 | Logout, Login als `eltern@monteweb.local` | Erfolgreich angemeldet |
| 9 | Raum oeffnen, Tab "Dateien" | Ordner "Vertraulich — Eltern" ist sichtbar |

**Akzeptanzkriterien:**
- [ ] Ordner mit Audience "Nur Eltern" ist fuer STUDENT nicht sichtbar
- [ ] Ordner ist fuer PARENT, TEACHER, LEADER und SUPERADMIN sichtbar
- [ ] Dateien innerhalb des Ordners erben die Sichtbarkeit
- [ ] API-Zugriff auf geschuetzte Ordner durch Schueler wird abgelehnt

---

### US-144: Ordner-Audience — Nur Schueler
**Als** Lehrkraft **moechte ich** einen Ordner erstellen, der nur fuer Schueler sichtbar ist, **damit** Schueler-spezifische Materialien getrennt abgelegt werden.

**Vorbedingungen:** Login als `lehrer@monteweb.local`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Tab "Dateien" im Raum oeffnen, "Neuer Ordner" klicken | Dialog oeffnet sich |
| 2 | Ordnername: "Aufgaben", Sichtbarkeit: "Nur Schueler" | Felder befuellt |
| 3 | Ordner erstellen | Ordner mit Label "Nur Schueler" erscheint |
| 4 | Logout, Login als `eltern@monteweb.local` | Erfolgreich angemeldet |
| 5 | Raum oeffnen, Tab "Dateien" | Ordner "Aufgaben" ist NICHT sichtbar fuer Eltern |
| 6 | Logout, Login als `schueler@monteweb.local` | Erfolgreich angemeldet |
| 7 | Raum oeffnen, Tab "Dateien" | Ordner "Aufgaben" IST sichtbar fuer Schueler |

**Akzeptanzkriterien:**
- [ ] Ordner mit Audience "Nur Schueler" ist fuer PARENT nicht sichtbar
- [ ] Ordner ist fuer STUDENT, TEACHER, LEADER und SUPERADMIN sichtbar
- [ ] Eltern-Erstellung von Ordnern setzt Audience automatisch auf "Nur Eltern"

---

### US-145: Datei loeschen
**Als** Raummitglied **moechte ich** eine von mir hochgeladene Datei loeschen, **damit** veraltete oder falsche Dateien entfernt werden.

**Vorbedingungen:** Benutzer hat eine Datei hochgeladen. Login als Uploader.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Tab "Dateien" im Raum oeffnen | Dateiliste wird angezeigt |
| 2 | Loeschen-Button bei der eigenen Datei klicken | Bestaetigungsdialog erscheint |
| 3 | Loeschung bestaetigen | Datei verschwindet aus der Liste |
| 4 | API: `GET /api/v1/rooms/{id}/files/{fileId}` | HTTP 404 — Datei nicht gefunden |

**Akzeptanzkriterien:**
- [ ] Nur der Uploader, LEADER oder SUPERADMIN kann eine Datei loeschen
- [ ] Datei wird aus MinIO und Datenbank entfernt
- [ ] Bestaetigungsdialog vor Loeschung
- [ ] Geloeschte Datei ist sofort nicht mehr downloadbar

---

### US-146: Ordner loeschen
**Als** Raummitglied **moechte ich** einen leeren Ordner loeschen, **damit** die Dateistruktur aufgeraeumt bleibt.

**Vorbedingungen:** Ein leerer Ordner existiert. Login als Ersteller oder LEADER.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Tab "Dateien" im Raum oeffnen | Dateistruktur wird angezeigt |
| 2 | Loeschen-Button bei einem Ordner klicken | Bestaetigungsdialog erscheint |
| 3 | Loeschung bestaetigen | Ordner wird entfernt |
| 4 | Versuch, einen Ordner mit Dateien zu loeschen | Fehlermeldung: Ordner muss leer sein, oder Dateien werden mitgeloescht |

**Akzeptanzkriterien:**
- [ ] Leere Ordner koennen geloescht werden
- [ ] Nur Ersteller, LEADER oder SUPERADMIN kann Ordner loeschen
- [ ] Bestaetigungsdialog vor Loeschung

---

### US-147: WOPI/ONLYOFFICE — Dokument online bearbeiten
**Als** Raummitglied **moechte ich** ein Dokument direkt im Browser bearbeiten, **damit** ich ohne lokale Software Aenderungen vornehmen kann.

**Vorbedingungen:** WOPI-Modul ist aktiviert (Admin > Module > WOPI). ONLYOFFICE Document Server laeuft. Dokument (DOCX, XLSX, PPTX) ist im Raum hochgeladen. Login als Raummitglied.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Tab "Dateien" im Raum oeffnen | Dateiliste wird angezeigt |
| 2 | Bei einem Office-Dokument: Button "Online bearbeiten" klicken | ONLYOFFICE-Editor oeffnet sich im Browser |
| 3 | Aenderungen im Dokument vornehmen | Aenderungen werden im Editor angezeigt |
| 4 | Button "Zurueck zu Dateien" klicken | Rueckkehr zur Dateiliste |
| 5 | Datei herunterladen und pruefen | Aenderungen sind in der heruntergeladenen Datei enthalten |

**Akzeptanzkriterien:**
- [ ] WOPI-Session wird mit gueltigem Token erstellt
- [ ] ONLYOFFICE-Editor laedt das Dokument korrekt
- [ ] Aenderungen werden in MinIO gespeichert
- [ ] Button "Online bearbeiten" nur bei unterstuetzten Dateitypen sichtbar
- [ ] Button nur sichtbar, wenn WOPI-Modul aktiviert ist

---

### US-148: WOPI-Modul deaktiviert — kein Online-Editor
**Als** System **moechte ich** den Online-Editor ausblenden, wenn WOPI deaktiviert ist, **damit** keine nicht verfuegbaren Funktionen angezeigt werden.

**Vorbedingungen:** Login als `admin@monteweb.local`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Admin > Module > WOPI deaktivieren | Modul wird deaktiviert |
| 2 | Raum oeffnen, Tab "Dateien", Office-Dokument suchen | Datei ist sichtbar |
| 3 | Pruefen, ob "Online bearbeiten"-Button sichtbar ist | Button ist NICHT sichtbar |
| 4 | API: `POST /api/v1/rooms/{id}/files/{fileId}/wopi-session` | HTTP 400: "WOPI/ONLYOFFICE is not enabled" |

**Akzeptanzkriterien:**
- [ ] Kein "Online bearbeiten"-Button bei deaktiviertem WOPI
- [ ] API lehnt WOPI-Session-Erstellung ab
- [ ] Dateien koennen weiterhin hoch-/heruntergeladen werden

---

### US-149: ClamAV-Virenscan beim Upload
**Als** System **moechte ich** hochgeladene Dateien auf Viren scannen, **damit** schaedliche Dateien nicht verbreitet werden.

**Vorbedingungen:** ClamAV-Modul ist aktiviert. ClamAV-Server laeuft. Login als beliebiges Raummitglied.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Tab "Dateien" im Raum oeffnen | Dateibereich wird angezeigt |
| 2 | Saubere Datei hochladen | Datei wird nach Scan erfolgreich hochgeladen |
| 3 | Infizierte Test-Datei hochladen (EICAR-Testdatei) | Upload wird abgelehnt, Fehlermeldung: Virus gefunden |
| 4 | Dateiliste pruefen | Infizierte Datei ist NICHT in der Liste |

**Akzeptanzkriterien:**
- [ ] Scan erfolgt automatisch bei jedem Upload (wenn ClamAV aktiviert)
- [ ] Saubere Dateien werden normal hochgeladen
- [ ] Infizierte Dateien werden abgelehnt mit verstaendlicher Fehlermeldung
- [ ] Ohne ClamAV-Modul: Upload ohne Scan moeglich
- [ ] ClamAV nutzt INSTREAM-Protokoll (keine Datei auf Disk)

---

### US-150: Dateien-Modul deaktiviert
**Als** System **moechte ich** das Dateien-Modul komplett abschalten koennen, **damit** die Schule bei Bedarf auf Dateiablage verzichten kann.

**Vorbedingungen:** Login als `admin@monteweb.local`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Admin > Module > Dateien deaktivieren | Modul wird deaktiviert |
| 2 | Raum oeffnen | Raum wird angezeigt |
| 3 | Pruefen, ob Tab "Dateien" sichtbar ist | Tab ist NICHT sichtbar |
| 4 | API: `GET /api/v1/rooms/{id}/files` | HTTP 404 oder 503 |
| 5 | Modul wieder aktivieren | Tab "Dateien" erscheint wieder, vorhandene Dateien sind erhalten |

**Akzeptanzkriterien:**
- [ ] Bei deaktiviertem Modul: kein Dateien-Tab in Raeumen
- [ ] API-Endpunkte sind nicht erreichbar
- [ ] Reaktivierung stellt alle Dateien und Ordner wieder her
- [ ] Automatische Ordner-Erstellung bei Raumerstellung findet nicht statt

---

### US-151: Zugriffsbeschraenkung — Nicht-Mitglieder
**Als** System **moechte ich** verhindern, dass Nicht-Mitglieder auf Raum-Dateien zugreifen, **damit** die Datensicherheit gewaehrleistet ist.

**Vorbedingungen:** Ein Raum mit Dateien existiert. Login als Benutzer, der NICHT Mitglied dieses Raums ist.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | API: `GET /api/v1/rooms/{id}/files` mit fremder Room-ID | HTTP 403 — Zugriff verweigert |
| 2 | API: `GET /api/v1/rooms/{id}/files/{fileId}` (Download-Versuch) | HTTP 403 — Zugriff verweigert |
| 3 | API: `POST /api/v1/rooms/{id}/files` (Upload-Versuch) | HTTP 403 — Zugriff verweigert |

**Akzeptanzkriterien:**
- [ ] Nur Raummitglieder haben Zugriff auf Dateien
- [ ] SUPERADMIN hat Zugriff auf alle Raum-Dateien
- [ ] Fehlermeldung ist klar und verstaendlich
- [ ] Keine Information ueber Existenz der Dateien wird preisgegeben

---

## Modul: Formulare

### US-160: Umfrage erstellen (Raum-Scope, LEADER)
**Als** Raumleiter **moechte ich** eine Umfrage fuer meinen Raum erstellen, **damit** ich Meinungen der Raummitglieder einholen kann.

**Vorbedingungen:** Benutzer ist LEADER in einem Raum. Login als `lehrer@monteweb.local`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Im Hauptmenue auf "Formulare" klicken | Formularuebersicht wird angezeigt mit Tabs "Verfuegbar" und "Meine" |
| 2 | Button "Formular erstellen" klicken | Erstellungsformular oeffnet sich |
| 3 | "Titel" eingeben: "Elternumfrage Fruehstueck" | Titel wird angenommen |
| 4 | "Beschreibung" eingeben: "Umfrage zum gemeinsamen Fruehstueck" | Beschreibung wird angenommen |
| 5 | "Typ" auf "Umfrage" setzen | Typ ist "Umfrage" |
| 6 | "Sichtbarkeit" auf "Raum" setzen, Raum "Sonnengruppe" waehlen | Raum ist selektiert |
| 7 | "Anonym" Checkbox aktivieren | Anonyme Auswertung ist aktiviert |
| 8 | Optional: "Frist" setzen auf Datum in 2 Wochen | Fristdatum ist gesetzt |
| 9 | Button "Frage hinzufuegen" klicken | Neue Frage wird hinzugefuegt |
| 10 | Fragetyp "Einfachauswahl" waehlen, Fragetext: "Welcher Tag passt am besten?" | Frage konfiguriert |
| 11 | Optionen hinzufuegen: "Montag", "Mittwoch", "Freitag" | 3 Optionen angelegt |
| 12 | "Pflichtfeld" aktivieren | Frage ist als Pflichtfeld markiert |
| 13 | Weitere Frage hinzufuegen: Freitext "Anmerkungen" | Zweite Frage angelegt |
| 14 | Button "Als Entwurf speichern" klicken | Toast "Formular gespeichert", Status ist "Entwurf" |

**Akzeptanzkriterien:**
- [ ] Formular wird mit Status "Entwurf" erstellt
- [ ] Alle Fragetypen verfuegbar: Freitext, Einfachauswahl, Mehrfachauswahl, Bewertung, Ja/Nein
- [ ] Mindestens eine Frage muss vorhanden sein
- [ ] Optionen sind Pflicht fuer Einfach-/Mehrfachauswahl
- [ ] Reihenfolge der Fragen kann per Drag & Drop geaendert werden

---

### US-161: Einverstaendniserklaerung erstellen (CONSENT)
**Als** Raumleiter **moechte ich** eine Einverstaendniserklaerung erstellen, **damit** Eltern fuer Aktivitaeten zustimmen koennen.

**Vorbedingungen:** Login als `lehrer@monteweb.local` (LEADER).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | "Formular erstellen" klicken | Formular oeffnet sich |
| 2 | Titel: "Einverstaendnis Ausflug Zoo" | Titel wird angenommen |
| 3 | Typ: "Einverstaendnis" waehlen | Typ ist "Einverstaendnis" |
| 4 | Sichtbarkeit: "Raum", Raum waehlen | Raum ist selektiert |
| 5 | Frage hinzufuegen: Typ "Ja/Nein", Text: "Ich stimme der Teilnahme zu" | Ja/Nein-Frage angelegt |
| 6 | Weitere Frage: Typ "Freitext", Text: "Allergien oder Besonderheiten" | Freitext-Frage angelegt |
| 7 | "Als Entwurf speichern" klicken | Formular gespeichert |

**Akzeptanzkriterien:**
- [ ] Typ "Einverstaendnis" (CONSENT) ist auswahlbar neben "Umfrage" (SURVEY)
- [ ] Ja/Nein-Fragetyp zeigt "Ja" und "Nein" als einzige Optionen
- [ ] Einverstaendnis-Formulare funktionieren identisch zu Umfragen
- [ ] Typ kann nach Erstellung nicht mehr geaendert werden

---

### US-162: Formular veroeffentlichen
**Als** Formularersteller **moechte ich** ein Entwurfsformular veroeffentlichen, **damit** die Zielgruppe es ausfuellen kann.

**Vorbedingungen:** Ein Formular im Status "Entwurf" existiert. Login als Ersteller.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Tab "Meine" in der Formularuebersicht oeffnen | Eigene Formulare werden angezeigt |
| 2 | Formular mit Status "Entwurf" anklicken | Formulardetails werden angezeigt |
| 3 | Button "Veroeffentlichen" klicken | Bestaetigungsdialog oder sofortige Veroeffentlichung |
| 4 | Veroeffentlichung bestaetigen | Toast "Formular veroeffentlicht", Status wechselt auf "Veroeffentlicht" |
| 5 | Logout, Login als Raummitglied (z.B. `eltern@monteweb.local`) | Erfolgreich angemeldet |
| 6 | "Formulare" oeffnen, Tab "Verfuegbar" | Veroeffentlichtes Formular erscheint in der Liste |

**Akzeptanzkriterien:**
- [ ] Status wechselt von "Entwurf" auf "Veroeffentlicht"
- [ ] Veroeffentlichungszeitpunkt wird gespeichert (publishedAt)
- [ ] Veroeffentlichtes Formular ist fuer die Zielgruppe sichtbar
- [ ] Fragen koennen nach Veroeffentlichung NICHT mehr bearbeitet werden
- [ ] Dashboard-Widget "Offene Formulare" zeigt das Formular an

---

### US-163: Formular ausfuellen (Antwort abgeben)
**Als** Raummitglied **moechte ich** ein veroeffentlichtes Formular ausfuellen, **damit** meine Meinung oder Einverstaendnis erfasst wird.

**Vorbedingungen:** Ein veroeffentlichtes Formular existiert. Login als Zielgruppen-Benutzer (z.B. `eltern@monteweb.local`).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | "Formulare" oeffnen, Tab "Verfuegbar" | Verfuegbare Formulare werden angezeigt |
| 2 | Auf das Formular klicken | Formular mit Fragen wird angezeigt |
| 3 | Alle Pflichtfragen beantworten | Antworten werden angenommen |
| 4 | Button "Antwort absenden" klicken | Toast "Vielen Dank fuer Ihre Antwort!" |
| 5 | Formular erneut oeffnen | Meldung "Sie haben dieses Formular bereits beantwortet." mit eigenen Antworten |

**Akzeptanzkriterien:**
- [ ] Pflichtfragen muessen beantwortet werden (Validierung)
- [ ] Optional-Fragen koennen uebersprungen werden
- [ ] Nach Absenden wird Bestaetigung angezeigt
- [ ] Benutzer kann seine Antwort sehen (bei nicht-anonymen Formularen bearbeiten)
- [ ] Zaehler "Antworten" im Formular erhoet sich

---

### US-164: Antwort bearbeiten (nicht-anonym)
**Als** Benutzer **moechte ich** meine Antwort auf ein nicht-anonymes Formular bearbeiten, **damit** ich Korrekturen vornehmen kann.

**Vorbedingungen:** Benutzer hat ein nicht-anonymes Formular beantwortet. Formular ist noch "Veroeffentlicht".

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Formular erneut oeffnen | Eigene Antworten werden angezeigt mit Button "Antwort bearbeiten" |
| 2 | "Antwort bearbeiten" klicken | Fragen mit vorausgefuellten Antworten werden editierbar |
| 3 | Antworten aendern | Neue Antworten werden angenommen |
| 4 | "Antwort aktualisieren" klicken | Toast "Antwort aktualisiert" |

**Akzeptanzkriterien:**
- [ ] Bearbeiten nur moeglich, solange Formular "Veroeffentlicht" ist
- [ ] Bei geschlossenem Formular: keine Bearbeitung moeglich
- [ ] Anonyme Antworten koennen NICHT bearbeitet werden ("Anonyme Antworten koennen nicht bearbeitet werden")
- [ ] Bearbeitbare Frist wird ggf. angezeigt ("Bearbeitbar bis")

---

### US-165: Formular schliessen
**Als** Formularersteller **moechte ich** ein Formular schliessen, **damit** keine weiteren Antworten eingehen.

**Vorbedingungen:** Ein veroeffentlichtes Formular mit Antworten existiert. Login als Ersteller.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Formulardetails oeffnen | Formular mit Status "Veroeffentlicht" wird angezeigt |
| 2 | Button "Schliessen" klicken | Bestaetigungsdialog oder sofortige Schliessung |
| 3 | Schliessung bestaetigen | Toast "Formular geschlossen", Status wechselt auf "Geschlossen" |
| 4 | Logout, Login als Zielgruppen-Benutzer | Erfolgreich angemeldet |
| 5 | Formular oeffnen | Formular zeigt Antworten an, aber "Antwort absenden"-Button ist deaktiviert |

**Akzeptanzkriterien:**
- [ ] Status wechselt auf "Geschlossen"
- [ ] Keine neuen Antworten moeglich nach Schliessung
- [ ] Bestehende Antworten bleiben erhalten
- [ ] Benutzer, die bereits geantwortet haben, koennen ihre Antwort sehen
- [ ] Ergebnisse sind nach Schliessung fuer Respondenten sichtbar

---

### US-166: Formular archivieren
**Als** Formularersteller **moechte ich** ein geschlossenes Formular archivieren, **damit** es aus der aktiven Uebersicht verschwindet.

**Vorbedingungen:** Ein geschlossenes Formular existiert. Login als Ersteller.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Geschlossenes Formular oeffnen | Formulardetails werden angezeigt |
| 2 | Button "Archivieren" klicken | Toast "Formular archiviert", Status wechselt auf "Archiviert" |
| 3 | Formularuebersicht pruefen | Archiviertes Formular ist nicht mehr unter "Verfuegbar" sichtbar |

**Akzeptanzkriterien:**
- [ ] Status wechselt auf "Archiviert"
- [ ] Archiviertes Formular wird aus der Hauptliste entfernt
- [ ] Ergebnisse sind weiterhin abrufbar ueber direkten Link
- [ ] Nur geschlossene Formulare koennen archiviert werden

---

### US-167: Formularergebnisse ansehen
**Als** Formularersteller **moechte ich** die Ergebnisse meines Formulars einsehen, **damit** ich die Antworten auswerten kann.

**Vorbedingungen:** Ein Formular mit Antworten existiert. Login als Ersteller.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Formulardetails oeffnen | Formular wird angezeigt |
| 2 | Button "Ergebnisse anzeigen" klicken | Ergebnisseite oeffnet sich |
| 3 | Zusammenfassung pruefen | Ruecklaufquote wird angezeigt (z.B. "5 von 20 — 25%") |
| 4 | Einfachauswahl-Ergebnisse pruefen | Balkendiagramm mit Anteil pro Option |
| 5 | Bewertungs-Ergebnisse pruefen | Durchschnittswert und Verteilung angezeigt |
| 6 | Freitext-Antworten pruefen | Liste aller Textantworten |
| 7 | Tab "Einzelantworten" klicken | Individuelle Antworten pro Benutzer (bei nicht-anonymen Formularen) |

**Akzeptanzkriterien:**
- [ ] Zusammenfassung zeigt Ruecklaufquote (Antworten / Zielgruppe)
- [ ] Pro Frage: passende Visualisierung (Balken, Durchschnitt, Textliste)
- [ ] Einzelantworten zeigen Benutzername und Einreichungsdatum
- [ ] Bei anonymen Formularen: keine Benutzernamen sichtbar
- [ ] Nur Ersteller oder SUPERADMIN kann Ergebnisse sehen (bei veroeffentlichten Formularen)
- [ ] Respondenten koennen Ergebnisse geschlossener/archivierter Formulare sehen

---

### US-168: CSV-Export der Ergebnisse
**Als** Formularersteller **moechte ich** die Ergebnisse als CSV exportieren, **damit** ich sie in Excel oder anderen Tools weiterverarbeiten kann.

**Vorbedingungen:** Ein Formular mit Antworten existiert. Login als Ersteller.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Ergebnisseite des Formulars oeffnen | Ergebnisse werden angezeigt |
| 2 | Button "CSV Export" klicken | CSV-Datei wird heruntergeladen |
| 3 | CSV-Datei in Tabellenprogramm oeffnen | Daten sind korrekt formatiert mit Spalten pro Frage |

**Akzeptanzkriterien:**
- [ ] CSV enthaelt alle Antworten mit Spaltenkoepfen (Fragentexte)
- [ ] Bei anonymen Formularen: keine Benutzernamen im CSV
- [ ] CSV-Encoding ist UTF-8 (Umlaute korrekt)
- [ ] Nur Ersteller oder SUPERADMIN kann exportieren

---

### US-169: PDF-Export der Ergebnisse
**Als** Formularersteller **moechte ich** die Ergebnisse als PDF exportieren, **damit** ich sie ausdrucken oder archivieren kann.

**Vorbedingungen:** Ein Formular mit Antworten existiert. Login als Ersteller.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Ergebnisseite des Formulars oeffnen | Ergebnisse werden angezeigt |
| 2 | Button "PDF Export" klicken | PDF-Datei wird heruntergeladen |
| 3 | PDF oeffnen und pruefen | Formulartitel, Zusammenfassung und Ergebnisse pro Frage sind enthalten |

**Akzeptanzkriterien:**
- [ ] PDF enthaelt Formulartitel, Beschreibung und Erstellungsdatum
- [ ] Zusammenfassung mit Ruecklaufquote
- [ ] Pro Frage: Ergebnisse mit Zahlen/Prozenten
- [ ] PDF ist druckoptimiert
- [ ] Nur Ersteller oder SUPERADMIN kann exportieren

---

### US-170: Bereichs-Formular erstellen (SECTION-Scope)
**Als** Lehrkraft **moechte ich** ein Formular fuer einen Schulbereich erstellen, **damit** alle Benutzer des Bereichs es ausfuellen koennen.

**Vorbedingungen:** Login als `lehrer@monteweb.local` (TEACHER).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | "Formular erstellen" klicken | Formular oeffnet sich |
| 2 | Titel: "Feedback Schulbereich" | Titel wird angenommen |
| 3 | Sichtbarkeit: "Schulbereich" waehlen | Dropdown "Schulbereiche auswaehlen" erscheint (Mehrfachauswahl) |
| 4 | Einen Schulbereich auswaehlen | Bereich ist selektiert |
| 5 | Fragen hinzufuegen und Formular speichern | Formular wird erstellt |

**Akzeptanzkriterien:**
- [ ] Nur TEACHER, SECTION_ADMIN und SUPERADMIN koennen Bereichs-Formulare erstellen
- [ ] PARENT und STUDENT koennen KEINE Bereichs-Formulare erstellen
- [ ] Formular ist fuer alle Benutzer des gewaehlten Bereichs sichtbar

---

### US-171: Multi-Section-Formular erstellen
**Als** Lehrkraft **moechte ich** ein Formular fuer mehrere Schulbereiche gleichzeitig erstellen, **damit** ich bereichsuebergreifende Umfragen durchfuehren kann.

**Vorbedingungen:** Login als `lehrer@monteweb.local` oder `admin@monteweb.local`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | "Formular erstellen" klicken | Formular oeffnet sich |
| 2 | Titel: "Bereichsuebergreifende Elternumfrage" | Titel wird angenommen |
| 3 | Sichtbarkeit: "Schulbereich" waehlen | Mehrfachauswahl fuer Bereiche erscheint |
| 4 | Zwei oder mehr Schulbereiche auswaehlen | Bereiche sind selektiert |
| 5 | Fragen hinzufuegen und speichern | Formular wird mit mehreren section_ids erstellt |
| 6 | Formular veroeffentlichen | Status wechselt auf "Veroeffentlicht" |
| 7 | Login als Benutzer aus Bereich 1 | Formular ist sichtbar |
| 8 | Login als Benutzer aus Bereich 2 | Formular ist ebenfalls sichtbar |
| 9 | Login als Benutzer aus Bereich 3 (nicht gewaehlt) | Formular ist NICHT sichtbar |

**Akzeptanzkriterien:**
- [ ] Mehrere Schulbereiche koennen gleichzeitig ausgewaehlt werden
- [ ] Formular zeigt alle gewaehlten Bereichsnamen an (sectionNames)
- [ ] Nur Benutzer der ausgewaehlten Bereiche sehen das Formular
- [ ] section_ids werden als UUID-Array (mit GIN-Index) gespeichert

---

### US-172: Schulweites Formular erstellen (SCHOOL-Scope, SA)
**Als** Superadmin **moechte ich** ein schulweites Formular erstellen, **damit** alle Benutzer der Schule es ausfuellen koennen.

**Vorbedingungen:** Login als `admin@monteweb.local` (SUPERADMIN).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | "Formular erstellen" klicken | Formular oeffnet sich |
| 2 | Titel: "Schulweite Zufriedenheitsumfrage" | Titel wird angenommen |
| 3 | Sichtbarkeit: "Schulweit" waehlen | Kein weiteres Dropdown noetig |
| 4 | Fragen hinzufuegen (diverse Typen) und speichern | Formular wird erstellt |
| 5 | Veroeffentlichen | Status "Veroeffentlicht" |
| 6 | Login als beliebiger Benutzer (SA, T, P, S) | Formular ist fuer alle sichtbar |

**Akzeptanzkriterien:**
- [ ] Nur SUPERADMIN kann schulweite Formulare erstellen
- [ ] Schulweites Formular ist fuer ALLE Benutzer sichtbar
- [ ] targetCount zeigt Gesamtzahl aller Benutzer

---

### US-173: Berechtigungspruefung — Eltern/Schueler koennen keine Formulare erstellen
**Als** System **moechte ich** verhindern, dass Eltern oder Schueler Formulare erstellen, **damit** nur autorisierte Rollen Umfragen anlegen.

**Vorbedingungen:** Login als `eltern@monteweb.local` (PARENT).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | "Formulare" oeffnen | Formularuebersicht wird angezeigt |
| 2 | Pruefen, ob Button "Formular erstellen" sichtbar ist | Button ist NICHT sichtbar fuer PARENT |
| 3 | API: `POST /api/v1/forms` als PARENT | HTTP 400 oder 403 — Zugriff verweigert |
| 4 | Logout, Login als `schueler@monteweb.local` | Erfolgreich angemeldet |
| 5 | Pruefen, ob Button "Formular erstellen" sichtbar ist | Button ist NICHT sichtbar fuer STUDENT |

**Akzeptanzkriterien:**
- [ ] PARENT sieht keinen "Formular erstellen"-Button
- [ ] STUDENT sieht keinen "Formular erstellen"-Button
- [ ] API lehnt Erstellung durch nicht-autorisierte Rollen ab
- [ ] Berechtigungsmatrix: LEADER (Raum), TEACHER/SECADMIN (Bereich), SA (Schulweit)

---

### US-174: Formular loeschen
**Als** Formularersteller **moechte ich** ein Formular loeschen, **damit** fehlerhafte oder nicht mehr benoetigte Formulare entfernt werden.

**Vorbedingungen:** Ein Formular existiert (beliebiger Status). Login als Ersteller.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Formulardetails oeffnen | Formular wird angezeigt |
| 2 | Button "Formular loeschen" klicken | Bestaetigungsdialog erscheint |
| 3 | Loeschung bestaetigen | Toast "Formular geloescht", Weiterleitung zur Uebersicht |
| 4 | Formularuebersicht pruefen | Formular ist nicht mehr vorhanden |

**Akzeptanzkriterien:**
- [ ] Ersteller kann eigene Formulare loeschen (jeder Status)
- [ ] SUPERADMIN kann jedes Formular loeschen (unabhaengig vom Ersteller und Status)
- [ ] Alle zugehoerigen Antworten werden mitgeloescht
- [ ] Bestaetigungsdialog vor Loeschung

---

### US-175: Dashboard-Widget — Offene Formulare
**Als** Benutzer **moechte ich** auf dem Dashboard sehen, welche Formulare ich noch ausfuellen muss, **damit** ich keine Fristen verpasse.

**Vorbedingungen:** Veroeffentlichte Formulare existieren, die der Benutzer noch nicht beantwortet hat. Login als Zielgruppen-Benutzer.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Dashboard oeffnen | Dashboard wird angezeigt |
| 2 | Widget "Offene Formulare" pruefen | Unausgefuellte Formulare werden mit Titel und Frist angezeigt |
| 3 | Auf ein Formular im Widget klicken | Weiterleitung zur Formulardetailseite |
| 4 | Formular ausfuellen und absenden | Formular verschwindet aus dem Dashboard-Widget |

**Akzeptanzkriterien:**
- [ ] Widget zeigt nur Formulare, die der Benutzer noch NICHT beantwortet hat
- [ ] Widget zeigt Formulartitel und ggf. Frist
- [ ] Abgelaufene Fristen werden markiert ("Frist abgelaufen")
- [ ] Klick fuehrt direkt zum Formular
- [ ] Widget wird auf dem Dashboard angezeigt (wenn Formulare-Modul aktiv)

---

### US-176: Formulartypen — Alle Fragetypen testen
**Als** Formularersteller **moechte ich** alle verfuegbaren Fragetypen nutzen, **damit** ich flexible Umfragen gestalten kann.

**Vorbedingungen:** Login als Benutzer mit Erstellberechtigung.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | "Formular erstellen" klicken | Formular oeffnet sich |
| 2 | Frage 1: Typ "Freitext", Text: "Was wuenschen Sie sich?" | Freitext-Frage angelegt |
| 3 | Frage 2: Typ "Einfachauswahl", Text: "Bevorzugter Tag?", Optionen: "Mo, Mi, Fr" | Einfachauswahl angelegt |
| 4 | Frage 3: Typ "Mehrfachauswahl", Text: "Welche AGs interessieren?", Optionen: "Sport, Musik, Kunst" | Mehrfachauswahl angelegt |
| 5 | Frage 4: Typ "Bewertung", Text: "Wie zufrieden sind Sie?" | Bewertungsfrage (1-5 Sterne) angelegt |
| 6 | Frage 5: Typ "Ja/Nein", Text: "Einverstanden?" | Ja/Nein-Frage angelegt |
| 7 | Formular speichern und veroeffentlichen | Formular wird veroeffentlicht |
| 8 | Als anderer Benutzer ausfuellen | Alle Fragetypen funktionieren korrekt |
| 9 | Ergebnisse pruefen | Jeder Fragetyp hat passende Auswertung |

**Akzeptanzkriterien:**
- [ ] Freitext: Freies Eingabefeld, Ergebnis zeigt alle Textantworten
- [ ] Einfachauswahl: Genau eine Option waehlbar, Ergebnis zeigt Balkendiagramm
- [ ] Mehrfachauswahl: Mehrere Optionen waehlbar, Ergebnis zeigt Balkendiagramm
- [ ] Bewertung: Sternebewertung, Ergebnis zeigt Durchschnitt und Verteilung
- [ ] Ja/Nein: Zwei Optionen, Ergebnis zeigt Ja/Nein-Zaehler

---

### US-177: Formular mit Frist — Fristablauf
**Als** System **moechte ich** nach Fristablauf keine Antworten mehr annehmen, **damit** Fristen eingehalten werden.

**Vorbedingungen:** Ein Formular mit Frist in der Vergangenheit existiert. Login als Zielgruppen-Benutzer.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Formular mit abgelaufener Frist oeffnen | Formular wird angezeigt mit Hinweis "Frist abgelaufen" |
| 2 | Versuch, eine Antwort abzusenden | Button "Antwort absenden" ist deaktiviert oder Fehlermeldung |
| 3 | API: `POST /api/v1/forms/{id}/responses` | HTTP 400 — Frist abgelaufen |

**Akzeptanzkriterien:**
- [ ] Nach Fristablauf: keine neuen Antworten moeglich
- [ ] Visueller Hinweis "Frist abgelaufen" auf dem Formular
- [ ] Bestehende Antworten bleiben erhalten
- [ ] Ersteller kann Formular trotz Fristablauf schliessen/archivieren

---

### US-178: Formulare-Modul deaktiviert
**Als** System **moechte ich** das Formulare-Modul komplett abschalten koennen, **damit** die Schule bei Bedarf auf Umfragen verzichten kann.

**Vorbedingungen:** Login als `admin@monteweb.local`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Admin > Module > Formulare deaktivieren | Modul wird deaktiviert |
| 2 | Logout, Login als `lehrer@monteweb.local` | Erfolgreich angemeldet |
| 3 | Hauptnavigation pruefen | Menue-Punkt "Formulare" ist NICHT sichtbar |
| 4 | URL `/formulare` direkt aufrufen | Weiterleitung oder 404-Seite |
| 5 | Dashboard pruefen | Widget "Offene Formulare" ist NICHT sichtbar |
| 6 | Modul wieder aktivieren | Alle Formulare und Antworten sind wiederhergestellt |

**Akzeptanzkriterien:**
- [ ] Kein Menue-Eintrag "Formulare" bei deaktiviertem Modul
- [ ] Dashboard-Widget verschwindet
- [ ] API-Endpunkte nicht erreichbar
- [ ] Reaktivierung stellt alle Daten wieder her

---

### US-179: Einzelantworten einsehen (nicht-anonymes Formular)
**Als** Formularersteller **moechte ich** die Einzelantworten einsehen, **damit** ich sehen kann, wer was geantwortet hat.

**Vorbedingungen:** Ein nicht-anonymes Formular mit mehreren Antworten existiert. Login als Ersteller.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Ergebnisseite oeffnen | Zusammenfassung wird angezeigt |
| 2 | Tab "Einzelantworten" klicken | Liste aller Antworten mit Benutzernamen wird angezeigt |
| 3 | Eine Einzelantwort aufklappen | Alle Antworten dieses Benutzers werden angezeigt |
| 4 | Einreichungsdatum pruefen | "Eingereicht am" mit Datum und Uhrzeit |

**Akzeptanzkriterien:**
- [ ] Einzelantworten zeigen Benutzername und Einreichungszeitpunkt
- [ ] Alle Fragen und Antworten des Benutzers werden angezeigt
- [ ] Bei anonymen Formularen: Benutzername ist "Anonym"
- [ ] Nur Ersteller oder SUPERADMIN kann Einzelantworten einsehen

---

### US-180: Formular bearbeiten (im Entwurf)
**Als** Formularersteller **moechte ich** ein Entwurfsformular bearbeiten, **damit** ich Fragen vor der Veroeffentlichung anpassen kann.

**Vorbedingungen:** Ein Formular im Status "Entwurf" existiert. Login als Ersteller.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Entwurfsformular oeffnen | Formulardetails werden angezeigt |
| 2 | Button "Fragen bearbeiten" klicken | Bearbeitungsmodus oeffnet sich |
| 3 | Titel aendern | Neuer Titel wird angenommen |
| 4 | Frage hinzufuegen | Neue Frage erscheint |
| 5 | Bestehende Frage entfernen | Frage wird entfernt (Button "Frage entfernen") |
| 6 | Frage-Reihenfolge aendern | Reihenfolge wird aktualisiert |
| 7 | Aenderungen speichern | Toast "Formular gespeichert" |

**Akzeptanzkriterien:**
- [ ] Im Entwurfsstatus: alle Felder editierbar (Titel, Beschreibung, Frist, Fragen)
- [ ] Fragen koennen hinzugefuegt, entfernt und umsortiert werden
- [ ] Optionen bei Auswahlfragen koennen angepasst werden
- [ ] Nach Veroeffentlichung: nur noch Titel, Beschreibung und Frist editierbar
- [ ] Fragen koennen nach Veroeffentlichung NICHT mehr geaendert werden

---

### US-181: Negativtest — Formular ohne Fragen erstellen
**Als** System **moechte ich** verhindern, dass ein Formular ohne Fragen veroeffentlicht wird, **damit** nur sinnvolle Formulare erstellt werden.

**Vorbedingungen:** Login als Benutzer mit Erstellberechtigung.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | "Formular erstellen" klicken | Formular oeffnet sich |
| 2 | Titel ausfuellen, aber KEINE Fragen hinzufuegen | Hinweis: "Noch keine Fragen hinzugefuegt. Klicken Sie auf 'Frage hinzufuegen'." |
| 3 | Versuch, Formular zu speichern ohne Fragen | Fehlermeldung — mindestens eine Frage erforderlich |
| 4 | Einfachauswahl-Frage hinzufuegen OHNE Optionen | Fehlermeldung — Optionen sind Pflicht fuer diesen Fragetyp |
| 5 | Formular ohne Titel speichern | Fehlermeldung — Titel ist Pflichtfeld |

**Akzeptanzkriterien:**
- [ ] Validierung: mindestens eine Frage erforderlich
- [ ] Validierung: Titel ist Pflichtfeld
- [ ] Validierung: Einfach-/Mehrfachauswahl benoetigt mindestens 2 Optionen
- [ ] Validierung: Pflichtfeld "Fragetext" bei jeder Frage
- [ ] Validierungsfehler werden als verstaendliche Fehlermeldungen angezeigt

---

### US-182: Formular-Scope-Berechtigungen im Ueberblick
**Als** System **moechte ich** sicherstellen, dass alle Scope-Berechtigungen korrekt durchgesetzt werden.

**Vorbedingungen:** Verschiedene Benutzer mit verschiedenen Rollen.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Login als LEADER — Raum-Formular erstellen | Erfolgreich |
| 2 | Login als normales MEMBER — Raum-Formular erstellen | Fehlermeldung: nur LEADER |
| 3 | Login als TEACHER — Bereichs-Formular erstellen | Erfolgreich |
| 4 | Login als PARENT — Bereichs-Formular erstellen | Fehlermeldung |
| 5 | Login als SECTION_ADMIN — Bereichs-Formular erstellen | Erfolgreich |
| 6 | Login als TEACHER — Schulweites Formular erstellen | Fehlermeldung: nur Admins |
| 7 | Login als SUPERADMIN — Schulweites Formular erstellen | Erfolgreich |
| 8 | Login als SUPERADMIN — beliebiges Formular loeschen | Erfolgreich (unabhaengig vom Ersteller) |

**Akzeptanzkriterien:**
- [ ] ROOM-Scope: nur LEADER des Raums oder SUPERADMIN
- [ ] SECTION-Scope: nur TEACHER, SECTION_ADMIN oder SUPERADMIN
- [ ] SCHOOL-Scope: nur SUPERADMIN
- [ ] SUPERADMIN kann jedes Formular verwalten und loeschen
- [ ] Ersteller kann sein eigenes Formular immer verwalten

---


**Projekt:** MonteWeb -- Schul-Intranet fuer Montessori-Schulkomplexe
**Erstellt:** 2026-03-02
**Module:** Jobboard/Elternstunden (US-200--US-219), Putz-Orga/Cleaning (US-220--US-249), Fotobox (US-250--US-269), Fundgrube (US-270--US-285)
**Rollen:** SUPERADMIN (SA), SECTION_ADMIN (SECADMIN), TEACHER (T), PARENT (P), STUDENT (S)

**Testkonten:**
| Konto | Rolle | Passwort |
|-------|-------|----------|
| admin@monteweb.local | SUPERADMIN | admin123 |
| sectionadmin@monteweb.local | SECTION_ADMIN | test1234 |
| lehrer@monteweb.local | TEACHER | test1234 |
| eltern@monteweb.local | PARENT | test1234 |
| schueler@monteweb.local | STUDENT | test1234 |

---

## Modul: Jobboard / Elternstunden

### US-200: Job erstellen als Elternteil
**Als** Elternteil (P) **moechte ich** einen neuen Job in der Jobboerse erstellen, **damit** andere Eltern sich fuer die Aufgabe bewerben koennen.

**Vorbedingungen:** Benutzer ist als `eltern@monteweb.local` eingeloggt. Modul Jobboard ist aktiviert (`monteweb.modules.jobboard.enabled=true`). Benutzer gehoert einem Familienverbund an.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Navigiere zu Jobboerse | Jobboerse-Uebersicht wird angezeigt |
| 2 | Klicke auf "Neuen Job erstellen" | Formular zum Erstellen eines Jobs oeffnet sich |
| 3 | Fuelle Titel ("Kuchen backen fuer Sommerfest"), Beschreibung, Kategorie ("Feste"), Ort ("Schulkueche"), geschaetzte Stunden (3.0), max. Zugewiesene (2), geplantes Datum und Uhrzeit aus | Alle Felder werden akzeptiert |
| 4 | Waehle Sichtbarkeit "PUBLIC" und klicke "Erstellen" | Job wird mit Status OPEN erstellt und in der Liste angezeigt |
| 5 | Pruefe die Job-Detailansicht | Alle eingegebenen Daten werden korrekt angezeigt, Ersteller ist der eingeloggte Benutzer |

**Akzeptanzkriterien:**
- [ ] Job wird mit Status `OPEN` und Sichtbarkeit `PUBLIC` erstellt
- [ ] Titel (max 300 Zeichen), Beschreibung, Kategorie (max 100 Zeichen), Ort (max 200 Zeichen) werden gespeichert
- [ ] Geschaetzte Stunden (BigDecimal, Precision 5, Scale 2) werden korrekt gesetzt
- [ ] `createdBy` wird automatisch auf den eingeloggten Benutzer gesetzt
- [ ] `createdAt` und `updatedAt` werden automatisch gesetzt

---

### US-201: Student kann keinen Job erstellen
**Als** Schueler (S) **moechte ich** sicherstellen, dass ich keine Jobs erstellen kann, **damit** die Jobboerse nur von berechtigten Rollen genutzt wird.

**Vorbedingungen:** Benutzer ist als `schueler@monteweb.local` eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Navigiere zu Jobboerse | Jobboerse-Uebersicht wird angezeigt |
| 2 | Versuche einen Job ueber POST /api/v1/jobs zu erstellen | HTTP 403 Forbidden: "Students cannot create jobs" |

**Akzeptanzkriterien:**
- [ ] Schueler erhalten beim Versuch, einen Job zu erstellen, einen 403-Fehler
- [ ] Der Button "Job erstellen" ist fuer Schueler im Frontend nicht sichtbar

---

### US-202: Privaten Job erstellen mit Auto-Zuweisung
**Als** Elternteil (P) **moechte ich** einen privaten Job (PRIVATE) erstellen, **damit** die Stunden direkt meiner Familie gutgeschrieben werden, ohne dass andere sich bewerben muessen.

**Vorbedingungen:** Benutzer ist als `eltern@monteweb.local` eingeloggt und gehoert einem Familienverbund an.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Erstelle einen neuen Job mit Sichtbarkeit "PRIVATE" | Job wird erstellt |
| 2 | Pruefe den Job-Status | Status ist `ASSIGNED` (nicht OPEN) |
| 3 | Pruefe die Zuweisungen des Jobs | Eine Zuweisung fuer den Ersteller existiert automatisch mit Status `ASSIGNED` |
| 4 | Pruefe `maxAssignees` | Wurde automatisch auf 1 gesetzt, unabhaengig vom eingegebenen Wert |

**Akzeptanzkriterien:**
- [ ] Bei PRIVATE-Sichtbarkeit wird `maxAssignees` automatisch auf 1 gesetzt
- [ ] Eine automatische Zuweisung (Assignment) fuer den Ersteller wird angelegt
- [ ] Job-Status wird sofort auf `ASSIGNED` gesetzt
- [ ] Die `familyId` der Zuweisung entspricht dem ersten Familienverbund des Erstellers

---

### US-203: Draft-Job erstellen und genehmigen
**Als** Elternteil (P) **moechte ich** einen Job als Entwurf (DRAFT) erstellen, **damit** ein Admin ihn vor Veroeffentlichung pruefen kann.

**Vorbedingungen:** Benutzer ist als `eltern@monteweb.local` eingeloggt. Ein SUPERADMIN oder JOBBOARD_ADMIN steht zur Genehmigung bereit.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Erstelle einen Job mit Sichtbarkeit "DRAFT" | Job wird mit Sichtbarkeit DRAFT erstellt |
| 2 | Pruefe als anderer Elternteil die Jobboerse | Der Draft-Job ist NICHT in der oeffentlichen Liste sichtbar |
| 3 | Logge dich als `admin@monteweb.local` ein | Login erfolgreich |
| 4 | Navigiere zu "Drafts" (GET /api/v1/jobs/drafts) | Der Draft-Job wird in der Liste angezeigt |
| 5 | Klicke auf "Genehmigen" (POST /api/v1/jobs/{id}/approve) | Job-Sichtbarkeit wechselt zu PUBLIC, `approvedBy` und `approvedAt` werden gesetzt |
| 6 | Pruefe als Elternteil die Jobboerse erneut | Der genehmigte Job ist jetzt in der Liste sichtbar |

**Akzeptanzkriterien:**
- [ ] Nur DRAFT-Jobs koennen genehmigt werden (Fehler bei bereits PUBLIC-Jobs)
- [ ] Nur SUPERADMIN, SECTION_ADMIN oder Benutzer mit specialRole JOBBOARD_ADMIN koennen genehmigen
- [ ] Nach Genehmigung: `visibility=PUBLIC`, `approvedBy` und `approvedAt` werden gesetzt

---

### US-204: Fuer einen Job bewerben
**Als** Elternteil (P) **moechte ich** mich fuer einen offenen Job bewerben, **damit** ich Elternstunden sammeln kann.

**Vorbedingungen:** Ein Job mit Status OPEN existiert. Benutzer ist als `eltern@monteweb.local` eingeloggt und gehoert einem Familienverbund an.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Navigiere zur Jobboerse und waehle einen offenen Job | Job-Detailansicht wird angezeigt mit Status OPEN |
| 2 | Klicke auf "Bewerben" (POST /api/v1/jobs/{id}/apply) | Zuweisung wird mit Status `ASSIGNED` erstellt |
| 3 | Pruefe die Zuweisung | `familyId` entspricht dem Familienverbund des Bewerbers |
| 4 | Pruefe den Job-Status | Bei maxAssignees=1: Status wechselt zu `ASSIGNED`. Bei maxAssignees>1: Status wechselt zu `PARTIALLY_ASSIGNED` |

**Akzeptanzkriterien:**
- [ ] Nur Eltern koennen sich bewerben (SUPERADMIN, SECTION_ADMIN, TEACHER werden mit 403 abgelehnt: "This role does not perform parent hours")
- [ ] Doppelte Bewerbung wird verhindert ("You have already applied for this job")
- [ ] Bei Erreichen von maxAssignees wird der Job-Status zu ASSIGNED
- [ ] Bewerber muss einem Familienverbund angehoeren ("You must belong to a family to apply for jobs")
- [ ] Pessimistic Lock verhindert Race Conditions bei gleichzeitigen Bewerbungen

---

### US-205: Stunden starten, abschliessen und bestaetigen (manuell)
**Als** Elternteil (P) **moechte ich** meine zugewiesenen Stunden starten und nach Erledigung abschliessen, **damit** ein Verantwortlicher die Stunden bestaetigen kann.

**Vorbedingungen:** Eine Zuweisung mit Status `ASSIGNED` existiert fuer den Benutzer. `tenant_config.require_assignment_confirmation = true`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Klicke auf "Starten" (PUT /api/v1/jobs/assignments/{id}/start) | Status wechselt zu `IN_PROGRESS`, `startedAt` wird gesetzt |
| 2 | Pruefe den Job-Status | Job-Status wechselt zu `IN_PROGRESS` |
| 3 | Klicke auf "Abschliessen" mit tatsaechlichen Stunden (2.5) und Notizen | Status wechselt zu `COMPLETED`, `actualHours=2.5`, `completedAt` wird gesetzt |
| 4 | Pruefe das `confirmed`-Flag | `confirmed=false` (manuelle Bestaetigung erforderlich) |
| 5 | Logge dich als `lehrer@monteweb.local` ein | Login erfolgreich |
| 6 | Navigiere zu "Ausstehende Bestaetigungen" (GET /api/v1/jobs/assignments/pending-confirmation) | Die abgeschlossene Zuweisung erscheint in der Liste |
| 7 | Klicke auf "Bestaetigen" (PUT /api/v1/jobs/assignments/{id}/confirm) | `confirmed=true`, `confirmedBy` und `confirmedAt` werden gesetzt |
| 8 | Pruefe das Familien-Stundenkonto | Die bestaetigten Stunden (2.5h) werden dem Familienverbund gutgeschrieben |

**Akzeptanzkriterien:**
- [ ] Nur der Zugewiesene kann seine eigene Zuweisung starten und abschliessen
- [ ] Status-Uebergaenge: ASSIGNED -> IN_PROGRESS -> COMPLETED
- [ ] Abschliessen ist auch direkt aus ASSIGNED moeglich (ohne vorheriges Starten)
- [ ] Nur COMPLETED-Zuweisungen koennen bestaetigt werden
- [ ] Nach Bestaetigung wird ein `JobCompletedEvent` publiziert
- [ ] Bereits bestaetigte Zuweisungen koennen nicht erneut bestaetigt werden

---

### US-206: Auto-Bestaetigung bei deaktivierter Konfiguration
**Als** Administrator (SA) **moechte ich** die automatische Bestaetigung konfigurieren, **damit** abgeschlossene Stunden ohne manuelle Pruefung gutgeschrieben werden.

**Vorbedingungen:** `tenant_config.require_assignment_confirmation = false`. Eine Zuweisung mit Status ASSIGNED oder IN_PROGRESS existiert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Logge dich als `admin@monteweb.local` ein und setze `require_assignment_confirmation` auf `false` | Konfiguration wird gespeichert |
| 2 | Logge dich als `eltern@monteweb.local` ein und schliesse eine Zuweisung ab mit actualHours=3.0 | Zuweisung wird abgeschlossen |
| 3 | Pruefe das `confirmed`-Flag | `confirmed=true` (automatisch bestaetigt) |
| 4 | Pruefe `confirmedBy` | Entspricht der `userId` des Zuweisungsnehmers (Selbstbestaetigung) |
| 5 | Pruefe das Familien-Stundenkonto | Die 3.0 Stunden werden sofort gutgeschrieben |
| 6 | Pruefe ob ein `JobCompletedEvent` publiziert wurde | Event wurde sofort beim Abschliessen ausgeloest |

**Akzeptanzkriterien:**
- [ ] Bei `require_assignment_confirmation=false` wird die Zuweisung beim Abschliessen automatisch bestaetigt
- [ ] `confirmedBy` wird auf den abschliessenden Benutzer gesetzt
- [ ] `JobCompletedEvent` wird sofort publiziert (nicht erst bei manueller Bestaetigung)

---

### US-207: Zuweisung ablehnen (Reject)
**Als** Lehrer (T) **moechte ich** eine abgeschlossene Zuweisung ablehnen, **damit** nicht korrekt geleistete Stunden nicht gutgeschrieben werden.

**Vorbedingungen:** Eine Zuweisung mit Status `COMPLETED` existiert, die noch nicht bestaetigt ist (`confirmed=false`).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Logge dich als `lehrer@monteweb.local` ein | Login erfolgreich |
| 2 | Navigiere zu den ausstehenden Bestaetigungen | Die unbestaetigte Zuweisung wird angezeigt |
| 3 | Klicke auf "Ablehnen" (PUT /api/v1/jobs/assignments/{id}/reject) | Zuweisungs-Status wechselt zu `CANCELLED` |
| 4 | Pruefe den Job-Status | Job wird wieder auf `OPEN` oder `PARTIALLY_ASSIGNED` gesetzt (abhaengig von verbleibenden Zuweisungen) |
| 5 | Versuche, eine bereits bestaetigte Zuweisung abzulehnen | Fehler: "Cannot reject a confirmed assignment" |

**Akzeptanzkriterien:**
- [ ] Nur TEACHER, SECTION_ADMIN und SUPERADMIN koennen ablehnen
- [ ] Nur COMPLETED (nicht bestaetigte) Zuweisungen koennen abgelehnt werden
- [ ] Bei Ablehnung wird der Job ggf. wieder geoeffnet
- [ ] Bestaetigte Zuweisungen koennen nicht abgelehnt werden

---

### US-208: Zuweisung stornieren durch Zugewiesenen
**Als** Elternteil (P) **moechte ich** meine eigene Zuweisung stornieren, **damit** ich eine Aufgabe absagen kann, die ich nicht wahrnehmen kann.

**Vorbedingungen:** Eine Zuweisung mit Status `ASSIGNED` oder `IN_PROGRESS` existiert fuer den Benutzer.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Navigiere zu "Meine Zuweisungen" | Die aktive Zuweisung wird angezeigt |
| 2 | Klicke auf "Stornieren" (DELETE /api/v1/jobs/assignments/{id}) | Zuweisungs-Status wechselt zu `CANCELLED` |
| 3 | Pruefe den Job-Status | Job wird auf `OPEN` oder `PARTIALLY_ASSIGNED` zurueckgesetzt |
| 4 | Versuche, eine bestaetigte COMPLETED-Zuweisung zu stornieren | Fehler: "Cannot cancel a confirmed assignment" |

**Akzeptanzkriterien:**
- [ ] Nur der Zugewiesene selbst kann seine Zuweisung stornieren
- [ ] Bestaetigte COMPLETED-Zuweisungen koennen nicht storniert werden
- [ ] Bei Stornierung wird der Job-Status korrekt zurueckgesetzt

---

### US-209: Familien-Stundenkonto einsehen
**Als** Elternteil (P) **moechte ich** das Stundenkonto meiner Familie einsehen, **damit** ich weiss, wie viele Stunden noch zu leisten sind.

**Vorbedingungen:** Benutzer gehoert einem Familienverbund an. Es existieren bestaetigte Zuweisungen fuer die Familie.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Navigiere zum Familien-Stundenkonto (GET /api/v1/jobs/family-hours/{familyId}) | Stundenuebersicht wird angezeigt |
| 2 | Pruefe die angezeigten Werte | `targetHours` (Zielstunden), `completedHours` (bestaetigte Elternstunden), `cleaningHours` (Putzstunden), `totalHours`, `remainingHours`, `pendingHours` werden korrekt angezeigt |
| 3 | Pruefe die Ampel-Farbe (trafficLight) | Gruen (>= Ziel), Gelb (teilweise erfuellt), Rot (weit unter Ziel) |
| 4 | Pruefe das Putz-Sonder-Unterkonto | `cleaningHours` zeigt separat die Stunden aus Reinigungskategorie + QR-Putzstunden |
| 5 | Pruefe `cleaningTrafficLight` | Separate Ampelfarbe fuer Putzstunden basierend auf `targetCleaningHours` |

**Akzeptanzkriterien:**
- [ ] Stundenkonto ist familienverbund-basiert (nicht benutzerbasiert)
- [ ] `completedHours` enthaelt nur bestaetigte, nicht-Reinigungs-Zuweisungen
- [ ] `cleaningHours` ist die Summe aus Reinigungs-Job-Stunden und QR-Check-in-Putzstunden
- [ ] `totalHours = completedHours + cleaningHours`
- [ ] `remainingHours = max(0, targetHours - totalHours)`
- [ ] Ampelfarben werden korrekt berechnet

---

### US-210: Stunden-Befreiung fuer Familien
**Als** Administrator (SA) **moechte ich** eine Familie von den Elternstunden befreien, **damit** Sonderfaelle (z.B. Alleinerziehende) beruecksichtigt werden.

**Vorbedingungen:** Eine Familie existiert mit `is_hours_exempt = false`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Logge dich als `admin@monteweb.local` ein | Login erfolgreich |
| 2 | Setze `is_hours_exempt = true` fuer die Familie | Aenderung wird gespeichert |
| 3 | Pruefe das Familien-Stundenkonto | Alle Stundenwerte sind 0, `trafficLight` ist "GREEN", `hoursExempt=true` |
| 4 | Pruefe den Gesamtbericht (GET /api/v1/jobs/report/summary) | Die befreite Familie wird bei der Ampel-Zaehlung (greenCount, yellowCount, redCount) NICHT mitgezaehlt |

**Akzeptanzkriterien:**
- [ ] Befreite Familien erhalten immer `trafficLight=GREEN` und alle Stunden auf 0
- [ ] Befreite Familien werden im Report-Summary NICHT in der Ampel-Zaehlung beruecksichtigt
- [ ] Die Befreiung kann jederzeit aktiviert und deaktiviert werden

---

### US-211: Jahresabrechnung erstellen und abschliessen
**Als** Administrator (SA) **moechte ich** eine Jahresabrechnung (Billing Period) erstellen und abschliessen, **damit** die Elternstunden periodenweise abgerechnet werden.

**Vorbedingungen:** Benutzer ist als `admin@monteweb.local` eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Erstelle eine neue Abrechnungsperiode (POST /api/v1/billing/periods) mit Name "Schuljahr 2025/26", Start- und Enddatum | Periode wird mit Status `ACTIVE` erstellt |
| 2 | Pruefe die aktive Periode (GET /api/v1/billing/periods/active) | Die erstellte Periode wird zurueckgegeben |
| 3 | Rufe den Bericht ab (GET /api/v1/billing/periods/{id}/report) | Bericht zeigt alle Familien mit ihren Stunden |
| 4 | Schliesse die Periode (POST /api/v1/billing/periods/{id}/close) | Status wechselt zu "CLOSED", `closedAt` und `closedBy` werden gesetzt |
| 5 | Pruefe die geschlossene Periode | `reportData` enthaelt den Bericht als JSONB-Snapshot |

**Akzeptanzkriterien:**
- [ ] Nur ein Abrechnungszeitraum kann gleichzeitig ACTIVE sein
- [ ] Beim Abschliessen wird der aktuelle Stundenbericht als JSONB-Snapshot in `reportData` gespeichert
- [ ] `closedBy` wird auf den abschliessenden Admin gesetzt
- [ ] Abgeschlossene Perioden koennen nicht erneut geschlossen werden

---

### US-212: PDF-Export des Stundenberichts
**Als** Administrator (SA) **moechte ich** den Familien-Stundenbericht als PDF exportieren, **damit** ich ihn ausdrucken oder archivieren kann.

**Vorbedingungen:** Bestaetigte Zuweisungen existieren fuer mehrere Familien.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Navigiere zum Stundenbericht | Uebersicht aller Familien mit Stunden wird angezeigt |
| 2 | Klicke auf "PDF exportieren" (GET /api/v1/jobs/report/pdf) | PDF-Datei wird heruntergeladen mit Dateiname "familien-stundenbericht.pdf" |
| 3 | Oeffne die PDF-Datei | Schulname, alle Familien mit Stunden (bestaetigte, Putz-, Gesamt-, ausstehende, verbleibende) werden angezeigt |
| 4 | Exportiere als CSV (GET /api/v1/jobs/report/export) | CSV-Datei "familien-stundenbericht.csv" mit Semikolon-Trennung, UTF-8 BOM, Ampel-Uebersetzung (GREEN->Gruen, YELLOW->Gelb, RED->Rot) |

**Akzeptanzkriterien:**
- [ ] PDF-Export: Content-Type `application/pdf`, Content-Disposition mit Dateiname
- [ ] CSV-Export: Semikolon-getrennt, UTF-8 mit BOM (fuer Excel-Kompatibilitaet)
- [ ] CSV-Spalten: Familie, Zielstunden, Elternstunden, Putzstunden, Gesamt, Ausstehend, Verbleibend, Ampel
- [ ] Ampelfarben werden ins Deutsche uebersetzt (Gruen/Gelb/Rot)

---

### US-213: Billing-Perioden-PDF-Export (Jahresabrechnung)
**Als** Administrator (SA) **moechte ich** den PDF-Export einer Abrechnungsperiode herunterladen, **damit** ich die Jahresabrechnung archivieren kann.

**Vorbedingungen:** Eine Abrechnungsperiode existiert (aktiv oder geschlossen).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Navigiere zur Abrechnungsperiode | Periodendetails werden angezeigt |
| 2 | Klicke auf "PDF exportieren" (GET /api/v1/billing/periods/{id}/export/pdf) | PDF-Datei wird heruntergeladen mit Dateiname "jahresabrechnung.pdf" |
| 3 | Pruefe den PDF-Inhalt | Periodenname, Zeitraum, alle Familien mit Stundendetails |

**Akzeptanzkriterien:**
- [ ] Content-Type `application/pdf`
- [ ] Content-Disposition: `attachment; filename="jahresabrechnung.pdf"`
- [ ] PDF enthaelt den Schulnamen aus der Tenant-Konfiguration

---

### US-214: Admin-Gesamtuebersicht (Report Summary)
**Als** Administrator (SA) **moechte ich** eine Zusammenfassung aller Jobs und Familienstunden sehen, **damit** ich den Ueberblick ueber die Elternstunden behalte.

**Vorbedingungen:** Jobs in verschiedenen Status existieren. Mehrere Familien haben Stunden gesammelt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Rufe die Gesamtuebersicht auf (GET /api/v1/jobs/report/summary) | Zusammenfassung wird angezeigt |
| 2 | Pruefe die Job-Zaehler | `openJobs`, `activeJobs` (IN_PROGRESS), `completedJobs` werden korrekt gezaehlt |
| 3 | Pruefe die Ampel-Zaehler | `greenCount`, `yellowCount`, `redCount` fuer alle nicht-befreiten Familien |
| 4 | Pruefe die Sortierung im Gesamtbericht (GET /api/v1/jobs/report/all) | Familien sind sortiert: Rot zuerst, dann Gelb, dann Gruen. Innerhalb gleicher Ampel alphabetisch |

**Akzeptanzkriterien:**
- [ ] Befreite Familien (`hoursExempt=true`) werden bei den Ampelzaehlern ausgeschlossen
- [ ] Sortierung: Rot (hoechste Prioritaet) > Gelb > Gruen
- [ ] Innerhalb gleicher Ampelfarbe: alphabetisch nach Familienname (case-insensitive)

---

### US-215: Job-Anhaenge hochladen und herunterladen
**Als** Elternteil (P) **moechte ich** Dateien an einen Job anhaengen, **damit** zusaetzliche Informationen (z.B. Einladung, Anfahrt) bereitgestellt werden.

**Vorbedingungen:** Ein Job existiert, der vom Benutzer erstellt wurde.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Oeffne den Job und klicke auf "Datei hochladen" | Datei-Upload-Dialog oeffnet sich |
| 2 | Lade eine PDF-Datei hoch (POST /api/v1/jobs/{id}/attachments) | Datei wird in MinIO gespeichert, Attachment-Eintrag wird erstellt |
| 3 | Pruefe die Anhaenge-Liste | Dateiname, Dateityp, Dateigroesse werden angezeigt |
| 4 | Klicke auf "Herunterladen" (GET /api/v1/jobs/{id}/attachments/{attachmentId}/download) | Datei wird korrekt heruntergeladen |
| 5 | Klicke auf "Loeschen" (DELETE /api/v1/jobs/{id}/attachments/{attachmentId}) | Anhang wird aus MinIO und Datenbank entfernt |

**Akzeptanzkriterien:**
- [ ] Nur der Ersteller oder Admins koennen Anhaenge hochladen und loeschen
- [ ] Dateien werden in MinIO gespeichert
- [ ] Dateiname, Dateityp, Dateigroesse und `uploaded_by` werden in der DB gespeichert

---

### US-216: Job loeschen vs. stornieren
**Als** Ersteller eines Jobs **moechte ich** einen Job stornieren oder permanent loeschen, **damit** nicht mehr benoetigte Jobs aus dem System entfernt werden.

**Vorbedingungen:** Ein Job mit Status OPEN existiert, erstellt vom Benutzer.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Storniere den Job (DELETE /api/v1/jobs/{id}?permanent=false) | Job-Status wechselt zu `CANCELLED`, `closedAt` wird gesetzt, Job bleibt in der DB |
| 2 | Pruefe den Job in der DB | Job existiert noch mit Status CANCELLED |
| 3 | Erstelle einen neuen Job und loesche ihn permanent (DELETE /api/v1/jobs/{id}?permanent=true) | Job und alle zugehoerigen Zuweisungen werden aus der DB entfernt |
| 4 | Versuche als fremder Benutzer einen Job zu loeschen | Fehler: "Only the creator or administrators can delete this job" |

**Akzeptanzkriterien:**
- [ ] Stornieren (`permanent=false`): Status -> CANCELLED, Job bleibt in DB
- [ ] Loeschen (`permanent=true`): Job und Zuweisungen werden permanent geloescht
- [ ] Nur Ersteller, SUPERADMIN oder SECTION_ADMIN koennen stornieren/loeschen

---

### US-217: Job mit Kalender-Event verknuepfen
**Als** Lehrer (T) oder Elternteil (P) **moechte ich** einen Job mit einem Kalender-Event verknuepfen, **damit** Events direkt mit Helfer-Jobs verbunden sind.

**Vorbedingungen:** Ein Job und ein Kalender-Event existieren.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Verknuepfe den Job mit dem Event (PUT /api/v1/jobs/{id}/link-event) mit eventId | `eventId` wird im Job gesetzt |
| 2 | Rufe Jobs nach Event ab (GET /api/v1/jobs/by-event/{eventId}) | Der verknuepfte Job wird zurueckgegeben |

**Akzeptanzkriterien:**
- [ ] Nur Ersteller, Lehrer, SECTION_ADMIN, SUPERADMIN oder Benutzer mit specialRole ELTERNBEIRAT koennen verknuepfen
- [ ] Event-Ersteller kann ebenfalls verknuepfen
- [ ] Das verknuepfte Event muss existieren (Validierung ueber CalendarModuleApi)

---

### US-218: Putzaktion erzeugt automatisch einen Job
**Als** System **moechte ich** bei Erstellung einer Putzaktion automatisch einen Job in der Jobboerse anlegen, **damit** Putzstunden ueber das Stundenkonto erfasst werden.

**Vorbedingungen:** Cleaning- und Jobboard-Module sind aktiviert. Eine Putzaktion wird erstellt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Erstelle eine neue Putzaktion (Cleaning Config) | `PutzaktionCreatedEvent` wird publiziert |
| 2 | Pruefe die Jobboerse | Ein neuer Job mit Kategorie "Reinigung" wurde automatisch erstellt |
| 3 | Pruefe die Job-Details | Titel, Beschreibung, Bereich, Raum, geschaetzte Stunden und geplantes Datum stimmen mit der Putzaktion ueberein |
| 4 | Pruefe die Verknuepfung | `eventId` des Jobs zeigt auf das Kalender-Event der Putzaktion. Die Cleaning-Config hat die `jobId` des neuen Jobs |

**Akzeptanzkriterien:**
- [ ] `PutzaktionCreatedEvent` wird vom Cleaning-Modul publiziert
- [ ] Jobboard-Modul reagiert via `@ApplicationModuleListener` auf das Event
- [ ] Job wird mit Kategorie "Reinigung", Status OPEN erstellt
- [ ] Bidirektionale Verknuepfung: Job hat `eventId`, CleaningConfig hat `jobId`

---

### US-219: Familien-Zuweisungen abrufen
**Als** Elternteil (P) **moechte ich** alle bestaetigten Zuweisungen meiner Familie sehen, **damit** ich die Stundenhistorie nachvollziehen kann.

**Vorbedingungen:** Bestaetigte Zuweisungen existieren fuer die Familie des Benutzers.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Rufe die Familien-Zuweisungen ab (GET /api/v1/jobs/family/{familyId}/assignments) | Liste der bestaetigten Zuweisungen wird angezeigt |
| 2 | Pruefe den Filter | Nur COMPLETED und bestaetigte Zuweisungen werden angezeigt |
| 3 | Pruefe die Sortierung | Neueste zuerst (absteigend nach completedAt) |

**Akzeptanzkriterien:**
- [ ] Nur bestaetigte COMPLETED-Zuweisungen werden zurueckgegeben
- [ ] Sortierung absteigend nach `completedAt`
- [ ] Nur Familienmitglieder oder Admins koennen die Familien-Zuweisungen abrufen

---

## Modul: Putz-Orga / Cleaning

### US-220: Putzaktion erstellen (wiederkehrend)
**Als** Putz-Administrator **moechte ich** eine wiederkehrende Putzaktion konfigurieren, **damit** woechentlich automatisch Slots generiert werden.

**Vorbedingungen:** Benutzer hat die Rolle SA, SECADMIN oder die spezielle Rolle PUTZORGA. Cleaning-Modul ist aktiviert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Navigiere zur Putz-Orga-Verwaltung (GET /api/v1/cleaning/admin/configs) | Putz-Konfigurations-Uebersicht wird angezeigt |
| 2 | Erstelle eine neue Konfiguration (POST /api/v1/cleaning/admin/configs) mit: Titel "Wochenputz Sonnengruppe", Wochentag 3 (Mittwoch), 14:00-16:00, min 2, max 5, 2.0 Stunden-Gutschrift, ohne specificDate | Konfiguration wird erstellt |
| 3 | Pruefe die Konfiguration | `dayOfWeek=3`, `startTime=14:00`, `endTime=16:00`, `specificDate=null` (wiederkehrend), `active=true` |
| 4 | Pruefe `participantCircle` | Standard: "SECTION" |

**Akzeptanzkriterien:**
- [ ] `specificDate = null` kennzeichnet eine wiederkehrende Putzaktion
- [ ] `dayOfWeek` bestimmt den Wochentag (1=Montag, 7=Sonntag)
- [ ] `hoursCredit` (BigDecimal) definiert die Stundengutschrift
- [ ] `minParticipants` und `maxParticipants` definieren die Teilnehmergrenzen
- [ ] Nur Cleaning-Admins (SA, SECADMIN mit passendem Bereich, oder Benutzer mit PUTZORGA-Rolle) koennen erstellen

---

### US-221: Putzaktion erstellen (einmalig)
**Als** Putz-Administrator **moechte ich** eine einmalige Putzaktion fuer ein bestimmtes Datum erstellen, **damit** Sonderaktionen (z.B. Fruehjahrsputz) geplant werden koennen.

**Vorbedingungen:** Benutzer hat Putz-Admin-Rechte.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Erstelle eine neue Konfiguration mit `specificDate` = 2026-04-15 und Wochentag 3 (Mittwoch passend zum Datum) | Konfiguration wird erstellt |
| 2 | Pruefe die Konfiguration | `specificDate = 2026-04-15` (einmalig) |
| 3 | Generiere Slots fuer den Zeitraum 2026-04-01 bis 2026-04-30 | Nur EIN Slot fuer den 15. April wird generiert (nicht fuer jeden Mittwoch) |

**Akzeptanzkriterien:**
- [ ] `specificDate` ist gesetzt: Putzaktion gilt nur fuer dieses eine Datum
- [ ] Bei Slot-Generierung wird nur fuer das `specificDate` ein Slot erstellt
- [ ] Ein `PutzaktionCreatedEvent` wird publiziert (erzeugt Kalender-Event und Job)

---

### US-222: Slots generieren fuer einen Zeitraum
**Als** Putz-Administrator **moechte ich** Putzslots fuer einen bestimmten Zeitraum generieren, **damit** Eltern sich registrieren koennen.

**Vorbedingungen:** Eine wiederkehrende Putzaktion (ohne specificDate) existiert mit dayOfWeek=3 (Mittwoch).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Generiere Slots (POST /api/v1/cleaning/admin/configs/{id}/generate) fuer 2026-03-01 bis 2026-03-31 | Slots werden fuer jeden Mittwoch im Maerz generiert |
| 2 | Pruefe die generierten Slots | Slots existieren fuer 04.03., 11.03., 18.03., 25.03. |
| 3 | Pruefe die Slot-Details | Jeder Slot hat `configId`, `sectionId`, `slotDate`, `startTime`, `endTime`, `minParticipants`, `maxParticipants`, `status=OPEN` |
| 4 | Generiere nochmals fuer denselben Zeitraum | Bereits existierende Slots werden nicht dupliziert |

**Akzeptanzkriterien:**
- [ ] Slots werden nur fuer den korrekten Wochentag generiert
- [ ] Feiertage und Schulferien werden NICHT automatisch uebersprungen (Anzeige im DatePicker)
- [ ] Doppelte Generierung fuer dasselbe Datum wird verhindert
- [ ] Jeder Slot erhaelt einen eindeutigen QR-Token

---

### US-223: Feiertage und Schulferien im DatePicker
**Als** Putz-Administrator **moechte ich** Feiertage (rot) und Schulferien (orange) im DatePicker sehen, **damit** ich keine Putzaktionen auf freie Tage lege.

**Vorbedingungen:** `tenant_config.bundesland = 'BY'` (Bayern). Schulferien sind in `tenant_config.school_vacations` konfiguriert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Oeffne den DatePicker zur Slot-Generierung | Kalender wird angezeigt |
| 2 | Pruefe gesetzliche Feiertage (z.B. 01.01., Ostern, 01.05., Pfingsten, 03.10., 25.12.) | Feiertage werden ROT markiert |
| 3 | Pruefe Schulferientage | Schulferien werden ORANGE markiert |
| 4 | Aendere das Bundesland auf 'NW' (Nordrhein-Westfalen) | Andere Feiertage werden angezeigt (z.B. Fronleichnam in BY, aber nicht in HH) |

**Akzeptanzkriterien:**
- [ ] Feiertage werden je `bundesland` korrekt berechnet (Gauss-Osteralgorithmus fuer bewegliche Feiertage)
- [ ] Alle 16 Bundeslaender werden unterstuetzt
- [ ] Schulferien stammen aus `tenant_config.school_vacations` (JSONB: `{name, from, to}`)
- [ ] Feiertage: rote Markierung, Schulferien: orange Markierung
- [ ] `useHolidays.ts` Composable berechnet Feiertage clientseitig

---

### US-224: Putzslots anzeigen und filtern
**Als** Elternteil (P) **moechte ich** die verfuegbaren Putzslots sehen, **damit** ich mich fuer einen passenden Termin registrieren kann.

**Vorbedingungen:** Putzslots existieren fuer den aktuellen und naechsten Monat.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Navigiere zur Putz-Uebersicht (GET /api/v1/cleaning/slots/upcoming) | Liste der kommenden Putzslots wird angezeigt |
| 2 | Pruefe die angezeigten Informationen | Datum, Uhrzeit, verfuegbare Plaetze (aktuelle/max Teilnehmer), Status |
| 3 | Filtere nach meinem Bereich | Nur Slots fuer den eigenen Schulbereich werden angezeigt |
| 4 | Pruefe die Status-Anzeige | OPEN (Plaetze frei), FULL (voll), IN_PROGRESS, COMPLETED, CANCELLED |

**Akzeptanzkriterien:**
- [ ] Nur zukuenftige Slots werden in der "upcoming"-Ansicht angezeigt
- [ ] Slot-Status: OPEN, FULL, IN_PROGRESS, COMPLETED, CANCELLED
- [ ] Teilnehmer-Zaehlung wird live aktualisiert

---

### US-225: Fuer Putzslot registrieren
**Als** Elternteil (P) **moechte ich** mich fuer einen Putzslot registrieren, **damit** ich am Putzen teilnehmen und Stunden sammeln kann.

**Vorbedingungen:** Ein offener Putzslot existiert. Benutzer gehoert einem Familienverbund an.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Waehle einen offenen Putzslot | Slot-Detailansicht wird angezeigt |
| 2 | Klicke auf "Registrieren" (POST /api/v1/cleaning/slots/{id}/register) | Registrierung wird erstellt mit `checkedIn=false`, `checkedOut=false`, `confirmed=false` |
| 3 | Pruefe die Registrierung | `userId`, `familyId`, `userName` werden korrekt gesetzt |
| 4 | Pruefe den Slot-Status | Bei Erreichen von maxParticipants: Status wechselt zu FULL |
| 5 | Versuche, sich ein zweites Mal zu registrieren | Fehler: Doppelregistrierung wird verhindert |

**Akzeptanzkriterien:**
- [ ] Registrierung enthaelt `userId`, `familyId`, `userName`, `slotId`
- [ ] Bei vollen Slots (maxParticipants erreicht) ist keine weitere Registrierung moeglich
- [ ] `noShow=false`, `swapOffered=false` als Standardwerte

---

### US-226: Von Putzslot abmelden
**Als** Elternteil (P) **moechte ich** mich von einem Putzslot abmelden, **damit** der Platz fuer andere frei wird.

**Vorbedingungen:** Benutzer ist fuer einen zukuenftigen Putzslot registriert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Navigiere zu "Meine Putztermine" (GET /api/v1/cleaning/slots/mine) | Eigene registrierte Slots werden angezeigt |
| 2 | Klicke auf "Abmelden" (DELETE /api/v1/cleaning/slots/{id}/register) | Registrierung wird entfernt |
| 3 | Pruefe den Slot-Status | Falls vorher FULL: Status wechselt zurueck zu OPEN |
| 4 | Versuche, sich nach bereits erfolgtem Check-in abzumelden | Fehler: Abmeldung nach Check-in nicht moeglich |

**Akzeptanzkriterien:**
- [ ] Abmeldung ist nur VOR dem Check-in moeglich
- [ ] Bei Abmeldung wird der Slot-Status ggf. von FULL auf OPEN zurueckgesetzt
- [ ] Abmeldung loescht die Registrierung aus der Datenbank

---

### US-227: QR-Code-Check-in
**Als** Elternteil (P) **moechte ich** mich per QR-Code am Putztag einchecken, **damit** meine Anwesenheit dokumentiert wird.

**Vorbedingungen:** Benutzer ist fuer einen heutigen Putzslot registriert. Ein QR-Code mit dem `qrToken` des Slots liegt vor.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Scanne den QR-Code oder gib den Token manuell ein | Check-in-Dialog oeffnet sich |
| 2 | Fuehre den Check-in aus (POST /api/v1/cleaning/slots/{id}/checkin mit qrToken) | `checkedIn=true`, `checkInAt` wird auf aktuellen Zeitpunkt gesetzt |
| 3 | Pruefe die Registrierung | Check-in-Zeitstempel ist gesetzt |
| 4 | Versuche ein zweites Check-in | Fehler: Bereits eingecheckt |
| 5 | Versuche Check-in mit falschem QR-Token | Fehler: Ungueltiger Token |

**Akzeptanzkriterien:**
- [ ] QR-Token wird gegen den gespeicherten `qrToken` des Slots validiert
- [ ] `checkInAt` wird als `Instant` (Zeitstempel) gespeichert
- [ ] Doppeltes Check-in wird verhindert
- [ ] Falscher/abgelaufener Token fuehrt zu einem Fehler

---

### US-228: QR-Code-Check-out
**Als** Elternteil (P) **moechte ich** mich nach dem Putzen per Check-out abmelden, **damit** meine tatsaechliche Putzzeit erfasst wird.

**Vorbedingungen:** Benutzer hat bereits eingecheckt (`checkedIn=true`).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Klicke auf "Check-out" (POST /api/v1/cleaning/slots/{id}/checkout) | `checkedOut=true`, `checkOutAt` wird gesetzt |
| 2 | Pruefe die tatsaechliche Dauer | `actualMinutes` wird aus der Differenz von checkInAt und checkOutAt berechnet |
| 3 | Versuche ein Check-out ohne vorheriges Check-in | Fehler: Erst einchecken |

**Akzeptanzkriterien:**
- [ ] Check-out ist nur nach vorherigem Check-in moeglich
- [ ] `actualMinutes` wird aus der Check-in/Check-out-Differenz berechnet
- [ ] `checkOutAt` wird als Zeitstempel gespeichert

---

### US-229: QR-Codes generieren und exportieren
**Als** Putz-Administrator **moechte ich** QR-Codes fuer Putzslots generieren und als PDF exportieren, **damit** sie vor Ort ausgehaengt werden koennen.

**Vorbedingungen:** Putzslots existieren fuer den Bereich.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Rufe den QR-Token fuer einen Slot ab (GET /api/v1/cleaning/admin/slots/{id}/qr-token) | QR-Token wird zurueckgegeben |
| 2 | Exportiere QR-Codes als PDF (GET /api/v1/cleaning/admin/qr-codes/pdf) | PDF mit QR-Codes wird generiert |
| 3 | Pruefe den PDF-Inhalt | Jeder QR-Code enthaelt den Slot-spezifischen Token, Datum und Uhrzeit |

**Akzeptanzkriterien:**
- [ ] Jeder Slot hat einen eindeutigen `qrToken`
- [ ] PDF enthaelt druckbare QR-Codes mit Slot-Informationen (Datum, Uhrzeit)
- [ ] Nur Putz-Admins koennen QR-Codes abrufen

---

### US-230: Swap-Angebot erstellen
**Als** Elternteil (P) **moechte ich** meinen Putztermin zum Tausch anbieten, **damit** ein anderer Elternteil meinen Platz uebernehmen kann.

**Vorbedingungen:** Benutzer ist fuer einen zukuenftigen Putzslot registriert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Navigiere zu meinem registrierten Putzslot | Slot-Details werden angezeigt |
| 2 | Klicke auf "Zum Tausch anbieten" (POST /api/v1/cleaning/slots/{id}/swap) | `swapOffered=true` wird bei der Registrierung gesetzt |
| 3 | Pruefe die oeffentlichen Swap-Angebote (GET /api/v1/cleaning/slots/swaps) | Der angebotene Slot erscheint in der Liste |
| 4 | Ein anderer Elternteil uebernimmt das Angebot | Registrierung wird auf den neuen Benutzer uebertragen |

**Akzeptanzkriterien:**
- [ ] `swapOffered=true` markiert die Registrierung als Tauschangebot
- [ ] Nur der registrierte Benutzer kann seinen Platz anbieten
- [ ] Swap-Angebote sind fuer andere Eltern des gleichen Bereichs sichtbar

---

### US-231: Putzstunden bestaetigen (Admin)
**Als** Putz-Administrator **moechte ich** die Putzstunden eines Teilnehmers bestaetigen, **damit** die Stunden dem Familienverbund gutgeschrieben werden.

**Vorbedingungen:** Ein Teilnehmer hat eingecheckt und ausgecheckt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Navigiere zu den ausstehenden Bestaetigungen (GET /api/v1/cleaning/registrations/pending-confirmation) | Unbestaetigte Registrierungen werden angezeigt |
| 2 | Bestaetigen die Registrierung (POST /api/v1/cleaning/registrations/{id}/confirm) | `confirmed=true`, `confirmedBy`, `confirmedAt` werden gesetzt |
| 3 | Pruefe das Familien-Stundenkonto | Putzstunden werden dem Sonder-Unterkonto (cleaningHours) gutgeschrieben |
| 4 | Lehne eine Registrierung ab (POST /api/v1/cleaning/registrations/{id}/reject) | Registrierung wird als abgelehnt markiert |

**Akzeptanzkriterien:**
- [ ] Nur Putz-Admins koennen Stunden bestaetigen/ablehnen
- [ ] Bestaetigte Stunden fliessen in das Familien-Stundenkonto (Putz-Sonder-Unterkonto)
- [ ] `CleaningCompletedEvent` wird publiziert

---

### US-232: Putzminuten manuell anpassen
**Als** Putz-Administrator **moechte ich** die tatsaechlichen Putzminuten manuell korrigieren, **damit** Fehler bei Check-in/Check-out korrigiert werden koennen.

**Vorbedingungen:** Eine Registrierung mit Check-in und Check-out existiert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Oeffne die Registrierungsdetails | Aktuelle `actualMinutes` werden angezeigt |
| 2 | Aendere die Minuten (PUT /api/v1/cleaning/registrations/{id}/minutes mit neuem Wert) | `actualMinutes` wird aktualisiert, `durationConfirmed=true` |
| 3 | Pruefe die Stundengutschrift | Die neuen Minuten werden fuer die Berechnung verwendet |

**Akzeptanzkriterien:**
- [ ] Nur Putz-Admins koennen Minuten anpassen
- [ ] `durationConfirmed=true` markiert die manuell bestaetigte Dauer
- [ ] Die angepassten Minuten wirken sich auf die Stundengutschrift aus

---

### US-233: Putzslot stornieren
**Als** Putz-Administrator **moechte ich** einen Putzslot stornieren, **damit** ausgefallene Termine korrekt behandelt werden.

**Vorbedingungen:** Ein Putzslot mit Registrierungen existiert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Storniere den Slot (PUT /api/v1/cleaning/admin/slots/{id}/cancel) mit Begruendung | Slot-Status wechselt zu CANCELLED, `cancelled=true`, `cancelReason` wird gesetzt |
| 2 | Pruefe die betroffenen Registrierungen | Registrierte Teilnehmer werden benachrichtigt |
| 3 | Versuche, sich fuer einen stornierten Slot zu registrieren | Fehler: Slot ist storniert |

**Akzeptanzkriterien:**
- [ ] `cancelled=true` und `cancelReason` werden gesetzt
- [ ] Slot-Status wird auf CANCELLED gesetzt
- [ ] Keine neuen Registrierungen fuer stornierte Slots moeglich

---

### US-234: Putz-Dashboard (Admin-Uebersicht)
**Als** Putz-Administrator **moechte ich** ein Dashboard mit Kennzahlen sehen, **damit** ich den Putz-Status meines Bereichs ueberblicke.

**Vorbedingungen:** Putzslots in verschiedenen Status existieren fuer den Bereich.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Rufe das Dashboard ab (GET /api/v1/cleaning/admin/dashboard?sectionId=...&from=...&to=...) | Dashboard-Daten werden angezeigt |
| 2 | Pruefe die Kennzahlen | `totalSlots`, `completedSlots`, `noShows`, `slotsNeedingParticipants` werden korrekt angezeigt |
| 3 | Aendere den Zeitraum | Kennzahlen werden fuer den neuen Zeitraum berechnet |

**Akzeptanzkriterien:**
- [ ] Dashboard zeigt: Gesamtzahl Slots, abgeschlossene Slots, No-Shows, Slots die noch Teilnehmer brauchen
- [ ] Filterbar nach Bereich (`sectionId`) und Zeitraum (`from`, `to`)
- [ ] Nur Putz-Admins des jeweiligen Bereichs haben Zugriff

---

### US-235: Putzaktion mit Raum-Scope
**Als** Raumleiter (LEADER) **moechte ich** eine Putzaktion fuer meinen Raum erstellen, **damit** nur Raum-Mitglieder sich registrieren koennen.

**Vorbedingungen:** Benutzer ist LEADER eines Raums.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Erstelle eine Putzaktion mit `roomId` des eigenen Raums | Konfiguration wird erstellt mit `roomId` gesetzt |
| 2 | Pruefe den `participantCircle` | Standard "SECTION" oder "ROOM" je nach Konfiguration |
| 3 | Pruefe die Slot-Generierung | Slots werden mit der `roomId` generiert |

**Akzeptanzkriterien:**
- [ ] Raumleiter koennen Putzaktionen fuer ihren Raum erstellen
- [ ] `participantCircle` und `participantCircleId` steuern den Teilnehmerkreis
- [ ] Nur Mitglieder des definierten Kreises koennen sich registrieren

---

### US-236: Putzaktion Kalender-Event-Verknuepfung
**Als** System **moechte ich** bei Erstellung einer Putzaktion automatisch ein Kalender-Event erstellen, **damit** der Putztermin im Schulkalender sichtbar ist.

**Vorbedingungen:** Cleaning- und Calendar-Module sind aktiviert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Erstelle eine neue Putzaktion | `PutzaktionCreatedEvent` wird publiziert |
| 2 | Pruefe das Kalender-Modul | Ein neuer Kalender-Event mit `eventType='CLEANING'` wurde erstellt |
| 3 | Pruefe die Verknuepfung | `cleaning_configs.calendar_event_id` zeigt auf das neue Event |

**Akzeptanzkriterien:**
- [ ] Putzaktion erstellt automatisch ein Kalender-Event
- [ ] `calendarEventId` in der CleaningConfig wird gesetzt
- [ ] Event ist im Kalender als Putz-Event erkennbar

---

### US-237: Putzslot bearbeiten
**Als** Putz-Administrator **moechte ich** einen einzelnen Putzslot bearbeiten, **damit** Zeiten oder Teilnehmergrenzen angepasst werden koennen.

**Vorbedingungen:** Ein Putzslot existiert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Bearbeite den Slot (PUT /api/v1/cleaning/admin/slots/{id}) mit neuer startTime, endTime, minParticipants, maxParticipants | Slot-Daten werden aktualisiert |
| 2 | Pruefe die aktualisierten Werte | Neue Werte sind gesetzt, `updatedAt` ist aktualisiert |

**Akzeptanzkriterien:**
- [ ] Nur startTime, endTime, minParticipants, maxParticipants sind aenderbar
- [ ] Datum und configId sind nicht aenderbar
- [ ] Nur Putz-Admins des Bereichs koennen Slots bearbeiten

---

### US-238: Putzaktion-Konfiguration bearbeiten
**Als** Putz-Administrator **moechte ich** eine Putzaktion-Konfiguration bearbeiten, **damit** Titel, Zeiten oder Stundengutschrift angepasst werden koennen.

**Vorbedingungen:** Eine CleaningConfig existiert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Bearbeite die Konfiguration (PUT /api/v1/cleaning/admin/configs/{id}) mit neuem Titel und Zeiten | Konfiguration wird aktualisiert |
| 2 | Aendere `active` auf `false` | Konfiguration wird deaktiviert, keine neuen Slots werden generiert |
| 3 | Pruefe `hoursCredit` | Stundengutschrift kann angepasst werden |

**Akzeptanzkriterien:**
- [ ] Aenderbar: title, description, startTime, endTime, minParticipants, maxParticipants, hoursCredit, active
- [ ] NICHT aenderbar: sectionId, dayOfWeek, specificDate (hierzu neue Config erstellen)
- [ ] Deaktivierte Configs (`active=false`) generieren keine neuen Slots

---

## Modul: Fotobox

### US-250: Fotobox-Einstellungen konfigurieren (Raumleiter)
**Als** Raumleiter (LEADER) **moechte ich** die Fotobox-Einstellungen fuer meinen Raum konfigurieren, **damit** Mitglieder Fotos teilen koennen.

**Vorbedingungen:** Benutzer ist LEADER eines Raums. Fotobox-Modul ist aktiviert (`monteweb.modules.fotobox.enabled=true`).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Navigiere zu den Fotobox-Einstellungen (GET /api/v1/rooms/{roomId}/fotobox/settings) | Aktuelle Einstellungen werden angezeigt (oder Standardwerte) |
| 2 | Aktiviere die Fotobox (PUT /api/v1/rooms/{roomId}/fotobox/settings) mit `enabled=true` | Fotobox wird fuer den Raum aktiviert |
| 3 | Setze `defaultPermission` auf "POST_IMAGES" | Standard-Berechtigung fuer Raum-Mitglieder wird auf POST_IMAGES gesetzt |
| 4 | Setze `maxImagesPerThread` auf 50 und `maxFileSizeMb` auf 10 | Limits werden gespeichert |

**Akzeptanzkriterien:**
- [ ] Standard-Einstellungen: `enabled=false`, `defaultPermission=VIEW_ONLY`, `maxFileSizeMb=10`
- [ ] Nur LEADER oder SUPERADMIN koennen Einstellungen aendern
- [ ] `maxImagesPerThread` <= 0 oder null bedeutet kein Limit
- [ ] Einstellungen gelten raumweit

---

### US-251: Fotobox-Thread erstellen
**Als** Raumleiter (LEADER) oder Benutzer mit CREATE_THREADS-Berechtigung **moechte ich** einen neuen Foto-Thread erstellen, **damit** Fotos thematisch gruppiert werden koennen.

**Vorbedingungen:** Fotobox ist fuer den Raum aktiviert. Benutzer hat mindestens CREATE_THREADS-Berechtigung.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Klicke auf "Neuen Thread erstellen" | Thread-Erstellungsdialog oeffnet sich |
| 2 | Gib Titel "Sommerfest 2026" (max 300 Zeichen) und Beschreibung (max 2000 Zeichen) ein | Felder werden akzeptiert |
| 3 | Waehle Audience "ALL" und klicke "Erstellen" (POST /api/v1/rooms/{roomId}/fotobox/threads) | Thread wird erstellt mit `audience=ALL`, `createdBy` wird gesetzt |
| 4 | Pruefe die Thread-Liste (GET /api/v1/rooms/{roomId}/fotobox/threads) | Neuer Thread erscheint in der Liste |

**Akzeptanzkriterien:**
- [ ] CREATE_THREADS-Berechtigung wird geprueft (ordinal-basiert: CREATE_THREADS > POST_IMAGES > VIEW_ONLY)
- [ ] LEADER und SUPERADMIN haben immer CREATE_THREADS-Berechtigung
- [ ] Thread hat Felder: title, description, audience, createdBy, coverImageId (optional)
- [ ] `FotoboxThreadCreatedEvent` wird publiziert

---

### US-252: Thread-Audience fuer verschiedene Rollen
**Als** Elternteil (P) **moechte ich** Threads mit Audience PARENTS_ONLY erstellen, **damit** nur Eltern die Fotos sehen koennen.

**Vorbedingungen:** Benutzer ist Elternteil mit CREATE_THREADS-Berechtigung.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Erstelle einen Thread ohne explizite Audience-Angabe als Elternteil | Thread wird mit `audience=PARENTS_ONLY` erstellt (automatisch fuer Eltern) |
| 2 | Erstelle als LEADER einen Thread mit Audience "ALL" | Thread mit `audience=ALL` wird erstellt |
| 3 | Erstelle als LEADER einen Thread mit Audience "STUDENTS_ONLY" | Thread mit `audience=STUDENTS_ONLY` wird erstellt |
| 4 | Logge dich als Schueler ein und pruefe die Thread-Liste | Nur Threads mit Audience ALL und STUDENTS_ONLY sind sichtbar |
| 5 | Logge dich als Elternteil ein und pruefe die Thread-Liste | Nur Threads mit Audience ALL und PARENTS_ONLY sind sichtbar |

**Akzeptanzkriterien:**
- [ ] Eltern erstellen automatisch `PARENTS_ONLY`-Threads (resolveAudience)
- [ ] Lehrer/Leader/Admins koennen die Audience explizit waehlen (ALL, PARENTS_ONLY, STUDENTS_ONLY)
- [ ] Thread-Liste wird serverseitig nach Audience gefiltert basierend auf der Benutzerrolle
- [ ] `getAllowedAudiences()` bestimmt die sichtbaren Audiences pro Benutzer

---

### US-253: Fotos hochladen (max 20 pro Upload)
**Als** Raum-Mitglied mit POST_IMAGES-Berechtigung **moechte ich** Fotos in einen Thread hochladen, **damit** ich Bilder mit der Gruppe teilen kann.

**Vorbedingungen:** Ein Thread existiert. Benutzer hat mindestens POST_IMAGES-Berechtigung und eine aktive PHOTO_CONSENT.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Waehle einen Thread und klicke auf "Fotos hochladen" | Upload-Dialog oeffnet sich |
| 2 | Waehle 5 JPEG-Dateien und lade sie hoch (POST /api/v1/rooms/{roomId}/fotobox/threads/{threadId}/images) | 5 Bilder werden hochgeladen, Thumbnails automatisch generiert |
| 3 | Pruefe die Thumbnail-Anzeige | Thumbnails (400x400, JPEG, 80% Qualitaet) werden angezeigt |
| 4 | Versuche 21 Dateien gleichzeitig hochzuladen | Fehler: Maximal 20 Dateien pro Upload |
| 5 | Versuche ohne PHOTO_CONSENT hochzuladen | Fehler 403: "PHOTO_CONSENT required to upload images to Fotobox" |

**Akzeptanzkriterien:**
- [ ] Maximum 20 Dateien pro Upload (`MAX_FILES_PER_UPLOAD = 20`)
- [ ] Erlaubte Content-Types: image/jpeg, image/png, image/webp, image/gif
- [ ] Thumbnails werden automatisch generiert (400px, JPEG, 80% Qualitaet)
- [ ] PHOTO_CONSENT ist Pflicht fuer Uploads
- [ ] POST_IMAGES-Berechtigung ist Mindestanforderung

---

### US-254: Content-Type-Validierung ueber Magic Bytes
**Als** System **moechte ich** den Dateityp ueber Magic Bytes (nicht den deklarierten Content-Type) pruefen, **damit** keine als Bilder getarnten Schadateien hochgeladen werden koennen.

**Vorbedingungen:** Upload-Endpunkt ist erreichbar.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Lade eine echte JPEG-Datei hoch (Magic Bytes: FF D8 FF) | Upload erfolgreich, `contentType=image/jpeg` |
| 2 | Lade eine echte PNG-Datei hoch (Magic Bytes: 89 50 4E 47) | Upload erfolgreich, `contentType=image/png` |
| 3 | Lade eine echte GIF-Datei hoch (Magic Bytes: 47 49 46) | Upload erfolgreich, `contentType=image/gif` |
| 4 | Lade eine echte WebP-Datei hoch (Magic Bytes: 52 49 46 46 ... 57 45 42 50) | Upload erfolgreich, `contentType=image/webp` |
| 5 | Benenne eine EXE-Datei in .jpg um und versuche den Upload | Fehler: "File content does not match a valid image format. Allowed: JPEG, PNG, WebP, GIF" |
| 6 | Lade eine Datei < 4 Bytes hoch | Fehler: "File too small to be a valid image" |

**Akzeptanzkriterien:**
- [ ] JPEG: Bytes 0-2 = `FF D8 FF`
- [ ] PNG: Bytes 0-3 = `89 50 4E 47`
- [ ] GIF: Bytes 0-2 = `47 49 46`
- [ ] WebP: Bytes 0-3 = `52 49 46 46` UND Bytes 8-11 = `57 45 42 50`
- [ ] Dateien < 4 Bytes werden abgelehnt
- [ ] Der deklarierte Content-Type-Header wird ignoriert (nur Magic Bytes zaehlen)

---

### US-255: Lightbox-Ansicht
**Als** Raum-Mitglied mit VIEW_ONLY-Berechtigung **moechte ich** Fotos in einer Lightbox-Ansicht betrachten, **damit** ich Bilder im Grossformat sehen kann.

**Vorbedingungen:** Ein Thread mit hochgeladenen Bildern existiert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Oeffne einen Thread und klicke auf ein Thumbnail | Lightbox oeffnet sich mit dem Bild im Grossformat |
| 2 | Navigiere mit Pfeiltasten zum naechsten/vorherigen Bild | Bilder werden in der Reihenfolge von `sortOrder` angezeigt |
| 3 | Pruefe die Bild-URL | Bild wird ueber GET /api/v1/fotobox/images/{id}?token={jwt} geladen |
| 4 | Pruefe die Thumbnail-URL | Thumbnail ueber GET /api/v1/fotobox/images/{id}/thumbnail?token={jwt} |

**Akzeptanzkriterien:**
- [ ] Bilder werden ueber JWT-Token-Authentifizierung per Query-Parameter geladen (`?token=`)
- [ ] Sortierung nach `sortOrder` aufsteigend, dann `createdAt` aufsteigend
- [ ] VIEW_ONLY-Berechtigung genuegt zum Betrachten
- [ ] Lightbox unterstuetzt Vor-/Zurueck-Navigation

---

### US-256: Berechtigungshierarchie der Fotobox
**Als** System **moechte ich** die Fotobox-Berechtigungen hierarchisch pruefen, **damit** die richtige Zugriffsebene durchgesetzt wird.

**Vorbedingungen:** Fotobox ist fuer einen Raum aktiviert. Verschiedene Benutzerrollen existieren.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Pruefe SUPERADMIN-Berechtigung | Immer CREATE_THREADS (hoechste Stufe) |
| 2 | Pruefe LEADER-Berechtigung | Immer CREATE_THREADS (hoechste Stufe) |
| 3 | Setze `defaultPermission=VIEW_ONLY` und pruefe normales Mitglied | Nur VIEW_ONLY (kann Bilder ansehen, nicht hochladen oder Threads erstellen) |
| 4 | Setze `defaultPermission=POST_IMAGES` und pruefe normales Mitglied | POST_IMAGES (kann Bilder hochladen, aber keine Threads erstellen) |
| 5 | Versuche als VIEW_ONLY-Benutzer Bilder hochzuladen | Fehler: "Insufficient fotobox permissions" |
| 6 | Versuche als Nicht-Mitglied auf die Fotobox zuzugreifen | Fehler: "Not a member of this room" |
| 7 | Versuche auf einen Raum mit deaktivierter Fotobox zuzugreifen | Fehler: "Fotobox not enabled for this room" |

**Akzeptanzkriterien:**
- [ ] Hierarchie: CREATE_THREADS (ordinal 2) > POST_IMAGES (ordinal 1) > VIEW_ONLY (ordinal 0)
- [ ] SUPERADMIN: immer CREATE_THREADS, auch ohne Raum-Mitgliedschaft
- [ ] LEADER: immer CREATE_THREADS
- [ ] Andere Mitglieder: `defaultPermission` aus den Raum-Einstellungen
- [ ] Nicht-Mitglieder: Zugriff verweigert (ausser SUPERADMIN)
- [ ] Deaktivierte Fotobox: Zugriff fuer alle verweigert

---

### US-257: Foto loeschen
**Als** Foto-Uploader oder Raumleiter **moechte ich** ein Foto loeschen, **damit** fehlerhafte oder ungewuenschte Bilder entfernt werden koennen.

**Vorbedingungen:** Ein Bild existiert in einem Thread.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Logge dich als Uploader des Bildes ein | Login erfolgreich |
| 2 | Loesche das Bild (DELETE /api/v1/rooms/{roomId}/fotobox/images/{imageId}) | Bild und Thumbnail werden aus MinIO und DB geloescht |
| 3 | Versuche als anderer Benutzer (nicht Uploader, nicht Leader) zu loeschen | Fehler: Nur Uploader oder Leader/Admin koennen loeschen |
| 4 | Logge dich als LEADER ein und loesche ein beliebiges Bild | Loeschung erfolgreich (Leader kann alle Bilder loeschen) |

**Akzeptanzkriterien:**
- [ ] Bild-Uploader kann eigene Bilder loeschen
- [ ] LEADER und SUPERADMIN koennen alle Bilder loeschen (isImageOwnerOrLeader)
- [ ] Original-Datei UND Thumbnail werden aus MinIO geloescht
- [ ] DB-Eintrag wird entfernt

---

### US-258: Thread loeschen mit allen Bildern
**Als** Thread-Ersteller oder Raumleiter **moechte ich** einen Thread inklusive aller Bilder loeschen, **damit** ein ganzes Fotoalbum entfernt werden kann.

**Vorbedingungen:** Ein Thread mit mehreren Bildern existiert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Loesche den Thread (DELETE /api/v1/rooms/{roomId}/fotobox/threads/{threadId}) | Thread und ALLE zugehoerigen Bilder werden geloescht |
| 2 | Pruefe MinIO | Alle Originale und Thumbnails des Threads sind geloescht |
| 3 | Pruefe die Datenbank | Thread-Eintrag und alle Image-Eintraege sind entfernt |
| 4 | Versuche als fremder Benutzer zu loeschen | Fehler: "Only thread creator or room leader can delete this thread" |

**Akzeptanzkriterien:**
- [ ] Nur Thread-Ersteller oder Leader/Admin koennen den Thread loeschen (isThreadOwnerOrLeader)
- [ ] Alle Bilder des Threads werden zuerst aus MinIO geloescht (Originale + Thumbnails)
- [ ] Dann werden alle Image-DB-Eintraege geloescht (`deleteAllByThreadId`)
- [ ] Zuletzt wird der Thread-Eintrag selbst geloescht

---

### US-259: Thread bearbeiten
**Als** Thread-Ersteller oder Raumleiter **moechte ich** den Titel und die Beschreibung eines Threads aendern, **damit** Korrekturen moeglich sind.

**Vorbedingungen:** Ein Thread existiert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Bearbeite den Thread (PUT /api/v1/rooms/{roomId}/fotobox/threads/{threadId}) mit neuem Titel | Titel wird aktualisiert |
| 2 | Bearbeite die Beschreibung | Beschreibung wird aktualisiert |
| 3 | Pruefe, dass der Thread einem anderen Raum zugeordnet ist | Fehler: ResourceNotFoundException |
| 4 | Versuche als fremder Benutzer zu bearbeiten | Fehler: "Only thread creator or room leader can edit this thread" |

**Akzeptanzkriterien:**
- [ ] Nur Thread-Ersteller oder Leader/Admin koennen bearbeiten
- [ ] Nur `title` und `description` sind aenderbar (nicht `audience`, `roomId`)
- [ ] `updatedAt` wird aktualisiert
- [ ] Room-Zugehoerigkeit wird validiert

---

### US-260: Bild-Caption aktualisieren
**Als** Bild-Uploader oder Raumleiter **moechte ich** die Bildbeschreibung (Caption) aendern, **damit** ich Kommentare zu Fotos hinzufuegen kann.

**Vorbedingungen:** Ein Bild existiert in einem Thread.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Bearbeite das Bild (PUT /api/v1/rooms/{roomId}/fotobox/images/{imageId}) mit Caption (max 500 Zeichen) | Caption wird aktualisiert |
| 2 | Pruefe die Bildliste des Threads | Neue Caption wird angezeigt |

**Akzeptanzkriterien:**
- [ ] Caption max 500 Zeichen
- [ ] Nur Uploader oder Leader/Admin koennen die Caption aendern
- [ ] Bild hat zusaetzliche Metadaten: width, height, sortOrder

---

## Modul: Fundgrube / Lost & Found

### US-270: Fundgegenstand einstellen
**Als** beliebiger Benutzer **moechte ich** einen Fundgegenstand einstellen, **damit** der Eigentuemer ihn wiederfinden kann.

**Vorbedingungen:** Benutzer ist eingeloggt. Fundgrube-Modul ist aktiviert (`monteweb.modules.fundgrube.enabled=true`).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Navigiere zur Fundgrube | Fundgrube-Uebersicht wird angezeigt |
| 2 | Klicke auf "Gegenstand einstellen" | Formular oeffnet sich |
| 3 | Gib Titel "Blaue Trinkflasche" (max 300 Zeichen), Beschreibung "Gefunden in der Turnhalle" (max 2000 Zeichen) ein | Felder werden akzeptiert |
| 4 | Waehle optional einen Schulbereich (sectionId) | Bereich wird gesetzt (oder null fuer schulweit) |
| 5 | Klicke auf "Erstellen" (POST /api/v1/fundgrube/items) | Item wird erstellt mit `createdBy`, `createdAt`, ohne `claimedBy` |

**Akzeptanzkriterien:**
- [ ] Alle eingeloggten Benutzer koennen Gegenstaende einstellen
- [ ] Titel ist Pflichtfeld (max 300 Zeichen), Beschreibung optional (max 2000 Zeichen)
- [ ] `sectionId` ist optional (null = schulweit sichtbar)
- [ ] `claimedBy`, `claimedAt`, `expiresAt` sind initial null

---

### US-271: Fotos zum Fundgegenstand hochladen
**Als** Ersteller eines Fundgegenstands **moechte ich** Fotos hochladen, **damit** der Eigentuemer den Gegenstand leichter identifizieren kann.

**Vorbedingungen:** Ein Fundgrube-Item existiert, erstellt vom Benutzer.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Lade ein Foto hoch (POST /api/v1/fundgrube/items/{itemId}/images) | Bild wird in MinIO gespeichert, Thumbnail automatisch generiert |
| 2 | Pruefe die Bild-Details | `originalFilename`, `storagePath`, `thumbnailPath`, `contentType`, `fileSize`, `createdAt` werden gespeichert |
| 3 | Rufe das Bild ab (GET /api/v1/fundgrube/images/{imageId}?token={jwt}) | Originalbild wird zurueckgegeben |
| 4 | Rufe das Thumbnail ab (GET /api/v1/fundgrube/images/{imageId}/thumbnail?token={jwt}) | Thumbnail wird zurueckgegeben |

**Akzeptanzkriterien:**
- [ ] Bilder werden in MinIO gespeichert
- [ ] Thumbnails werden automatisch generiert
- [ ] Bilder sind ueber JWT-Token-Parameter (`?token=`) abrufbar
- [ ] Content-Type wird korrekt erkannt und gespeichert

---

### US-272: Fundgegenstaende auflisten mit Bereichsfilter
**Als** Benutzer **moechte ich** die Fundgrube nach Schulbereich filtern, **damit** ich schneller finde, was ich suche.

**Vorbedingungen:** Mehrere Fundgegenstaende mit verschiedenen sectionIds existieren.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Rufe alle Gegenstaende ab (GET /api/v1/fundgrube/items) | Alle nicht-beanspruchten Gegenstaende werden angezeigt |
| 2 | Filtere nach Bereich (GET /api/v1/fundgrube/items?sectionId={id}) | Nur Gegenstaende des gewaehlten Bereichs werden angezeigt |
| 3 | Pruefe einen Gegenstand ohne sectionId | Dieser erscheint bei ALLEN Bereichsfiltern (schulweit) |

**Akzeptanzkriterien:**
- [ ] Gegenstaende ohne `sectionId` sind schulweit sichtbar
- [ ] Bereichsfilter ist optional
- [ ] Bereits beanspruchte (claimed) Gegenstaende koennen separat gefiltert werden

---

### US-273: Fundgegenstand beanspruchen (Claim)
**Als** Benutzer **moechte ich** einen Fundgegenstand beanspruchen, **damit** ich ihn abholen kann.

**Vorbedingungen:** Ein unbeanspruchter Fundgegenstand existiert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Oeffne einen Fundgegenstand und klicke auf "Beanspruchen" | Claim-Dialog oeffnet sich |
| 2 | Gib optionalen Kommentar ein (max 1000 Zeichen) und sende (POST /api/v1/fundgrube/items/{itemId}/claim) | `claimedBy` wird auf den Benutzer gesetzt, `claimedAt` auf den aktuellen Zeitpunkt |
| 3 | Pruefe `expiresAt` | `expiresAt = claimedAt + 24 Stunden` |
| 4 | Versuche, einen bereits beanspruchten Gegenstand erneut zu beanspruchen | Fehler: Gegenstand ist bereits beansprucht |

**Akzeptanzkriterien:**
- [ ] `claimedBy` wird auf den beanspruchenden Benutzer gesetzt
- [ ] `claimedAt` wird auf `Instant.now()` gesetzt
- [ ] `expiresAt` wird auf `claimedAt + 24 Stunden` gesetzt
- [ ] Kommentar ist optional (max 1000 Zeichen)
- [ ] Doppelbeanspruchung wird verhindert

---

### US-274: Claim-Ablauf nach 24 Stunden
**Als** System **moechte ich** beanspruchte Gegenstaende nach 24 Stunden automatisch loeschen, **damit** nicht abgeholte Gegenstaende nicht dauerhaft blockiert werden.

**Vorbedingungen:** Ein Gegenstand wurde vor mehr als 24 Stunden beansprucht. Der `@Scheduled`-Job laeuft taeglich um 3:00 Uhr.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Beanspruche einen Gegenstand | `expiresAt` wird auf claimedAt + 24h gesetzt |
| 2 | Warte bis der Cleanup-Job laeuft (taeglich 3:00 Uhr) oder simuliere den Ablauf | `FundgrubeCleanupService.deleteExpiredItems()` wird ausgefuehrt |
| 3 | Pruefe den Gegenstand nach Ablauf | Item und alle zugehoerigen Bilder (Originale + Thumbnails) sind aus DB und MinIO geloescht |
| 4 | Pruefe nicht-abgelaufene Gegenstaende | Noch nicht abgelaufene Gegenstaende bleiben erhalten |

**Akzeptanzkriterien:**
- [ ] Cleanup-Job laeuft taeglich um 3:00 Uhr (`@Scheduled(cron = "0 0 3 * * *")`)
- [ ] Nur Gegenstaende mit `expiresAt < Instant.now()` werden geloescht
- [ ] Fuer jeden abgelaufenen Gegenstand: alle Bilder (Originale + Thumbnails) aus MinIO loeschen
- [ ] Dann DB-Eintraege loeschen (Item + Images)
- [ ] Log-Eintrag: "Fundgrube cleanup: deleted X expired items"

---

### US-275: Fundgegenstand bearbeiten
**Als** Ersteller eines Fundgegenstands **moechte ich** den Titel und die Beschreibung aendern, **damit** ich Fehler korrigieren kann.

**Vorbedingungen:** Ein Fundgrube-Item existiert, erstellt vom Benutzer.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Bearbeite den Gegenstand (PUT /api/v1/fundgrube/items/{itemId}) mit neuem Titel | Titel wird aktualisiert, `updatedAt` wird gesetzt |
| 2 | Aendere die Beschreibung | Beschreibung wird aktualisiert |
| 3 | Versuche als anderer Benutzer zu bearbeiten | Fehler: Nur Ersteller oder Admins koennen bearbeiten |

**Akzeptanzkriterien:**
- [ ] Nur der Ersteller oder Admins koennen bearbeiten
- [ ] `updatedAt` wird bei jeder Aenderung aktualisiert
- [ ] Titel und Beschreibung sind aenderbar, sectionId kann geaendert werden

---

### US-276: Fundgegenstand loeschen
**Als** Ersteller eines Fundgegenstands **moechte ich** den Gegenstand loeschen, **damit** er aus der Liste entfernt wird (z.B. wenn der Eigentuemer gefunden wurde).

**Vorbedingungen:** Ein Fundgrube-Item mit Bildern existiert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Loesche den Gegenstand (DELETE /api/v1/fundgrube/items/{itemId}) | Item, alle zugehoerigen Bilder (DB + MinIO) werden geloescht |
| 2 | Pruefe MinIO | Originale und Thumbnails sind entfernt |
| 3 | Pruefe die Datenbank | Kein Item- oder Image-Eintrag mehr vorhanden |
| 4 | Versuche als fremder Benutzer zu loeschen | Fehler: Nur Ersteller oder Admins koennen loeschen |

**Akzeptanzkriterien:**
- [ ] Nur der Ersteller oder Admins koennen loeschen
- [ ] Alle Bilder werden vor dem Item aus MinIO geloescht
- [ ] Kaskadenloesung: Images zuerst, dann Item

---

### US-277: Bild vom Fundgegenstand loeschen
**Als** Ersteller eines Fundgegenstands **moechte ich** einzelne Fotos entfernen, **damit** falsche oder doppelte Bilder geloescht werden koennen.

**Vorbedingungen:** Ein Fundgrube-Item mit mehreren Bildern existiert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Loesche ein einzelnes Bild (DELETE /api/v1/fundgrube/images/{imageId}) | Bild (Original + Thumbnail) wird aus MinIO und DB entfernt |
| 2 | Pruefe die verbleibenden Bilder | Andere Bilder bleiben erhalten |
| 3 | Pruefe den Fundgegenstand | Item existiert weiterhin |

**Akzeptanzkriterien:**
- [ ] Nur der Ersteller des Items oder Admins koennen Bilder loeschen
- [ ] Original und Thumbnail werden aus MinIO geloescht
- [ ] DB-Eintrag des Bildes wird entfernt
- [ ] Das Item selbst bleibt erhalten

---

### US-278: Fundgegenstand-Detailansicht
**Als** Benutzer **moechte ich** die Details eines Fundgegenstands mit allen Bildern sehen, **damit** ich pruefen kann, ob es mein verlorener Gegenstand ist.

**Vorbedingungen:** Ein Fundgegenstand mit Bildern existiert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Oeffne die Detailansicht (GET /api/v1/fundgrube/items/{itemId}) | Titel, Beschreibung, Bereich, Ersteller, Erstelldatum werden angezeigt |
| 2 | Pruefe den Claim-Status | Unbeansprucht: kein claimedBy/claimedAt. Beansprucht: claimedBy, claimedAt, expiresAt |
| 3 | Pruefe die Bilder | Thumbnails werden geladen, Klick auf Thumbnail zeigt Originalbild |
| 4 | Pruefe die `isClaimed()`-Methode | `true` wenn `claimedBy != null`, sonst `false` |

**Akzeptanzkriterien:**
- [ ] Alle Item-Details werden angezeigt (inklusive Claim-Status)
- [ ] Bilder werden als Thumbnails mit Lightbox-Funktion angezeigt
- [ ] Bild-URLs enthalten JWT-Token als Query-Parameter
- [ ] `isClaimed()` prueft ob `claimedBy != null`

---

### US-279: Fundgrube bei deaktiviertem Modul
**Als** System **moechte ich** sicherstellen, dass die Fundgrube nur verfuegbar ist, wenn das Modul aktiviert ist.

**Vorbedingungen:** `monteweb.modules.fundgrube.enabled = false`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Versuche auf GET /api/v1/fundgrube/items zuzugreifen | HTTP 404 (Endpoint nicht registriert) |
| 2 | Pruefe das Frontend-Menue | Fundgrube-Menuepunkt ist nicht sichtbar |
| 3 | Aktiviere das Modul und starte die Anwendung neu | Fundgrube ist verfuegbar |

**Akzeptanzkriterien:**
- [ ] `@ConditionalOnProperty(prefix = "monteweb.modules", name = "fundgrube.enabled", havingValue = "true")` auf allen Beans
- [ ] Bei deaktiviertem Modul: kein Controller, kein Service, kein Cleanup-Job registriert
- [ ] Frontend zeigt den Menuepunkt nur bei aktiviertem Modul

---

### US-280: Fundgrube-Item mit abgelaufenem Claim wieder verfuegbar
**Als** System **moechte ich** beanspruchte Gegenstaende nach Ablauf der 24-Stunden-Frist loeschen, **damit** die Fundgrube nicht mit ungenutzten Claims blockiert wird.

**Vorbedingungen:** Mehrere Items existieren: eines mit abgelaufenem Claim, eines mit aktivem Claim, eines unbeansprucht.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Fuehre den Cleanup-Job aus | Nur das Item mit abgelaufenem Claim wird geloescht |
| 2 | Pruefe das Item mit aktivem Claim | Bleibt erhalten (expiresAt liegt in der Zukunft) |
| 3 | Pruefe das unbeanspruchte Item | Bleibt erhalten (kein expiresAt gesetzt) |

**Akzeptanzkriterien:**
- [ ] `itemRepo.findExpired(now)` findet nur Items mit `expiresAt < now`
- [ ] Items ohne `expiresAt` (unbeansprucht) werden NICHT geloescht
- [ ] Items mit `expiresAt` in der Zukunft werden NICHT geloescht
- [ ] Geloeschte Items: erst MinIO-Dateien loeschen, dann `itemRepo.deleteExpired(now)`

---

### US-281: Fundgrube-Suche ueber mehrere Bereiche
**Als** SUPERADMIN (SA) **moechte ich** alle Fundgegenstaende aller Bereiche sehen, **damit** ich einen vollstaendigen Ueberblick habe.

**Vorbedingungen:** Fundgegenstaende in verschiedenen Schulbereichen existieren.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Rufe alle Items ohne Bereichsfilter ab (GET /api/v1/fundgrube/items) | Alle Gegenstaende aller Bereiche werden angezeigt |
| 2 | Filtere nach einem bestimmten Bereich | Nur Gegenstaende dieses Bereichs und schulweite (sectionId=null) werden angezeigt |
| 3 | Pruefe die Sortierung | Neueste Gegenstaende zuerst |

**Akzeptanzkriterien:**
- [ ] Ohne Filter: alle Gegenstaende sichtbar
- [ ] Mit sectionId-Filter: nur Gegenstaende des Bereichs + schulweite Items
- [ ] SUPERADMIN hat Zugriff auf alle Bereiche

---

### US-282: Fundgrube Berechtigungspruefung fuer Loeschen/Bearbeiten
**Als** System **moechte ich** sicherstellen, dass nur berechtigte Benutzer Items bearbeiten oder loeschen koennen.

**Vorbedingungen:** Fundgegenstaende verschiedener Ersteller existieren.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Ersteller bearbeitet sein eigenes Item | Erfolgreich |
| 2 | Fremder Benutzer (gleiche Rolle) versucht zu bearbeiten | Fehler: Zugriff verweigert |
| 3 | SUPERADMIN bearbeitet fremdes Item | Erfolgreich |
| 4 | Ersteller loescht sein eigenes Item | Erfolgreich |
| 5 | Fremder Benutzer versucht zu loeschen | Fehler: Zugriff verweigert |
| 6 | SUPERADMIN loescht fremdes Item | Erfolgreich |

**Akzeptanzkriterien:**
- [ ] Ersteller hat volle Rechte auf sein eigenes Item (CRUD)
- [ ] Fremde Benutzer gleicher oder niedrigerer Rolle koennen nicht bearbeiten/loeschen
- [ ] SUPERADMIN kann alle Items bearbeiten/loeschen
- [ ] Beanspruchen (Claim) kann jeder eingeloggte Benutzer bei unbeanspruchten Items

---


**Rollen:** SUPERADMIN (SA), SECTION_ADMIN (SECADMIN), TEACHER (T), PARENT (P), STUDENT (S)

**Testkonten:**
| Konto | Rolle | Passwort |
|-------|-------|----------|
| admin@monteweb.local | SUPERADMIN | admin123 |
| sectionadmin@monteweb.local | SECTION_ADMIN | test1234 |
| lehrer@monteweb.local | TEACHER | test1234 |
| eltern@monteweb.local | PARENT | test1234 |
| schueler@monteweb.local | STUDENT | test1234 |

---

## Modul: Wiki

### US-300: Wiki-Seite erstellen
**Als** Raum-Mitglied **moechte ich** eine Wiki-Seite in meinem Raum erstellen, **damit** ich Wissen fuer alle Raummitglieder dokumentieren kann.

**Vorbedingungen:** Wiki-Modul aktiviert (`monteweb.modules.wiki.enabled=true`). Benutzer ist Mitglied eines Raumes.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `lehrer@monteweb.local` (T) einloggen | Login erfolgreich |
| 2 | Raum "Sonnengruppe" oeffnen | Raum-Ansicht wird angezeigt |
| 3 | Wiki-Tab im Raum-Menue anklicken | Wiki-Seiten-Baum wird angezeigt (ggf. leer) |
| 4 | "Neue Seite"-Button klicken | Formular zum Erstellen einer Wiki-Seite oeffnet sich |
| 5 | Titel eingeben: "Klassenregeln" | Titel-Feld wird befuellt |
| 6 | Markdown-Inhalt eingeben: `# Regeln\n- Regel 1\n- Regel 2` | Editor zeigt Markdown-Text |
| 7 | Optional: Elternseite (parent) auswaehlen | Dropdown mit vorhandenen Seiten wird angezeigt |
| 8 | "Speichern" klicken | Seite wird erstellt, Slug wird automatisch generiert (z.B. `klassenregeln`) |
| 9 | Seite wird im Wiki-Baum angezeigt | Neuer Eintrag "Klassenregeln" im Baum sichtbar |

**Akzeptanzkriterien:**
- [ ] Wiki-Seite wird mit Titel und Markdown-Inhalt erstellt
- [ ] Slug wird automatisch aus dem Titel generiert
- [ ] Seite erscheint im Seitenbaum des Raumes
- [ ] Erstellungsdatum und Autor werden gespeichert
- [ ] API-Aufruf: `POST /api/v1/rooms/{roomId}/wiki/pages` liefert 200

---

### US-301: Wiki-Seite per Slug aufrufen
**Als** Raum-Mitglied **moechte ich** eine Wiki-Seite ueber ihren Slug aufrufen, **damit** ich stabile, lesbare URLs fuer Wiki-Seiten habe.

**Vorbedingungen:** Mindestens eine Wiki-Seite im Raum vorhanden.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `lehrer@monteweb.local` (T) einloggen | Login erfolgreich |
| 2 | Raum "Sonnengruppe" oeffnen und Wiki-Tab waehlen | Wiki-Baum wird angezeigt |
| 3 | Seite "Klassenregeln" im Baum anklicken | Seite wird geoeffnet, URL enthaelt den Slug `klassenregeln` |
| 4 | Markdown-Inhalt wird gerendert angezeigt | Ueberschriften, Listen etc. korrekt formatiert |
| 5 | Autor und Aenderungsdatum werden angezeigt | Metadaten sichtbar |

**Akzeptanzkriterien:**
- [ ] Seite wird ueber `GET /api/v1/rooms/{roomId}/wiki/pages/{slug}` geladen
- [ ] Slug ist eindeutig pro Raum
- [ ] Markdown wird korrekt zu HTML gerendert
- [ ] Seitentitel, Inhalt und Metadaten werden angezeigt

---

### US-302: Wiki-Seite bearbeiten
**Als** Raum-Mitglied **moechte ich** eine bestehende Wiki-Seite bearbeiten, **damit** ich Inhalte aktualisieren kann.

**Vorbedingungen:** Wiki-Seite "Klassenregeln" existiert im Raum.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `lehrer@monteweb.local` (T) einloggen | Login erfolgreich |
| 2 | Wiki-Seite "Klassenregeln" oeffnen | Seiteninhalt wird angezeigt |
| 3 | "Bearbeiten"-Button klicken | Markdown-Editor oeffnet sich mit aktuellem Inhalt |
| 4 | Inhalt aendern: dritte Regel hinzufuegen `- Regel 3` | Editor zeigt geaenderten Text |
| 5 | "Speichern" klicken | Seite wird aktualisiert, neue Version wird erstellt |
| 6 | Geaenderten Inhalt pruefen | Regel 3 ist sichtbar, Aenderungsdatum aktualisiert |

**Akzeptanzkriterien:**
- [ ] `PUT /api/v1/rooms/{roomId}/wiki/pages/{pageId}` aktualisiert die Seite
- [ ] Eine neue Version wird bei jeder Bearbeitung angelegt
- [ ] Aenderungsdatum und bearbeitender Benutzer werden aktualisiert
- [ ] Slug kann optional geaendert werden

---

### US-303: Wiki-Seite loeschen
**Als** Raum-Mitglied **moechte ich** eine Wiki-Seite loeschen, **damit** veraltete Inhalte entfernt werden koennen.

**Vorbedingungen:** Wiki-Seite existiert im Raum.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `lehrer@monteweb.local` (T) einloggen | Login erfolgreich |
| 2 | Wiki-Seite oeffnen, die geloescht werden soll | Seiteninhalt wird angezeigt |
| 3 | "Loeschen"-Button klicken | Bestaetigung-Dialog erscheint |
| 4 | Loeschung bestaetigen | Seite wird geloescht |
| 5 | Wiki-Baum pruefen | Geloeschte Seite ist nicht mehr im Baum sichtbar |

**Akzeptanzkriterien:**
- [ ] `DELETE /api/v1/rooms/{roomId}/wiki/pages/{pageId}` loescht die Seite
- [ ] Kindseiten werden korrekt behandelt (verwaist oder mitgeloescht)
- [ ] Bestaetigung-Dialog wird vor Loeschung angezeigt
- [ ] Seite verschwindet aus dem Wiki-Baum

---

### US-304: Wiki-Versionshistorie anzeigen
**Als** Raum-Mitglied **moechte ich** die Versionshistorie einer Wiki-Seite einsehen, **damit** ich Aenderungen nachvollziehen kann.

**Vorbedingungen:** Wiki-Seite wurde mindestens zweimal bearbeitet (mind. 2 Versionen).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `lehrer@monteweb.local` (T) einloggen | Login erfolgreich |
| 2 | Wiki-Seite "Klassenregeln" oeffnen | Seiteninhalt wird angezeigt |
| 3 | "Versionen" / "History"-Button klicken | Liste der Versionen wird angezeigt |
| 4 | Aeltere Version anklicken | Inhalt der ausgewaehlten Version wird angezeigt |
| 5 | Versionsnummer, Autor und Datum pruefen | Metadaten jeder Version sind sichtbar |

**Akzeptanzkriterien:**
- [ ] `GET /api/v1/rooms/{roomId}/wiki/pages/{pageId}/versions` liefert Versionsliste
- [ ] `GET /api/v1/rooms/{roomId}/wiki/versions/{versionId}` liefert einzelne Version
- [ ] Jede Version enthaelt: Inhalt, Autor, Zeitstempel
- [ ] Versionen sind chronologisch sortiert (neueste zuerst)

---

### US-305: Wiki-Seitenhierarchie (Parent-Child)
**Als** Raum-Mitglied **moechte ich** Wiki-Seiten hierarchisch organisieren (Eltern-Kind-Beziehung), **damit** zusammengehoerende Inhalte strukturiert dargestellt werden.

**Vorbedingungen:** Mindestens eine Wiki-Seite existiert im Raum.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `lehrer@monteweb.local` (T) einloggen | Login erfolgreich |
| 2 | Neue Wiki-Seite erstellen: Titel "Allgemeine Regeln" | Seite wird als Top-Level-Seite erstellt |
| 3 | Weitere Seite erstellen: Titel "Pausenregeln", Parent: "Allgemeine Regeln" | Seite wird als Kindseite erstellt |
| 4 | Weitere Seite erstellen: Titel "Klassenraumregeln", Parent: "Allgemeine Regeln" | Zweite Kindseite erstellt |
| 5 | Wiki-Baum pruefen | "Allgemeine Regeln" hat zwei Kindseiten, eingerueckt dargestellt |
| 6 | Kindseite oeffnen | Breadcrumb oder Parent-Link zeigt Elternseite an |

**Akzeptanzkriterien:**
- [ ] `parent_id` (self-referencing FK) verknuepft Kind- mit Elternseite
- [ ] Wiki-Baum (`GET /api/v1/rooms/{roomId}/wiki`) zeigt hierarchische Struktur
- [ ] Kindseiten werden unter der Elternseite eingerueckt angezeigt
- [ ] Tiefe Verschachtelung ist moeglich (mind. 3 Ebenen)

---

### US-306: Wiki-Suche innerhalb eines Raumes
**Als** Raum-Mitglied **moechte ich** Wiki-Seiten innerhalb meines Raumes durchsuchen, **damit** ich schnell relevante Inhalte finde.

**Vorbedingungen:** Mehrere Wiki-Seiten mit unterschiedlichem Inhalt im Raum vorhanden.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `lehrer@monteweb.local` (T) einloggen | Login erfolgreich |
| 2 | Wiki-Tab im Raum oeffnen | Wiki-Baum wird angezeigt |
| 3 | Suchfeld im Wiki-Bereich nutzen, Suchbegriff "Regel" eingeben | Suche wird ausgefuehrt |
| 4 | Ergebnisse pruefen | Alle Seiten mit "Regel" im Titel oder Inhalt werden angezeigt |
| 5 | Suchergebnis anklicken | Entsprechende Wiki-Seite wird geoeffnet |

**Akzeptanzkriterien:**
- [ ] `GET /api/v1/rooms/{roomId}/wiki/search?q={suchbegriff}` liefert Treffer
- [ ] Suche durchsucht Titel und Inhalt der Wiki-Seiten
- [ ] Ergebnisse werden als Liste mit Seitentitel angezeigt
- [ ] Nur Seiten des aktuellen Raumes werden durchsucht

---

## Modul: Tasks / Kanban

### US-307: Kanban-Board im Raum anzeigen
**Als** Raum-Mitglied **moechte ich** das Kanban-Board meines Raumes sehen, **damit** ich einen Ueberblick ueber alle Aufgaben habe.

**Vorbedingungen:** Tasks-Modul aktiviert (`monteweb.modules.tasks.enabled=true`). Benutzer ist Mitglied eines Raumes.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `lehrer@monteweb.local` (T) einloggen | Login erfolgreich |
| 2 | Raum "Sonnengruppe" oeffnen | Raum-Ansicht wird angezeigt |
| 3 | Tasks-Tab im Raum-Menue anklicken | Kanban-Board wird angezeigt |
| 4 | Board-Struktur pruefen | Standard-Spalten sind sichtbar (z.B. "Offen", "In Arbeit", "Erledigt") |

**Akzeptanzkriterien:**
- [ ] `GET /api/v1/rooms/{roomId}/tasks` liefert das Board mit Spalten und Tasks
- [ ] Board ist eindeutig pro Raum (unique constraint)
- [ ] Spalten werden in korrekter Reihenfolge (`position`) angezeigt
- [ ] Leere Spalten werden mit Platzhaltertext angezeigt

---

### US-308: Spalte im Kanban-Board erstellen
**Als** Raum-Mitglied **moechte ich** neue Spalten im Kanban-Board erstellen, **damit** ich den Workflow an unsere Beduerfnisse anpassen kann.

**Vorbedingungen:** Kanban-Board fuer den Raum existiert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `lehrer@monteweb.local` (T) einloggen | Login erfolgreich |
| 2 | Kanban-Board im Raum oeffnen | Board wird angezeigt |
| 3 | "Spalte hinzufuegen"-Button klicken | Dialog oder Eingabefeld erscheint |
| 4 | Spaltenname eingeben: "Review" | Name wird eingetragen |
| 5 | "Erstellen" klicken | Neue Spalte "Review" erscheint im Board |
| 6 | Position der neuen Spalte pruefen | Spalte ist an der richtigen Position (rechts von bestehenden Spalten) |

**Akzeptanzkriterien:**
- [ ] `POST /api/v1/rooms/{roomId}/tasks/columns` erstellt eine neue Spalte
- [ ] Spalte erhaelt eine Position (`position`-Feld fuer Sortierung)
- [ ] Spalte wird sofort im Board angezeigt
- [ ] Spaltenname ist Pflichtfeld

---

### US-309: Spalte im Kanban-Board umbenennen und loeschen
**Als** Raum-Mitglied **moechte ich** Spalten umbenennen oder loeschen, **damit** ich das Board aktuell halten kann.

**Vorbedingungen:** Kanban-Board mit mindestens 2 Spalten vorhanden.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `lehrer@monteweb.local` (T) einloggen | Login erfolgreich |
| 2 | Kanban-Board oeffnen | Board mit Spalten wird angezeigt |
| 3 | Spalte "Review" bearbeiten (Stift-Icon) | Bearbeitungsmodus fuer Spaltennamen aktiv |
| 4 | Namen aendern zu "Qualitaetskontrolle" | Neuer Name wird eingegeben |
| 5 | Speichern | Spalte wird umbenannt (`PUT /api/v1/rooms/{roomId}/tasks/columns/{columnId}`) |
| 6 | Leere Spalte "Qualitaetskontrolle" loeschen (Muelleimer-Icon) | Bestaetigung-Dialog erscheint |
| 7 | Loeschung bestaetigen | Spalte wird entfernt (`DELETE /api/v1/rooms/{roomId}/tasks/columns/{columnId}`) |

**Akzeptanzkriterien:**
- [ ] Spalten koennen umbenannt werden
- [ ] Leere Spalten koennen geloescht werden
- [ ] Spalten mit Tasks: Warnung oder Verschiebung der Tasks vor Loeschung
- [ ] Bestaetigung-Dialog bei Loeschung

---

### US-310: Task erstellen
**Als** Raum-Mitglied **moechte ich** eine neue Aufgabe (Task) im Kanban-Board erstellen, **damit** Arbeit verfolgt werden kann.

**Vorbedingungen:** Kanban-Board mit Spalten vorhanden.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `lehrer@monteweb.local` (T) einloggen | Login erfolgreich |
| 2 | Kanban-Board im Raum oeffnen | Board wird angezeigt |
| 3 | In der Spalte "Offen" auf "+"-Button klicken | Formular zum Erstellen eines Tasks oeffnet sich |
| 4 | Titel eingeben: "Elternabend vorbereiten" | Titel wird befuellt |
| 5 | Beschreibung eingeben: "Einladungen verschicken und Raum reservieren" | Beschreibung wird befuellt |
| 6 | "Erstellen" klicken | Task erscheint in der Spalte "Offen" |

**Akzeptanzkriterien:**
- [ ] `POST /api/v1/rooms/{roomId}/tasks` erstellt einen neuen Task
- [ ] Task wird in der angegebenen Spalte angezeigt
- [ ] Titel ist Pflichtfeld, Beschreibung optional
- [ ] Ersteller und Erstellungsdatum werden gespeichert

---

### US-311: Task verschieben (Drag & Drop)
**Als** Raum-Mitglied **moechte ich** Tasks per Drag & Drop zwischen Spalten verschieben, **damit** ich den Fortschritt visuell aktualisieren kann.

**Vorbedingungen:** Mindestens ein Task in der Spalte "Offen" vorhanden.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `lehrer@monteweb.local` (T) einloggen | Login erfolgreich |
| 2 | Kanban-Board oeffnen | Board mit Tasks wird angezeigt |
| 3 | Task "Elternabend vorbereiten" per Drag & Drop von "Offen" nach "In Arbeit" ziehen | Task bewegt sich visuell mit dem Cursor |
| 4 | Task in Spalte "In Arbeit" ablegen | Task erscheint in "In Arbeit", verschwindet aus "Offen" |
| 5 | Seite neu laden | Task ist weiterhin in "In Arbeit" (Persistenz pruefen) |

**Akzeptanzkriterien:**
- [ ] `PUT /api/v1/rooms/{roomId}/tasks/{taskId}/move` verschiebt den Task
- [ ] Request enthaelt `columnId` und `position`
- [ ] Drag & Drop funktioniert zwischen allen Spalten
- [ ] Position innerhalb einer Spalte wird respektiert
- [ ] Aenderung wird sofort persistiert

---

### US-312: Task bearbeiten und loeschen
**Als** Raum-Mitglied **moechte ich** Tasks bearbeiten oder loeschen, **damit** ich Aufgaben aktuell halten kann.

**Vorbedingungen:** Mindestens ein Task im Board vorhanden.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `lehrer@monteweb.local` (T) einloggen | Login erfolgreich |
| 2 | Task "Elternabend vorbereiten" anklicken | Task-Detailansicht oeffnet sich |
| 3 | Titel aendern zu "Elternabend am 15.03. vorbereiten" | Titel wird aktualisiert |
| 4 | Beschreibung erweitern | Beschreibung wird gespeichert |
| 5 | "Speichern" klicken | Task wird aktualisiert (`PUT /api/v1/rooms/{roomId}/tasks/{taskId}`) |
| 6 | Anderen Task oeffnen und "Loeschen" klicken | Bestaetigung-Dialog erscheint |
| 7 | Loeschung bestaetigen | Task wird entfernt (`DELETE /api/v1/rooms/{roomId}/tasks/{taskId}`) |

**Akzeptanzkriterien:**
- [ ] Titel und Beschreibung koennen bearbeitet werden
- [ ] Tasks koennen geloescht werden mit Bestaetigung
- [ ] Geloeschte Tasks verschwinden aus dem Board

---

### US-313: Checkliste / Sub-Tasks hinzufuegen
**Als** Raum-Mitglied **moechte ich** einem Task Checklisten-Eintraege hinzufuegen, **damit** ich Teilaufgaben verfolgen kann.

**Vorbedingungen:** Mindestens ein Task im Board vorhanden.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `lehrer@monteweb.local` (T) einloggen | Login erfolgreich |
| 2 | Task "Elternabend vorbereiten" oeffnen | Task-Detail wird angezeigt |
| 3 | Im Checklisten-Bereich "Neuer Eintrag" klicken | Eingabefeld erscheint |
| 4 | Eintrag eingeben: "Einladungen drucken" | Text wird eingetragen |
| 5 | Bestaetigen (Enter oder Button) | Checklisten-Eintrag wird hinzugefuegt (`POST /api/v1/rooms/{roomId}/tasks/{taskId}/checklist`) |
| 6 | Weiteren Eintrag hinzufuegen: "Raum buchen" | Zweiter Eintrag erscheint |
| 7 | Ersten Eintrag als erledigt markieren (Checkbox) | Checkbox wird aktiviert (`PUT .../checklist/{itemId}/toggle`) |
| 8 | Fortschrittsanzeige pruefen | "1/2 erledigt" oder aehnlich wird angezeigt |

**Akzeptanzkriterien:**
- [ ] Checklisten-Eintraege koennen hinzugefuegt werden
- [ ] Eintraege koennen als erledigt/offen umgeschaltet werden (Toggle)
- [ ] Eintraege koennen geloescht werden (`DELETE .../checklist/{itemId}`)
- [ ] Fortschritt (erledigt/gesamt) wird angezeigt
- [ ] Daten in `task_checklist_items`-Tabelle gespeichert

---

## Modul: Lesezeichen / Bookmarks

### US-314: Feed-Post bookmarken
**Als** Benutzer **moechte ich** einen Feed-Post als Lesezeichen speichern, **damit** ich ihn spaeter schnell wiederfinde.

**Vorbedingungen:** Bookmarks-Modul aktiviert (`monteweb.modules.bookmarks.enabled=true`). Mindestens ein Feed-Post vorhanden.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Feed oeffnen | Feed-Posts werden angezeigt |
| 3 | Lesezeichen-Icon an einem Post klicken | Icon wechselt zu "bookmarked" (ausgefuellt) |
| 4 | `POST /api/v1/bookmarks` mit `contentType: "POST"` und `contentId` | Toggle-Antwort: `bookmarked: true` |
| 5 | Erneut auf Lesezeichen-Icon klicken | Bookmark wird entfernt (Toggle), Icon wechselt zurueck |

**Akzeptanzkriterien:**
- [ ] Bookmark wird per Toggle-Mechanismus gesetzt/entfernt
- [ ] Visuelles Feedback (Icon-Wechsel) bei Bookmark-Aktion
- [ ] `contentType` = "POST", `contentId` = UUID des Posts
- [ ] Bookmark wird in `bookmarks`-Tabelle gespeichert

---

### US-315: Kalender-Event bookmarken
**Als** Benutzer **moechte ich** ein Kalender-Event als Lesezeichen speichern, **damit** ich wichtige Termine schnell wiederfinde.

**Vorbedingungen:** Mindestens ein Kalender-Event vorhanden.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `lehrer@monteweb.local` (T) einloggen | Login erfolgreich |
| 2 | Kalender oeffnen und ein Event anklicken | Event-Detail wird angezeigt |
| 3 | Lesezeichen-Icon am Event klicken | Bookmark wird gesetzt |
| 4 | Bookmark-Status pruefen: `GET /api/v1/bookmarks/check?contentType=EVENT&contentId={id}` | `bookmarked: true` |

**Akzeptanzkriterien:**
- [ ] Events koennen gebookmarkt werden (`contentType: "EVENT"`)
- [ ] Bookmark-Status wird korrekt angezeigt
- [ ] Toggle funktioniert auch bei Events

---

### US-316: Job bookmarken
**Als** Benutzer **moechte ich** einen Job aus der Jobboerse als Lesezeichen speichern, **damit** ich interessante Angebote im Blick behalte.

**Vorbedingungen:** Jobboard-Modul aktiviert. Mindestens ein Job vorhanden.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Jobboerse oeffnen | Jobs werden aufgelistet |
| 3 | Lesezeichen-Icon an einem Job klicken | Bookmark wird gesetzt |
| 4 | Bookmark pruefen | `bookmarked: true` fuer `contentType: "JOB"` |

**Akzeptanzkriterien:**
- [ ] Jobs koennen gebookmarkt werden
- [ ] Bookmark-Status wird beim Laden der Jobliste mitgeladen

---

### US-317: Wiki-Seite bookmarken
**Als** Benutzer **moechte ich** eine Wiki-Seite als Lesezeichen speichern, **damit** ich wichtige Dokumentationen schnell aufrufen kann.

**Vorbedingungen:** Wiki-Modul aktiviert. Mindestens eine Wiki-Seite vorhanden.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `lehrer@monteweb.local` (T) einloggen | Login erfolgreich |
| 2 | Wiki-Seite in einem Raum oeffnen | Seiteninhalt wird angezeigt |
| 3 | Lesezeichen-Icon klicken | Bookmark wird gesetzt (`contentType: "WIKI"`) |

**Akzeptanzkriterien:**
- [ ] Wiki-Seiten koennen gebookmarkt werden
- [ ] Bookmark-Status wird auf der Wiki-Seite angezeigt

---

### US-318: Bookmarks-Uebersicht anzeigen
**Als** Benutzer **moechte ich** alle meine Lesezeichen auf einer Uebersichtsseite sehen, **damit** ich schnell auf gespeicherte Inhalte zugreifen kann.

**Vorbedingungen:** Benutzer hat mindestens 3 Bookmarks verschiedener Typen gesetzt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Lesezeichen-Seite (BookmarksView) oeffnen | Alle Bookmarks werden aufgelistet |
| 3 | Nach Typ filtern (z.B. nur "POST") | Nur Posts-Bookmarks werden angezeigt |
| 4 | Alle Typen anzeigen lassen | Posts, Events, Jobs, Wiki-Seiten sichtbar |
| 5 | Auf ein Bookmark klicken | Navigation zum gebookmarkten Inhalt |
| 6 | Pagination pruefen (bei >20 Bookmarks) | Seitennavigation funktioniert |

**Akzeptanzkriterien:**
- [ ] `GET /api/v1/bookmarks?type={type}&page={page}&size={size}` liefert paginierte Liste
- [ ] Filter nach Typ funktioniert (POST, EVENT, JOB, WIKI oder alle)
- [ ] Jeder Bookmark zeigt Titel und Typ an
- [ ] Klick auf Bookmark navigiert zum Originalinhalt
- [ ] `GET /api/v1/bookmarks/ids?contentType={type}` liefert alle bookmarkten IDs fuer Batch-Pruefung

---

### US-319: Bookmark entfernen
**Als** Benutzer **moechte ich** ein Lesezeichen entfernen, **damit** meine Bookmarks-Liste aktuell bleibt.

**Vorbedingungen:** Mindestens ein Bookmark vorhanden.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Bookmarks-Uebersicht oeffnen | Bookmarks werden angezeigt |
| 3 | Lesezeichen-Icon an einem Bookmark klicken (Toggle) | Bookmark wird entfernt |
| 4 | Liste neu laden | Entferntes Bookmark ist nicht mehr sichtbar |
| 5 | Alternativ: Am Originalinhalt (Post/Event/Job/Wiki) den Bookmark-Icon klicken | Bookmark wird ebenfalls entfernt (gleicher Toggle) |

**Akzeptanzkriterien:**
- [ ] Toggle-Mechanismus entfernt Bookmark beim zweiten Klick
- [ ] Entfernte Bookmarks verschwinden aus der Uebersicht
- [ ] Keine Bestaetigung noetig (sofortige Aktion)

---

## Modul: Suche

### US-320: Globale Suche oeffnen (Ctrl+K)
**Als** Benutzer **moechte ich** die globale Suche mit Ctrl+K (bzw. Cmd+K auf Mac) oeffnen, **damit** ich schnell nach Inhalten suchen kann.

**Vorbedingungen:** Benutzer ist eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `lehrer@monteweb.local` (T) einloggen | Login erfolgreich |
| 2 | Auf einer beliebigen Seite Ctrl+K druecken | Such-Dialog (Modal) oeffnet sich |
| 3 | Suchfeld hat automatisch Fokus | Cursor blinkt im Suchfeld |
| 4 | Filter-Chips werden angezeigt: Alle, Benutzer, Raeume, Posts, Events, Dateien, Wiki, Tasks | 8 Filter-Buttons sichtbar |
| 5 | Esc druecken | Such-Dialog schliesst sich |

**Akzeptanzkriterien:**
- [ ] Tastenkombination Ctrl+K / Cmd+K oeffnet den Such-Dialog
- [ ] Dialog ist modal mit 600px Breite
- [ ] Suchfeld hat Autofokus
- [ ] 8 Filter-Typen: ALL, USER, ROOM, POST, EVENT, FILE, WIKI, TASK
- [ ] Esc schliesst den Dialog

---

### US-321: Volltextsuche ausfuehren
**Als** Benutzer **moechte ich** einen Suchbegriff eingeben und Ergebnisse sehen, **damit** ich Inhalte im System finde.

**Vorbedingungen:** Diverse Inhalte vorhanden (Posts, Events, Raeume, Benutzer, Wiki-Seiten, Tasks, Dateien).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `lehrer@monteweb.local` (T) einloggen | Login erfolgreich |
| 2 | Ctrl+K druecken, Such-Dialog oeffnet sich | Dialog mit Suchfeld sichtbar |
| 3 | "Sonnengruppe" eingeben (mind. 2 Zeichen) | Suche wird nach 300ms Debounce ausgefuehrt |
| 4 | Ergebnisse werden gruppiert nach Typ angezeigt | Gruppen-Header mit Icon und Typ-Label (z.B. "Raeume", "Posts") |
| 5 | Ergebnis-Eintraege zeigen Titel und optional Untertitel/Snippet | Suchergebnisse mit relevanten Infos |
| 6 | Mit Pfeiltasten durch Ergebnisse navigieren | Selektiertes Ergebnis wird hervorgehoben |
| 7 | Enter druecken auf selektiertem Ergebnis | Navigation zum Ergebnis, Dialog schliesst sich |

**Akzeptanzkriterien:**
- [ ] `GET /api/v1/search?q={suchbegriff}&type={typ}&limit=20` liefert Ergebnisse
- [ ] Mindestens 2 Zeichen erforderlich fuer Suche
- [ ] 300ms Debounce verzoegerung
- [ ] Ergebnisse gruppiert nach Typ mit Icons
- [ ] Tastaturnavigation: Pfeiltasten + Enter
- [ ] Limit: max 50 Ergebnisse (serverseitig begrenzt)

---

### US-322: Suche nach Typ filtern
**Als** Benutzer **moechte ich** die Suche nach bestimmten Dokumenttypen filtern, **damit** ich gezielter suchen kann.

**Vorbedingungen:** Such-Dialog ist geoeffnet.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Such-Dialog oeffnen (Ctrl+K) | Dialog mit Filter-Chips |
| 2 | "Benutzer"-Filter-Chip anklicken | Filter ist aktiv (hervorgehoben) |
| 3 | Suchbegriff "Anna" eingeben | Nur Benutzer-Ergebnisse werden angezeigt |
| 4 | "Raeume"-Filter waehlen | Nur Raum-Ergebnisse werden angezeigt |
| 5 | "Alle"-Filter waehlen | Ergebnisse aller Typen werden angezeigt |

**Akzeptanzkriterien:**
- [ ] Filter-Chips fuer alle 8 Typen: ALL, USER, ROOM, POST, EVENT, FILE, WIKI, TASK
- [ ] Aktiver Filter wird visuell hervorgehoben
- [ ] Filterwechsel loest sofortige Neusuche aus
- [ ] Typ wird als Query-Parameter `type` an API uebergeben

---

### US-323: Solr-Volltextsuche (wenn aktiviert)
**Als** Administrator **moechte ich** dass die Suche Apache Solr nutzt (wenn aktiviert), **damit** eine leistungsstarke Volltextsuche mit deutscher Sprachanalyse verfuegbar ist.

**Vorbedingungen:** Solr-Modul aktiviert (`monteweb.modules.solr.enabled=true`), Solr-Container laeuft.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin-Panel oeffnen, Suchkonfiguration pruefen | Solr ist als aktiviert angezeigt |
| 3 | Reindex ausloesen: `POST /api/v1/admin/search/reindex` | Reindexierung startet |
| 4 | Globale Suche oeffnen, "Elternabend" suchen | Solr liefert Ergebnisse mit Stemming (findet auch "Elternabende") |
| 5 | Nach Dateiinhalt suchen (z.B. PDF-Inhalt) | Tika-Extraktion findet Text innerhalb von Dokumenten |

**Akzeptanzkriterien:**
- [ ] Solr 9.8 mit deutscher Sprachanalyse (Stemming, Stopwords)
- [ ] 7 Dokumenttypen werden indexiert: USER, ROOM, POST, EVENT, FILE, WIKI, TASK
- [ ] Echtzeit-Indexierung via Spring Events bei Erstellung/Aenderung
- [ ] Tika-Extraktion fuer Dateiinhalte (PDF, DOCX etc.)
- [ ] Admin-Reindex ueber API moeglich
- [ ] Fallback auf DB-Suche wenn Solr deaktiviert/nicht erreichbar

---

### US-324: DB-Fallback bei deaktiviertem Solr
**Als** Benutzer **moechte ich** auch ohne Solr suchen koennen, **damit** die Suche immer funktioniert.

**Vorbedingungen:** Solr-Modul deaktiviert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `lehrer@monteweb.local` (T) einloggen | Login erfolgreich |
| 2 | Ctrl+K druecken, Suchbegriff eingeben | Suche wird ausgefuehrt |
| 3 | Ergebnisse werden angezeigt | DB-basierte Suche liefert Treffer |
| 4 | Filter nach Typ funktioniert | Filterung funktioniert auch mit DB-Fallback |

**Akzeptanzkriterien:**
- [ ] Suche funktioniert ohne Solr (DB-Fallback)
- [ ] Ergebnisformat ist identisch (gleiche SearchResult-Struktur)
- [ ] Keine Fehlermeldung bei deaktiviertem Solr

---

## Modul: Benachrichtigungen

### US-325: In-App-Benachrichtigungsliste anzeigen
**Als** Benutzer **moechte ich** meine Benachrichtigungen in einer Liste sehen, **damit** ich ueber relevante Aktivitaeten informiert bin.

**Vorbedingungen:** Benutzer hat Benachrichtigungen erhalten (z.B. durch Posts, Kommentare, Nachrichten).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Glocken-Icon in der Navigation anklicken | Benachrichtigungsliste oeffnet sich |
| 3 | Benachrichtigungen pruefen | Titel, Nachricht, Typ-Icon und Zeitstempel je Eintrag |
| 4 | Ungelesene Benachrichtigungen sind hervorgehoben | Visuell unterscheidbar von gelesenen |
| 5 | Auf eine Benachrichtigung klicken | Navigation zum verlinkten Inhalt (z.B. Post, Raum) |

**Akzeptanzkriterien:**
- [ ] `GET /api/v1/notifications` liefert paginierte Benachrichtigungsliste
- [ ] Jede Benachrichtigung hat: id, type, title, message, link, read, createdAt
- [ ] Ungelesene werden visuell hervorgehoben
- [ ] Klick auf Link navigiert zum Kontext

---

### US-326: Unread-Count Badge anzeigen
**Als** Benutzer **moechte ich** die Anzahl ungelesener Benachrichtigungen als Badge sehen, **damit** ich sofort erkenne, ob neue Nachrichten vorliegen.

**Vorbedingungen:** Mindestens eine ungelesene Benachrichtigung vorhanden.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Glocken-Icon in der Navigation pruefen | Rotes Badge mit Anzahl ungelesener Benachrichtigungen |
| 3 | Alle Benachrichtigungen als gelesen markieren | Badge verschwindet oder zeigt 0 |
| 4 | Neue Benachrichtigung erhalten (z.B. neuer Post) | Badge erscheint wieder mit aktueller Zahl |

**Akzeptanzkriterien:**
- [ ] `GET /api/v1/notifications/unread-count` liefert Anzahl
- [ ] Badge wird nur angezeigt wenn Count > 0
- [ ] Badge wird in Echtzeit aktualisiert (Polling oder WebSocket)
- [ ] NotificationBell-Komponente zeigt Badge korrekt an

---

### US-327: Benachrichtigung als gelesen markieren
**Als** Benutzer **moechte ich** einzelne Benachrichtigungen als gelesen markieren, **damit** ich den Ueberblick behalte.

**Vorbedingungen:** Ungelesene Benachrichtigungen vorhanden.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Benachrichtigungsliste oeffnen | Ungelesene sind hervorgehoben |
| 3 | Auf eine ungelesene Benachrichtigung klicken | Benachrichtigung wird als gelesen markiert |
| 4 | Benachrichtigung ist nicht mehr hervorgehoben | Visueller Status aendert sich |
| 5 | Unread-Count Badge aktualisiert sich | Badge-Zahl sinkt um 1 |

**Akzeptanzkriterien:**
- [ ] `PUT /api/v1/notifications/{id}/read` markiert einzelne als gelesen
- [ ] Visuelles Feedback sofort sichtbar
- [ ] Unread-Count wird aktualisiert

---

### US-328: Alle Benachrichtigungen als gelesen markieren
**Als** Benutzer **moechte ich** alle Benachrichtigungen auf einmal als gelesen markieren, **damit** ich schnell aufraeumen kann.

**Vorbedingungen:** Mehrere ungelesene Benachrichtigungen vorhanden.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Benachrichtigungsliste oeffnen | Mehrere ungelesene sichtbar |
| 3 | "Alle als gelesen markieren"-Button klicken | Alle Benachrichtigungen werden als gelesen markiert |
| 4 | Liste pruefen | Keine hervorgehobenen (ungelesenen) Eintraege mehr |
| 5 | Badge pruefen | Unread-Count ist 0, Badge verschwindet |

**Akzeptanzkriterien:**
- [ ] `PUT /api/v1/notifications/read-all` markiert alle als gelesen
- [ ] Alle Eintraege visuell aktualisiert
- [ ] Badge-Count auf 0

---

### US-329: Benachrichtigung loeschen
**Als** Benutzer **moechte ich** einzelne Benachrichtigungen loeschen, **damit** meine Liste uebersichtlich bleibt.

**Vorbedingungen:** Mindestens eine Benachrichtigung vorhanden.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Benachrichtigungsliste oeffnen | Benachrichtigungen werden angezeigt |
| 3 | Loeschen-Button (Muelleimer) an einer Benachrichtigung klicken | Benachrichtigung wird entfernt |
| 4 | Geloeschte Benachrichtigung ist nicht mehr in der Liste | Liste aktualisiert sich |

**Akzeptanzkriterien:**
- [ ] `DELETE /api/v1/notifications/{id}` loescht die Benachrichtigung
- [ ] Sofortige Entfernung aus der Liste
- [ ] Unread-Count wird ggf. aktualisiert

---

### US-330: Benachrichtigungstypen pruefen
**Als** Benutzer **moechte ich** verschiedene Typen von Benachrichtigungen erhalten, **damit** ich ueber alle relevanten Ereignisse informiert werde.

**Vorbedingungen:** Verschiedene Aktionen im System ausgefuehrt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Neuer Post im Feed erstellt → Raummitglieder erhalten POST-Benachrichtigung | Typ: `POST` |
| 2 | Kommentar zu einem Post → Autor erhaelt COMMENT-Benachrichtigung | Typ: `COMMENT` |
| 3 | Direktnachricht erhalten → MESSAGE-Benachrichtigung | Typ: `MESSAGE` |
| 4 | Event erstellt → EVENT_CREATED-Benachrichtigung | Typ: `EVENT_CREATED` |
| 5 | Event abgesagt → EVENT_CANCELLED-Benachrichtigung | Typ: `EVENT_CANCELLED` |
| 6 | Formular veroeffentlicht → FORM_PUBLISHED-Benachrichtigung | Typ: `FORM_PUBLISHED` |
| 7 | Raum-Beitrittsanfrage → ROOM_JOIN_REQUEST-Benachrichtigung (an Leader) | Typ: `ROOM_JOIN_REQUEST` |
| 8 | Beitrittsanfrage genehmigt → ROOM_JOIN_APPROVED (an Antragsteller) | Typ: `ROOM_JOIN_APPROVED` |
| 9 | Familieneinladung → FAMILY_INVITATION | Typ: `FAMILY_INVITATION` |
| 10 | @Erwaehnung in Post/Kommentar → MENTION | Typ: `MENTION` |

**Akzeptanzkriterien:**
- [ ] Mindestens 22 Benachrichtigungstypen unterstuetzt (siehe NotificationType)
- [ ] Jeder Typ hat passendes Icon und Beschreibung
- [ ] Link fuehrt zum relevanten Kontext
- [ ] `referenceType` und `referenceId` ermoeglichen Zuordnung

---

### US-331: Push-Benachrichtigungen (wenn aktiviert)
**Als** Benutzer **moechte ich** Push-Benachrichtigungen auf meinem Geraet erhalten, **damit** ich auch bei geschlossenem Browser informiert werde.

**Vorbedingungen:** Push-Modul aktiviert (`monteweb.push.enabled=true`). Browser unterstuetzt Web Push (VAPID).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Push-Benachrichtigungen aktivieren (Profil oder Prompt) | Browser fragt nach Berechtigung |
| 3 | Berechtigung erteilen | Subscription wird registriert (`POST /api/v1/notifications/push/subscribe`) |
| 4 | VAPID Public Key abrufen: `GET /api/v1/notifications/push/public-key` | Public Key wird zurueckgegeben |
| 5 | Neuen Post in einem Raum erstellen (durch anderen User) | Push-Benachrichtigung auf dem Geraet |
| 6 | Push-Benachrichtigungen deaktivieren | Subscription wird entfernt (`POST /api/v1/notifications/push/unsubscribe`) |

**Akzeptanzkriterien:**
- [ ] VAPID-basierte Web Push Notifications
- [ ] Benutzer kann Push ein-/ausschalten
- [ ] Push wird nur bei aktiviertem Modul angeboten
- [ ] `usePushNotifications` Composable verwaltet Subscription
- [ ] Deregistrierung funktioniert zuverlaessig

---

## Modul: Familie

### US-332: Familienverbund erstellen
**Als** Elternteil **moechte ich** einen Familienverbund erstellen, **damit** meine Familie als Einheit im System abgebildet wird.

**Vorbedingungen:** Benutzer hat die Rolle PARENT.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Familie-Seite (FamilyView) oeffnen | Familien-Uebersicht wird angezeigt |
| 3 | "Familie erstellen"-Button klicken | Formular oeffnet sich |
| 4 | Familienname eingeben: "Familie Mueller" | Name wird eingetragen |
| 5 | "Erstellen" klicken | Familie wird erstellt, Ersteller ist automatisch PARENT-Mitglied |
| 6 | Familie erscheint in "Meine Familien" | Familie mit Name und Ersteller sichtbar |

**Akzeptanzkriterien:**
- [ ] Familie wird erstellt mit Name
- [ ] Ersteller wird automatisch als PARENT-Mitglied hinzugefuegt
- [ ] Familienverbund ist Abrechnungseinheit fuer Elternstunden
- [ ] `GET /api/v1/families/mine` zeigt eigene Familien

---

### US-333: Familienmitglied per User-Suche einladen
**Als** Elternteil **moechte ich** andere Benutzer per Suche in meine Familie einladen, **damit** Familienmitglieder verknuepft werden.

**Vorbedingungen:** Familienverbund existiert. Zu einladender Benutzer ist registriert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Familie oeffnen | Familiendetails werden angezeigt |
| 3 | "Mitglied einladen"-Button klicken | Einladungs-Dialog mit User-Suche oeffnet sich |
| 4 | Namen des zu einladenden Benutzers suchen | Suchergebnisse werden angezeigt |
| 5 | Benutzer auswaehlen | Benutzer wird selektiert |
| 6 | Rolle waehlen: PARENT oder CHILD | Rollen-Auswahl erscheint |
| 7 | Einladung senden (`POST /api/v1/families/{id}/invite`) | Einladung wird gesendet |
| 8 | Eingeladener Benutzer erhaelt Benachrichtigung | FAMILY_INVITATION-Notification erscheint |

**Akzeptanzkriterien:**
- [ ] User-Suche findet registrierte Benutzer
- [ ] Rollenwahl: PARENT oder CHILD
- [ ] Einladung erzeugt Benachrichtigung beim Eingeladenen
- [ ] Ein Elternteil kann nur einem Familienverbund angehoeren
- [ ] Kind kann mehreren Familien zugeordnet sein (getrennte Eltern)

---

### US-334: Einladung annehmen / ablehnen
**Als** eingeladener Benutzer **moechte ich** eine Familieneinladung annehmen oder ablehnen, **damit** ich selbst entscheiden kann.

**Vorbedingungen:** Benutzer hat eine offene Familieneinladung.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als eingeladener Benutzer einloggen | Login erfolgreich |
| 2 | Benachrichtigungsliste oeffnen | FAMILY_INVITATION-Benachrichtigung sichtbar |
| 3 | Einladungen pruefen: `GET /api/v1/families/invitations/mine` | Offene Einladungen werden angezeigt |
| 4 | Einladung annehmen: `POST /api/v1/families/invitations/{id}/accept` | Benutzer wird Familienmitglied |
| 5 | Familie in "Meine Familien" pruefen | Familie ist jetzt sichtbar |
| 6 | Alternative: Einladung ablehnen: `POST /api/v1/families/invitations/{id}/decline` | Einladung wird entfernt |

**Akzeptanzkriterien:**
- [ ] Annehmen fuegt Benutzer zur Familie hinzu (mit gewaehlter Rolle)
- [ ] Ablehnen entfernt die Einladung ohne Beitritt
- [ ] FAMILY_INVITATION_ACCEPTED-Benachrichtigung an Einladenden bei Annahme
- [ ] Einladung verschwindet nach Aktion

---

### US-335: Familien-Uebersicht und Kinder zuordnen
**Als** Elternteil **moechte ich** die Mitglieder meiner Familie sehen und Kinder zuordnen, **damit** die Familienstruktur korrekt abgebildet ist.

**Vorbedingungen:** Familienverbund mit mindestens einem Elternteil existiert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Familie oeffnen | Mitgliederliste mit Rollen (PARENT/CHILD) wird angezeigt |
| 3 | Kind hinzufuegen: `POST /api/v1/families/{id}/children` | Kind wird der Familie zugeordnet |
| 4 | Pruefen: Kind erscheint mit Rolle CHILD in der Mitgliederliste | Kind ist sichtbar |
| 5 | Mitglied entfernen: `DELETE /api/v1/families/{id}/members/{userId}` | Mitglied wird aus Familie entfernt |

**Akzeptanzkriterien:**
- [ ] Mitgliederliste zeigt alle Familienmitglieder mit Rolle
- [ ] Kinder koennen zugeordnet werden
- [ ] Mitglieder koennen entfernt werden
- [ ] Kind kann in mehreren Familien sein

---

### US-336: Stundenkonto einsehen
**Als** Elternteil **moechte ich** das Stundenkonto meiner Familie einsehen, **damit** ich den Stand der Elternstunden kenne.

**Vorbedingungen:** Familie hat Stunden aus Jobboerse/Putz-Orga angesammelt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Familien-Seite oeffnen | Stundenkonto wird angezeigt |
| 3 | Gesamtstunden pruefen | Summe aus Job-Stunden und Putz-Stunden sichtbar |
| 4 | Putz-Stunden werden als Sonder-Unterkonto angezeigt | Separate Anzeige der Putzstunden |

**Akzeptanzkriterien:**
- [ ] Stundenkonto zeigt Gesamtstunden der Familie
- [ ] Putzstunden werden separat ausgewiesen (Sonder-Unterkonto)
- [ ] Stunden stammen aus Jobboerse und Putz-Orga
- [ ] Familienverbund ist die Abrechnungseinheit

---

### US-337: Familie deaktivieren und Stunden-Befreiung
**Als** Administrator **moechte ich** eine Familie deaktivieren oder von Elternstunden befreien, **damit** Sonderfaelle abgedeckt werden.

**Vorbedingungen:** Familienverbund existiert. Admin-Zugang.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin-Panel > Familien oeffnen (AdminFamilies) | Liste aller Familien |
| 3 | Familie "Mueller" waehlen | Familiendetails werden angezeigt |
| 4 | "Stunden-Befreiung" aktivieren (`PUT /api/v1/families/{id}/hours-exempt`) | `is_hours_exempt=true` wird gesetzt |
| 5 | Familie ist von Elternstunden befreit | Kein Stunden-Soll mehr fuer diese Familie |
| 6 | "Deaktivieren" klicken (`PUT /api/v1/families/{id}/active`) | `is_active=false` wird gesetzt |
| 7 | Familie erscheint als deaktiviert | Visuell als inaktiv markiert |

**Akzeptanzkriterien:**
- [ ] `families.is_hours_exempt` befreit Familie von Elternstunden
- [ ] `families.is_active` deaktiviert den Familienverbund
- [ ] Nur SUPERADMIN kann diese Aktionen durchfuehren
- [ ] Deaktivierte Familien werden im Stundenbericht nicht beruecksichtigt

---

### US-338: Familie verlassen
**Als** Familienmitglied **moechte ich** eine Familie verlassen koennen, **damit** ich nicht mehr mit diesem Verbund verknuepft bin.

**Vorbedingungen:** Benutzer ist Mitglied einer Familie.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als Familienmitglied einloggen | Login erfolgreich |
| 2 | Familien-Seite oeffnen | Meine Familien werden angezeigt |
| 3 | "Familie verlassen" klicken (`POST /api/v1/families/{id}/leave`) | Bestaetigung-Dialog |
| 4 | Verlassen bestaetigen | Benutzer wird aus der Familie entfernt |
| 5 | Familie erscheint nicht mehr in "Meine Familien" | Liste aktualisiert |

**Akzeptanzkriterien:**
- [ ] Mitglied kann Familie verlassen
- [ ] Bestaetigung erforderlich
- [ ] Letzte PARENT kann Familie nicht verlassen (oder Familie wird geloescht)

---

## Modul: Admin-Panel

### US-339: Benutzer auflisten und filtern
**Als** Superadmin **moechte ich** alle Benutzer auflisten und filtern, **damit** ich die Benutzerverwaltung effizient durchfuehren kann.

**Vorbedingungen:** Mehrere Benutzer im System (~220 Seed-User).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin-Panel > Benutzer (AdminUsers) oeffnen | Benutzerliste mit Pagination |
| 3 | Nach Rolle filtern: "TEACHER" | Nur Lehrer werden angezeigt |
| 4 | Nach Status filtern: "Aktiv" / "Inaktiv" | Entsprechende Benutzer werden angezeigt |
| 5 | Suchbegriff eingeben: "Mueller" | Benutzer mit Name Mueller werden angezeigt |
| 6 | Pagination: Naechste Seite | Weitere Benutzer werden geladen |

**Akzeptanzkriterien:**
- [ ] `GET /api/v1/admin/users?role={role}&active={bool}&search={text}&page={p}&size={s}` funktioniert
- [ ] Filter: Rolle (SUPERADMIN, SECTION_ADMIN, TEACHER, PARENT, STUDENT)
- [ ] Filter: Aktiv/Inaktiv
- [ ] Freitext-Suche ueber Name und E-Mail
- [ ] Pagination mit 20 Eintraegen pro Seite

---

### US-340: Benutzer erstellen und Profil bearbeiten (Admin)
**Als** Superadmin **moechte ich** Benutzerprofile bearbeiten, **damit** ich Daten korrigieren oder aktualisieren kann.

**Vorbedingungen:** Benutzer existiert im System.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin > Benutzer > Benutzer "Anna Mueller" oeffnen | Benutzerdetails werden angezeigt |
| 3 | E-Mail, Vorname, Nachname, Telefon aendern | Felder werden aktualisiert |
| 4 | Speichern: `PUT /api/v1/admin/users/{id}/profile` | Profil wird aktualisiert |
| 5 | Audit-Log pruefen | Aenderung wurde protokolliert |

**Akzeptanzkriterien:**
- [ ] Admin kann E-Mail, Vorname, Nachname, Telefon aendern
- [ ] Nur SUPERADMIN hat Zugriff (`@PreAuthorize("hasRole('SUPERADMIN')")`)
- [ ] Datenzugriff wird im Data-Access-Log protokolliert (DSGVO)

---

### US-341: Benutzerrolle zuweisen
**Als** Superadmin **moechte ich** einem Benutzer eine Rolle zuweisen, **damit** Berechtigungen korrekt vergeben werden.

**Vorbedingungen:** Benutzer existiert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin > Benutzer > Benutzer waehlen | Benutzerdetails angezeigt |
| 3 | Rolle aendern: von PARENT zu TEACHER | Rollen-Dropdown oder -Auswahl |
| 4 | Speichern: `PUT /api/v1/admin/users/{id}/roles` | Rolle wird aktualisiert |
| 5 | Benutzer hat jetzt TEACHER-Berechtigungen | Rollenwechsel wirksam |

**Akzeptanzkriterien:**
- [ ] Rolle kann auf SUPERADMIN, SECTION_ADMIN, TEACHER, PARENT, STUDENT gesetzt werden
- [ ] Rollenwechsel wirkt sich sofort auf Berechtigungen aus
- [ ] Mindestens ein SUPERADMIN muss im System bleiben

---

### US-342: Benutzer sperren und freischalten
**Als** Superadmin **moechte ich** Benutzer sperren oder freischalten, **damit** ich den Zugang kontrollieren kann.

**Vorbedingungen:** Benutzer existiert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin > Benutzer > Benutzer waehlen | Benutzerdetails angezeigt |
| 3 | "Sperren" klicken: `PUT /api/v1/admin/users/{id}/status?active=false` | Benutzer wird deaktiviert |
| 4 | Gesperrter Benutzer versucht sich einzuloggen | Login wird verweigert |
| 5 | "Freischalten" klicken: `PUT /api/v1/admin/users/{id}/status?active=true` | Benutzer wird reaktiviert |
| 6 | Benutzer kann sich wieder einloggen | Login erfolgreich |

**Akzeptanzkriterien:**
- [ ] Gesperrte Benutzer koennen sich nicht einloggen
- [ ] Status-Aenderung ist reversibel
- [ ] Admin kann sich nicht selbst sperren

---

### US-343: Spezialrollen zuweisen (PUTZORGA, ELTERNBEIRAT, SECTION_ADMIN)
**Als** Superadmin **moechte ich** Benutzern Spezialrollen zuweisen, **damit** sie besondere Funktionen nutzen koennen.

**Vorbedingungen:** Benutzer existiert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin > Benutzer > Benutzer waehlen | Benutzerdetails angezeigt |
| 3 | Spezialrolle "PUTZORGA" hinzufuegen: `POST /api/v1/admin/users/{id}/special-roles` | Rolle wird hinzugefuegt |
| 4 | Spezialrolle "SECTION_ADMIN:{sectionId}" hinzufuegen | Bereichsleiter-Rolle fuer Bereich |
| 5 | Spezialrolle entfernen: `DELETE /api/v1/admin/users/{id}/special-roles/{role}` | Rolle wird entfernt |
| 6 | Assigned-Roles aktualisieren: `PUT /api/v1/admin/users/{id}/assigned-roles` | Mehrere Rollen gleichzeitig setzen |

**Akzeptanzkriterien:**
- [ ] Spezialrollen: PUTZORGA, ELTERNBEIRAT, SECTION_ADMIN:{sectionId}
- [ ] Mehrere Spezialrollen pro Benutzer moeglich
- [ ] SECTION_ADMIN erhaelt Zugriff auf den zugeordneten Bereich

---

### US-344: System-Konfiguration aendern
**Als** Superadmin **moechte ich** die Systemkonfiguration aendern, **damit** ich die Plattform an unsere Schule anpassen kann.

**Vorbedingungen:** Admin-Zugang.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin > Einstellungen (AdminSettings) oeffnen | Konfigurationsformular wird angezeigt |
| 3 | Schulname aendern | Neuer Name wird gespeichert |
| 4 | Bundesland aendern (z.B. "BY" zu "NW") | Feiertage aendern sich entsprechend |
| 5 | Schulferien konfigurieren (JSONB-Array) | Ferienzeiten werden gespeichert |
| 6 | Maximale Upload-Groesse aendern (`max_upload_size_mb`) | Neuer Wert wird gespeichert |
| 7 | Sprachen konfigurieren (`available_languages`) | Verfuegbare Sprachen werden aktualisiert |
| 8 | Speichern: `PUT /api/v1/admin/config` | Konfiguration wird persistiert |

**Akzeptanzkriterien:**
- [ ] Konfigurationsfelder: Schulname, Bundesland, Ferien, Upload-Groesse, Sprachen
- [ ] `GET /api/v1/admin/config` laedt aktuelle Konfiguration
- [ ] `PUT /api/v1/admin/config` speichert Aenderungen
- [ ] Bundesland bestimmt Feiertage (16 deutsche Bundeslaender)

---

### US-345: Theme / Design anpassen
**Als** Superadmin **moechte ich** das Design der Plattform anpassen, **damit** es zum Erscheinungsbild unserer Schule passt.

**Vorbedingungen:** Admin-Zugang.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin > Design (AdminTheme) oeffnen | Theme-Editor wird angezeigt |
| 3 | Primaerfarbe aendern | Farbwahl (Colorpicker) |
| 4 | Speichern: `PUT /api/v1/admin/theme` | Theme wird aktualisiert |
| 5 | Seite neu laden | Neue Farben werden angewendet (CSS Custom Properties `--mw-*`) |

**Akzeptanzkriterien:**
- [ ] Theme-Farben koennen angepasst werden
- [ ] CSS Custom Properties `--mw-*` werden aktualisiert
- [ ] Theme wird aus Backend-Tenant-Config geladen
- [ ] Aenderungen sind sofort sichtbar nach Neuladen

---

### US-346: Module aktivieren / deaktivieren
**Als** Superadmin **moechte ich** optionale Module ein- oder ausschalten, **damit** ich nur benoetigte Funktionen aktiviere.

**Vorbedingungen:** Admin-Zugang.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin > Module (AdminModules) oeffnen | Liste aller Module mit Toggles |
| 3 | Modul "Fotobox" deaktivieren | Toggle auf "aus" |
| 4 | Speichern: `PUT /api/v1/admin/modules` | Modulstatus wird in `tenant_config.modules` JSONB gespeichert |
| 5 | Fotobox-Menueeintrag verschwindet in der Navigation | Frontend blendet deaktivierte Module aus |
| 6 | Modul "Fotobox" wieder aktivieren | Toggle auf "ein", Menueeintrag erscheint wieder |

**Akzeptanzkriterien:**
- [ ] Property-basierte Module: messaging, files, jobboard, cleaning, calendar, forms, fotobox, fundgrube, bookmarks, tasks, wiki, profilefields
- [ ] DB-managed Toggles (JSONB): jitsi, wopi, clamav, maintenance, ldap, directoryAdminOnly, impersonation
- [ ] Backend: `@ConditionalOnProperty` fuer Property-Module, `adminModuleApi.isModuleEnabled()` fuer DB-Toggles
- [ ] Frontend: Menue-Eintraege nur bei aktiviertem Modul sichtbar
- [ ] Module koennen jederzeit ein-/ausgeschaltet werden

---

### US-347: Logo hochladen
**Als** Superadmin **moechte ich** das Schullogo hochladen, **damit** es in der Navigation und auf der Login-Seite angezeigt wird.

**Vorbedingungen:** Admin-Zugang.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin-Panel oeffnen, Logo-Bereich suchen | Logo-Upload-Bereich sichtbar |
| 3 | Logo-Datei auswaehlen (PNG/JPG) | Datei wird hochgeladen: `POST /api/v1/admin/logo` |
| 4 | Vorschau des Logos pruefen | Hochgeladenes Logo wird angezeigt |
| 5 | Seite neu laden | Logo erscheint in Navigation und Login |

**Akzeptanzkriterien:**
- [ ] Logo kann hochgeladen werden (PNG, JPG)
- [ ] Logo wird in Navigation und Login-Seite angezeigt
- [ ] Altes Logo wird beim Hochladen ersetzt

---

### US-348: Audit-Log einsehen
**Als** Superadmin **moechte ich** das Audit-Log einsehen, **damit** ich sicherheitsrelevante Aktionen nachvollziehen kann.

**Vorbedingungen:** Admin-Zugang. Verschiedene Aktionen wurden durchgefuehrt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin > Audit-Log oeffnen | Liste der Audit-Eintraege |
| 3 | Eintraege pruefen: Aktion, Benutzer, Zeitstempel, Details | Alle Felder ausgefuellt |
| 4 | Filtern nach Zeitraum oder Aktion | Gefilterte Ergebnisse |
| 5 | `GET /api/v1/admin/audit-log` | Paginierte Audit-Log-Eintraege |

**Akzeptanzkriterien:**
- [ ] Audit-Log protokolliert sicherheitsrelevante Aktionen
- [ ] Eintraege enthalten: Aktion, Benutzer, Zeitstempel, IP-Adresse, Details
- [ ] Paginierung und Filterung funktionieren
- [ ] Nur SUPERADMIN hat Zugriff

---

### US-349: Error-Reports verwalten und GitHub-Issue erstellen
**Als** Superadmin **moechte ich** Fehlerberichte einsehen und optional als GitHub-Issue erstellen, **damit** Fehler systematisch verfolgt werden.

**Vorbedingungen:** Error-Reports vorhanden. Optional: `tenant_config.github_repo` und `tenant_config.github_pat` konfiguriert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin > Error-Reports (AdminErrorReports) oeffnen | Liste der Fehlerberichte |
| 3 | Fehlerbericht oeffnen | Details: Fingerprint, Occurrences, Status, Stack-Trace |
| 4 | Status aendern: NEW -> REPORTED | `PUT /api/v1/admin/error-reports/{id}/status` |
| 5 | GitHub-Issue erstellen | `POST /api/v1/admin/error-reports/{id}/github-issue` |
| 6 | GitHub-Issue-URL wird angezeigt | `github_issue_url` wird gespeichert |
| 7 | Status auf RESOLVED setzen | Fehlerbericht als geloest markiert |

**Akzeptanzkriterien:**
- [ ] Error-Reports mit Fingerprint-basierter Deduplizierung
- [ ] Status-Workflow: NEW -> REPORTED -> RESOLVED / IGNORED
- [ ] GitHub-Issue-Erstellung mit konfiguriertem PAT
- [ ] GitHub-Config aktualisierbar: `PUT /api/v1/admin/error-reports/github-config`
- [ ] Occurrence-Tracking (Haeufigkeit des Fehlers)

---

### US-350: Analytics Dashboard
**Als** Superadmin **moechte ich** ein Analytics Dashboard sehen, **damit** ich die Nutzung der Plattform verstehe.

**Vorbedingungen:** Admin-Zugang. Plattform wird aktiv genutzt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin > Analytics (AdminAnalytics) oeffnen | Dashboard mit Statistiken |
| 3 | Benutzer-Statistiken pruefen | Gesamtanzahl, aktive Benutzer, nach Rolle |
| 4 | Content-Statistiken pruefen | Anzahl Posts, Events, Nachrichten, Dateien |
| 5 | Engagement-Statistiken pruefen | Kommentare, Reaktionen, Logins |
| 6 | `GET /api/v1/admin/analytics` | Analytics-Daten werden zurueckgegeben |

**Akzeptanzkriterien:**
- [ ] User-Stats: Gesamt, aktiv, nach Rolle
- [ ] Content-Stats: Posts, Events, Messages, Files
- [ ] Engagement-Stats: Kommentare, Reaktionen, Login-Haeufigkeit
- [ ] Daten werden serverseitig berechnet (AnalyticsService)

---

### US-351: 2FA-Einstellungen verwalten (Admin)
**Als** Superadmin **moechte ich** die Zwei-Faktor-Authentifizierung systemweit konfigurieren, **damit** die Sicherheit entsprechend den Anforderungen eingestellt ist.

**Vorbedingungen:** Admin-Zugang.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin > Einstellungen oeffnen | 2FA-Konfiguration sichtbar |
| 3 | 2FA-Modus auf "OPTIONAL" setzen | Benutzer koennen 2FA freiwillig aktivieren |
| 4 | 2FA-Modus auf "MANDATORY" setzen | Alle Benutzer muessen 2FA einrichten |
| 5 | Grace-Period-Deadline setzen (z.B. +7 Tage) | `two_factor_grace_deadline` wird gespeichert |
| 6 | Innerhalb der Grace Period koennen Benutzer ohne 2FA einloggen | Login funktioniert, Hinweis auf 2FA-Pflicht |
| 7 | Nach Ablauf der Grace Period wird 2FA erzwungen | Login ohne 2FA nicht mehr moeglich |
| 8 | 2FA-Modus auf "DISABLED" setzen | 2FA fuer alle deaktiviert |

**Akzeptanzkriterien:**
- [ ] Drei Modi: DISABLED, OPTIONAL, MANDATORY (`tenant_config.two_factor_mode`)
- [ ] MANDATORY mit Grace Period (7 Tage Standard): `two_factor_grace_deadline`
- [ ] 2FA-Setup: `POST /api/v1/auth/2fa/setup` → TOTP Secret + QR-Code
- [ ] 2FA-Bestaetigung: `POST /api/v1/auth/2fa/confirm` mit TOTP-Code
- [ ] 2FA-Deaktivierung: `POST /api/v1/auth/2fa/disable` mit Passwort
- [ ] Recovery Codes bei 2FA-Setup generiert
- [ ] `POST /api/v1/auth/2fa/verify` bei Login wenn 2FA aktiv

---

### US-352: Impersonation (Als anderer Benutzer agieren)
**Als** Superadmin **moechte ich** als ein anderer Benutzer agieren (Impersonation), **damit** ich Probleme aus Sicht des Benutzers nachvollziehen kann.

**Vorbedingungen:** Impersonation-Modul aktiviert (DB-Toggle `impersonation`). Admin-Zugang.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin > Benutzer > Benutzer "eltern@monteweb.local" waehlen | Benutzerdetails angezeigt |
| 3 | "Als dieser Benutzer agieren" klicken: `POST /api/v1/auth/impersonate` | Impersonation startet |
| 4 | Anwendung zeigt Hinweis-Banner "Sie agieren als eltern@monteweb.local" | Visueller Hinweis sichtbar |
| 5 | Navigation pruefen: Sicht des PARENT-Benutzers | Menue und Berechtigungen wie PARENT |
| 6 | Terms-Check wird uebersprungen (TermsAcceptanceFilter) | Kein Nutzungsbedingungen-Dialog |
| 7 | "Zurueck zum Admin"-Button klicken: `POST /api/v1/auth/stop-impersonation` | Admin-Session wird wiederhergestellt |
| 8 | Anwendung zeigt wieder Admin-Ansicht | SUPERADMIN-Rechte aktiv |

**Akzeptanzkriterien:**
- [ ] Nur SUPERADMIN kann impersonieren
- [ ] Impersonation-Modul muss aktiviert sein (DB-Toggle)
- [ ] Kann keinen anderen SUPERADMIN impersonieren
- [ ] JWT enthaelt `impersonatedBy`-Claim mit Admin-UUID
- [ ] Impersonation kann jederzeit beendet werden
- [ ] Audit-Log protokolliert Start und Ende der Impersonation

---

### US-353: CSV-Import von Benutzern
**Als** Superadmin **moechte ich** Benutzer per CSV-Datei importieren, **damit** ich viele Benutzer gleichzeitig anlegen kann.

**Vorbedingungen:** Admin-Zugang.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin > CSV-Import (AdminCsvImport) oeffnen | Import-Formular wird angezeigt |
| 3 | Beispiel-CSV herunterladen: `GET /api/v1/admin/csv/example` | CSV-Vorlage wird heruntergeladen |
| 4 | CSV-Datei mit Benutzerdaten befuellen | Vorname, Nachname, E-Mail, Rolle |
| 5 | CSV hochladen: `POST /api/v1/admin/csv/import` | Import wird ausgefuehrt |
| 6 | Ergebnis pruefen: Anzahl importierter Benutzer | Erfolgsmeldung mit Statistik |
| 7 | Importierte Benutzer in der Benutzerliste pruefen | Neue Benutzer sind sichtbar |

**Akzeptanzkriterien:**
- [ ] CSV-Import mit Pflichtfeldern: E-Mail, Vorname, Nachname, Rolle
- [ ] Beispiel-CSV zum Download verfuegbar
- [ ] Fehlerbehandlung bei ungueltigem Format oder doppelten E-Mails
- [ ] Erfolgsmeldung mit Anzahl importierter Benutzer

---

### US-354: LDAP-Verbindung testen
**Als** Superadmin **moechte ich** die LDAP-Verbindung testen, **damit** ich die Konfiguration vor Aktivierung ueberpruefen kann.

**Vorbedingungen:** LDAP-Konfiguration in Tenant-Config vorhanden.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin > Einstellungen > LDAP-Konfiguration oeffnen | LDAP-Felder: URL, Base DN, Bind DN, etc. |
| 3 | LDAP-Server-URL, Base DN und Bind-Credentials eingeben | Felder werden befuellt |
| 4 | "Verbindung testen" klicken: `POST /api/v1/admin/ldap/test` | Verbindungstest wird ausgefuehrt |
| 5 | Erfolgsmeldung bei gueltigem Server | "Verbindung erfolgreich" |
| 6 | Fehlermeldung bei ungueltigem Server | Aussagekraeftige Fehlerbeschreibung |

**Akzeptanzkriterien:**
- [ ] LDAP-Konfiguration: url, base_dn, bind_dn, bind_password, user_search_filter
- [ ] Attribut-Mapping: attr_email, attr_firstname, attr_lastname
- [ ] Verbindungstest ohne LDAP-Modul zu aktivieren
- [ ] LDAP-Toggle in `tenant_config.modules` JSONB

---

## Modul: Bereichsleitung / Section-Admin

### US-355: Eigene Bereiche anzeigen
**Als** Bereichsleiter **moechte ich** meine zugeordneten Schulbereiche sehen, **damit** ich weiss, welche Bereiche ich verwalte.

**Vorbedingungen:** Benutzer hat Rolle SECTION_ADMIN mit Spezialrolle `SECTION_ADMIN:{sectionId}`.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `sectionadmin@monteweb.local` (SECADMIN) einloggen | Login erfolgreich |
| 2 | Bereichsleitung-Seite (SectionAdminView) oeffnen | Uebersicht der zugeordneten Bereiche |
| 3 | `GET /api/v1/section-admin/my-sections` | Liste der eigenen Bereiche mit Name und ID |
| 4 | Nur aktive Bereiche werden angezeigt | Inaktive Bereiche sind ausgeblendet |

**Akzeptanzkriterien:**
- [ ] Bereiche werden aus `SECTION_ADMIN:{sectionId}` Spezialrollen ermittelt
- [ ] SUPERADMIN sieht alle aktiven Bereiche
- [ ] Nur aktive Bereiche (`active=true`) werden angezeigt

---

### US-356: Raeume eines Bereichs verwalten
**Als** Bereichsleiter **moechte ich** die Raeume meines Bereichs sehen und neue erstellen, **damit** ich die Raumstruktur meines Bereichs verwalten kann.

**Vorbedingungen:** Bereichsleiter ist fuer mindestens einen Bereich zustaendig.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `sectionadmin@monteweb.local` (SECADMIN) einloggen | Login erfolgreich |
| 2 | Bereich auswaehlen | Bereichsdetails werden angezeigt |
| 3 | Raeume des Bereichs anzeigen: `GET /api/v1/section-admin/sections/{sectionId}/rooms` | Liste der Raeume im Bereich |
| 4 | "Neuen Raum erstellen" klicken | Formular: Name, Beschreibung, Typ |
| 5 | Raum erstellen: `POST /api/v1/section-admin/rooms` mit sectionId | Neuer Raum wird erstellt |
| 6 | Neuer Raum erscheint in der Raumliste | Raum mit korrektem Bereich und Typ |

**Akzeptanzkriterien:**
- [ ] Bereichsleiter sieht nur Raeume seines Bereichs
- [ ] Neue Raeume koennen mit Name, Beschreibung und Typ erstellt werden
- [ ] Raum-Typ Standard: KLASSE
- [ ] SectionId muss zum verwalteten Bereich gehoeren

---

### US-357: Mitglieder eines Bereichs verwalten
**Als** Bereichsleiter **moechte ich** die Mitglieder meines Bereichs sehen und Spezialrollen vergeben, **damit** ich die Organisation meines Bereichs steuern kann.

**Vorbedingungen:** Bereich hat Raeume mit Mitgliedern.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `sectionadmin@monteweb.local` (SECADMIN) einloggen | Login erfolgreich |
| 2 | Bereich auswaehlen | Bereichsuebersicht |
| 3 | Mitglieder anzeigen: `GET /api/v1/section-admin/sections/{sectionId}/users` | Liste aller Benutzer in Raeumen dieses Bereichs |
| 4 | Spezialrolle "PUTZORGA" zuweisen: `POST /api/v1/section-admin/users/{userId}/special-roles` | Rolle wird hinzugefuegt |
| 5 | Spezialrolle "ELTERNBEIRAT" zuweisen | Rolle wird hinzugefuegt |
| 6 | Ungueltige Rolle zuweisen (z.B. "SUPERADMIN") | Fehler: "Only PUTZORGA and ELTERNBEIRAT roles are allowed" |
| 7 | Spezialrolle entfernen: `DELETE /api/v1/section-admin/users/{userId}/special-roles/{role}` | Rolle wird entfernt |

**Akzeptanzkriterien:**
- [ ] Mitgliederliste enthaelt alle Benutzer aus Raeumen des Bereichs
- [ ] Nur PUTZORGA und ELTERNBEIRAT Spezialrollen duerfen vergeben werden
- [ ] Ziel-Benutzer muss in einem Raum des verwalteten Bereichs sein
- [ ] Unberechtigte Rollenvergabe wird abgelehnt

---

### US-358: Zugriffsbeschraenkung fuer Section-Admin
**Als** System **moechte ich** sicherstellen, dass Bereichsleiter nur auf ihre eigenen Bereiche zugreifen, **damit** die Datensicherheit gewaehrleistet ist.

**Vorbedingungen:** Bereichsleiter mit Zugriff auf Bereich A, aber nicht auf Bereich B.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `sectionadmin@monteweb.local` (SECADMIN) einloggen | Login erfolgreich |
| 2 | Versuch, auf Bereich B zuzugreifen (fremder Bereich) | 403 Forbidden: "You do not manage this section" |
| 3 | Versuch, Spezialrolle an Benutzer in Bereich B zu vergeben | 403 Forbidden: "Target user does not belong to any of your sections" |
| 4 | Zugriff auf eigenen Bereich A | Zugriff erlaubt |

**Akzeptanzkriterien:**
- [ ] Zugriffscheck via `requireAccessToSection()` und `requireUserInAdminSections()`
- [ ] Fremde Bereiche: 403 Forbidden
- [ ] SUPERADMIN hat Zugriff auf alle Bereiche

---

## Modul: DSGVO / Datenschutz

### US-359: Datenschutzerklaerung anzeigen
**Als** Benutzer **moechte ich** die Datenschutzerklaerung lesen koennen, **damit** ich informiert bin, wie meine Daten verarbeitet werden.

**Vorbedingungen:** Datenschutztext ist in `tenant_config.privacy_policy_text` konfiguriert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Datenschutzerklaerung aufrufen (oeffentlich zugaenglich) | Seite wird angezeigt |
| 2 | `GET /api/v1/privacy/policy` | Text und Version werden zurueckgegeben |
| 3 | Platzhalter im Text werden ersetzt (z.B. Schulname) | Dynamische Inhalte korrekt |

**Akzeptanzkriterien:**
- [ ] Datenschutzerklaerung ist oeffentlich zugaenglich (kein Login noetig)
- [ ] Text und Version kommen aus `tenant_config`
- [ ] Platzhalter werden serverseitig ersetzt

---

### US-360: Nutzungsbedingungen akzeptieren
**Als** Benutzer **moechte ich** die Nutzungsbedingungen akzeptieren muessen, **damit** die rechtliche Grundlage gegeben ist.

**Vorbedingungen:** `tenant_config.terms_text` und `tenant_config.terms_version` sind konfiguriert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als neuer Benutzer einloggen | Login erfolgreich |
| 2 | Nutzungsbedingungen-Dialog wird angezeigt (wenn nicht akzeptiert) | Dialog mit Bedingungen-Text |
| 3 | Status pruefen: `GET /api/v1/privacy/terms/status` | `accepted: false` |
| 4 | Nutzungsbedingungen lesen und "Akzeptieren" klicken | `POST /api/v1/privacy/terms/accept` |
| 5 | Status pruefen | `accepted: true`, Zeitstempel gespeichert |
| 6 | Bei neuer Version: Dialog erscheint erneut | Versionierte Akzeptanz |

**Akzeptanzkriterien:**
- [ ] Nutzungsbedingungen muessen akzeptiert werden (TermsAcceptanceFilter)
- [ ] Versionierung: Neue Version erfordert erneute Akzeptanz
- [ ] Akzeptanz wird in `terms_acceptances`-Tabelle gespeichert (unique per user+version)
- [ ] IP-Adresse wird bei Akzeptanz protokolliert
- [ ] Impersonation-Sessions ueberspringen Terms-Check

---

### US-361: Consent verwalten (Foto/Chat fuer Kinder)
**Als** Elternteil **moechte ich** Einwilligungen fuer mein Kind verwalten (Foto-Consent, Chat-Consent), **damit** ich die Datenschutzrechte meines Kindes wahrnehme.

**Vorbedingungen:** Elternteil hat ein Kind in der Familie. Consent-Typen: PHOTO_CONSENT, CHAT_CONSENT.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Datenschutz-Einstellungen oeffnen | Consent-Uebersicht wird angezeigt |
| 3 | Eigene Consents anzeigen: `GET /api/v1/privacy/consents` | Liste der Consent-Records |
| 4 | PHOTO_CONSENT fuer Kind erteilen: `PUT /api/v1/privacy/consents` mit `targetUserId` (Kind) | Consent wird gespeichert |
| 5 | Request-Body: `{ consentType: "PHOTO_CONSENT", granted: true, targetUserId: "{kindId}" }` | `granted_by` wird auf Eltern-ID gesetzt |
| 6 | CHAT_CONSENT verweigern | `granted: false` wird gespeichert |
| 7 | Consent spaeter widerrufen | `granted: false` ueberschreibt vorherigen Consent |

**Akzeptanzkriterien:**
- [ ] Consent-Typen: PHOTO_CONSENT, CHAT_CONSENT
- [ ] Eltern koennen Consent fuer ihre Kinder erteilen (`targetUserId`)
- [ ] `granted_by` protokolliert, wer die Einwilligung gegeben hat
- [ ] Consent kann jederzeit widerrufen werden
- [ ] Consent wird in `consent_records`-Tabelle gespeichert

---

### US-362: Datenexport (Art. 15 DSGVO)
**Als** Benutzer **moechte ich** alle meine personenbezogenen Daten exportieren (Auskunftsrecht Art. 15), **damit** ich weiss, welche Daten ueber mich gespeichert sind.

**Vorbedingungen:** Benutzer ist eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Profil > Datenschutz > "Meine Daten exportieren" klicken | Export wird gestartet |
| 3 | `GET /api/v1/users/me/data-export` | JSON mit allen personenbezogenen Daten |
| 4 | Export enthaelt: Profildaten, Raum-Mitgliedschaften, Posts, Nachrichten, Dateien | Vollstaendiger Datenexport |
| 5 | Export wird im `data_access_log` protokolliert | Audit-Trail fuer Art. 15 |

**Akzeptanzkriterien:**
- [ ] Export umfasst alle personenbezogenen Daten des Benutzers
- [ ] Daten werden als JSON zurueckgegeben
- [ ] Export wird im Data-Access-Log protokolliert (DSGVO-Compliance)
- [ ] Auch Admin kann Export fuer Benutzer ausloesen (`GET /api/v1/admin/users/{id}/data-export`)
- [ ] Admin-Export wird ebenfalls protokolliert (ADMIN_DATA_EXPORT)

---

### US-363: Kontoloesung anfordern (14-Tage-Frist)
**Als** Benutzer **moechte ich** die Loeschung meines Kontos anfordern, **damit** ich mein Recht auf Vergessenwerden wahrnehme (Art. 17 DSGVO).

**Vorbedingungen:** Benutzer ist eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Profil > Datenschutz > "Konto loeschen" klicken | Bestaetigung-Dialog mit Warnung |
| 3 | Loeschung bestaetigen: `DELETE /api/v1/users/me` | Meldung: "Loeschung beantragt. Konto wird in 14 Tagen geloescht." |
| 4 | `deletion_requested_at` und `scheduled_deletion_at` werden gesetzt | Felder in der Datenbank befuellt |
| 5 | Loeschstatus pruefen: `GET /api/v1/users/me/deletion-status` | `deletionRequested: true`, `scheduledDeletionAt: [Datum in 14 Tagen]` |
| 6 | Benutzer kann sich weiterhin einloggen (14-Tage Grace Period) | Login funktioniert noch |
| 7 | Hinweis-Banner: "Ihr Konto wird am [Datum] geloescht" | Warnung sichtbar nach Login |

**Akzeptanzkriterien:**
- [ ] 14-Tage Grace Period zwischen Anforderung und Loeschung
- [ ] `deletion_requested_at` und `scheduled_deletion_at` in Users-Tabelle
- [ ] Benutzer wird ueber bevorstehende Loeschung informiert
- [ ] Konto ist waehrend Grace Period weiterhin nutzbar

---

### US-364: Kontoloesung abbrechen
**Als** Benutzer **moechte ich** eine angeforderte Kontoloesung abbrechen, **damit** ich mein Konto behalten kann, wenn ich es mir anders ueberlege.

**Vorbedingungen:** Loeschung wurde angefordert, aber 14-Tage-Frist ist noch nicht abgelaufen.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als Benutzer mit Loeschanfrage einloggen | Login erfolgreich, Loeschwarnungsbanner sichtbar |
| 2 | "Loeschung abbrechen" klicken: `POST /api/v1/users/me/cancel-deletion` | Meldung: "Loeschung abgebrochen" |
| 3 | Loeschstatus pruefen | `deletionRequested: false`, Felder zurueckgesetzt |
| 4 | Kein Warnungs-Banner mehr sichtbar | Normaler Betrieb wiederhergestellt |

**Akzeptanzkriterien:**
- [ ] Loeschung kann innerhalb der 14-Tage-Frist abgebrochen werden
- [ ] `deletion_requested_at` und `scheduled_deletion_at` werden auf NULL gesetzt
- [ ] Auch Admin kann Loeschung abbrechen: `POST /api/v1/admin/users/{id}/cancel-deletion`
- [ ] Nach Ablauf der Frist ist Abbruch nicht mehr moeglich

---

### US-365: Eltern-Consent fuer minderjaehrige Schueler
**Als** System **moechte ich** sicherstellen, dass Einwilligungen fuer minderjaehrige Schueler durch Erziehungsberechtigte erteilt werden, **damit** die DSGVO-Vorgaben eingehalten werden.

**Vorbedingungen:** Schueler ist als CHILD in einer Familie eingetragen. Elternteil ist PARENT in derselben Familie.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Consent-Verwaltung oeffnen | Eigene und Kinder-Consents sichtbar |
| 3 | PHOTO_CONSENT fuer Kind erteilen | `targetUserId` = Kind-ID, `granted_by` = Eltern-ID |
| 4 | Als Schueler (Kind) einloggen | Login erfolgreich |
| 5 | Schueler kann eigenen PHOTO_CONSENT nicht selbst aendern | Nur Eltern duerfen fuer Minderjaehrige entscheiden |
| 6 | `consent_records` pruefen | `granted_by` zeigt Eltern-UUID, nicht Kind-UUID |

**Akzeptanzkriterien:**
- [ ] Eltern erteilen Consent fuer ihre Kinder (`targetUserId`)
- [ ] `granted_by`-Feld dokumentiert den Einwilligenden
- [ ] Kinder koennen ihren eigenen Consent nicht aendern
- [ ] DSGVO Art. 8: Einwilligung bei Kindern unter 16 durch Eltern

---

### US-366: Datenaufbewahrungsfristen (Retention Policy)
**Als** Superadmin **moechte ich** Datenaufbewahrungsfristen konfigurieren, **damit** personenbezogene Daten nicht laenger als noetig gespeichert werden.

**Vorbedingungen:** Admin-Zugang.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin > Datenschutz (AdminPrivacyView) oeffnen | Datenschutz-Konfiguration |
| 3 | Aufbewahrungsfrist fuer Benachrichtigungen aendern | `data_retention_days_notifications` wird gesetzt |
| 4 | Aufbewahrungsfrist fuer Audit-Log aendern | `data_retention_days_audit` wird gesetzt |
| 5 | Datenschutzerklaerung-Text und Version aktualisieren | `privacy_policy_text`, `privacy_policy_version` |
| 6 | Nutzungsbedingungen-Text und Version aktualisieren | `terms_text`, `terms_version` |
| 7 | Speichern | Konfiguration wird persistiert |

**Akzeptanzkriterien:**
- [ ] Konfigurierbare Aufbewahrungsfristen fuer Notifications und Audit-Log
- [ ] Datenschutztext und Nutzungsbedingungen editierbar
- [ ] Versionsaenderung erfordert erneute Akzeptanz durch Benutzer
- [ ] Chat-Bilder: automatische Bereinigung nach 90 Tagen

---

## Modul: Cross-Cutting

### US-367: PWA installieren
**Als** Benutzer **moechte ich** MonteWeb als Progressive Web App auf meinem Geraet installieren, **damit** ich schnellen Zugriff wie bei einer nativen App habe.

**Vorbedingungen:** Browser unterstuetzt PWA-Installation (Chrome, Edge, Safari). Service Worker registriert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | MonteWeb im Browser oeffnen | Seite wird geladen |
| 2 | Nach einiger Zeit erscheint Install-Banner | PWA-Installationsprompt wird angezeigt |
| 3 | "Installieren" klicken | Browser-eigener Installationsdialog oeffnet sich |
| 4 | Installation bestaetigen | App wird auf dem Geraet installiert |
| 5 | App ueber Geraet-Icon starten | App oeffnet sich im Standalone-Modus (ohne Browser-Leiste) |
| 6 | Alternative: Banner schliessen ("Spaeter") | Banner verschwindet fuer 7 Tage |
| 7 | Nach 7 Tagen: Banner erscheint erneut | `usePwaInstall` Composable mit 7-Tage-Dismiss-Delay |

**Akzeptanzkriterien:**
- [ ] `vite-plugin-pwa` + Workbox Service Worker konfiguriert
- [ ] Install-Banner mit `usePwaInstall` Composable
- [ ] 7-Tage-Dismiss-Delay bei "Spaeter"-Klick
- [ ] Service Worker: NetworkFirst-Caching fuer API-Calls
- [ ] App-Icons in `public/icons/`
- [ ] Standalone-Modus bei Installation

---

### US-368: Dark Mode umschalten
**Als** Benutzer **moechte ich** zwischen Light Mode, Dark Mode und System-Praeferenz wechseln, **damit** ich die Anzeige meinen Vorlieben anpasse.

**Vorbedingungen:** Benutzer ist eingeloggt.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Profil / Einstellungen oeffnen | Dark-Mode-Auswahl sichtbar |
| 3 | "LIGHT" waehlen | Helle Farbpalette wird angewendet |
| 4 | "DARK" waehlen | Dunkle Farbpalette wird angewendet (CSS `--mw-*` Variablen) |
| 5 | "SYSTEM" waehlen | Folgt der Betriebssystem-Praeferenz (`prefers-color-scheme`) |
| 6 | Einstellung wird gespeichert: `PUT /api/v1/users/me/dark-mode` | `users.dark_mode` in DB aktualisiert |
| 7 | Seite neu laden | Gespeicherte Praeferenz wird angewendet |
| 8 | Dark-Mode auch auf Login-Seite waehlbar | Wechsel ohne Login moeglich |

**Akzeptanzkriterien:**
- [ ] Drei Modi: SYSTEM, LIGHT, DARK
- [ ] Wert in `users.dark_mode` VARCHAR(10) gespeichert (Default: SYSTEM)
- [ ] `useDarkMode` Composable verwaltet den Modus
- [ ] CSS Custom Properties `--mw-*` schalten um
- [ ] Dark Mode auf Login-Seite waehlbar (auch ohne Login)
- [ ] Validierung: nur SYSTEM, LIGHT, DARK akzeptiert (400 bei ungueltigem Wert)

---

### US-369: Sprache wechseln
**Als** Benutzer **moechte ich** die Sprache der Anwendung wechseln, **damit** ich die Oberflaeche in meiner bevorzugten Sprache nutzen kann.

**Vorbedingungen:** Mehrere Sprachen konfiguriert (`tenant_config.available_languages` enthaelt mehr als eine Sprache).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `eltern@monteweb.local` (P) einloggen | Login erfolgreich |
| 2 | Profil oeffnen | LanguageSwitcher-Komponente ist sichtbar |
| 3 | Sprache von "Deutsch" auf "English" wechseln | Alle UI-Texte wechseln zu Englisch |
| 4 | Sprachwahl auch auf Login-Seite moeglich | LanguageSwitcher im Login sichtbar |
| 5 | LanguageSwitcher nur sichtbar wenn >1 Sprache aktiviert | Bei nur einer Sprache kein Switcher |
| 6 | Standardsprache: Deutsch (de) | Default-Sprache korrekt |

**Akzeptanzkriterien:**
- [ ] i18n: de.ts + en.ts fuer alle UI-Texte
- [ ] `available_languages TEXT[]` bestimmt waehlbare Sprachen
- [ ] LanguageSwitcher in Profil und Login (nicht im Header)
- [ ] Switcher nur sichtbar wenn >1 Sprache aktiviert
- [ ] Sprachpraeferenz wird gespeichert

---

### US-370: Wartungsmodus aktivieren
**Als** Superadmin **moechte ich** einen Wartungsmodus aktivieren, **damit** das System fuer nicht-Admins gesperrt wird waehrend Wartungsarbeiten stattfinden.

**Vorbedingungen:** Admin-Zugang.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Admin > Module > Wartungsmodus aktivieren | Toggle in `tenant_config.modules` JSONB: `maintenance: true` |
| 3 | Wartungsmeldung eingeben: "System wird aktualisiert. Bitte versuchen Sie es in 30 Minuten erneut." | Meldung in `tenant_config.maintenance_message` |
| 4 | Speichern: `PUT /api/v1/admin/maintenance` | Wartungsmodus wird aktiviert |
| 5 | Als `eltern@monteweb.local` (P) einloggen versuchen | 503 Service Unavailable mit Wartungsmeldung |
| 6 | Admin-Zugang funktioniert weiterhin | SUPERADMIN ist nicht gesperrt |
| 7 | Wartungsmodus deaktivieren | System ist wieder fuer alle zugaenglich |

**Akzeptanzkriterien:**
- [ ] Wartungsmodus sperrt alle Nicht-Admins (503)
- [ ] Konfigurierbare Wartungsmeldung
- [ ] SUPERADMIN hat weiterhin Zugang
- [ ] Aktivierung/Deaktivierung ueber Admin-Panel
- [ ] `MaintenanceModeFilter` prueft bei jedem Request

---

### US-371: Impersonation als Cross-Cutting-Funktion
**Als** Superadmin **moechte ich** die Impersonation aus verschiedenen Kontexten starten, **damit** ich flexibel Probleme diagnostizieren kann.

**Vorbedingungen:** Impersonation-Modul aktiviert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Als `admin@monteweb.local` (SA) einloggen | Login erfolgreich |
| 2 | Benutzer "lehrer@monteweb.local" impersonieren | Sicht wechselt zu Teacher-Ansicht |
| 3 | Feed pruefen | Feed des Teachers wird angezeigt |
| 4 | Raeume pruefen | Nur Raeume des Teachers sichtbar |
| 5 | Benachrichtigungen pruefen | Notifications des Teachers |
| 6 | Impersonation beenden | Zurueck zur Admin-Ansicht |
| 7 | Audit-Log pruefen | Impersonation-Start und -Ende protokolliert |

**Akzeptanzkriterien:**
- [ ] Impersonierter Benutzer hat exakt die Berechtigungen der Zielrolle
- [ ] Visuelles Banner waehrend der Impersonation
- [ ] Keine Impersonation eines anderen SUPERADMIN
- [ ] Alle Aktionen waehrend Impersonation werden im Audit-Log dem Admin zugeordnet

---

### US-372: Responsive Design und Touch-Bedienung
**Als** Benutzer **moechte ich** MonteWeb auf verschiedenen Geraeten nutzen (Desktop, Tablet, Smartphone), **damit** ich flexibel auf die Plattform zugreifen kann.

**Vorbedingungen:** MonteWeb ist erreichbar.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | MonteWeb auf Desktop-Browser oeffnen (1920x1080) | Vollstaendiges Layout, Sidebar sichtbar |
| 2 | Browser-Fenster auf Tablet-Breite verkleinern (768px) | Responsive Anpassung, Hamburger-Menue |
| 3 | Browser-Fenster auf Smartphone-Breite verkleinern (375px) | Mobile Ansicht, Touch-optimierte Buttons |
| 4 | Kanban-Board auf Mobile pruefen | Board horizontal scrollbar oder angepasst |
| 5 | GlobalSearch (Ctrl+K) auf Mobile pruefen | Dialog passt sich an (`maxWidth: 95vw`) |

**Akzeptanzkriterien:**
- [ ] Responsive Breakpoints fuer Desktop, Tablet, Mobile
- [ ] Touch-optimierte Interaktionselemente
- [ ] Such-Dialog: `maxWidth: 95vw` fuer Mobile
- [ ] Kanban: Horizontal scrollbar auf kleinen Bildschirmen

---

### US-373: Tastaturnavigation in der globalen Suche
**Als** Benutzer **moechte ich** die globale Suche komplett per Tastatur bedienen, **damit** ich effizient arbeiten kann.

**Vorbedingungen:** Such-Dialog ist geoeffnet (Ctrl+K).

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Ctrl+K druecken | Such-Dialog oeffnet sich |
| 2 | Suchbegriff eingeben | Ergebnisse erscheinen |
| 3 | Pfeil-nach-unten druecken | Erstes Ergebnis wird selektiert (hervorgehoben) |
| 4 | Mehrmals Pfeil-nach-unten druecken | Selektion wandert durch die Ergebnisliste |
| 5 | Pfeil-nach-oben druecken | Selektion wandert zurueck |
| 6 | Enter druecken auf selektiertem Ergebnis | Navigation zum Ergebnis, Dialog schliesst sich |
| 7 | Esc druecken | Dialog schliesst sich ohne Navigation |

**Akzeptanzkriterien:**
- [ ] Pfeil-Tasten navigieren durch Ergebnisse (zirkulaer)
- [ ] Enter oeffnet selektiertes Ergebnis
- [ ] Esc schliesst den Dialog
- [ ] `scrollIntoView({ block: 'nearest' })` bei Selektion aenderung
- [ ] Selektiertes Ergebnis visuell hervorgehoben (CSS-Klasse `selected`)

---

### US-374: Fehlerberichterstattung (Frontend Error Reporting)
**Als** System **moechte ich** Frontend-Fehler automatisch melden, **damit** Entwickler ueber Probleme informiert werden.

**Vorbedingungen:** Error-Reporting ist konfiguriert.

| # | Testschritt | Erwartetes Ergebnis |
|---|------------|---------------------|
| 1 | Frontend-Fehler tritt auf (JavaScript-Fehler) | Fehler wird automatisch erfasst |
| 2 | `POST /api/v1/error-reports` wird aufgerufen (oeffentlich) | Fehlerbericht wird gespeichert |
| 3 | Fingerprint-basierte Deduplizierung | Gleicher Fehler wird nicht doppelt angelegt |
| 4 | Occurrence-Counter wird hochgezaehlt | Haeufigkeit des Fehlers sichtbar |
| 5 | Admin > Error-Reports: neuer Eintrag sichtbar | Admin sieht den Fehlerbericht |

**Akzeptanzkriterien:**
- [ ] Fehlerberichte werden per `POST /api/v1/error-reports` gemeldet (oeffentlicher Endpoint)
- [ ] Fingerprint-basierte Deduplizierung (gleiche Fehler werden zusammengefasst)
- [ ] Occurrence-Tracking (Haeufigkeit)
- [ ] `useErrorReporting` Composable im Frontend

---
