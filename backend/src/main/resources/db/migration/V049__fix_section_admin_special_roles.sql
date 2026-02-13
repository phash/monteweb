-- V049: Assign SECTION_ADMIN special roles to sectionadmin test user
-- SECTION_ADMIN permissions are now derived from explicit SECTION_ADMIN:<sectionId>
-- entries in special_roles, not from LEADER room membership.

-- sectionadmin@ gets SECTION_ADMIN for the Kinderhaus section
-- (Sonnengruppe + Sternengruppe both belong to Kinderhaus)
UPDATE users
SET special_roles = special_roles || ARRAY['SECTION_ADMIN:' || (SELECT id::text FROM school_sections WHERE slug = 'kinderhaus')]
WHERE email = 'sectionadmin@monteweb.local'
  AND NOT ('SECTION_ADMIN:' || (SELECT id::text FROM school_sections WHERE slug = 'kinderhaus')) = ANY(special_roles);
