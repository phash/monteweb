-- Phase 5: Interest rooms & time-limited rooms

-- Add discoverable flag for interest rooms (browse/search)
ALTER TABLE rooms ADD COLUMN discoverable BOOLEAN NOT NULL DEFAULT false;

-- Add expires_at for time-limited rooms (auto-archive after this date)
ALTER TABLE rooms ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;

-- Add tags for interest rooms (searchable keywords)
ALTER TABLE rooms ADD COLUMN tags TEXT[] DEFAULT '{}';

-- Index for browsing discoverable rooms
CREATE INDEX idx_rooms_discoverable ON rooms(discoverable) WHERE discoverable = true AND is_archived = false;

-- Index for expiry check (scheduled auto-archival)
CREATE INDEX idx_rooms_expires ON rooms(expires_at) WHERE expires_at IS NOT NULL AND is_archived = false;
