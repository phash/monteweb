-- V100: Extended seed data — replace V040 data with larger dataset
-- 4 sections, 30 teachers, 300 parents, 200 students, 150 families, 20 rooms, 50 events, 30 jobs
-- Password for all users: test1234
-- IMPORTANT: Remove or disable in production!

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 1: Delete existing V040 seed data (reverse dependency order)
-- ═══════════════════════════════════════════════════════════════════════════════

-- Protected accounts that must NOT be deleted
-- admin@monteweb.local, lehrer@monteweb.local, eltern@monteweb.local,
-- schueler@monteweb.local, sectionadmin@monteweb.local

DO $$
DECLARE
    protected_emails TEXT[] := ARRAY[
        'admin@monteweb.local',
        'lehrer@monteweb.local',
        'eltern@monteweb.local',
        'schueler@monteweb.local',
        'sectionadmin@monteweb.local'
    ];
BEGIN
    -- 1. Delete feed post comments by seed users
    DELETE FROM feed_post_comments
    WHERE author_id IN (
        SELECT id FROM users
        WHERE email LIKE '%.%@monteweb.local'
          AND email != ALL(protected_emails)
    );

    -- 2. Delete feed posts by seed users
    DELETE FROM feed_posts
    WHERE author_id IN (
        SELECT id FROM users
        WHERE email LIKE '%.%@monteweb.local'
          AND email != ALL(protected_emails)
    );

    -- 3. Delete job assignments for seed users
    DELETE FROM job_assignments
    WHERE user_id IN (
        SELECT id FROM users
        WHERE email LIKE '%.%@monteweb.local'
          AND email != ALL(protected_emails)
    );

    -- 4. Delete jobs created by seed users
    DELETE FROM jobs
    WHERE created_by IN (
        SELECT id FROM users
        WHERE email LIKE '%.%@monteweb.local'
          AND email != ALL(protected_emails)
    );

    -- 5. Delete calendar event RSVPs for seed users
    DELETE FROM calendar_event_rsvps
    WHERE user_id IN (
        SELECT id FROM users
        WHERE email LIKE '%.%@monteweb.local'
          AND email != ALL(protected_emails)
    );

    -- 6. Delete calendar events created by seed users
    DELETE FROM calendar_events
    WHERE created_by IN (
        SELECT id FROM users
        WHERE email LIKE '%.%@monteweb.local'
          AND email != ALL(protected_emails)
    );

    -- 7. Delete cleaning registrations for seed users
    DELETE FROM cleaning_registrations
    WHERE user_id IN (
        SELECT id FROM users
        WHERE email LIKE '%.%@monteweb.local'
          AND email != ALL(protected_emails)
    );

    -- 7b. Delete password reset tokens for seed users (no CASCADE on FK)
    DELETE FROM password_reset_tokens
    WHERE user_id IN (
        SELECT id FROM users
        WHERE email LIKE '%.%@monteweb.local'
          AND email != ALL(protected_emails)
    );

    -- 8. Delete room members, subscriptions, and join requests for seed users
    DELETE FROM room_members
    WHERE user_id IN (
        SELECT id FROM users
        WHERE email LIKE '%.%@monteweb.local'
          AND email != ALL(protected_emails)
    );
    DELETE FROM room_subscriptions
    WHERE user_id IN (
        SELECT id FROM users
        WHERE email LIKE '%.%@monteweb.local'
          AND email != ALL(protected_emails)
    );
    DELETE FROM room_join_requests
    WHERE user_id IN (
        SELECT id FROM users
        WHERE email LIKE '%.%@monteweb.local'
          AND email != ALL(protected_emails)
    );

    -- 9. Delete family members and invitations for seed users
    DELETE FROM family_invitations
    WHERE inviter_id IN (
        SELECT id FROM users
        WHERE email LIKE '%.%@monteweb.local'
          AND email != ALL(protected_emails)
    ) OR invitee_id IN (
        SELECT id FROM users
        WHERE email LIKE '%.%@monteweb.local'
          AND email != ALL(protected_emails)
    );
    DELETE FROM family_members
    WHERE user_id IN (
        SELECT id FROM users
        WHERE email LIKE '%.%@monteweb.local'
          AND email != ALL(protected_emails)
    );

    -- 10. Delete orphan families (no remaining members)
    DELETE FROM families
    WHERE id NOT IN (SELECT DISTINCT family_id FROM family_members);

    -- 11. Delete V040 rooms and related data (by known names)
    -- Room discussion replies/threads have non-cascading FK to users but CASCADE from rooms
    -- Room folders/files have non-cascading FK to users but CASCADE from rooms
    DELETE FROM rooms
    WHERE name IN (
        'Sonnengruppe', 'Sternengruppe',
        'Erdkinder 1-3', 'Kosmische Klasse 3-4',
        'Lernwerkstatt 5-6', 'Forscherklasse 7-8',
        'Projektklasse 9-10', 'Abiturjahrgang 11-12'
    );

    -- 12. Delete remaining references with non-cascading FKs to seed users
    DELETE FROM room_discussion_replies
    WHERE author_id IN (
        SELECT id FROM users
        WHERE email LIKE '%.%@monteweb.local'
          AND email != ALL(protected_emails)
    );
    DELETE FROM room_discussion_threads
    WHERE created_by IN (
        SELECT id FROM users
        WHERE email LIKE '%.%@monteweb.local'
          AND email != ALL(protected_emails)
    );
    DELETE FROM room_files
    WHERE uploaded_by IN (
        SELECT id FROM users
        WHERE email LIKE '%.%@monteweb.local'
          AND email != ALL(protected_emails)
    );
    DELETE FROM room_folders
    WHERE created_by IN (
        SELECT id FROM users
        WHERE email LIKE '%.%@monteweb.local'
          AND email != ALL(protected_emails)
    );

    -- 12b. Delete the seed users themselves
    DELETE FROM users
    WHERE email LIKE '%.%@monteweb.local'
      AND email != ALL(protected_emails);

    -- 13. Delete cleaning slots/configs referencing V040 sections (FK no CASCADE)
    DELETE FROM cleaning_registrations
    WHERE slot_id IN (
        SELECT cs.id FROM cleaning_slots cs
        JOIN school_sections ss ON cs.section_id = ss.id
        WHERE ss.slug IN ('kinderhaus', 'grundstufe', 'mittelstufe', 'oberstufe')
    );
    DELETE FROM cleaning_slots
    WHERE section_id IN (SELECT id FROM school_sections WHERE slug IN ('kinderhaus', 'grundstufe', 'mittelstufe', 'oberstufe'));
    DELETE FROM cleaning_configs
    WHERE section_id IN (SELECT id FROM school_sections WHERE slug IN ('kinderhaus', 'grundstufe', 'mittelstufe', 'oberstufe'));

    -- 14. Delete jobs referencing V040 sections (FK no CASCADE)
    DELETE FROM job_assignments WHERE job_id IN (
        SELECT j.id FROM jobs j
        JOIN school_sections ss ON j.section_id = ss.id
        WHERE ss.slug IN ('kinderhaus', 'grundstufe', 'mittelstufe', 'oberstufe')
    );
    DELETE FROM jobs
    WHERE section_id IN (SELECT id FROM school_sections WHERE slug IN ('kinderhaus', 'grundstufe', 'mittelstufe', 'oberstufe'));

    -- 15. Detach any remaining rooms from V040 sections (nullable FK)
    UPDATE rooms SET section_id = NULL
    WHERE section_id IN (SELECT id FROM school_sections WHERE slug IN ('kinderhaus', 'grundstufe', 'mittelstufe', 'oberstufe'));

    -- 16. Delete V040 school sections (by slug)
    DELETE FROM school_sections
    WHERE slug IN ('kinderhaus', 'grundstufe', 'mittelstufe', 'oberstufe');

    RAISE NOTICE 'V040 seed data cleaned up (protected accounts preserved)';
