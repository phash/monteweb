-- V111: Always ensure admin@monteweb.local has SUPERADMIN role and test1234 password
-- Password hash for 'test1234': $2a$10$dQa6248nzH.S5yYnSpGfr.b7BnuGroDZ/G18yp.vzoAx45FQ7hecO
UPDATE users
SET role = 'SUPERADMIN',
    password_hash = '$2a$10$dQa6248nzH.S5yYnSpGfr.b7BnuGroDZ/G18yp.vzoAx45FQ7hecO'
WHERE email = 'admin@monteweb.local';
