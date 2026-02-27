-- ============================================================
-- MonteWeb — Lebendige Montessori-Schule München
-- Schuljahr 2025/2026, Bundesland Bayern
--
-- Erzeugt:
--   • 4 Schulbereiche
--   • 12 Lehrkräfte
--   • 50 Schüler + 50 Elternteile in 50 Familien
--   • 9 Klassenräume (mit Mitgliedern)
--   • 8 andere Räume (AGs, Gremien)
--   • 20 Kalender-Events (Schuljahr Bayern 2025/2026)
--   • 16 Feed-Posts
--   • 20 Elternstunden-Jobs
--
-- Alle Passwörter: test1234
-- ============================================================

CREATE OR REPLACE FUNCTION _mw_email(t TEXT) RETURNS TEXT AS $$
BEGIN
  RETURN lower(
    replace(replace(replace(replace(replace(
      t,
      'ä','ae'), 'ö','oe'), 'ü','ue'), 'ß','ss'), ' ','-')
  );
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
  pw CONSTANT TEXT := '$2a$10$dQa6248nzH.S5yYnSpGfr.b7BnuGroDZ/G18yp.vzoAx45FQ7hecO'; -- test1234

  -- ── Lehrkräfte ────────────────────────────────────────────
  t_first TEXT[] := ARRAY[
    'Maria','Klaus','Anna','Thomas','Petra','Markus',
    'Sabine','Wolfgang','Julia','Andreas','Christine','Stefan'];
  t_last TEXT[] := ARRAY[
    'Huber','Bauer','Schmidt','Müller','Weber','Fischer',
    'Meier','Krause','Braun','Richter','Lehmann','König'];
  t_ids UUID[12];

  -- ── Schüler ───────────────────────────────────────────────
  s_first TEXT[] := ARRAY[
    -- Krippe (1-5)
    'Mia','Finn','Emma','Noah','Lena',
    -- Kindergarten Regenbogen (6-11)
    'Leon','Sophia','Elias','Hannah','Ben','Clara',
    -- Kindergarten Schmetterlinge (12-16)
    'Felix','Greta','Paul','Marie','Jonas',
    -- Klasse 1/2 Haselnuss (17-22)
    'Lea','Theo','Johanna','Milan','Amelie','Moritz',
    -- Klasse 3/4 Eichenwald (23-28)
    'Oscar','Ida','Anton','Nele','Emil','Frieda',
    -- Klasse 5/6 Horizont (29-33)
    'Julian','Marlene','Leo','Eva','David',
    -- Klasse 6/7 Entdecker (34-39)
    'Helena','Jan','Mathilda','Tim','Pauline','Erik',
    -- Klasse 8/9 Forscherlabor (40-45)
    'Luise','Jakob','Mara','Tom','Lotte','Nico',
    -- Klasse 9/10 Weitsicht (46-50)
    'Victoria','Max','Paula','Sam','Thea'];
  s_last TEXT[] := ARRAY[
    'Müller','Schmidt','Fischer','Weber','Wagner',
    'Becker','Hoffmann','Schäfer','Koch','Bauer','Richter',
    'Klein','Wolf','Neumann','Schwarz',
    'Zimmermann','Braun','Krüger','Hartmann','Lange','Werner',
    'Meier','Lehmann','Schmid','Krause','Schulz',
    'Maier','Köhler','Jung','Hahn','Keller',
    'Frank','Berger','Winkler','Roth','Beck',
    'Lorenz','Baumann','Franke','Albrecht','Schuster',
    'Simon','Ludwig','Böhm','Winter','Sommer',
    'Haas','Graf','Heinrich','Seidel'];
  s_ids UUID[50];

  -- ── Eltern ────────────────────────────────────────────────
  p_first TEXT[] := ARRAY[
    'Anna','Thomas','Maria','Michael','Sabine',
    'Stefan','Claudia','Peter','Andrea','Martin',
    'Susanne','Jürgen','Monika','Uwe','Petra',
    'Frank','Nicole','Ralf','Birgit','Holger',
    'Katrin','Bernd','Heike','Markus','Karin',
    'Dirk','Anja','Sven','Silke','Olaf',
    'Tanja','René','Nadine','Jens','Yvonne',
    'Kai','Simone','Thorsten','Daniela','Lars',
    'Sandra','Christian','Melanie','Torsten','Janine',
    'Matthias','Stefanie','Carsten','Manuela','Gerd'];
  p_ids UUID[50];

  f_ids UUID[50];

  -- ── Schulbereiche ─────────────────────────────────────────
  sec_kinderhaus   UUID;
  sec_grundstufe   UUID;
  sec_mittelstufe  UUID;
  sec_oberstufe    UUID;

  -- ── Klassenräume (9) ─────────────────────────────────────
  class_names TEXT[] := ARRAY[
    'Krippe "Sonnenkäfer"',
    'Kindergarten "Regenbogen"',
    'Kindergarten "Schmetterlinge"',
    'Klasse 1/2 "Haselnuss"',
    'Klasse 3/4 "Eichenwald"',
    'Klasse 5/6 "Horizont"',
    'Klasse 6/7 "Entdecker"',
    'Klasse 8/9 "Forscherlabor"',
    'Klasse 9/10 "Weitsicht"'];
  class_descs TEXT[] := ARRAY[
    'Liebevolle Betreuung für Kinder von 1-3 Jahren nach Montessori-Pädagogik.',
    'Unsere Kindergartengruppe für 3-6-Jährige - Morgengruppe.',
    'Unsere Kindergartengruppe für 3-6-Jährige - Nachmittagsgruppe.',
    'Altersgemischte Lerngruppe der Klassen 1 und 2 (Kosmische Erziehung).',
    'Altersgemischte Lerngruppe der Klassen 3 und 4 (Kosmische Erziehung).',
    'Selbstständiges Arbeiten in der Lernwerkstatt, Klasse 5 und 6.',
    'Forschendes Lernen und Erdkinderplan, Klasse 6 und 7.',
    'Projektbasiertes Arbeiten und Praktikumsvorbereitungen, Klasse 8 und 9.',
    'Abschlussjahr mit Großem Projekt und Übergangsbegleitung, Klasse 9/10.'];
  -- Welcher Bereich (1=Kinderhaus, 2=Grundstufe, 3=Mittelstufe, 4=Oberstufe)
  class_sec INT[] := ARRAY[1,1,1, 2,2, 3,3, 4,4];
  -- Hauptlehrkraft je Klasse (Index in t_ids)
  class_lead INT[] := ARRAY[1,2,3, 4,5, 6,7, 8,9, 10];
  -- Schüler: Startindex und Endindex in s_ids
  class_s_start INT[] := ARRAY[1,  6, 12, 17, 23, 29, 34, 40, 46];
  class_s_end   INT[] := ARRAY[5, 11, 16, 22, 28, 33, 39, 45, 50];
  class_ids UUID[9];

  -- ── Andere Räume (8) ─────────────────────────────────────
  other_names TEXT[] := ARRAY[
    'Lehrerzimmer',
    'Elternbeirat',
    'Schülervertretung (SV)',
    'AG Musik & Theater',
    'AG Sport & Bewegung',
    'Schulküche',
    'Schulbibliothek',
    'Natur & Garten AG'];
  other_descs TEXT[] := ARRAY[
    'Interner Raum für das Lehrkollegium - Austausch und Koordination.',
    'Gremium der gewählten Elternvertreterinnen und -vertreter.',
    'Mitbestimmungsgremium der Schülerinnen und Schüler.',
    'Wöchentliche AG für Musik, Chor und Schultheater.',
    'Bewegungsförderung und Sportwettkämpfe - AG trifft sich 2× pro Woche.',
    'Gemeinsames Kochen und Ernährungsbildung für alle Jahrgänge.',
    'Leseförderung, Medienbildung und freies Stöbern in Büchern.',
    'Schulgemüsegarten, Kompost, Bienenstöcke - Umweltbildung live.'];
  other_ids UUID[8];

  -- ── Kalender-Events ──────────────────────────────────────
  -- scope: SCHOOL | SECTION | ROOM
  ev_titles TEXT[] := ARRAY[
    'Schuljahresbeginn 2025/26',
    'Elternabend Kinderhaus',
    'Elternabend Grundstufe',
    'Elternabend Mittelstufe',
    'Elternabend Oberstufe',
    'Herbstfest der ganzen Schule',
    'Projektwoche "Unsere Erde"',
    'Elternsprechtag',
    'Nikolausfeier',
    'Weihnachtsfeier',
    'Schulkonferenz',
    'Vorlesewettbewerb',
    'Wintersportfest',
    'Frühjahrs-Elternabend Grundstufe',
    'Projekttag "Berufsorientierung" Kl. 8/9',
    'Sportfest',
    'Tag der offenen Tür',
    'Abschlussfest Klasse 9/10',
    'AG Musik - Jahreskonzert',
    'Schuljahresabschluss'];
  ev_descs TEXT[] := ARRAY[
    'Herzlich willkommen im neuen Schuljahr! Alle Schülerinnen, Schüler und Lehrkräfte versammeln sich im Hof.',
    'Informationsabend für Eltern des Kinderhauses. Themen: Montessori-Methode, Jahresziele, Ansprechpartner.',
    'Informationsabend für Eltern der Grundstufe. Kosmische Erziehung, Leistungsrückmeldungen, Projekte.',
    'Informationsabend für Eltern der Mittelstufe. Erdkinderplan, Praktikum, individuelle Lernpläne.',
    'Informationsabend für Eltern der Oberstufe. Großes Projekt, Übergänge, Abschlussplanung.',
    'Gemeinsames Herbstfest mit Laternenumzug, Marktständen und Musik. Alle sind herzlich eingeladen!',
    'Schulweite Projektwoche zum Thema Nachhaltigkeit und Umwelt. Alle Klassen arbeiten jahrgangsübergreifend.',
    'Individuelle Gesprächstermine mit den Klassenlehrkräften. Bitte vorher anmelden.',
    'Kleines Fest für die Jüngsten mit dem Nikolaus. Kinderhaus und Grundstufe.',
    'Schulweite Weihnachtsfeier mit Aufführungen der AGs und gemeinsamen Liedern.',
    'Jährliche Schulkonferenz aller Lehrkräfte, Elternbeirat und Schulleitung.',
    'Schülerinnen und Schüler der Klassen 3-6 lesen vor. Publikum herzlich willkommen.',
    'Wintersportfest auf dem Schulgelände: Hindernislauf, Schlittenrennen (falls Schnee), Bewegungsspiele.',
    'Frühjahrs-Elternabend für Eltern der Grundstufe mit Rückblick und Ausblick.',
    'Oberstufenschüler erkunden Berufsfelder und präsentieren ihre Ergebnisse.',
    'Großes Jahressportfest auf dem Schulhof und der nahegelegenen Sportanlage.',
    'Einblicke in den Schulalltag für interessierte Familien und zukünftige Eltern.',
    'Feierlicher Abschluss der Klasse 9/10 mit Großem-Projekt-Präsentation und Zeugnisübergabe.',
    'Jahreskonzert der AG Musik & Theater: Chor, Band und Theaterszenen.',
    'Gemeinsamer Abschluss des Schuljahres im großen Hof. Mit Zeugnisausgabe und Sommerfest.'];
  ev_scopes TEXT[] := ARRAY[
    'SCHOOL','SECTION','SECTION','SECTION','SECTION',
    'SCHOOL','SCHOOL','SCHOOL','SECTION','SCHOOL',
    'SCHOOL','SCHOOL','SCHOOL','SECTION','ROOM',
    'SCHOOL','SCHOOL','SECTION','ROOM','SCHOOL'];
  -- 0=school, 1=kinderhaus, 2=grundstufe, 3=mittelstufe, 4=oberstufe, 5=class 8/9, 6=ag musik
  ev_scope_ref INT[] := ARRAY[
    0,1,2,3,4,
    0,0,0,1,0,
    0,0,0,2,5,
    0,0,4,6,0];
  -- Startdatum (Schuljahr 2025/2026 Bayern)
  ev_start_dates DATE[] := ARRAY[
    '2025-09-15'::DATE,  -- Schuljahresbeginn
    '2025-09-25'::DATE,  -- Elternabend Kinderhaus
    '2025-09-30'::DATE,  -- Elternabend Grundstufe
    '2025-10-02'::DATE,  -- Elternabend Mittelstufe
    '2025-10-07'::DATE,  -- Elternabend Oberstufe
    '2025-10-17'::DATE,  -- Herbstfest
    '2025-10-20'::DATE,  -- Projektwoche
    '2025-11-06'::DATE,  -- Elternsprechtag
    '2025-12-05'::DATE,  -- Nikolaus
    '2025-12-19'::DATE,  -- Weihnachtsfeier
    '2026-01-15'::DATE,  -- Schulkonferenz
    '2026-02-12'::DATE,  -- Vorlesewettbewerb
    '2026-02-20'::DATE,  -- Wintersportfest
    '2026-03-19'::DATE,  -- Frühjahrs-Elternabend Grundstufe
    '2026-04-23'::DATE,  -- Berufsorientierung
    '2026-06-12'::DATE,  -- Sportfest
    '2026-06-27'::DATE,  -- Tag der offenen Tür
    '2026-07-10'::DATE,  -- Abschlussfest
    '2026-07-03'::DATE,  -- AG Konzert
    '2026-07-24'::DATE]; -- Schuljahresabschluss
  ev_end_dates DATE[] := ARRAY[
    '2025-09-15'::DATE,
    '2025-09-25'::DATE,
    '2025-09-30'::DATE,
    '2025-10-02'::DATE,
    '2025-10-07'::DATE,
    '2025-10-17'::DATE,
    '2025-10-24'::DATE,  -- Projektwoche 1 Woche
    '2025-11-06'::DATE,
    '2025-12-05'::DATE,
    '2025-12-19'::DATE,
    '2026-01-15'::DATE,
    '2026-02-12'::DATE,
    '2026-02-20'::DATE,
    '2026-03-19'::DATE,
    '2026-04-23'::DATE,
    '2026-06-12'::DATE,
    '2026-06-27'::DATE,
    '2026-07-10'::DATE,
    '2026-07-03'::DATE,
    '2026-07-24'::DATE];
  ev_all_day BOOLEAN[] := ARRAY[
    true, false, false, false, false,
    true, true, false, false, true,
    false, false, true, false, false,
    true, true, true, false, true];
  ev_start_times TIME[] := ARRAY[
    NULL,'19:00','19:00','19:00','19:00',
    NULL,NULL,'15:00','15:00',NULL,
    '14:00','09:00',NULL,'19:00','08:00',
    NULL,NULL,NULL,'16:00',NULL];
  ev_end_times TIME[] := ARRAY[
    NULL,'21:00','21:00','21:00','21:00',
    NULL,NULL,'19:00','17:00',NULL,
    '16:00','11:00',NULL,'21:00','16:00',
    NULL,NULL,NULL,'18:30',NULL];

  -- ── Feed-Posts ───────────────────────────────────────────
  post_titles TEXT[] := ARRAY[
    'Herzlich willkommen im Schuljahr 2025/26!',
    'Herbstfest - Wir brauchen eure Hilfe!',
    'Projektwoche "Unsere Erde" - Rückblick',
    'Nikolausfeier: Bitte Socken mitbringen',
    'Neue Bücher in der Schulbibliothek',
    'Elternabend Krippe - Zusammenfassung',
    'Klasse 1/2: Unser Kosmischer Kalender',
    'Klasse 3/4: Matheolympiade - Tolle Ergebnisse!',
    'AG Sport: Neue Zeiten ab November',
    'Klasse 8/9: Praktikumsberichte abgeben',
    'Frühjahrsputz im Schulgarten - Alle sind eingeladen',
    'Schulbibliothek sucht Buchpaten',
    'Klasse 6/7: Exkursion zum Englischen Garten',
    'Weihnachtsfeier - Programm steht fest',
    'Tag der offenen Tür - Helfer gesucht',
    'Sportfest 2026 - Anmeldung offen'];
  post_contents TEXT[] := ARRAY[
    'Liebe Schulgemeinschaft, wir freuen uns, alle Kinder, Eltern und Lehrkräfte im neuen Schuljahr 2025/26 begrüßen zu dürfen! Die Türen öffnen am 15. September um 8:00 Uhr. Wir starten gemeinsam mit einer kleinen Feier im Hof.',
    'Unser Herbstfest findet am 17. Oktober statt. Wir suchen noch fleißige Helfer für Aufbau, Stände und Abbau. Wer kann, meldet sich bitte bis 10. Oktober im Sekretariat. Herzlichen Dank!',
    'Was für eine bereichernde Projektwoche! Die Kinder haben geforscht, gebaut, gesät und gemalt. Eine Wanderausstellung mit den Ergebnissen ist ab nächster Woche im Foyer zu sehen.',
    'Am 5. Dezember kommt der Nikolaus! Bitte bringt jedes Kind einen kleinen Stiefel oder eine Socke mit. Die Krippe und Grundstufe feiern gemeinsam um 15:00 Uhr im großen Saal.',
    'Die Schulbibliothek hat 40 neue Bücher erhalten - Sachbücher, Abenteuerromane und Bilderbücher. Ein großes Dankeschön an den Förderverein! Die Ausleihe läuft täglich von 10-12 Uhr.',
    'Beim Elternabend haben wir die Jahresziele besprochen und uns gegenseitig kennengelernt. Die Präsentation gibt es als PDF auf Anfrage. Nächstes Treffen ist im März.',
    'Die Klasse 1/2 hat ihren ersten Kosmischen Kalender fertiggestellt! Auf der großen Zeitlinie sind Dinosaurier, die ersten Menschen und unsere Schule zu sehen. Schaut es euch in der Klasse an!',
    'Drei Kinder der Klasse 3/4 haben bei der Schulolympiade mitgemacht und tolle Platzierungen erzielt. Wir sind sehr stolz! Die Ergebnisse werden beim Elternabend vorgestellt.',
    'Die AG Sport trifft sich ab dem 3. November dienstags von 14-15:30 Uhr und donnerstags von 13-14:30 Uhr. Anmeldung ist noch möglich - sprecht Herrn Krause an.',
    'Liebe Schülerinnen und Schüler der Klasse 8/9, eure Praktikumsberichte müssen bis zum 28. Februar abgegeben werden. Format: A4, mindestens 5 Seiten, Deckblatt mit Schullogo.',
    'Am letzten Samstag im März laden wir alle Familien zum Frühjahrsputz in den Schulgarten ein. Gartenhandschuhe und Schubkarren mitbringen. Es gibt Brotzeit für alle Helfer!',
    'Die Schulbibliothek sucht Buchpaten: Wer 10 € spendet, darf ein Buch auswählen und erhält ein Exlibris mit seinem Namen. Interesse? Einfach bei Frau Lehmann melden.',
    'Die Klasse 6/7 war heute im Englischen Garten! Die Kinder haben botanische Skizzen angefertigt, die Isar beobachtet und im Surfertreff geschaut. Ein toller Tag!',
    'Das Programm der Weihnachtsfeier steht: 10:00 Uhr Schulhof, Lieder vom Chor, Aufführung der AG Theater, Wichteln im Klassenverbund. Eltern sind herzlich willkommen!',
    'Für den Tag der offenen Tür am 27. Juni suchen wir noch 15 freiwillige Helfer für Führungen und Infostände. Wer kann? Bitte bis 20. Juni melden. Vielen Dank!',
    'Das Sportfest 2026 findet am 12. Juni statt. Alle Klassen nehmen teil. Eltern können als Streckenposten helfen. Anmeldung bis 1. Juni bei Frau Weber.'];
  -- post_rooms[i]: 0=SCHOOL, 1-9=Klassen, 10-17=andere Räume
  -- scope_type: SCHOOL, ROOM
  post_room_idx INT[] := ARRAY[0,0,0,0,0,  1,4,5, 10,8, 0,7,6, 0,0,0];
  -- Lehrkraft-Index die den Post erstellt
  post_teacher INT[] := ARRAY[12,2,4,1,9, 1,4,5, 6,8, 4,9,7, 12,2,5];

  -- ── Jobs (Elternstunden) ─────────────────────────────────
  job_titles TEXT[] := ARRAY[
    'Herbstfest Aufbau und Dekoration',
    'Schulküche: Elterncafé vorbereiten',
    'Schulgarten: Herbstbepflanzung',
    'Nikolausfeier Dekoration aufbauen',
    'Schulbibliothek einräumen und sortieren',
    'Klassenraum Krippe renovieren (streichen)',
    'Spielplatz-Geräte warten und sichern',
    'Weihnachtsbasar Standaufbau',
    'Schulhof Frühjahrsputz',
    'Bühne für AG-Konzert aufbauen',
    'Sandkasten befüllen (Kinderhaus)',
    'Sommerfest Hüpfburg und Stände',
    'Schulweg-Begleitung organisieren',
    'Erste-Hilfe-Schränke auffüllen',
    'Laternenfest Laternen aufhängen',
    'Fenster putzen Erdgeschoss',
    'Elternsprechtag Bewirtung',
    'Fahrradständer montieren',
    'Hochbeete im Schulgarten bauen',
    'Tag der offenen Tür: Führungen'];
  job_cats TEXT[] := ARRAY[
    'VERANSTALTUNG','KUECHE','GARTEN','VERANSTALTUNG','BUERO',
    'RENOVIERUNG','WERKSTATT','VERANSTALTUNG','REINIGUNG','WERKSTATT',
    'GARTEN','VERANSTALTUNG','TRANSPORT','BUERO','VERANSTALTUNG',
    'REINIGUNG','KUECHE','WERKSTATT','GARTEN','TRANSPORT'];
  job_hours DECIMAL[] := ARRAY[
    3.0, 2.0, 2.5, 2.0, 2.5,
    4.0, 2.0, 3.0, 3.0, 3.0,
    1.5, 4.0, 1.0, 1.0, 2.0,
    2.0, 2.0, 2.5, 3.5, 2.0];
  job_max INT[] := ARRAY[
    5,3,4,4,3,
    3,2,5,6,3,
    2,6,2,1,4,
    3,3,2,3,5];
  job_dates DATE[] := ARRAY[
    '2025-10-15'::DATE, '2025-10-10'::DATE, '2025-10-08'::DATE,
    '2025-12-03'::DATE, '2025-11-15'::DATE,
    '2025-09-20'::DATE, '2025-10-04'::DATE, '2025-12-13'::DATE,
    '2026-03-28'::DATE, '2026-07-01'::DATE,
    '2025-09-25'::DATE, '2026-07-18'::DATE, '2025-09-17'::DATE,
    '2025-10-01'::DATE, '2025-11-10'::DATE,
    '2025-10-28'::DATE, '2025-11-06'::DATE, '2025-10-14'::DATE,
    '2026-04-18'::DATE, '2026-06-25'::DATE];

  -- ── Lokale Variablen ─────────────────────────────────────
  i         INT;
  em        TEXT;
  tmp_id    UUID;
  post_id   UUID;
  sec_id    UUID;
  scope_id  UUID;
  _job_id   UUID;
  room_for_post UUID;
  leader_id UUID;

