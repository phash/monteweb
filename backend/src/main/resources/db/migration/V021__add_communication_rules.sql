ALTER TABLE tenant_config ADD COLUMN parent_to_parent_messaging BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE tenant_config ADD COLUMN student_to_student_messaging BOOLEAN NOT NULL DEFAULT false;
