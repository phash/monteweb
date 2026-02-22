-- #96: School info fields on tenant_config
ALTER TABLE tenant_config ADD COLUMN school_full_name VARCHAR(300);
ALTER TABLE tenant_config ADD COLUMN school_address TEXT;
ALTER TABLE tenant_config ADD COLUMN school_principal VARCHAR(200);
ALTER TABLE tenant_config ADD COLUMN tech_contact_name VARCHAR(200);
ALTER TABLE tenant_config ADD COLUMN tech_contact_email VARCHAR(200);

-- #95: Default privacy policy (BaySchO-konform, Platzhalter für Schulinfos)
UPDATE tenant_config SET privacy_policy_text = '
<h2>Datenschutzerkl&auml;rung f&uuml;r das Schul-Intranet</h2>

<h3>1. Verantwortliche Stelle</h3>
<p>
{{SCHOOL_NAME}}<br>
{{SCHOOL_ADDRESS}}
</p>
<p>Schulleitung: {{SCHOOL_PRINCIPAL}}</p>
<p>Technischer Ansprechpartner: {{TECH_CONTACT_NAME}} ({{TECH_CONTACT_EMAIL}})</p>

<h3>2. Zweck der Datenverarbeitung</h3>
<p>Das Schul-Intranet dient der schulinternen Kommunikation und Organisation. Es werden personenbezogene Daten verarbeitet, soweit dies f&uuml;r den Betrieb der Plattform und die schulische Organisation erforderlich ist.</p>
<p>Insbesondere werden Daten verarbeitet f&uuml;r:</p>
<ul>
  <li>Benutzerkonten und Authentifizierung</li>
  <li>Schulinterne Kommunikation (Nachrichten, Feed)</li>
  <li>Organisation von R&auml;umen und Gruppen</li>
  <li>Kalender und Terminplanung</li>
  <li>Elternstunden und Putz-Organisation</li>
  <li>Formulare und Umfragen</li>
  <li>Dateiablage und Fotobox</li>
</ul>

<h3>3. Rechtsgrundlage</h3>
<p>Die Verarbeitung personenbezogener Daten erfolgt auf Grundlage von:</p>
<ul>
  <li>Art. 6 Abs. 1 lit. e DSGVO i.V.m. Art. 85 BayEUG &mdash; &ouml;ffentliche Aufgabe der Schule</li>
  <li>Art. 6 Abs. 1 lit. a DSGVO &mdash; Einwilligung (z.B. f&uuml;r Fotos)</li>
  <li>&sect; 46 BaySchO &mdash; Einsatz digitaler Kommunikationsmittel</li>
  <li>Anlage 2 Abschnitt 4 BaySchO &mdash; Passwortgesch&uuml;tzte Lernplattformen</li>
</ul>

<h3>4. Gespeicherte Daten</h3>
<p>Folgende personenbezogene Daten werden verarbeitet:</p>
<ul>
  <li><strong>Stammdaten:</strong> Name, Vorname, E-Mail-Adresse, Telefonnummer, Rolle (Lehrkraft/Eltern/Sch&uuml;ler)</li>
  <li><strong>Kommunikationsdaten:</strong> Nachrichten, Feed-Beitr&auml;ge, Kommentare</li>
  <li><strong>Organisationsdaten:</strong> Raum-Mitgliedschaften, Kalendereintr&auml;ge, Familienzugeh&ouml;rigkeit</li>
  <li><strong>Nutzungsdaten:</strong> Zeitstempel von Anmeldungen, Benachrichtigungseinstellungen</li>
  <li><strong>Dateien:</strong> Hochgeladene Dokumente und Bilder</li>
</ul>

<h3>5. Empf&auml;nger der Daten</h3>
<p>Personenbezogene Daten werden grunds&auml;tzlich nicht an Dritte weitergegeben. Der Zugang zu Daten ist auf berechtigte schulische Nutzer beschr&auml;nkt. Die Plattform wird selbst gehostet &mdash; es erfolgt keine Daten&uuml;bermittlung an externe Dienstleister oder in Drittl&auml;nder.</p>

