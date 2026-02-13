-- V040: Seed realistic school data
-- 4 sections, 20 teachers, 100 parents, 100 students, 50 families, 8 rooms, 50 jobs
-- Password for all users: test1234
-- IMPORTANT: Remove or disable in production!

-- Helper: make name email-safe (ä→ae, ö→oe, ü→ue, ß→ss, lowercase)
CREATE OR REPLACE FUNCTION _email_safe(t TEXT) RETURNS TEXT AS $fn$
BEGIN
    RETURN lower(replace(replace(replace(replace(t, 'ä', 'ae'), 'ö', 'oe'), 'ü', 'ue'), 'ß', 'ss'));
END;
$fn$ LANGUAGE plpgsql;

DO $$
DECLARE
    pw CONSTANT TEXT := '$2a$10$dQa6248nzH.S5yYnSpGfr.b7BnuGroDZ/G18yp.vzoAx45FQ7hecO';

    sec_ids UUID[4];

    t_first TEXT[] := ARRAY['Katharina','Markus','Sabine','Florian','Petra','Andreas','Monika','Stefan','Claudia','Thomas','Renate','Wolfgang','Dagmar','Rainer','Heike','Matthias','Brigitte','Joerg','Susanne','Ulrich'];
    t_last  TEXT[] := ARRAY['Bergmann','Vogt','Seidler','Heinze','Krause','Richter','Lindner','Hamann','Fiedler','Boettcher','Kunze','Hesse','Brandt','Wendt','Schreiber','Engel','Schiller','Fuchs','Pfeiffer','Busch'];
    t_ids UUID[20];

    p_first TEXT[] := ARRAY[
        'Anna','Thomas','Maria','Michael','Sabine','Stefan','Claudia','Peter','Andrea','Martin',
        'Susanne','Juergen','Monika','Uwe','Petra','Frank','Nicole','Ralf','Birgit','Holger',
        'Katrin','Bernd','Heike','Markus','Karin','Dirk','Anja','Sven','Silke','Olaf',
        'Tanja','Rene','Nadine','Jens','Yvonne','Kai','Simone','Thorsten','Daniela','Lars',
        'Sandra','Christian','Melanie','Torsten','Janine','Matthias','Stefanie','Carsten','Manuela','Gerd',
        'Cornelia','Detlef','Gabriele','Ingo','Ramona','Wolfgang','Britta','Norbert','Sonja','Christoph',
        'Elke','Axel','Marion','Jochen','Iris','Volker','Bettina','Rolf','Martina','Achim',
        'Ute','Reiner','Diana','Guido','Doris','Erich','Anke','Harald','Christine','Lutz',
        'Dagmar','Klaus','Beate','Werner','Astrid','Dieter','Barbara','Heinz','Renate','Kurt',
        'Ingrid','Herbert','Gisela','Horst','Ursula','Friedrich','Hannelore','Georg','Edith','Gerhard'];
    p_last TEXT[] := ARRAY[
        'Mueller','Schmidt','Fischer','Weber','Wagner','Becker','Hoffmann','Schaefer','Koch','Bauer',
        'Richter','Klein','Wolf','Neumann','Schwarz','Zimmermann','Braun','Krueger','Hartmann','Lange',
        'Werner','Meier','Lehmann','Schmid','Krause','Schulz','Maier','Koehler','Jung','Hahn',
        'Keller','Frank','Berger','Winkler','Roth','Beck','Lorenz','Baumann','Franke','Albrecht',
        'Schuster','Simon','Ludwig','Boehm','Winter','Sommer','Haas','Graf','Heinrich','Seidel'];
    p_ids UUID[100];

    -- Display last names (with real umlauts for display_name / last_name)
    p_last_display TEXT[] := ARRAY[
        'Müller','Schmidt','Fischer','Weber','Wagner','Becker','Hoffmann','Schäfer','Koch','Bauer',
        'Richter','Klein','Wolf','Neumann','Schwarz','Zimmermann','Braun','Krüger','Hartmann','Lange',
        'Werner','Meier','Lehmann','Schmid','Krause','Schulz','Maier','Köhler','Jung','Hahn',
        'Keller','Frank','Berger','Winkler','Roth','Beck','Lorenz','Baumann','Franke','Albrecht',
        'Schuster','Simon','Ludwig','Böhm','Winter','Sommer','Haas','Graf','Heinrich','Seidel'];

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
        'August','Flora','Franz','Minna','Benno','Helene','Caspar','Alma','Johann','Nora'];
    c_ids UUID[100];

    f_ids UUID[50];

    room_names TEXT[] := ARRAY['Sonnengruppe','Sternengruppe','Erdkinder 1-3','Kosmische Klasse 3-4','Lernwerkstatt 5-6','Forscherklasse 7-8','Projektklasse 9-10','Abiturjahrgang 11-12'];
    room_descs TEXT[] := ARRAY[
        'Die Sonnengruppe für unsere Jüngsten (1-3 Jahre)',
        'Die Sternengruppe für Kindergartenkinder (3-6 Jahre)',
        'Kosmische Erziehung und Grundlagen für Klasse 1-3',
        'Vertiefung und Übergang für Klasse 3-4',
        'Selbstständiges Arbeiten in der Lernwerkstatt (Klasse 5-6)',
        'Naturwissenschaftliches Forschen in Klasse 7-8',
        'Projektbasiertes Lernen in Klasse 9-10',
        'Abitur-Vorbereitung und Großes Projekt (Klasse 11-12)'];
    -- Which section each room belongs to (1-4 index into sec_ids)
    room_sec INT[] := ARRAY[1,1,2,2,3,3,4,4];
    -- Lead teacher index for each room
    room_lead INT[] := ARRAY[1,3,6,8,11,13,16,18];
    -- Family range per room [start, end] — each family contributes 2 students
    room_fam_start INT[] := ARRAY[1, 8,14,20,26,32,38,45];
    room_fam_end   INT[] := ARRAY[7,13,19,25,31,37,44,50];
    r_ids UUID[8];

    job_titles TEXT[] := ARRAY[
        'Hochbeete im Schulgarten bepflanzen','Klassenzimmer streichen (Grundstufe)',
        'Herbstfest Buffet vorbereiten','Spielplatz-Zaun reparieren',
        'Nikolausfeier Deko aufbauen','Schulküche Grundreinigung',
        'Elternabend Catering organisieren','Werkstatt: Regale bauen für Bibliothek',
        'Sommerfest Hüpfburg organisieren','Fahrradständer montieren',
        'Schulbibliothek neu einräumen','Adventskranz-Werkstatt vorbereiten',
        'Schulweg-Begleitung einrichten','Sportplatz mähen und pflegen',
        'Projektwoche Material einkaufen','Erste-Hilfe-Schränke auffüllen',
        'Garderobe Kinderhaus reparieren','Laternenfest Lichter aufhängen',
        'Schulhof Frühjahrsputz','Pausenspiele-Kiste zusammenstellen',
        'Elterncafé einrichten und betreuen','Schulgartentor streichen',
        'Sandkasten Sand nachfüllen','Weihnachtsbasar Standaufbau',
        'Theateraufführung Bühnenbild bauen','Turnhalle Geräte warten',
        'Schulfest Tombola organisieren','Flur-Ausstellung aufhängen',
        'Computerraum Kabel ordnen','Einschulungsfeier Deko vorbereiten',
        'Kräutergarten anlegen','Bücherregal Kinderbücherei aufbauen',
        'Fasching Kostüme sortieren','Osterbrunch Buffet aufbauen',
        'Fenster putzen (Erdgeschoss)','Werkbank schleifen und ölen',
        'Matschküche im Garten bauen','Musikinstrumente reparieren',
        'Ruhezone Sitzkissen nähen','Schulweg-Schilder aufstellen',
        'Wasserspielplatz winterfest machen','Fotowand gestalten',
        'Schulflohmarkt Tische aufstellen','Leseecke gemütlich einrichten',
        'Bewegungsparcours aufbauen','Elternsprechtag Wegweiser aufstellen',
        'Vogelhäuschen bauen und aufhängen','Schaukästen reinigen und bestücken',
        'Tag der offenen Tür: Führungen','Abschlussfeier Klasse 12 dekorieren'];
    job_cats TEXT[] := ARRAY[
        'GARTEN','RENOVIERUNG','VERANSTALTUNG','RENOVIERUNG','VERANSTALTUNG',
        'REINIGUNG','VERANSTALTUNG','WERKSTATT','VERANSTALTUNG','WERKSTATT',
        'BUERO','WERKSTATT','TRANSPORT','GARTEN','TRANSPORT',
        'BUERO','RENOVIERUNG','VERANSTALTUNG','REINIGUNG','BUERO',
        'KUECHE','RENOVIERUNG','GARTEN','VERANSTALTUNG','WERKSTATT',
        'WERKSTATT','VERANSTALTUNG','BUERO','WERKSTATT','VERANSTALTUNG',
        'GARTEN','WERKSTATT','VERANSTALTUNG','KUECHE','REINIGUNG',
        'WERKSTATT','GARTEN','WERKSTATT','WERKSTATT','TRANSPORT',
        'GARTEN','BUERO','VERANSTALTUNG','BUERO','WERKSTATT',
        'BUERO','WERKSTATT','REINIGUNG','VERANSTALTUNG','VERANSTALTUNG'];
    job_hours DECIMAL[] := ARRAY[
        3.0,4.0,2.5,2.0,1.5,3.0,2.0,4.0,3.0,2.0,
        2.5,2.0,1.0,2.0,1.5,1.0,2.5,1.5,3.0,1.0,
        2.0,2.0,1.5,3.0,4.0,2.0,3.0,1.0,2.0,2.0,
        2.5,3.0,1.5,2.5,2.0,2.5,3.5,1.5,3.0,1.0,
        2.0,1.0,2.5,2.0,2.5,0.5,3.0,1.5,2.0,3.0];
    job_max INT[] := ARRAY[
        4,3,5,2,4,3,5,2,6,2,
        3,4,2,2,2,1,2,4,5,2,
        3,2,2,5,3,2,6,2,2,4,
        3,2,4,5,3,2,3,2,3,2,
        2,2,4,2,3,2,3,2,4,5];
    j_ids UUID[50];

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
        'Liebe Eltern, am Freitag findet unser gemeinsamer Herbstspaziergang im Stadtwald statt. Treffpunkt ist um 9:30 Uhr am Schultor. Bitte wetterfeste Kleidung und einen kleinen Snack mitgeben. Wir freuen uns auf einen schönen Vormittag in der Natur!',
        'Liebe Eltern, bitte denken Sie daran, Ihrem Kind den Turnbeutel mitzugeben. Wir haben ab dieser Woche jeden Mittwoch Bewegungszeit in der Turnhalle. Hallenschuhe mit heller Sohle bitte nicht vergessen!',
        'Was für eine tolle Projektwoche! Die Kinder haben zum Thema „Wasser" geforscht, experimentiert und gebastelt. Ein großes Dankeschön an alle Eltern, die uns mit Materialien und Mithilfe unterstützt haben.',
        'Nächsten Dienstag öffnet wieder unser Elterncafé von 8:00 bis 9:30 Uhr im Foyer. Eine schöne Gelegenheit zum Austausch bei Kaffee und Kuchen. Kuchenspenden sind herzlich willkommen!',
        'Wir haben 25 neue Bücher für unsere Leseecke bekommen! Darunter viele spannende Sachbücher über Tiere, Planeten und Erfindungen. Die Kinder sind schon ganz begeistert. Vielen Dank an den Förderverein!',
        'Am 11. November ziehen wir mit unseren selbstgebastelten Laternen durch die Nachbarschaft. Start ist um 17:00 Uhr auf dem Schulhof. Bitte bringen Sie Laternen und Teelichter mit.',
        'Liebe Familien, für unser Adventsbasteln am 1. Dezember benötigen wir: Tannenzweige, Kerzen, Nüsse, Zimtstangen und Bastelkleber. Bitte bis Ende November mitbringen.',
        'Die Halbjahreszeugnisse werden am 31. Januar in der 4. Stunde ausgegeben. An diesem Tag endet der Unterricht um 11:00 Uhr. Bei Gesprächsbedarf können individuelle Termine vereinbart werden.',
        'Der Frühling kommt! Wir planen die Neubepflanzung unseres Schulgartens. Wer Samen, Setzlinge oder Gartengeräte spenden möchte, kann diese ab sofort im Sekretariat abgeben.',
        'Am 20. Juni findet unser großes Sportfest statt. Alle Kinder sind eingeladen teilzunehmen. Eltern können gerne als Streckenposten helfen. Bitte melden Sie sich bis zum 10. Juni bei mir.',
        'Diese Woche haben wir mit der Kosmischen Erzählung „Die Entstehung des Universums" begonnen. Die Kinder waren fasziniert! Wer möchte, kann zu Hause weiter über Sterne und Planeten forschen.',
        'In der Werkstatt haben die Kinder diese Woche Vogelhäuschen gebaut. Jedes Kind durfte sein eigenes Häuschen gestalten und mit nach Hause nehmen. Tolle Ergebnisse!',
        'Liebe Schülerinnen und Schüler, eure Praktikumsberichte müssen bis zum 28. Februar abgegeben werden. Bitte achtet auf die Formatvorgaben. Bei Fragen stehe ich gerne zur Verfügung.'];
    -- Which room each post belongs to (index 1-8)
    post_rooms INT[] := ARRAY[1,2,3,4,3,1,2,8,5,6,3,5,8];

    comment_texts TEXT[] := ARRAY[
        'Danke für die Info! Wir sind dabei.',
        'Super, mein Kind freut sich schon riesig!',
        'Können wir auch Gummistiefel anziehen?',
        'Ich bringe gerne einen Kuchen mit.',
        'Vielen Dank für die tolle Organisation!',
        'Wir kommen auf jeden Fall. Bis Freitag!',
        'Gibt es eine Alternative für Kinder mit Allergien?',
        'Tolle Idee! Kann ich beim Aufbau helfen?'];

    i INT;
    j INT;
    k INT;
    fam_idx INT;
    em TEXT;
    tmp_id UUID;
    post_id UUID;
    sec_for_room UUID;
    job_status TEXT;
    sched_date DATE;