END $$;

-- ═══════════════════════════════════════════════════════════════════════════════
-- PART 2: Create extended seed data
-- ═══════════════════════════════════════════════════════════════════════════════

-- Recreate email-safe helper (was dropped at end of V040)
CREATE OR REPLACE FUNCTION _email_safe(t TEXT) RETURNS TEXT AS $fn$
BEGIN
    RETURN lower(replace(replace(replace(replace(t, 'ä', 'ae'), 'ö', 'oe'), 'ü', 'ue'), 'ß', 'ss'));
END;
$fn$ LANGUAGE plpgsql;

DO $$
DECLARE
    pw CONSTANT TEXT := '$2a$10$dQa6248nzH.S5yYnSpGfr.b7BnuGroDZ/G18yp.vzoAx45FQ7hecO';

    -- ── Section IDs ──
    sec_ids UUID[4];

    -- ── Teachers (30) ──
    t_first TEXT[] := ARRAY[
        'Katharina','Markus','Sabine','Florian','Petra',
        'Andreas','Monika','Stefan','Claudia','Thomas',
        'Renate','Wolfgang','Dagmar','Rainer','Heike',
        'Matthias','Brigitte','Jörg','Susanne','Ulrich',
        'Birgit','Christian','Franziska','Georg','Irmgard',
        'Karl','Lena','Norbert','Silvia','Volker'];
    t_last  TEXT[] := ARRAY[
        'Bergmann','Vogt','Seidler','Heinze','Krause',
        'Richter','Lindner','Hamann','Fiedler','Böttcher',
        'Kunze','Hesse','Brandt','Wendt','Schreiber',
        'Engel','Schiller','Fuchs','Pfeiffer','Busch',
        'Steinbach','Kroll','Mertens','Altmann','Riedel',
        'Baumgart','Dietrich','Hartung','Sander','Thiele'];
    t_ids UUID[30];

    -- ── Parents (300) — first names ──
    p_first TEXT[] := ARRAY[
        'Anna','Thomas','Maria','Michael','Sabine','Stefan','Claudia','Peter','Andrea','Martin',
        'Susanne','Jürgen','Monika','Uwe','Petra','Frank','Nicole','Ralf','Birgit','Holger',
        'Katrin','Bernd','Heike','Markus','Karin','Dirk','Anja','Sven','Silke','Olaf',
        'Tanja','René','Nadine','Jens','Yvonne','Kai','Simone','Thorsten','Daniela','Lars',
        'Sandra','Christian','Melanie','Torsten','Janine','Matthias','Stefanie','Carsten','Manuela','Gerd',
        'Cornelia','Detlef','Gabriele','Ingo','Ramona','Wolfgang','Britta','Norbert','Sonja','Christoph',
        'Elke','Axel','Marion','Jochen','Iris','Volker','Bettina','Rolf','Martina','Achim',
        'Ute','Reiner','Diana','Guido','Doris','Erich','Anke','Harald','Christine','Lutz',
        'Dagmar','Klaus','Beate','Werner','Astrid','Dieter','Barbara','Heinz','Renate','Kurt',
        'Ingrid','Herbert','Gisela','Horst','Ursula','Friedrich','Hannelore','Georg','Edith','Gerhard',
        'Margit','Lothar','Helga','Manfred','Ilona','Siegfried','Waltraud','Erwin','Christa','Günter',
        'Edeltraut','Roland','Elisabeth','Otto','Hildegard','Heinrich','Elfriede','Rudolf','Rosemarie','Alfred',
        'Lieselotte','Konrad','Gertrud','Wilhelm','Margarete','Walter','Anneliese','Hans','Erna','Paul',
        'Ingeborg','Albert','Hedwig','Bruno','Johanna','Ludwig','Martha','Gustav','Hertha','Willi',
        'Käthe','August','Ruth','Ferdinand','Alma','Eberhard','Else','Adalbert','Luise','Oswald',
        'Dorothea','Clemens','Mathilde','Anton','Sophie','Viktor','Emilie','Leonard','Adele','Edmund',
        'Irene','Richard','Agnes','Hugo','Pauline','Ernst','Frieda','Theodor','Charlotte','Leopold',
        'Hilde','Engelbert','Minna','Alois','Thekla','Alfons','Josefine','Maximilian','Helene','Willibald',
        'Roswitha','Felix','Lydia','Valentin','Rosa','Benedikt','Klara','Julius','Ida','Lorenz',
        'Dorothee','Alexander','Franziska','Gregor','Veronika','Siegbert','Mechthild','Berthold','Ottilie','Norwin',
        'Bärbel','Hartmut','Antje','Winfried','Gerlinde','Reinhold','Annemarie','Ekkehard','Traute','Jürgen',
        'Kerstin','Burkhard','Marianne','Erhard','Rosalinde','Friedhelm','Gertrude','Claus','Heidemarie','Rüdiger',
        'Regine','Wilfried','Hannelore','Armin','Gudrun','Henning','Walburga','Konrad','Renilde','Detlev',
        'Margot','Egon','Elfriede','Helmut','Rotraud','Folkert','Isolde','Albrecht','Edda','Baldur',
        'Cordula','Dietmar','Evelyn','Folkhard','Gundula','Henryk','Irmtraud','Jobst','Kunigunde','Leberecht',
        'Mechthild','Neidhart','Ortrun','Pilgrim','Quirina','Rainald','Sieglinde','Trudbert','Ulfhild','Volkmar',
        'Adelheid','Bernward','Clothilde','Eginhard','Frederuna','Gerold','Heilwig','Isenbard','Kunhild','Ludolf',
        'Marlies','Otfried','Pilar','Raimund','Swanhild','Traugott','Urs','Vigdis','Wiebke','Xaver',
        'Almut','Burchard','Dörte','Eberhart','Gerlind','Hiltrude','Jutta','Karlheinz','Lisbeth','Meinolf',
        'Nele','Ottmar','Philippa','Ragnhild','Sibylle','Thilo','Ulrike','Wendelin','Yolanda','Zacharias'];
    -- ── Parents — last names (100, reused for email-safe + display) ──
    p_last TEXT[] := ARRAY[
        'Mueller','Schmidt','Fischer','Weber','Wagner','Becker','Hoffmann','Schaefer','Koch','Bauer',
        'Richter','Klein','Wolf','Neumann','Schwarz','Zimmermann','Braun','Krueger','Hartmann','Lange',
        'Werner','Meier','Lehmann','Schmid','Krause','Schulz','Maier','Koehler','Jung','Hahn',
        'Keller','Frank','Berger','Winkler','Roth','Beck','Lorenz','Baumann','Franke','Albrecht',
        'Schuster','Simon','Ludwig','Boehm','Winter','Sommer','Haas','Graf','Heinrich','Seidel',
        'Bruns','Dietrich','Ebert','Falk','Geiger','Hammer','Jansen','Kaiser','Lenz','Marx',
        'Nagel','Otto','Peters','Rauch','Stark','Thoma','Ullrich','Vogel','Walther','Ziegler',
        'Arndt','Barth','Conrad','Dorn','Eckert','Freund','Grosse','Huber','Illing','Jordan',
        'Kern','Lutz','Moll','Noll','Opitz','Pohl','Raab','Seifert','Trost','Unger',
        'Veit','Weise','Zander','Aigner','Bauer','Claus','Decker','Ernst','Fritsch','Gerber'];
    p_last_display TEXT[] := ARRAY[
        'Müller','Schmidt','Fischer','Weber','Wagner','Becker','Hoffmann','Schäfer','Koch','Bauer',
        'Richter','Klein','Wolf','Neumann','Schwarz','Zimmermann','Braun','Krüger','Hartmann','Lange',
        'Werner','Meier','Lehmann','Schmid','Krause','Schulz','Maier','Köhler','Jung','Hahn',
        'Keller','Frank','Berger','Winkler','Roth','Beck','Lorenz','Baumann','Franke','Albrecht',
        'Schuster','Simon','Ludwig','Böhm','Winter','Sommer','Haas','Graf','Heinrich','Seidel',
        'Bruns','Dietrich','Ebert','Falk','Geiger','Hammer','Jansen','Kaiser','Lenz','Marx',
        'Nagel','Otto','Peters','Rauch','Stark','Thoma','Ullrich','Vogel','Walther','Ziegler',
        'Arndt','Barth','Conrad','Dorn','Eckert','Freund','Große','Huber','Illing','Jordan',
        'Kern','Lutz','Moll','Noll','Opitz','Pohl','Raab','Seifert','Trost','Unger',
        'Veit','Weise','Zander','Aigner','Bauer','Claus','Decker','Ernst','Fritsch','Gerber'];
    p_ids UUID[300];

    -- ── Students (200) — modern German child names ──
    c_first TEXT[] := ARRAY[
        'Liam','Emma','Noah','Mia','Elias','Hannah','Finn','Sophia','Leon','Emilia',
        'Lukas','Marie','Jonas','Lena','Ben','Lea','Paul','Clara','Felix','Greta',
        'Maximilian','Lina','Henry','Ella','Theo','Johanna','Milan','Amelie','Moritz','Charlotte',
        'Oscar','Ida','Anton','Nele','Emil','Frieda','Julian','Marlene','Leo','Eva',
        'David','Helena','Jan','Mathilda','Tim','Pauline','Erik','Luise','Jakob','Mara',
        'Tom','Lotte','Nico','Victoria','Max','Paula','Sam','Thea','Linus','Rosalie',
        'Matteo','Alma','Vincent','Elif','Konrad','Anni','Robin','Juna','Phil','Zoe',
        'Carl','Lia','Ole','Romy','Levi','Vera','Bruno','Selma','Oskar','Merle',
        'Hugo','Lisa','Artur','Hanna','Fritz','Stella','Gustav','Karla','Valentin','Wanda',
        'August','Flora','Franz','Minna','Benno','Helene','Caspar','Alma','Johann','Nora',
        'Luis','Mila','Rafael','Ronja','Timo','Lotte','Yannik','Annika','Fabian','Lilli',
        'Hannes','Jette','Kilian','Matilda','Arian','Rieke','Silas','Tilda','Jasper','Carla',
        'Lennart','Marit','Cornelius','Johanne','Niklas','Finja','Lorenz','Norah','Peer','Alva',
        'Ruben','Svea','Tillmann','Ylva','Bastian','Frida','Hendrik','Meret','Quentin','Smilla',
        'Alexander','Marlena','Benedikt','Olivia','Cedric','Philippa','Dominik','Rebekka','Elijah','Saskia',
        'Florian','Tamara','Gabriel','Undine','Isaak','Vivienne','Jonathan','Wilhelmine','Konstantin','Xenia',
        'Laurenz','Yara','Marius','Zara','Nikolai','Adina','Raphael','Birte','Sebastian','Cosima',
        'Tristan','Daria','Ulrich','Eleonore','Wendelin','Gesa','Xavier','Henrike','Yannick','Irma',
        'Adrian','Jette','Bastian','Kira','Clemens','Laila','Damian','Malou','Eduard','Nadia',
        'Frederik','Ottilia','Gereon','Pia','Heinrich','Quendoline','Ivan','Rosmarie','Justus','Sinja'];
    c_ids UUID[200];

    -- ── Family IDs ──
    f_ids UUID[150];

    -- ── Room names (20, 5 per section) ──
    room_names TEXT[] := ARRAY[
        'Sonnenkinder','Mondkinder','Sternenkinder','Regenbogenkinder','Wolkenkinder',
        'Erdmännchen','Füchse','Delfine','Adler','Bären',
        'Kompass','Entdecker','Forscher','Pioniere','Horizont',
        'Galileo','Newton','Darwin','Curie','Einstein'];
    room_descs TEXT[] := ARRAY[
        'Die Sonnenkinder — altersgemischte Gruppe für 1-3 Jahre',
        'Die Mondkinder — altersgemischte Gruppe für 1-3 Jahre',
        'Die Sternenkinder — Kindergartengruppe für 3-6 Jahre',
        'Die Regenbogenkinder — Kindergartengruppe für 3-6 Jahre',
        'Die Wolkenkinder — Kindergartengruppe für 3-6 Jahre',
        'Die Erdmännchen — Grundstufe Klasse 1-2',
        'Die Füchse — Grundstufe Klasse 1-2',
        'Die Delfine — Grundstufe Klasse 2-3',
        'Die Adler — Grundstufe Klasse 3-4',
        'Die Bären — Grundstufe Klasse 3-4',
        'Kompass — Mittelstufe Klasse 5-6',
        'Entdecker — Mittelstufe Klasse 5-6',
        'Forscher — Mittelstufe Klasse 7-8',
        'Pioniere — Mittelstufe Klasse 7-8',
        'Horizont — Mittelstufe Klasse 7-8',
        'Galileo — Oberstufe Klasse 9-10',
        'Newton — Oberstufe Klasse 9-10',
        'Darwin — Oberstufe Klasse 11-12',
        'Curie — Oberstufe Klasse 11-12',
        'Einstein — Oberstufe Klasse 11-12'];
    -- Section index (1-4) for each of the 20 rooms
    room_sec INT[] := ARRAY[1,1,1,1,1, 2,2,2,2,2, 3,3,3,3,3, 4,4,4,4,4];
    -- Lead teacher index for each room (teacher 1-30)
    room_lead INT[] := ARRAY[1,3,5,7,9, 11,13,15,17,19, 21,23,25,27,29, 2,4,6,8,10];
    -- Second teacher per room
    room_second INT[] := ARRAY[2,4,6,8,10, 12,14,16,18,20, 22,24,26,28,30, 1,3,5,7,9];
    r_ids UUID[20];

    -- ── Job titles ──
    job_titles TEXT[] := ARRAY[
        'Hochbeete im Schulgarten bepflanzen',
        'Klassenzimmer streichen (Grundstufe)',
        'Herbstfest Buffet vorbereiten',
        'Spielplatz-Zaun reparieren',
        'Nikolausfeier Deko aufbauen',
        'Schulküche Grundreinigung',
        'Elternabend Catering organisieren',
        'Werkstatt: Regale bauen für Bibliothek',
        'Sommerfest Hüpfburg organisieren',
        'Fahrradständer montieren',
        'Schulbibliothek neu einräumen',
        'Adventskranz-Werkstatt vorbereiten',
        'Schulweg-Begleitung einrichten',
        'Sportplatz mähen und pflegen',
        'Projektwoche Material einkaufen',
        'Erste-Hilfe-Schränke auffüllen',
        'Garderobe Kinderhaus reparieren',
        'Laternenfest Lichter aufhängen',
        'Schulhof Frühjahrsputz',
        'Pausenspiele-Kiste zusammenstellen',
        'Elterncafé einrichten und betreuen',
        'Schulgartentor streichen',
        'Sandkasten Sand nachfüllen',
        'Weihnachtsbasar Standaufbau',
        'Theateraufführung Bühnenbild bauen',
        'Turnhalle Geräte warten',
        'Schulfest Tombola organisieren',
        'Flur-Ausstellung aufhängen',
        'Computerraum Kabel ordnen',
        'Einschulungsfeier Deko vorbereiten'];
    job_cats TEXT[] := ARRAY[
        'GARTEN','RENOVIERUNG','VERANSTALTUNG','RENOVIERUNG','VERANSTALTUNG',
        'REINIGUNG','VERANSTALTUNG','WERKSTATT','VERANSTALTUNG','WERKSTATT',
        'BUERO','WERKSTATT','TRANSPORT','GARTEN','TRANSPORT',
        'BUERO','RENOVIERUNG','VERANSTALTUNG','REINIGUNG','BUERO',
        'KUECHE','RENOVIERUNG','GARTEN','VERANSTALTUNG','WERKSTATT',
        'WERKSTATT','VERANSTALTUNG','BUERO','WERKSTATT','VERANSTALTUNG'];
    job_hours DECIMAL[] := ARRAY[
        3.0,4.0,2.5,2.0,1.5,3.0,2.0,4.0,3.0,2.0,
        2.5,2.0,1.0,2.0,1.5,1.0,2.5,1.5,3.0,1.0,
        2.0,2.0,1.5,3.0,4.0,2.0,3.0,1.0,2.0,2.0];
    job_max INT[] := ARRAY[
        4,3,5,2,4,3,5,2,6,2,
        3,4,2,2,2,1,2,4,5,2,
        3,2,2,5,3,2,6,2,2,4];
    j_ids UUID[30];

    -- ── Calendar event titles ──
    evt_titles TEXT[] := ARRAY[
        'Elternabend Kinderhaus','Schuljahreseröffnung','Herbstfest','Laternenumzug',
        'Nikolausfeier','Adventsbasteln','Weihnachtskonzert','Zeugnisausgabe 1. HJ',
        'Fasching','Schulskikurs','Frühlingsfest','Osterbrunch',
        'Projektwoche: Wasser','Tag der offenen Tür','Sportfest','Sommerfest',
        'Einschulungsfeier','Abschlussfeier Oberstufe','Schulgarten-Tag','Elterncafé',
        'Elternabend Grundstufe','Elternabend Mittelstufe','Elternabend Oberstufe',
        'Schulversammlung','Vorlesetag','Mathe-Olympiade','Kunstausstellung',
        'Theateraufführung','Schulkonzert Frühling','Bundesjugendspiele',
        'Wandertag Kinderhaus','Wandertag Grundstufe','Wandertag Mittelstufe',
        'Praktikumsmesse Oberstufe','Medienkompetenztag','Erste-Hilfe-Kurs Lehrer',
        'Pädagogischer Tag','Schulfotograf','Zahnärztliche Untersuchung',
        'Verkehrserziehung Grundstufe','Berufsinformationstag','Lesewettbewerb',
        'Schulschachturnier','Umweltprojekttag','Sponsorenlauf',
        'Elternbeiratssitzung','Fördervereinssitzung','Schülerratswahl',
        'Martinsumzug','Winterbasar'];
    evt_locs TEXT[] := ARRAY[
        'Aula','Schulhof','Schulhof','Schulhof',
        'Aula','Werkraum','Aula','Klassenzimmer',
        'Turnhalle','Skigebiet Alpen','Schulhof','Mensa',
        'Verschiedene Räume','Schulgebäude','Sportplatz','Schulhof',
        'Aula','Aula','Schulgarten','Foyer',
        'Raum 201','Raum 301','Raum 401',
        'Aula','Bibliothek','Raum 105','Flur EG',
        'Aula','Aula','Sportplatz',
        'Stadtwald','Botanischer Garten','Schloss Nymphenburg',
        'Aula','Computerraum','Raum 102',
        'Kollegium','Schulgebäude','Klassenzimmer',
        'Schulhof','Aula','Bibliothek',
        'Raum 103','Schulhof','Sportplatz',
        'Konferenzraum','Konferenzraum','Aula',
        'Schulhof','Pausenhalle'];

    -- ── Feed post titles/contents ──
    post_titles TEXT[] := ARRAY[
        'Herbstspaziergang am Freitag',
        'Erinnerung: Turnbeutel mitbringen!',
        'Tolle Projektwoche — Danke!',
        'Elterncafé nächste Woche',
        'Neue Bücher für die Leseecke',
        'Laternenumzug am 11. November',
        'Adventsbasteln — Materialien mitbringen',
        'Zeugnisausgabe am 31. Januar',
        'Schulgarten: Frühjahrspflanzung',
        'Sportfest Anmeldung',
        'Kosmische Erzählung: Das Universum',
        'Werkstatt-Tag: Was wir gebaut haben',
        'Praktikumsberichte abgeben!'];
    post_contents TEXT[] := ARRAY[
        'Liebe Eltern, am Freitag findet unser gemeinsamer Herbstspaziergang im Stadtwald statt. Treffpunkt ist um 9:30 Uhr am Schultor. Bitte wetterfeste Kleidung und einen kleinen Snack mitgeben.',
        'Liebe Eltern, bitte denken Sie daran, Ihrem Kind den Turnbeutel mitzugeben. Wir haben ab dieser Woche jeden Mittwoch Bewegungszeit in der Turnhalle.',
        'Was für eine tolle Projektwoche! Die Kinder haben zum Thema Wasser geforscht, experimentiert und gebastelt. Ein großes Dankeschön an alle helfenden Eltern.',
        'Nächsten Dienstag öffnet wieder unser Elterncafé von 8:00 bis 9:30 Uhr im Foyer. Eine schöne Gelegenheit zum Austausch bei Kaffee und Kuchen.',
        'Wir haben 25 neue Bücher für unsere Leseecke bekommen! Darunter viele spannende Sachbücher. Vielen Dank an den Förderverein!',
        'Am 11. November ziehen wir mit unseren selbstgebastelten Laternen durch die Nachbarschaft. Start um 17:00 Uhr auf dem Schulhof.',
        'Für unser Adventsbasteln am 1. Dezember benötigen wir: Tannenzweige, Kerzen, Nüsse, Zimtstangen und Bastelkleber. Bitte bis Ende November mitbringen.',
        'Die Halbjahreszeugnisse werden am 31. Januar in der 4. Stunde ausgegeben. An diesem Tag endet der Unterricht um 11:00 Uhr.',
        'Der Frühling kommt! Wir planen die Neubepflanzung unseres Schulgartens. Wer Samen oder Setzlinge spenden möchte, bitte im Sekretariat abgeben.',
        'Am 20. Juni findet unser großes Sportfest statt. Alle Kinder sind eingeladen. Eltern können als Streckenposten helfen — Anmeldung bis 10. Juni.',
        'Diese Woche haben wir mit der Kosmischen Erzählung begonnen. Die Kinder waren fasziniert! Wer möchte, kann zu Hause weiter forschen.',
        'In der Werkstatt haben die Kinder Vogelhäuschen gebaut. Jedes Kind durfte sein eigenes gestalten und mit nach Hause nehmen.',
        'Liebe Schülerinnen und Schüler, eure Praktikumsberichte müssen bis zum 28. Februar abgegeben werden. Bitte achtet auf die Formatvorgaben.'];
    post_rooms INT[] := ARRAY[1,2,6,8,6,1,3,20,11,13,7,11,19];

    comment_texts TEXT[] := ARRAY[
        'Danke für die Info! Wir sind dabei.',
        'Super, mein Kind freut sich schon riesig!',
        'Können wir auch Gummistiefel anziehen?',
        'Ich bringe gerne einen Kuchen mit.',
        'Vielen Dank für die tolle Organisation!',
        'Wir kommen auf jeden Fall. Bis Freitag!',
        'Gibt es eine Alternative für Kinder mit Allergien?',
        'Tolle Idee! Kann ich beim Aufbau helfen?'];

    -- ── Loop variables ──
    i INT;
    j INT;
    k INT;
    p_idx INT;
    c_idx INT;
    fam_last_idx INT;
    em TEXT;
    tmp_id UUID;
    post_id UUID;
    job_status TEXT;
    sched_date DATE;
    evt_scope TEXT;
    evt_scope_id UUID;
    evt_start DATE;
    evt_all_day BOOLEAN;