<h3>6. Speicherdauer</h3>
<p>Personenbezogene Daten werden gel&ouml;scht, sobald der Zweck der Verarbeitung entf&auml;llt:</p>
<ul>
  <li>Benutzerkonten: Bei Verlassen der Schule oder auf Antrag (14 Tage L&ouml;schfrist)</li>
  <li>Nachrichten und Bilder: Automatische L&ouml;schung nach 90 Tagen f&uuml;r Bilder</li>
  <li>Benachrichtigungen: Automatische L&ouml;schung nach 90 Tagen</li>
  <li>Audit-Protokolle: 3 Jahre (gesetzliche Aufbewahrungsfrist)</li>
</ul>

<h3>7. Betroffenenrechte</h3>
<p>Sie haben gem&auml;&szlig; der DSGVO folgende Rechte:</p>
<ul>
  <li><strong>Auskunftsrecht (Art. 15):</strong> Sie k&ouml;nnen Auskunft &uuml;ber Ihre gespeicherten Daten verlangen. Nutzen Sie daf&uuml;r die Funktion &bdquo;Meine Daten exportieren&ldquo; in Ihrem Profil.</li>
  <li><strong>Recht auf Berichtigung (Art. 16):</strong> Sie k&ouml;nnen die Berichtigung unrichtiger Daten verlangen. &Auml;ndern Sie Ihre Daten direkt in Ihrem Profil.</li>
  <li><strong>Recht auf L&ouml;schung (Art. 17):</strong> Sie k&ouml;nnen die L&ouml;schung Ihrer Daten verlangen. Nutzen Sie daf&uuml;r &bdquo;Konto l&ouml;schen&ldquo; in Ihrem Profil. Nach einer Sicherheitsfrist von 14 Tagen werden alle Ihre Daten unwiderruflich gel&ouml;scht.</li>
  <li><strong>Recht auf Daten&uuml;bertragbarkeit (Art. 20):</strong> Sie k&ouml;nnen Ihre Daten in einem maschinenlesbaren Format (JSON) exportieren.</li>
  <li><strong>Widerspruchsrecht (Art. 21):</strong> Sie k&ouml;nnen der Verarbeitung Ihrer Daten widersprechen.</li>
</ul>
<p>Zur Aus&uuml;bung Ihrer Rechte wenden Sie sich an die Schulleitung oder den technischen Ansprechpartner.</p>

<h3>8. Datensicherheit</h3>
<p>Wir treffen angemessene technische und organisatorische Ma&szlig;nahmen zum Schutz Ihrer Daten:</p>
<ul>
  <li>Verschl&uuml;sselte &Uuml;bertragung (HTTPS/TLS)</li>
  <li>Passwortgesch&uuml;tzte Benutzerkonten mit JWT-Authentifizierung</li>
  <li>Rollenbasierte Zugriffskontrolle</li>
  <li>Selbst-gehostete Infrastruktur ohne externe Cloud-Dienste</li>
  <li>Regelm&auml;&szlig;ige Sicherheitsupdates</li>
</ul>

<h3>9. Einwilligungen</h3>
<p>F&uuml;r bestimmte Verarbeitungen ist eine gesonderte Einwilligung erforderlich (z.B. Ver&ouml;ffentlichung von Fotos). Diese Einwilligungen k&ouml;nnen jederzeit in den Profileinstellungen widerrufen werden. Bei minderj&auml;hrigen Sch&uuml;lerinnen und Sch&uuml;lern erteilen die Erziehungsberechtigten die Einwilligung.</p>

<h3>10. Beschwerderecht</h3>
<p>Sie haben das Recht, sich bei der zust&auml;ndigen Datenschutzaufsichtsbeh&ouml;rde zu beschweren:</p>
<p>
Bayerischer Landesbeauftragter f&uuml;r den Datenschutz<br>
Wagm&uuml;llerstra&szlig;e 18, 80538 M&uuml;nchen<br>
Tel: 089 212672-0<br>
E-Mail: poststelle@datenschutz-bayern.de
</p>
', privacy_policy_version = '1.0'
WHERE privacy_policy_text IS NULL;

-- #98: Default terms of service
UPDATE tenant_config SET terms_text = '
<h2>Nutzungsbedingungen f&uuml;r das Schul-Intranet</h2>

<h3>1. Geltungsbereich</h3>
<p>Diese Nutzungsbedingungen regeln die Nutzung des Schul-Intranets von {{SCHOOL_NAME}}. Mit der Registrierung und Nutzung der Plattform erkl&auml;ren Sie sich mit diesen Bedingungen einverstanden.</p>

