-- iCal subscription import (external calendar subscriptions)
CREATE TABLE ical_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    color VARCHAR(7) DEFAULT '#6366f1',
    last_synced_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT true,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ical_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES ical_subscriptions(id) ON DELETE CASCADE,
    uid VARCHAR(500) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    location VARCHAR(300),
    start_date DATE NOT NULL,
    end_date DATE,
    start_time VARCHAR(5),
    end_time VARCHAR(5),
    all_day BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_ical_event UNIQUE (subscription_id, uid)
);

CREATE INDEX idx_ical_events_sub ON ical_events(subscription_id);
CREATE INDEX idx_ical_events_date ON ical_events(start_date);
