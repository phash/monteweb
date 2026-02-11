CREATE TABLE forms (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(300) NOT NULL,
    description     TEXT,
    type            VARCHAR(20) NOT NULL,
    scope           VARCHAR(20) NOT NULL,
    scope_id        UUID,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    anonymous       BOOLEAN NOT NULL DEFAULT false,
    deadline        DATE,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    published_at    TIMESTAMP WITH TIME ZONE,
    closed_at       TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_forms_scope ON forms(scope, scope_id);
CREATE INDEX idx_forms_status ON forms(status);
CREATE INDEX idx_forms_created_by ON forms(created_by);

CREATE TABLE form_questions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id         UUID NOT NULL REFERENCES forms(id) ON DELETE CASCADE,
    type            VARCHAR(20) NOT NULL,
    label           TEXT NOT NULL,
    description     TEXT,
    required        BOOLEAN NOT NULL DEFAULT false,
    sort_order      INT NOT NULL DEFAULT 0,
    options         JSONB
);

CREATE INDEX idx_form_questions_form ON form_questions(form_id);

CREATE TABLE form_responses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id         UUID NOT NULL REFERENCES forms(id) ON DELETE CASCADE,
    user_id         UUID REFERENCES users(id),
    submitted_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE(form_id, user_id)
);

CREATE INDEX idx_form_responses_form ON form_responses(form_id);

CREATE TABLE form_answers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    response_id     UUID NOT NULL REFERENCES form_responses(id) ON DELETE CASCADE,
    question_id     UUID NOT NULL REFERENCES form_questions(id) ON DELETE CASCADE,
    answer_text     TEXT,
    answer_options  JSONB,
    answer_rating   INT
);

CREATE INDEX idx_form_answers_response ON form_answers(response_id);
CREATE INDEX idx_form_answers_question ON form_answers(question_id);

CREATE TABLE form_response_tracking (
    form_id         UUID NOT NULL REFERENCES forms(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id),
    PRIMARY KEY(form_id, user_id)
);