BEGIN
    -- ═══════════════════════════════════════════════════════════════════
    -- 1. SCHOOL SECTIONS
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
    -- 2. TEACHERS (20)
    -- ═══════════════════════════════════════════════════════════════════
    FOR i IN 1..20 LOOP
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
    -- 3. PARENTS (100) + STUDENTS (100) + FAMILIES (50)
    -- ═══════════════════════════════════════════════════════════════════
    FOR i IN 1..50 LOOP
        -- Create family
        INSERT INTO families (id, name) VALUES (gen_random_uuid(), 'Familie ' || p_last_display[i])
        RETURNING id INTO tmp_id;
        f_ids[i] := tmp_id;

        -- Two parents per family
        FOR j IN 0..1 LOOP
            k := (i - 1) * 2 + j + 1;  -- parent index 1..100
            em := _email_safe(p_first[k]) || '.' || _email_safe(p_last[i]) || '@monteweb.local';
            INSERT INTO users (id, email, password_hash, first_name, last_name, display_name, phone, role, is_active, email_verified)
            VALUES (gen_random_uuid(), em, pw, p_first[k], p_last_display[i],
                    p_first[k] || ' ' || p_last_display[i],
                    '+49 171 ' || lpad((1000000 + k * 7919)::text, 7, '0'),
                    'PARENT', true, true)
            ON CONFLICT (email) DO NOTHING;
            p_ids[k] := (SELECT id FROM users WHERE email = em);

            INSERT INTO family_members (family_id, user_id, role)
            VALUES (f_ids[i], p_ids[k], 'PARENT')
            ON CONFLICT DO NOTHING;
        END LOOP;

        -- Two students per family
        FOR j IN 0..1 LOOP
            k := (i - 1) * 2 + j + 1;  -- student index 1..100
            em := _email_safe(c_first[k]) || '.' || _email_safe(p_last[i]) || '@monteweb.local';
            INSERT INTO users (id, email, password_hash, first_name, last_name, display_name, role, is_active, email_verified)
            VALUES (gen_random_uuid(), em, pw, c_first[k], p_last_display[i],
                    c_first[k] || ' ' || p_last_display[i],
                    'STUDENT', true, true)
            ON CONFLICT (email) DO NOTHING;
            c_ids[k] := (SELECT id FROM users WHERE email = em);

            INSERT INTO family_members (family_id, user_id, role)
            VALUES (f_ids[i], c_ids[k], 'CHILD')
            ON CONFLICT DO NOTHING;
        END LOOP;
    END LOOP;

    -- ═══════════════════════════════════════════════════════════════════
    -- 4. ROOMS (8) + ROOM MEMBERS
    -- ═══════════════════════════════════════════════════════════════════
    FOR i IN 1..8 LOOP
        sec_for_room := sec_ids[room_sec[i]];
        INSERT INTO rooms (id, name, description, type, section_id, settings, is_archived, created_by, join_policy)
        VALUES (gen_random_uuid(), room_names[i], room_descs[i], 'KLASSE', sec_for_room,
                '{"chatEnabled": true, "filesEnabled": true, "parentSpaceEnabled": true, "visibility": "MEMBERS_ONLY"}',
                false, t_ids[room_lead[i]], 'REQUEST')
        RETURNING id INTO tmp_id;
        r_ids[i] := tmp_id;

        -- Lead teacher as LEADER
        INSERT INTO room_members (room_id, user_id, role) VALUES (r_ids[i], t_ids[room_lead[i]], 'LEADER')
        ON CONFLICT DO NOTHING;
        -- Second teacher as LEADER (lead+1)
        IF room_lead[i] + 1 <= 20 THEN
            INSERT INTO room_members (room_id, user_id, role) VALUES (r_ids[i], t_ids[room_lead[i]+1], 'LEADER')
            ON CONFLICT DO NOTHING;
        END IF;
        -- For rooms with 3 teachers (rooms 2,4,6): add lead+2
        IF i IN (2,4,6) AND room_lead[i] + 2 <= 20 THEN
            INSERT INTO room_members (room_id, user_id, role) VALUES (r_ids[i], t_ids[room_lead[i]+2], 'LEADER')
            ON CONFLICT DO NOTHING;
        END IF;

        -- Students as MEMBER, their parents as PARENT_MEMBER
        FOR fam_idx IN room_fam_start[i]..room_fam_end[i] LOOP
            -- 2 students per family
            FOR j IN 0..1 LOOP
                k := (fam_idx - 1) * 2 + j + 1;
                IF k <= 100 AND c_ids[k] IS NOT NULL THEN
                    INSERT INTO room_members (room_id, user_id, role) VALUES (r_ids[i], c_ids[k], 'MEMBER')
                    ON CONFLICT DO NOTHING;
                END IF;
            END LOOP;
            -- 2 parents per family
            FOR j IN 0..1 LOOP
                k := (fam_idx - 1) * 2 + j + 1;
                IF k <= 100 AND p_ids[k] IS NOT NULL THEN
                    INSERT INTO room_members (room_id, user_id, role) VALUES (r_ids[i], p_ids[k], 'PARENT_MEMBER')
                    ON CONFLICT DO NOTHING;
                END IF;
            END LOOP;
        END LOOP;
    END LOOP;

    -- ═══════════════════════════════════════════════════════════════════
    -- 5. JOBS (50) + ASSIGNMENTS
    -- ═══════════════════════════════════════════════════════════════════
    FOR i IN 1..50 LOOP
        -- Status: 1-20 OPEN, 21-30 ASSIGNED, 31-40 IN_PROGRESS, 41-50 COMPLETED
        IF i <= 20 THEN job_status := 'OPEN';
        ELSIF i <= 30 THEN job_status := 'ASSIGNED';
        ELSIF i <= 40 THEN job_status := 'IN_PROGRESS';
        ELSE job_status := 'COMPLETED';
        END IF;

        -- Scheduled dates spread across 2025
        sched_date := '2025-03-01'::date + (i * 7);

        INSERT INTO jobs (id, title, description, category, estimated_hours, max_assignees,
                          status, scheduled_date, scheduled_time, created_by, section_id, contact_info,
                          closed_at)
        VALUES (gen_random_uuid(),
                job_titles[i],
                'Wir suchen engagierte Eltern für: ' || job_titles[i] || '. Dauer ca. ' || job_hours[i] || ' Stunden. Bitte bei Interesse einfach anmelden!',
                job_cats[i],
                job_hours[i],
                job_max[i],
                job_status,
                sched_date,
                CASE WHEN i % 3 = 0 THEN '09:00' WHEN i % 3 = 1 THEN '14:00' ELSE '10:30' END,
                t_ids[((i - 1) % 20) + 1],  -- rotating teacher as creator
                sec_ids[((i - 1) % 4) + 1],
                t_first[((i - 1) % 20) + 1] || ' ' || t_last[((i - 1) % 20) + 1],
                CASE WHEN job_status = 'COMPLETED' THEN sched_date::timestamp + interval '5 hours' ELSE NULL END)
        RETURNING id INTO tmp_id;
        j_ids[i] := tmp_id;

        -- Assignments for non-OPEN jobs
        IF job_status != 'OPEN' THEN
            -- Assign 1-2 parents per job
            FOR j IN 1..LEAST(2, job_max[i]) LOOP
                k := ((i * 3 + j * 7) % 100) + 1;  -- pseudo-random parent index
                IF p_ids[k] IS NOT NULL THEN
                    fam_idx := ((k - 1) / 2) + 1;  -- family index for this parent
                    INSERT INTO job_assignments (id, job_id, user_id, family_id, status, actual_hours,
                                                 confirmed, confirmed_by, confirmed_at, notes,
                                                 assigned_at, started_at, completed_at)
                    VALUES (gen_random_uuid(), j_ids[i], p_ids[k], f_ids[fam_idx],
                            CASE
                                WHEN job_status = 'ASSIGNED' THEN 'ASSIGNED'
                                WHEN job_status = 'IN_PROGRESS' THEN 'IN_PROGRESS'
                                ELSE 'COMPLETED'
                            END,
                            CASE WHEN job_status = 'COMPLETED' THEN job_hours[i] ELSE NULL END,
                            job_status = 'COMPLETED',
                            CASE WHEN job_status = 'COMPLETED' THEN t_ids[((i - 1) % 20) + 1] ELSE NULL END,
                            CASE WHEN job_status = 'COMPLETED' THEN NOW() ELSE NULL END,
                            CASE WHEN job_status = 'COMPLETED' THEN 'Gut gelaufen, danke!' ELSE NULL END,
                            sched_date::timestamp - interval '7 days',
                            CASE WHEN job_status IN ('IN_PROGRESS','COMPLETED') THEN sched_date::timestamp ELSE NULL END,
                            CASE WHEN job_status = 'COMPLETED' THEN sched_date::timestamp + interval '4 hours' ELSE NULL END)
                    ON CONFLICT (job_id, user_id) DO NOTHING;
                END IF;
            END LOOP;
        END IF;
    END LOOP;

    -- ═══════════════════════════════════════════════════════════════════
    -- 6. FEED POSTS (13) + COMMENTS
    -- ═══════════════════════════════════════════════════════════════════
    FOR i IN 1..13 LOOP
        INSERT INTO feed_posts (id, author_id, title, content, source_type, source_id,
                                is_pinned, is_parent_only, published_at)
        VALUES (gen_random_uuid(),
                t_ids[room_lead[post_rooms[i]]],  -- teacher who leads this room
                post_titles[i],
                post_contents[i],
                'ROOM',
                r_ids[post_rooms[i]],
                i <= 2,   -- pin the first 2 posts
                false,
                NOW() - ((14 - i) || ' days')::interval)
        RETURNING id INTO post_id;

        -- Add 1-3 parent comments on odd-numbered posts
        IF i % 2 = 1 THEN
            FOR j IN 1..LEAST(3, 8) LOOP
                k := room_fam_start[post_rooms[i]];  -- first family in this room
                IF k + j - 1 <= 50 AND p_ids[(k + j - 2) * 2 + 1] IS NOT NULL THEN
                    INSERT INTO feed_post_comments (id, post_id, author_id, content, created_at)
                    VALUES (gen_random_uuid(), post_id,
                            p_ids[(k + j - 2) * 2 + 1],
                            comment_texts[((i + j) % 8) + 1],
                            NOW() - ((14 - i) || ' days')::interval + (j || ' hours')::interval);
                END IF;
            END LOOP;
        END IF;
    END LOOP;

    RAISE NOTICE 'Seed data created: 4 sections, 20 teachers, 100 parents, 100 students, 50 families, 8 rooms, 50 jobs, 13 feed posts';
END $$;

-- Cleanup helper function
DROP FUNCTION IF EXISTS _email_safe(TEXT);