BEGIN

  -- ═══════════════════════════════════════════════════════
  -- 1. BUNDESLAND auf Bayern sicherstellen
  -- ═══════════════════════════════════════════════════════
  UPDATE tenant_config SET bundesland = 'BY', school_name = 'Montessori Schule München'
  WHERE bundesland IS DISTINCT FROM 'BY' OR school_name IS DISTINCT FROM 'Montessori Schule München';
  RAISE NOTICE 'Tenant: Bayern ✓';

  -- ═══════════════════════════════════════════════════════
  -- 2. SCHULBEREICHE
  -- ═══════════════════════════════════════════════════════
  INSERT INTO school_sections (id, name, slug, description, sort_order, is_active)
  VALUES
    (gen_random_uuid(), 'Kinderhaus (Krippe & Kindergarten)', 'kinderhaus',
     'Liebevolle Betreuung und Bildung für Kinder von 1 bis 6 Jahren', 1, true),
    (gen_random_uuid(), 'Grundstufe (Klasse 1-4)', 'grundstufe',
     'Kosmische Erziehung in altersgemischten Gruppen', 2, true),
    (gen_random_uuid(), 'Mittelstufe (Klasse 5-7)', 'mittelstufe',
     'Erdkinderplan - Selbstständigkeit und Projektarbeit', 3, true),
    (gen_random_uuid(), 'Oberstufe (Klasse 8-10)', 'oberstufe',
     'Großes Projekt, Berufs­orientierung und Abschluss', 4, true)
  ON CONFLICT (slug) DO NOTHING;

  sec_kinderhaus  := (SELECT id FROM school_sections WHERE slug = 'kinderhaus');
  sec_grundstufe  := (SELECT id FROM school_sections WHERE slug = 'grundstufe');
  sec_mittelstufe := (SELECT id FROM school_sections WHERE slug = 'mittelstufe');
  sec_oberstufe   := (SELECT id FROM school_sections WHERE slug = 'oberstufe');
  RAISE NOTICE 'Schulbereiche: 4 ✓';

  -- ═══════════════════════════════════════════════════════
  -- 3. LEHRKRÄFTE (12)
  -- ═══════════════════════════════════════════════════════
  FOR i IN 1..12 LOOP
    em := _mw_email(t_first[i]) || '.' || _mw_email(t_last[i]) || '@monteweb.local';
    INSERT INTO users (id, email, password_hash, first_name, last_name, display_name,
                       phone, role, is_active, email_verified)
    VALUES (gen_random_uuid(), em, pw,
            t_first[i], t_last[i], t_first[i] || ' ' || t_last[i],
            '+49 89 ' || lpad((2000000 + i * 13337)::text, 7, '0'),
            'TEACHER', true, true)
    ON CONFLICT (email) DO NOTHING;
    t_ids[i] := (SELECT id FROM users WHERE email = em);
  END LOOP;
  RAISE NOTICE 'Lehrkräfte: 12 ✓';

  -- ═══════════════════════════════════════════════════════
  -- 4. FAMILIEN + ELTERN + SCHÜLER (je 50)
  -- ═══════════════════════════════════════════════════════
  FOR i IN 1..50 LOOP
    -- Familie
    INSERT INTO families (id, name)
    VALUES (gen_random_uuid(), 'Familie ' || s_last[i])
    RETURNING id INTO tmp_id;
    f_ids[i] := tmp_id;

    -- Elternteil
    em := _mw_email(p_first[i]) || '.' || _mw_email(s_last[i]) || '@monteweb.local';
    INSERT INTO users (id, email, password_hash, first_name, last_name, display_name,
                       phone, role, is_active, email_verified)
    VALUES (gen_random_uuid(), em, pw,
            p_first[i], s_last[i], p_first[i] || ' ' || s_last[i],
            '+49 171 ' || lpad((1000000 + i * 7919)::text, 7, '0'),
            'PARENT', true, true)
    ON CONFLICT (email) DO NOTHING;
    p_ids[i] := (SELECT id FROM users WHERE email = em);
    INSERT INTO family_members (family_id, user_id, role)
    VALUES (f_ids[i], p_ids[i], 'PARENT') ON CONFLICT DO NOTHING;

    -- Schüler
    em := _mw_email(s_first[i]) || '.' || _mw_email(s_last[i]) || '@monteweb.local';
    INSERT INTO users (id, email, password_hash, first_name, last_name, display_name,
                       role, is_active, email_verified)
    VALUES (gen_random_uuid(), em, pw,
            s_first[i], s_last[i], s_first[i] || ' ' || s_last[i],
            'STUDENT', true, true)
    ON CONFLICT (email) DO NOTHING;
    s_ids[i] := (SELECT id FROM users WHERE email = em);
    INSERT INTO family_members (family_id, user_id, role)
    VALUES (f_ids[i], s_ids[i], 'CHILD') ON CONFLICT DO NOTHING;
  END LOOP;
  RAISE NOTICE 'Familien/Eltern/Schüler: 50 je ✓';

  -- ═══════════════════════════════════════════════════════
  -- 5. KLASSEN (9) + MITGLIEDER
  -- ═══════════════════════════════════════════════════════
  FOR i IN 1..9 LOOP
    CASE class_sec[i]
      WHEN 1 THEN sec_id := sec_kinderhaus;
      WHEN 2 THEN sec_id := sec_grundstufe;
      WHEN 3 THEN sec_id := sec_mittelstufe;
      ELSE        sec_id := sec_oberstufe;
    END CASE;

    leader_id := t_ids[class_lead[i]];

    INSERT INTO rooms (id, name, description, type, section_id, settings,
                       is_archived, created_by, join_policy)
    VALUES (gen_random_uuid(),
            class_names[i], class_descs[i], 'KLASSE', sec_id,
            '{"chatEnabled":true,"filesEnabled":true,"parentSpaceEnabled":true,"visibility":"MEMBERS_ONLY"}',
            false, leader_id, 'REQUEST')
    RETURNING id INTO tmp_id;
    class_ids[i] := tmp_id;

    -- Lehrkraft als LEADER
    INSERT INTO room_members (room_id, user_id, role)
    VALUES (class_ids[i], leader_id, 'LEADER') ON CONFLICT DO NOTHING;
    -- Zweite Lehrkraft (nächste im Array, wrap-around)
    INSERT INTO room_members (room_id, user_id, role)
    VALUES (class_ids[i], t_ids[(class_lead[i] % 12) + 1], 'LEADER') ON CONFLICT DO NOTHING;

    -- Schüler als MEMBER, Eltern als PARENT_MEMBER
    FOR j IN class_s_start[i]..class_s_end[i] LOOP
      IF s_ids[j] IS NOT NULL THEN
        INSERT INTO room_members (room_id, user_id, role)
        VALUES (class_ids[i], s_ids[j], 'MEMBER') ON CONFLICT DO NOTHING;
      END IF;
      IF p_ids[j] IS NOT NULL THEN
        INSERT INTO room_members (room_id, user_id, role)
        VALUES (class_ids[i], p_ids[j], 'PARENT_MEMBER') ON CONFLICT DO NOTHING;
      END IF;
    END LOOP;
  END LOOP;
  RAISE NOTICE 'Klassenräume: 9 ✓';

  -- ═══════════════════════════════════════════════════════
  -- 6. ANDERE RÄUME (8)
  -- ═══════════════════════════════════════════════════════
  -- Lehrerzimmer (alle Lehrkräfte)
  INSERT INTO rooms (id, name, description, type, section_id, settings,
                     is_archived, created_by, join_policy)
  VALUES (gen_random_uuid(), other_names[1], other_descs[1], 'GRUPPE', NULL,
          '{"chatEnabled":true,"filesEnabled":true,"parentSpaceEnabled":false,"visibility":"MEMBERS_ONLY"}',
          false, t_ids[12], 'REQUEST')
  RETURNING id INTO tmp_id;
  other_ids[1] := tmp_id;
  FOR i IN 1..12 LOOP
    INSERT INTO room_members (room_id, user_id, role)
    VALUES (other_ids[1], t_ids[i], CASE WHEN i = 12 THEN 'LEADER' ELSE 'MEMBER' END)
    ON CONFLICT DO NOTHING;
  END LOOP;

  -- Elternbeirat (je 1 Elternteil pro Klasse als Mitglied)
  INSERT INTO rooms (id, name, description, type, section_id, settings,
                     is_archived, created_by, join_policy)
  VALUES (gen_random_uuid(), other_names[2], other_descs[2], 'GRUPPE', NULL,
          '{"chatEnabled":true,"filesEnabled":true,"parentSpaceEnabled":false,"visibility":"MEMBERS_ONLY"}',
          false, t_ids[12], 'REQUEST')
  RETURNING id INTO tmp_id;
  other_ids[2] := tmp_id;
  INSERT INTO room_members (room_id, user_id, role)
  VALUES (other_ids[2], t_ids[12], 'LEADER') ON CONFLICT DO NOTHING;
  FOR i IN 1..9 LOOP
    IF p_ids[class_s_start[i]] IS NOT NULL THEN
      INSERT INTO room_members (room_id, user_id, role)
      VALUES (other_ids[2], p_ids[class_s_start[i]], 'MEMBER') ON CONFLICT DO NOTHING;
    END IF;
  END LOOP;

  -- Schülervertretung (ältere Schüler aus Klassen 6-9)
  INSERT INTO rooms (id, name, description, type, section_id, settings,
                     is_archived, created_by, join_policy)
  VALUES (gen_random_uuid(), other_names[3], other_descs[3], 'GRUPPE', NULL,
          '{"chatEnabled":true,"filesEnabled":true,"parentSpaceEnabled":false,"visibility":"MEMBERS_ONLY"}',
          false, t_ids[9], 'OPEN')
  RETURNING id INTO tmp_id;
  other_ids[3] := tmp_id;
  INSERT INTO room_members (room_id, user_id, role)
  VALUES (other_ids[3], t_ids[9], 'LEADER') ON CONFLICT DO NOTHING;
  FOR i IN 34..50 LOOP
    IF s_ids[i] IS NOT NULL THEN
      INSERT INTO room_members (room_id, user_id, role)
      VALUES (other_ids[3], s_ids[i], 'MEMBER') ON CONFLICT DO NOTHING;
    END IF;
  END LOOP;

  -- AG Musik (gemischt, alle Jahrgänge)
  INSERT INTO rooms (id, name, description, type, section_id, settings,
                     is_archived, created_by, join_policy)
  VALUES (gen_random_uuid(), other_names[4], other_descs[4], 'GRUPPE', NULL,
          '{"chatEnabled":true,"filesEnabled":true,"parentSpaceEnabled":false,"visibility":"OPEN"}',
          false, t_ids[11], 'OPEN')
  RETURNING id INTO tmp_id;
  other_ids[4] := tmp_id;
  INSERT INTO room_members (room_id, user_id, role)
  VALUES (other_ids[4], t_ids[11], 'LEADER') ON CONFLICT DO NOTHING;
  FOREACH i IN ARRAY ARRAY[12,17,20,23,29,34,37,40,43,46,48] LOOP
    IF s_ids[i] IS NOT NULL THEN
      INSERT INTO room_members (room_id, user_id, role)
      VALUES (other_ids[4], s_ids[i], 'MEMBER') ON CONFLICT DO NOTHING;
    END IF;
  END LOOP;

  -- AG Sport
  INSERT INTO rooms (id, name, description, type, section_id, settings,
                     is_archived, created_by, join_policy)
  VALUES (gen_random_uuid(), other_names[5], other_descs[5], 'GRUPPE', NULL,
          '{"chatEnabled":true,"filesEnabled":false,"parentSpaceEnabled":false,"visibility":"OPEN"}',
          false, t_ids[6], 'OPEN')
  RETURNING id INTO tmp_id;
  other_ids[5] := tmp_id;
  INSERT INTO room_members (room_id, user_id, role)
  VALUES (other_ids[5], t_ids[6], 'LEADER') ON CONFLICT DO NOTHING;
  FOREACH i IN ARRAY ARRAY[17,19,22,25,28,30,33,36,39,42,45,49] LOOP
    IF s_ids[i] IS NOT NULL THEN
      INSERT INTO room_members (room_id, user_id, role)
      VALUES (other_ids[5], s_ids[i], 'MEMBER') ON CONFLICT DO NOTHING;
    END IF;
  END LOOP;

  -- Schulküche
  INSERT INTO rooms (id, name, description, type, section_id, settings,
                     is_archived, created_by, join_policy)
  VALUES (gen_random_uuid(), other_names[6], other_descs[6], 'GRUPPE', NULL,
          '{"chatEnabled":true,"filesEnabled":true,"parentSpaceEnabled":true,"visibility":"OPEN"}',
          false, t_ids[3], 'OPEN')
  RETURNING id INTO tmp_id;
  other_ids[6] := tmp_id;
  INSERT INTO room_members (room_id, user_id, role)
  VALUES (other_ids[6], t_ids[3], 'LEADER') ON CONFLICT DO NOTHING;

  -- Schulbibliothek
  INSERT INTO rooms (id, name, description, type, section_id, settings,
                     is_archived, created_by, join_policy)
  VALUES (gen_random_uuid(), other_names[7], other_descs[7], 'GRUPPE', NULL,
          '{"chatEnabled":true,"filesEnabled":true,"parentSpaceEnabled":true,"visibility":"OPEN"}',
          false, t_ids[10], 'OPEN')
  RETURNING id INTO tmp_id;
  other_ids[7] := tmp_id;
  INSERT INTO room_members (room_id, user_id, role)
  VALUES (other_ids[7], t_ids[10], 'LEADER') ON CONFLICT DO NOTHING;
  -- Alle Lehrkräfte als Mitglieder
  FOR i IN 1..12 LOOP
    INSERT INTO room_members (room_id, user_id, role)
    VALUES (other_ids[7], t_ids[i], 'MEMBER') ON CONFLICT DO NOTHING;
  END LOOP;
  -- Alle Eltern als Mitglieder
  FOR i IN 1..50 LOOP
    IF p_ids[i] IS NOT NULL THEN
      INSERT INTO room_members (room_id, user_id, role)
      VALUES (other_ids[7], p_ids[i], 'MEMBER') ON CONFLICT DO NOTHING;
    END IF;
  END LOOP;

  -- Natur & Garten AG
  INSERT INTO rooms (id, name, description, type, section_id, settings,
                     is_archived, created_by, join_policy)
  VALUES (gen_random_uuid(), other_names[8], other_descs[8], 'GRUPPE', NULL,
          '{"chatEnabled":true,"filesEnabled":true,"parentSpaceEnabled":true,"visibility":"OPEN"}',
          false, t_ids[5], 'OPEN')
  RETURNING id INTO tmp_id;
  other_ids[8] := tmp_id;
  INSERT INTO room_members (room_id, user_id, role)
  VALUES (other_ids[8], t_ids[5], 'LEADER') ON CONFLICT DO NOTHING;
  FOREACH i IN ARRAY ARRAY[1,3,5,8,14,17,21,29,35,41] LOOP
    IF p_ids[i] IS NOT NULL THEN
      INSERT INTO room_members (room_id, user_id, role)
      VALUES (other_ids[8], p_ids[i], 'MEMBER') ON CONFLICT DO NOTHING;
    END IF;
  END LOOP;
  FOREACH i IN ARRAY ARRAY[4,6,11,16,20,24,30,38,44,47] LOOP
    IF s_ids[i] IS NOT NULL THEN
      INSERT INTO room_members (room_id, user_id, role)
      VALUES (other_ids[8], s_ids[i], 'MEMBER') ON CONFLICT DO NOTHING;
    END IF;
  END LOOP;

  RAISE NOTICE 'Andere Räume: 8 ✓';

  -- ═══════════════════════════════════════════════════════
  -- 7. KALENDER-EVENTS (20, Schuljahr Bayern 2025/2026)
  -- ═══════════════════════════════════════════════════════
  FOR i IN 1..20 LOOP
    -- scope_id bestimmen
    scope_id := CASE ev_scope_ref[i]
      WHEN 0 THEN NULL
      WHEN 1 THEN sec_kinderhaus
      WHEN 2 THEN sec_grundstufe
      WHEN 3 THEN sec_mittelstufe
      WHEN 4 THEN sec_oberstufe
      WHEN 5 THEN class_ids[8]   -- Klasse 8/9 Forscherlabor
      WHEN 6 THEN other_ids[4]   -- AG Musik
      ELSE NULL
    END;

    INSERT INTO calendar_events (
      id, title, description,
      all_day, start_date, start_time, end_date, end_time,
      scope, scope_id,
      recurrence, cancelled, created_by
    ) VALUES (
      gen_random_uuid(),
      ev_titles[i], ev_descs[i],
      ev_all_day[i],
      ev_start_dates[i], ev_start_times[i],
      ev_end_dates[i],   ev_end_times[i],
      ev_scopes[i], scope_id,
      'NONE', false,
      t_ids[CASE WHEN i <= 5 THEN i WHEN i <= 10 THEN i - 4 ELSE 12 END]
    );
  END LOOP;
  RAISE NOTICE 'Kalender-Events: 20 ✓';

  -- ═══════════════════════════════════════════════════════
  -- 8. FEED-POSTS (16)
  -- ═══════════════════════════════════════════════════════
  FOR i IN 1..16 LOOP
    -- Welcher Raum für diesen Post?
    room_for_post := CASE post_room_idx[i]
      WHEN 0  THEN NULL          -- SCHOOL scope
      WHEN 1  THEN class_ids[1]  -- Krippe
      WHEN 4  THEN class_ids[4]  -- Kl 1/2
      WHEN 5  THEN class_ids[5]  -- Kl 3/4
      WHEN 6  THEN class_ids[6]  -- Kl 5/6
      WHEN 7  THEN other_ids[7]  -- Schulbibliothek
      WHEN 8  THEN class_ids[8]  -- Kl 8/9
      WHEN 10 THEN other_ids[5]  -- AG Sport
      ELSE NULL
    END;

    INSERT INTO feed_posts (
      id, author_id, title, content,
      source_type, source_id,
      is_pinned, is_parent_only, published_at
    ) VALUES (
      gen_random_uuid(),
      t_ids[post_teacher[i]],
      post_titles[i], post_contents[i],
      CASE WHEN room_for_post IS NULL THEN 'SCHOOL' ELSE 'ROOM' END,
      room_for_post,
      i <= 2,   -- erste 2 Posts gepinnt
      false,
      NOW() - ((16 - i) || ' days')::interval
    )
    RETURNING id INTO post_id;

    -- Kommentare auf ungerade Posts
    IF i % 2 = 1 THEN
      FOR j IN 1..3 LOOP
        INSERT INTO feed_post_comments (id, post_id, author_id, content, created_at)
        VALUES (
          gen_random_uuid(), post_id,
          p_ids[((i * 3 + j * 7) % 50) + 1],
          CASE j % 4
            WHEN 0 THEN 'Danke für die Info! Wir sind dabei.'
            WHEN 1 THEN 'Super, mein Kind freut sich schon riesig!'
            WHEN 2 THEN 'Vielen Dank für die tolle Organisation!'
            ELSE        'Wir kommen auf jeden Fall! Bis dann.'
          END,
          NOW() - ((16 - i) || ' days')::interval + (j || ' hours')::interval
        );
      END LOOP;
    END IF;
  END LOOP;
  RAISE NOTICE 'Feed-Posts: 16 ✓';

  -- ═══════════════════════════════════════════════════════
  -- 9. ELTERNSTUNDEN-JOBS (20)
  -- ═══════════════════════════════════════════════════════
  FOR i IN 1..20 LOOP
    INSERT INTO jobs (
      id, title, description, category,
      estimated_hours, max_assignees,
      status, scheduled_date, scheduled_time,
      created_by, section_id, contact_info
    ) VALUES (
      gen_random_uuid(),
      job_titles[i],
      'Wir suchen engagierte Eltern für: ' || job_titles[i] ||
      '. Geschätzte Dauer: ca. ' || job_hours[i] || ' Stunden. ' ||
      'Bitte direkt anmelden!',
      job_cats[i],
      job_hours[i], job_max[i],
      CASE
        WHEN i <= 8  THEN 'OPEN'
        WHEN i <= 14 THEN 'ASSIGNED'
        ELSE              'COMPLETED'
      END,
      job_dates[i],
      CASE WHEN i % 3 = 0 THEN '09:00' WHEN i % 3 = 1 THEN '14:00' ELSE '10:30' END,
      t_ids[((i - 1) % 12) + 1],
      CASE WHEN i % 4 = 0 THEN sec_kinderhaus
           WHEN i % 4 = 1 THEN sec_grundstufe
           WHEN i % 4 = 2 THEN sec_mittelstufe
           ELSE                 sec_oberstufe END,
      t_first[((i - 1) % 12) + 1] || ' ' || t_last[((i - 1) % 12) + 1]
    )
    RETURNING id INTO _job_id;

    -- Für ASSIGNED und COMPLETED: Zuweisung erstellen
    IF i > 8 THEN
      INSERT INTO job_assignments (
        id, job_id, user_id, family_id,
        status, actual_hours, confirmed, confirmed_by, confirmed_at,
        notes, assigned_at, started_at, completed_at
      ) VALUES (
        gen_random_uuid(), _job_id,
        p_ids[((i * 5) % 50) + 1],
        f_ids[((i * 5) % 50) + 1],
        CASE WHEN i <= 14 THEN 'ASSIGNED' ELSE 'COMPLETED' END,
        CASE WHEN i > 14 THEN job_hours[i] ELSE NULL END,
        i > 14,
        CASE WHEN i > 14 THEN t_ids[((i-1) % 12) + 1] ELSE NULL END,
        CASE WHEN i > 14 THEN NOW() ELSE NULL END,
        CASE WHEN i > 14 THEN 'Gut gelaufen, herzlichen Dank!' ELSE NULL END,
        job_dates[i]::timestamp - interval '7 days',
        CASE WHEN i > 14 THEN job_dates[i]::timestamp ELSE NULL END,
        CASE WHEN i > 14 THEN job_dates[i]::timestamp + interval '3 hours' ELSE NULL END
      ) ON CONFLICT (job_id, user_id) DO NOTHING;
    END IF;
  END LOOP;
  RAISE NOTICE 'Elternstunden-Jobs: 20 ✓';

  RAISE NOTICE '════════════════════════════════════════════════════════';
  RAISE NOTICE 'SEED ABGESCHLOSSEN:';
  RAISE NOTICE '  • 4 Schulbereiche';
  RAISE NOTICE '  • 12 Lehrkräfte';
  RAISE NOTICE '  • 50 Schüler + 50 Eltern + 50 Familien';
  RAISE NOTICE '  • 9 Klassen + 8 andere Räume = 17 Räume';
  RAISE NOTICE '  • 20 Kalender-Events (Bayern SJ 2025/2026)';
  RAISE NOTICE '  • 16 Feed-Posts + Kommentare';
  RAISE NOTICE '  • 20 Elternstunden-Jobs';
  RAISE NOTICE '════════════════════════════════════════════════════════';

END $$;

DROP FUNCTION IF EXISTS _mw_email(TEXT);
