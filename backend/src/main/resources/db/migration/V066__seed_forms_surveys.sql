-- V066: Seed forms/surveys with realistic data
-- 4 forms in Sonnengruppe room: 2 PUBLISHED, 2 CLOSED
-- Responses from seed users

DO $$
DECLARE
    v_room_id UUID;
    v_admin_id UUID;
    v_section_id UUID;

    v_form1_id UUID;
    v_form2_id UUID;
    v_form3_id UUID;
    v_form4_id UUID;

    v_q1_id UUID;
    v_q2_id UUID;
    v_q3_id UUID;
    v_q4_id UUID;

    v_resp_id UUID;
    v_user_id UUID;
    v_user_ids UUID[];
    v_i INT;
    v_choice TEXT;
    v_days JSONB;
    v_rating INT;
BEGIN
    -- Find Sonnengruppe room
    SELECT id, section_id INTO v_room_id, v_section_id
    FROM rooms WHERE name = 'Sonnengruppe' LIMIT 1;

    IF v_room_id IS NULL THEN
        RAISE NOTICE 'Sonnengruppe room not found, skipping forms seed';
        RETURN;
    END IF;

    -- Find admin user
    SELECT id INTO v_admin_id
    FROM users WHERE email = 'admin@monteweb.local' LIMIT 1;

    IF v_admin_id IS NULL THEN
        RAISE NOTICE 'Admin user not found, skipping forms seed';
        RETURN;
    END IF;

    -- ============================================================
    -- Form 1: Elternabend Themenvorschlaege (PUBLISHED, deadline +14 days)
    -- ============================================================
    v_form1_id := gen_random_uuid();
    INSERT INTO forms (id, title, description, type, scope, scope_id, status, anonymous, deadline, created_by, created_at, updated_at, published_at)
    VALUES (v_form1_id, 'Elternabend Themenvorschläge',
            'Bitte teilen Sie uns Ihre Wünsche für den nächsten Elternabend mit.',
            'SURVEY', 'ROOM', v_room_id, 'PUBLISHED', false,
            CURRENT_DATE + 14, v_admin_id, now() - interval '3 days', now() - interval '3 days', now() - interval '3 days');

    -- Q1: SINGLE_CHOICE
    v_q1_id := gen_random_uuid();
    INSERT INTO form_questions (id, form_id, type, label, required, sort_order, options)
    VALUES (v_q1_id, v_form1_id, 'SINGLE_CHOICE', 'Bevorzugte Uhrzeit?', true, 0,
            '{"choices": ["18:00", "19:00", "20:00"]}'::jsonb);

    -- Q2: TEXT
    v_q2_id := gen_random_uuid();
    INSERT INTO form_questions (id, form_id, type, label, required, sort_order)
    VALUES (v_q2_id, v_form1_id, 'TEXT', 'Themenvorschläge?', false, 1);

    -- Q3: RATING
    v_q3_id := gen_random_uuid();
    INSERT INTO form_questions (id, form_id, type, label, required, sort_order, options)
    VALUES (v_q3_id, v_form1_id, 'RATING', 'Wie zufrieden sind Sie mit der Elternarbeit?', true, 2,
            '{"min": 1, "max": 5}'::jsonb);

    -- 8 responses from seed users
    SELECT array_agg(id) INTO v_user_ids
    FROM (
        SELECT id FROM users
        WHERE email LIKE '%@monteweb.local'
          AND email NOT IN ('admin@monteweb.local')
        ORDER BY created_at
        LIMIT 8
    ) sub;

    FOR v_i IN 1..array_length(v_user_ids, 1) LOOP
        v_user_id := v_user_ids[v_i];
        v_resp_id := gen_random_uuid();

        INSERT INTO form_responses (id, form_id, user_id, submitted_at)
        VALUES (v_resp_id, v_form1_id, v_user_id, now() - interval '2 days' + (v_i * interval '1 hour'));

        -- Q1 answer: distribute choices
        v_choice := CASE v_i % 3 WHEN 0 THEN '18:00' WHEN 1 THEN '19:00' ELSE '20:00' END;
        INSERT INTO form_answers (response_id, question_id, answer_options)
        VALUES (v_resp_id, v_q1_id, ('["' || v_choice || '"]')::jsonb);

        -- Q2 answer: text (some users skip optional)
        IF v_i % 2 = 0 THEN
            INSERT INTO form_answers (response_id, question_id, answer_text)
            VALUES (v_resp_id, v_q2_id,
                CASE v_i % 4
                    WHEN 0 THEN 'Mehr über Medienkompetenz sprechen'
                    WHEN 2 THEN 'Sportangebote am Nachmittag'
                    ELSE 'Projektwochen planen'
                END);
        END IF;

        -- Q3 answer: rating 3-5
        v_rating := 3 + (v_i % 3);
        INSERT INTO form_answers (response_id, question_id, answer_rating)
        VALUES (v_resp_id, v_q3_id, v_rating);
    END LOOP;

    -- ============================================================
    -- Form 2: Projektwochen Feedback (PUBLISHED, deadline +30 days, 0 responses)
    -- ============================================================
    v_form2_id := gen_random_uuid();
    INSERT INTO forms (id, title, description, type, scope, scope_id, status, anonymous, deadline, created_by, created_at, updated_at, published_at)
    VALUES (v_form2_id, 'Projektwochen Feedback',
            'Helfen Sie uns bei der Planung der nächsten Projektwoche!',
            'SURVEY', 'ROOM', v_room_id, 'PUBLISHED', false,
            CURRENT_DATE + 30, v_admin_id, now() - interval '1 day', now() - interval '1 day', now() - interval '1 day');

    -- Q1: SINGLE_CHOICE
    INSERT INTO form_questions (id, form_id, type, label, required, sort_order, options)
    VALUES (gen_random_uuid(), v_form2_id, 'SINGLE_CHOICE', 'Welches Projektthema bevorzugen Sie?', true, 0,
            '{"choices": ["Natur", "Kunst", "Technik", "Sport"]}'::jsonb);

    -- Q2: MULTIPLE_CHOICE
    INSERT INTO form_questions (id, form_id, type, label, required, sort_order, options)
    VALUES (gen_random_uuid(), v_form2_id, 'MULTIPLE_CHOICE', 'Welche Tage passen Ihnen?', true, 1,
            '{"choices": ["Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag"]}'::jsonb);

    -- Q3: TEXT
    INSERT INTO form_questions (id, form_id, type, label, required, sort_order)
    VALUES (gen_random_uuid(), v_form2_id, 'TEXT', 'Weitere Anmerkungen?', false, 2);

    -- Q4: YES_NO
    INSERT INTO form_questions (id, form_id, type, label, required, sort_order)
    VALUES (gen_random_uuid(), v_form2_id, 'YES_NO', 'Können Sie als Elternhelfer unterstützen?', true, 3);

    -- ============================================================
    -- Form 3: Herbstfest Rueckmeldung (CLOSED, 120 responses)
    -- ============================================================
    v_form3_id := gen_random_uuid();
    INSERT INTO forms (id, title, description, type, scope, scope_id, status, anonymous, created_by, created_at, updated_at, published_at, closed_at)
    VALUES (v_form3_id, 'Herbstfest Rückmeldung',
            'Vielen Dank für Ihre Teilnahme am Herbstfest! Bitte geben Sie uns Ihr Feedback.',
            'SURVEY', 'ROOM', v_room_id, 'CLOSED', false,
            v_admin_id, now() - interval '30 days', now() - interval '1 day', now() - interval '25 days', now() - interval '1 day');

    -- Q1: SINGLE_CHOICE
    v_q1_id := gen_random_uuid();
    INSERT INTO form_questions (id, form_id, type, label, required, sort_order, options)
    VALUES (v_q1_id, v_form3_id, 'SINGLE_CHOICE', 'Gesamtbewertung?', true, 0,
            '{"choices": ["Sehr gut", "Gut", "Befriedigend", "Ausreichend"]}'::jsonb);

    -- Q2: RATING
    v_q2_id := gen_random_uuid();
    INSERT INTO form_questions (id, form_id, type, label, required, sort_order, options)
    VALUES (v_q2_id, v_form3_id, 'RATING', 'Wie war die Organisation?', true, 1,
            '{"min": 1, "max": 5}'::jsonb);

    -- Q3: YES_NO
    v_q3_id := gen_random_uuid();
    INSERT INTO form_questions (id, form_id, type, label, required, sort_order)
    VALUES (v_q3_id, v_form3_id, 'YES_NO', 'Würden Sie wieder teilnehmen?', true, 2);

    -- 120 responses from seed users
    SELECT array_agg(id) INTO v_user_ids
    FROM (
        SELECT id FROM users
        WHERE email LIKE '%@monteweb.local'
          AND email NOT IN ('admin@monteweb.local')
        ORDER BY created_at
        LIMIT 120
    ) sub;

    FOR v_i IN 1..array_length(v_user_ids, 1) LOOP
        v_user_id := v_user_ids[v_i];
        v_resp_id := gen_random_uuid();

        INSERT INTO form_responses (id, form_id, user_id, submitted_at)
        VALUES (v_resp_id, v_form3_id, v_user_id,
                now() - interval '20 days' + (v_i * interval '10 minutes'));

        -- Q1: Distribute choices (60% Sehr gut, 25% Gut, 10% Befriedigend, 5% Ausreichend)
        v_choice := CASE
            WHEN v_i % 20 < 12 THEN 'Sehr gut'
            WHEN v_i % 20 < 17 THEN 'Gut'
            WHEN v_i % 20 < 19 THEN 'Befriedigend'
            ELSE 'Ausreichend'
        END;
        INSERT INTO form_answers (response_id, question_id, answer_options)
        VALUES (v_resp_id, v_q1_id, ('["' || v_choice || '"]')::jsonb);

        -- Q2: Rating 3-5, weighted towards 4-5
        v_rating := CASE
            WHEN v_i % 10 < 4 THEN 5
            WHEN v_i % 10 < 7 THEN 4
            WHEN v_i % 10 < 9 THEN 3
            ELSE 2
        END;
        INSERT INTO form_answers (response_id, question_id, answer_rating)
        VALUES (v_resp_id, v_q2_id, v_rating);

        -- Q3: Yes/No (80% yes)
        INSERT INTO form_answers (response_id, question_id, answer_text)
        VALUES (v_resp_id, v_q3_id, CASE WHEN v_i % 5 < 4 THEN 'yes' ELSE 'no' END);
    END LOOP;

    -- ============================================================
    -- Form 4: Mittagessen Zufriedenheit (CLOSED, 8 responses)
    -- ============================================================
    v_form4_id := gen_random_uuid();
    INSERT INTO forms (id, title, description, type, scope, scope_id, status, anonymous, created_by, created_at, updated_at, published_at, closed_at)
    VALUES (v_form4_id, 'Mittagessen Zufriedenheit',
            'Wie zufrieden sind Sie mit dem Mittagessen in unserer Schule?',
            'SURVEY', 'ROOM', v_room_id, 'CLOSED', false,
            v_admin_id, now() - interval '14 days', now() - interval '7 days', now() - interval '12 days', CURRENT_DATE - 7);

    -- Q1: SINGLE_CHOICE
    v_q1_id := gen_random_uuid();
    INSERT INTO form_questions (id, form_id, type, label, required, sort_order, options)
    VALUES (v_q1_id, v_form4_id, 'SINGLE_CHOICE', 'Wie bewerten Sie das Mittagessen?', true, 0,
            '{"choices": ["Sehr gut", "Gut", "Ausreichend", "Mangelhaft"]}'::jsonb);

    -- Q2: RATING
    v_q2_id := gen_random_uuid();
    INSERT INTO form_questions (id, form_id, type, label, required, sort_order, options)
    VALUES (v_q2_id, v_form4_id, 'RATING', 'Qualität der Zutaten?', true, 1,
            '{"min": 1, "max": 5}'::jsonb);

    -- 8 responses
    SELECT array_agg(id) INTO v_user_ids
    FROM (
        SELECT id FROM users
        WHERE email LIKE '%@monteweb.local'
          AND email NOT IN ('admin@monteweb.local')
        ORDER BY created_at
        LIMIT 8
    ) sub;

    FOR v_i IN 1..array_length(v_user_ids, 1) LOOP
        v_user_id := v_user_ids[v_i];
        v_resp_id := gen_random_uuid();

        INSERT INTO form_responses (id, form_id, user_id, submitted_at)
        VALUES (v_resp_id, v_form4_id, v_user_id,
                now() - interval '10 days' + (v_i * interval '2 hours'));

        -- Q1: Distribute choices
        v_choice := CASE v_i % 4 WHEN 0 THEN 'Sehr gut' WHEN 1 THEN 'Gut' WHEN 2 THEN 'Ausreichend' ELSE 'Mangelhaft' END;
        INSERT INTO form_answers (response_id, question_id, answer_options)
        VALUES (v_resp_id, v_q1_id, ('["' || v_choice || '"]')::jsonb);

        -- Q2: Rating 2-5
        v_rating := 2 + (v_i % 4);
        INSERT INTO form_answers (response_id, question_id, answer_rating)
        VALUES (v_resp_id, v_q2_id, v_rating);
    END LOOP;

    RAISE NOTICE 'Seeded 4 forms with responses in Sonnengruppe room';
END $$;
