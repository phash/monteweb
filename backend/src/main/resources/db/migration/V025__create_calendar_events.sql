CREATE TABLE calendar_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(300) NOT NULL,
    description     TEXT,
    location        VARCHAR(500),
    all_day         BOOLEAN NOT NULL DEFAULT false,
    start_date      DATE NOT NULL,
    start_time      TIME,
    end_date        DATE NOT NULL,
    end_time        TIME,
    scope           VARCHAR(20) NOT NULL,
    scope_id        UUID,
    recurrence      VARCHAR(20) NOT NULL DEFAULT 'NONE',
    recurrence_end  DATE,
    cancelled       BOOLEAN NOT NULL DEFAULT false,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_calendar_events_scope ON calendar_events(scope, scope_id);
CREATE INDEX idx_calendar_events_dates ON calendar_events(start_date, end_date);
CREATE INDEX idx_calendar_events_created_by ON calendar_events(created_by);

CREATE TABLE calendar_event_rsvps (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID NOT NULL REFERENCES calendar_events(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id),
    status          VARCHAR(20) NOT NULL,
    responded_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE(event_id, user_id)
);

CREATE INDEX idx_event_rsvps_event_user ON calendar_event_rsvps(event_id, user_id);
