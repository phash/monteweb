-- V113: Re-assign SECTION_ADMIN special role after V107 recreated school_sections with new UUIDs
-- V049 originally set special_roles with the old kinderhaus UUID (from V040).
-- V107 deleted and recreated all school_sections with gen_random_uuid(), invalidating the old UUID.

-- Clear any stale SECTION_ADMIN entries and re-add with current kinderhaus UUID
UPDATE users
SET special_roles = (
    SELECT array_agg(sr)
    FROM unnest(special_roles) AS sr
    WHERE sr NOT LIKE 'SECTION_ADMIN:%'
)
WHERE email = 'sectionadmin@monteweb.local'
  AND EXISTS (SELECT 1 FROM unnest(special_roles) AS sr WHERE sr LIKE 'SECTION_ADMIN:%');

-- Set to empty array if all special_roles were stale SECTION_ADMIN entries (avoid NULL)
UPDATE users
SET special_roles = '{}'
WHERE email = 'sectionadmin@monteweb.local'
  AND special_roles IS NULL;

-- Re-add with current kinderhaus section UUID
UPDATE users
SET special_roles = special_roles || ARRAY['SECTION_ADMIN:' || (SELECT id::text FROM school_sections WHERE slug = 'kinderhaus')]
WHERE email = 'sectionadmin@monteweb.local'
  AND (SELECT id FROM school_sections WHERE slug = 'kinderhaus') IS NOT NULL
  AND NOT ('SECTION_ADMIN:' || (SELECT id::text FROM school_sections WHERE slug = 'kinderhaus')) = ANY(special_roles);