<h3>2. Zugang und Registrierung</h3>
<p>Die Nutzung des Schul-Intranets ist auf Angeh&ouml;rige der Schulgemeinschaft beschr&auml;nkt: Lehrkr&auml;fte, Erziehungsberechtigte und Sch&uuml;lerinnen und Sch&uuml;ler. Der Zugang erfordert eine Registrierung mit g&uuml;ltiger E-Mail-Adresse. Die Freischaltung erfolgt durch die Schulverwaltung.</p>

<h3>3. Verantwortungsvoller Umgang</h3>
<p>Die Nutzerinnen und Nutzer verpflichten sich:</p>
<ul>
  <li>Die Plattform ausschlie&szlig;lich f&uuml;r schulische Zwecke zu nutzen</li>
  <li>Keine rechtswidrigen, beleidigenden oder diskriminierenden Inhalte zu ver&ouml;ffentlichen</li>
  <li>Die Pers&ouml;nlichkeitsrechte anderer Nutzer zu respektieren</li>
  <li>Keine Fotos oder pers&ouml;nliche Daten anderer Personen ohne deren Einwilligung zu ver&ouml;ffentlichen</li>
  <li>Ihr Passwort vertraulich zu behandeln und nicht an Dritte weiterzugeben</li>
  <li>Keine automatisierten Zugriffe oder Schadsoftware einzusetzen</li>
</ul>

<h3>4. Inhalte und Urheberrecht</h3>
<p>Nutzer sind f&uuml;r die von ihnen eingestellten Inhalte selbst verantwortlich. Durch das Hochladen von Inhalten r&auml;umen Nutzer der Schule ein einfaches Nutzungsrecht f&uuml;r den schulinternen Gebrauch ein. Das Urheberrecht Dritter ist zu beachten.</p>

<h3>5. Datenschutz</h3>
<p>Die Verarbeitung personenbezogener Daten erfolgt gem&auml;&szlig; der <a href="/privacy">Datenschutzerkl&auml;rung</a> und den geltenden datenschutzrechtlichen Bestimmungen (DSGVO, BayDSG, BayEUG).</p>

<h3>6. Kommunikationsregeln</h3>
<p>Die Kommunikation &uuml;ber das Schul-Intranet unterliegt den schulischen Kommunikationsregeln:</p>
<ul>
  <li>Sachlicher und respektvoller Umgangston</li>
  <li>Nachrichten zwischen Lehrkr&auml;ften und Erziehungsberechtigten sind stets erlaubt</li>
  <li>Die Kommunikation zwischen Sch&uuml;lern bzw. zwischen Eltern untereinander kann von der Schulverwaltung aktiviert oder deaktiviert werden</li>
</ul>

<h3>7. Pflichten der Erziehungsberechtigten</h3>
<p>Erziehungsberechtigte sind daf&uuml;r verantwortlich, die Nutzung der Plattform durch ihre minderj&auml;hrigen Kinder zu begleiten und zu &uuml;berwachen. Einwilligungen f&uuml;r minderj&auml;hrige Nutzer (z.B. Fotoeinwilligung) werden durch die Erziehungsberechtigten erteilt.</p>

<h3>8. Verst&ouml;&szlig;e und Sanktionen</h3>
<p>Bei Verst&ouml;&szlig;en gegen diese Nutzungsbedingungen kann die Schulverwaltung den Zugang vor&uuml;bergehend oder dauerhaft sperren. Schwerwiegende Verst&ouml;&szlig;e k&ouml;nnen schulrechtliche Konsequenzen nach sich ziehen.</p>

<h3>9. Haftung</h3>
<p>Die Schule haftet nicht f&uuml;r die Verf&uuml;gbarkeit der Plattform oder f&uuml;r Sch&auml;den durch von Nutzern eingestellte Inhalte. F&uuml;r die Richtigkeit schulischer Informationen wird nach bestem Wissen gehandelt.</p>

<h3>10. &Auml;nderungen</h3>
<p>&Auml;nderungen dieser Nutzungsbedingungen werden &uuml;ber die Plattform bekannt gegeben. Bei wesentlichen &Auml;nderungen ist eine erneute Zustimmung erforderlich.</p>

<p><em>Stand: Februar 2026</em></p>
', terms_version = '1.0'
WHERE terms_text IS NULL;
