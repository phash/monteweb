-- V041: Feedback Batch 1

-- FB-11: Add audience column to files for student/parent separation
ALTER TABLE room_files ADD COLUMN IF NOT EXISTS audience VARCHAR(20) NOT NULL DEFAULT 'ALL';

-- FB-08: Reassign V033 test-users to V040 rooms before deleting Glühwürmchen
-- lehrer@ → Sonnengruppe as LEADER
INSERT INTO room_members (room_id, user_id, role, joined_at)
SELECT r.id, u.id, 'LEADER', NOW()
FROM rooms r, users u
WHERE r.name = 'Sonnengruppe' AND u.email = 'lehrer@monteweb.local'
AND NOT EXISTS (SELECT 1 FROM room_members rm WHERE rm.room_id = r.id AND rm.user_id = u.id);

-- eltern@ → Sonnengruppe as PARENT_MEMBER
INSERT INTO room_members (room_id, user_id, role, joined_at)
SELECT r.id, u.id, 'PARENT_MEMBER', NOW()
FROM rooms r, users u
WHERE r.name = 'Sonnengruppe' AND u.email = 'eltern@monteweb.local'
AND NOT EXISTS (SELECT 1 FROM room_members rm WHERE rm.room_id = r.id AND rm.user_id = u.id);

-- schueler@ → Sonnengruppe as MEMBER
INSERT INTO room_members (room_id, user_id, role, joined_at)
SELECT r.id, u.id, 'MEMBER', NOW()
FROM rooms r, users u
WHERE r.name = 'Sonnengruppe' AND u.email = 'schueler@monteweb.local'
AND NOT EXISTS (SELECT 1 FROM room_members rm WHERE rm.room_id = r.id AND rm.user_id = u.id);

-- sectionadmin@ → Sonnengruppe + Sternengruppe as LEADER (needed for section admin detection)
INSERT INTO room_members (room_id, user_id, role, joined_at)
SELECT r.id, u.id, 'LEADER', NOW()
FROM rooms r, users u
WHERE r.name = 'Sonnengruppe' AND u.email = 'sectionadmin@monteweb.local'
AND NOT EXISTS (SELECT 1 FROM room_members rm WHERE rm.room_id = r.id AND rm.user_id = u.id);

INSERT INTO room_members (room_id, user_id, role, joined_at)
SELECT r.id, u.id, 'LEADER', NOW()
FROM rooms r, users u
WHERE r.name = 'Sternengruppe' AND u.email = 'sectionadmin@monteweb.local'
AND NOT EXISTS (SELECT 1 FROM room_members rm WHERE rm.room_id = r.id AND rm.user_id = u.id);

-- admin@ (SUPERADMIN) → LEADER in all rooms for full visibility
INSERT INTO room_members (room_id, user_id, role, joined_at)
SELECT r.id, u.id, 'LEADER', NOW()
FROM rooms r, users u
WHERE u.email = 'admin@monteweb.local'
AND NOT EXISTS (SELECT 1 FROM room_members rm WHERE rm.room_id = r.id AND rm.user_id = u.id);

-- Now safe to remove Glühwürmchen
DELETE FROM room_members WHERE room_id IN (SELECT id FROM rooms WHERE name = 'Glühwürmchen');
DELETE FROM feed_posts WHERE source_id IN (SELECT id FROM rooms WHERE name = 'Glühwürmchen');
DELETE FROM rooms WHERE name = 'Glühwürmchen';
