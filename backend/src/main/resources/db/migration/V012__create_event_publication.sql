-- Spring Modulith Event Publication Log
-- Required for reliable event-based inter-module communication
CREATE TABLE IF NOT EXISTS event_publication (
    id                 UUID NOT NULL PRIMARY KEY,
    listener_id        TEXT NOT NULL,
    event_type         TEXT NOT NULL,
    serialized_event   TEXT NOT NULL,
    publication_date   TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_event_publication_incomplete
    ON event_publication(publication_date)
    WHERE completion_date IS NULL;