BEGIN
    -- ═══════════════════════════════════════════════════════════════════
    -- 1. SCHOOL SECTIONS (4)
    -- ═══════════════════════════════════════════════════════════════════
    INSERT INTO school_sections (id, name, slug, description, sort_order, is_active)
    VALUES
        (gen_random_uuid(), 'Kinderhaus (Krippe & Kindergarten)', 'kinderhaus',
         'Betreuung und Bildung für Kinder von 1 bis 6 Jahren nach Montessori-Pädagogik', 1, true),
        (gen_random_uuid(), 'Grundstufe (Klasse 1-4)', 'grundstufe',
         'Kosmische Erziehung und Grundlagen in altersgemischten Lerngruppen', 2, true),
        (gen_random_uuid(), 'Mittelstufe (Klasse 5-8)', 'mittelstufe',
         'Erdkinderplan: Selbstständigkeit und Verantwortung in Projekten', 3, true),
        (gen_random_uuid(), 'Oberstufe (Klasse 9-12)', 'oberstufe',
         'Abitur-Vorbereitung mit individuellem Lerntempo und Großem Projekt', 4, true)
    ON CONFLICT (slug) DO NOTHING;

    sec_ids[1] := (SELECT id FROM school_sections WHERE slug = 'kinderhaus');
    sec_ids[2] := (SELECT id FROM school_sections WHERE slug = 'grundstufe');
    sec_ids[3] := (SELECT id FROM school_sections WHERE slug = 'mittelstufe');
    sec_ids[4] := (SELECT id FROM school_sections WHERE slug = 'oberstufe');

    -- ═══════════════════════════════════════════════════════════════════
    -- 2. TEACHERS (30)
    -- ═══════════════════════════════════════════════════════════════════
    FOR i IN 1..30 LOOP
        em := _email_safe(t_first[i]) || '.' || _email_safe(t_last[i]) || '@monteweb.local';
        INSERT INTO users (id, email, password_hash, first_name, last_name, display_name, phone, role, is_active, email_verified)
        VALUES (gen_random_uuid(), em, pw, t_first[i], t_last[i],
                t_first[i] || ' ' || t_last[i],
                '+49 170 ' || lpad((3000000 + i * 13579)::text, 7, '0'),
                'TEACHER', true, true)
        ON CONFLICT (email) DO NOTHING;
        t_ids[i] := (SELECT id FROM users WHERE email = em);
    END LOOP;

    -- ═══════════════════════════════════════════════════════════════════
    -- 3. PARENTS (300) + STUDENTS (200) + FAMILIES (150)
    --    Each family: 2 parents + 1-2 children
    --    Families 1-100: 2 children each (200 students)
    --    Families 101-150: 0 children (parents only — younger kids not yet in system)
    -- ═══════════════════════════════════════════════════════════════════
    c_idx := 0;  -- running student counter

    FOR i IN 1..150 LOOP
        -- Family last name index cycles through 100 last names
        fam_last_idx := ((i - 1) % 100) + 1;

        -- Create family
        INSERT INTO families (id, name) VALUES (gen_random_uuid(), 'Familie ' || p_last_display[fam_last_idx])
        RETURNING id INTO tmp_id;
        f_ids[i] := tmp_id;

        -- Two parents per family
        FOR j IN 0..1 LOOP
            p_idx := (i - 1) * 2 + j + 1;  -- parent index 1..300
            IF p_idx <= 300 THEN
                em := _email_safe(p_first[p_idx]) || '.' || _email_safe(p_last[fam_last_idx]) || '@monteweb.local';
                -- Avoid email collisions: families sharing a last name get a suffix
                IF i > 100 THEN
                    em := _email_safe(p_first[p_idx]) || '.' || _email_safe(p_last[fam_last_idx]) || (i / 100 + 1)::text || '@monteweb.local';
                END IF;
                INSERT INTO users (id, email, password_hash, first_name, last_name, display_name, phone, role, is_active, email_verified)
                VALUES (gen_random_uuid(), em, pw, p_first[p_idx], p_last_display[fam_last_idx],
                        p_first[p_idx] || ' ' || p_last_display[fam_last_idx],
                        '+49 171 ' || lpad((1000000 + p_idx * 7919)::text, 7, '0'),
                        'PARENT', true, true)
                ON CONFLICT (email) DO NOTHING;
                p_ids[p_idx] := (SELECT id FROM users WHERE email = em);

                INSERT INTO family_members (family_id, user_id, role)
                VALUES (f_ids[i], p_ids[p_idx], 'PARENT')
                ON CONFLICT DO NOTHING;
            END IF;
        END LOOP;

        -- Children: families 1-100 get 2 children each = 200 students
        IF i <= 100 THEN
            FOR j IN 0..1 LOOP
                c_idx := c_idx + 1;
                IF c_idx <= 200 THEN
                    em := _email_safe(c_first[c_idx]) || '.' || _email_safe(p_last[fam_last_idx]) || '@monteweb.local';
                    -- Avoid collision for families sharing a last name (i > 50 reuses last names)
                    IF i > 50 THEN
                        em := _email_safe(c_first[c_idx]) || '.' || _email_safe(p_last[fam_last_idx]) || '2@monteweb.local';
                    END IF;
                    INSERT INTO users (id, email, password_hash, first_name, last_name, display_name, role, is_active, email_verified)
                    VALUES (gen_random_uuid(), em, pw, c_first[c_idx], p_last_display[fam_last_idx],
                            c_first[c_idx] || ' ' || p_last_display[fam_last_idx],
                            'STUDENT', true, true)
                    ON CONFLICT (email) DO NOTHING;
                    c_ids[c_idx] := (SELECT id FROM users WHERE email = em);

                    INSERT INTO family_members (family_id, user_id, role)
                    VALUES (f_ids[i], c_ids[c_idx], 'CHILD')
                    ON CONFLICT DO NOTHING;
                END IF;
            END LOOP;
        END IF;
    END LOOP;

    -- ═══════════════════════════════════════════════════════════════════
    -- 4. ROOMS (20) + ROOM MEMBERS
    -- ═══════════════════════════════════════════════════════════════════
    FOR i IN 1..20 LOOP
        INSERT INTO rooms (id, name, description, type, section_id, settings, is_archived, created_by, join_policy)
        VALUES (gen_random_uuid(), room_names[i], room_descs[i], 'KLASSE', sec_ids[room_sec[i]],
                '{"chatEnabled": true, "filesEnabled": true, "parentSpaceEnabled": true, "visibility": "MEMBERS_ONLY"}',
                false, t_ids[room_lead[i]], 'REQUEST')
        RETURNING id INTO tmp_id;
        r_ids[i] := tmp_id;

        -- Lead teacher as LEADER
        INSERT INTO room_members (room_id, user_id, role) VALUES (r_ids[i], t_ids[room_lead[i]], 'LEADER')
        ON CONFLICT DO NOTHING;
        -- Second teacher as LEADER
        INSERT INTO room_members (room_id, user_id, role) VALUES (r_ids[i], t_ids[room_second[i]], 'LEADER')
        ON CONFLICT DO NOTHING;

        -- 10 students per room (students 1-200 distributed across 20 rooms)
        FOR j IN 1..10 LOOP
            k := (i - 1) * 10 + j;  -- student index 1..200
            IF k <= 200 AND c_ids[k] IS NOT NULL THEN
                INSERT INTO room_members (room_id, user_id, role) VALUES (r_ids[i], c_ids[k], 'MEMBER')
                ON CONFLICT DO NOTHING;
            END IF;
        END LOOP;

        -- Parents of those students as PARENT_MEMBER
        -- Students k map to families: family_idx = ceil(k/2)
        FOR j IN 1..10 LOOP
            k := (i - 1) * 10 + j;  -- student index
            IF k <= 200 THEN
                -- This student belongs to family ceil(k/2)
                -- That family has parents at indices (fam-1)*2+1 and (fam-1)*2+2
                DECLARE
                    fam INT := ((k - 1) / 2) + 1;
                    pa INT;
                    pb INT;
                BEGIN
                    pa := (fam - 1) * 2 + 1;
                    pb := (fam - 1) * 2 + 2;
                    IF pa <= 300 AND p_ids[pa] IS NOT NULL THEN
                        INSERT INTO room_members (room_id, user_id, role) VALUES (r_ids[i], p_ids[pa], 'PARENT_MEMBER')
                        ON CONFLICT DO NOTHING;
                    END IF;
                    IF pb <= 300 AND p_ids[pb] IS NOT NULL THEN
                        INSERT INTO room_members (room_id, user_id, role) VALUES (r_ids[i], p_ids[pb], 'PARENT_MEMBER')
                        ON CONFLICT DO NOTHING;
                    END IF;
                END;
            END IF;
        END LOOP;
    END LOOP;

    -- ═══════════════════════════════════════════════════════════════════
    -- 5. CALENDAR EVENTS (50) — mix of ROOM/SECTION/SCHOOL scopes
    -- ═══════════════════════════════════════════════════════════════════
    FOR i IN 1..50 LOOP
        -- Spread events from Sep 2025 to Jul 2026
        evt_start := '2025-09-01'::date + ((i - 1) * 7);

        -- Scope distribution: 1-20 ROOM, 21-35 SECTION, 36-50 SCHOOL
        IF i <= 20 THEN
            evt_scope := 'ROOM';
            evt_scope_id := r_ids[((i - 1) % 20) + 1];
        ELSIF i <= 35 THEN
            evt_scope := 'SECTION';
            evt_scope_id := sec_ids[((i - 1) % 4) + 1];
        ELSE
            evt_scope := 'SCHOOL';
            evt_scope_id := NULL;
        END IF;

        -- All-day for about every 3rd event
        evt_all_day := (i % 3 = 0);

        INSERT INTO calendar_events (id, title, description, location, all_day,
                                     start_date, start_time, end_date, end_time,
                                     scope, scope_id, created_by)
        VALUES (gen_random_uuid(),
                evt_titles[i],
                'Veranstaltung: ' || evt_titles[i] || '. Alle Beteiligten sind herzlich eingeladen!',
                evt_locs[i],
                evt_all_day,
                evt_start,
                CASE WHEN evt_all_day THEN NULL ELSE
                    CASE WHEN i % 4 = 0 THEN '09:00'::time
                         WHEN i % 4 = 1 THEN '14:00'::time
                         WHEN i % 4 = 2 THEN '10:00'::time
                         ELSE '16:00'::time END
                END,
                evt_start + CASE WHEN evt_all_day THEN 0 ELSE 0 END,
                CASE WHEN evt_all_day THEN NULL ELSE
                    CASE WHEN i % 4 = 0 THEN '11:00'::time
                         WHEN i % 4 = 1 THEN '17:00'::time
                         WHEN i % 4 = 2 THEN '12:00'::time
                         ELSE '18:00'::time END
                END,
                evt_scope,
                evt_scope_id,
                t_ids[((i - 1) % 30) + 1]);
    END LOOP;

    -- ═══════════════════════════════════════════════════════════════════
    -- 6. JOBS (30) + ASSIGNMENTS
    -- ═══════════════════════════════════════════════════════════════════
    FOR i IN 1..30 LOOP
        -- Status: 1-12 OPEN, 13-20 PARTIALLY_ASSIGNED (=ASSIGNED with fewer than max), 21-30 ASSIGNED
        IF i <= 12 THEN job_status := 'OPEN';
        ELSIF i <= 20 THEN job_status := 'ASSIGNED';
        ELSE job_status := 'COMPLETED';
        END IF;

        sched_date := '2025-09-01'::date + (i * 12);

        INSERT INTO jobs (id, title, description, category, estimated_hours, max_assignees,
                          status, scheduled_date, scheduled_time, created_by, section_id, contact_info,
                          closed_at)
        VALUES (gen_random_uuid(),
                job_titles[i],
                'Wir suchen engagierte Eltern für: ' || job_titles[i] || '. Dauer ca. ' || job_hours[i] || ' Stunden.',
                job_cats[i],
                job_hours[i],
                job_max[i],
                job_status,
                sched_date,
                CASE WHEN i % 3 = 0 THEN '09:00' WHEN i % 3 = 1 THEN '14:00' ELSE '10:30' END,
                t_ids[((i - 1) % 30) + 1],
                sec_ids[((i - 1) % 4) + 1],
                t_first[((i - 1) % 30) + 1] || ' ' || t_last[((i - 1) % 30) + 1],
                CASE WHEN job_status = 'COMPLETED' THEN sched_date::timestamp + interval '5 hours' ELSE NULL END)
        RETURNING id INTO tmp_id;
        j_ids[i] := tmp_id;

        -- Assignments for non-OPEN jobs
        IF job_status != 'OPEN' THEN
            FOR j IN 1..LEAST(2, job_max[i]) LOOP
                k := ((i * 3 + j * 7) % 200) + 1;  -- pseudo-random parent index (1-200)
                IF p_ids[k] IS NOT NULL THEN
                    DECLARE
                        fam_for_assign INT := ((k - 1) / 2) + 1;
                    BEGIN
                        IF f_ids[fam_for_assign] IS NOT NULL THEN
                            INSERT INTO job_assignments (id, job_id, user_id, family_id, status, actual_hours,
                                                         confirmed, confirmed_by, confirmed_at, notes,
                                                         assigned_at, started_at, completed_at)
                            VALUES (gen_random_uuid(), j_ids[i], p_ids[k], f_ids[fam_for_assign],
                                    CASE
                                        WHEN job_status = 'ASSIGNED' THEN 'ASSIGNED'
                                        ELSE 'COMPLETED'
                                    END,
                                    CASE WHEN job_status = 'COMPLETED' THEN job_hours[i] ELSE NULL END,
                                    job_status = 'COMPLETED',
                                    CASE WHEN job_status = 'COMPLETED' THEN t_ids[((i - 1) % 30) + 1] ELSE NULL END,
                                    CASE WHEN job_status = 'COMPLETED' THEN NOW() ELSE NULL END,
                                    CASE WHEN job_status = 'COMPLETED' THEN 'Gut gelaufen, danke!' ELSE NULL END,
                                    sched_date::timestamp - interval '7 days',
                                    CASE WHEN job_status = 'COMPLETED' THEN sched_date::timestamp ELSE NULL END,
                                    CASE WHEN job_status = 'COMPLETED' THEN sched_date::timestamp + interval '4 hours' ELSE NULL END)
                            ON CONFLICT (job_id, user_id) DO NOTHING;
                        END IF;
                    END;
                END IF;
            END LOOP;
        END IF;
    END LOOP;

    -- ═══════════════════════════════════════════════════════════════════
    -- 7. FEED POSTS (13) + COMMENTS
    -- ═══════════════════════════════════════════════════════════════════
    FOR i IN 1..13 LOOP
        INSERT INTO feed_posts (id, author_id, title, content, source_type, source_id,
                                is_pinned, is_parent_only, published_at)
        VALUES (gen_random_uuid(),
                t_ids[room_lead[post_rooms[i]]],
                post_titles[i],
                post_contents[i],
                'ROOM',
                r_ids[post_rooms[i]],
                i <= 2,
                false,
                NOW() - ((14 - i) || ' days')::interval)
        RETURNING id INTO post_id;

        -- Add 1-3 parent comments on odd-numbered posts
        IF i % 2 = 1 THEN
            FOR j IN 1..3 LOOP
                -- Pick a parent from the room's student families
                k := ((post_rooms[i] - 1) * 10 + j);  -- student index
                IF k <= 200 THEN
                    DECLARE
                        parent_for_comment INT := ((k - 1) / 2) * 2 + 1;  -- first parent of that family
                    BEGIN
                        IF parent_for_comment <= 300 AND p_ids[parent_for_comment] IS NOT NULL THEN
                            INSERT INTO feed_post_comments (id, post_id, author_id, content, created_at)
                            VALUES (gen_random_uuid(), post_id,
                                    p_ids[parent_for_comment],
                                    comment_texts[((i + j) % 8) + 1],
                                    NOW() - ((14 - i) || ' days')::interval + (j || ' hours')::interval);
                        END IF;
                    END;
                END IF;
            END LOOP;
        END IF;
    END LOOP;

    -- ═══════════════════════════════════════════════════════════════════
    -- 8. ELTERNBEIRAT (10 parents with special role)
    -- ═══════════════════════════════════════════════════════════════════
    -- Mark 10 parents as Elternbeirat via special_roles array
    FOR i IN 1..10 LOOP
        k := i * 20;  -- pick every 20th parent (indices 20, 40, ..., 200)
        IF p_ids[k] IS NOT NULL THEN
            UPDATE users SET special_roles = array_append(special_roles, 'ELTERNBEIRAT')
            WHERE id = p_ids[k] AND NOT ('ELTERNBEIRAT' = ANY(special_roles));
        END IF;
    END LOOP;

    -- ═══════════════════════════════════════════════════════════════════
    -- 9. PUTZORGA (10 parents with special role)
    -- ═══════════════════════════════════════════════════════════════════
    FOR i IN 1..10 LOOP
        k := i * 20 - 10;  -- pick indices 10, 30, 50, ..., 190
        IF p_ids[k] IS NOT NULL THEN
            UPDATE users SET special_roles = array_append(special_roles, 'PUTZORGA')
            WHERE id = p_ids[k] AND NOT ('PUTZORGA' = ANY(special_roles));
        END IF;
    END LOOP;

    RAISE NOTICE 'Extended seed data created: 4 sections, 30 teachers, 300 parents, 200 students, 150 families, 20 rooms, 50 events, 30 jobs, 10 Elternbeirat, 10 Putzorga';
END $$;

-- Cleanup helper function
DROP FUNCTION IF EXISTS _email_safe(TEXT);
