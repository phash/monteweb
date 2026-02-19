-- Seed Bavarian school vacations 2025/2026 as default
UPDATE tenant_config
SET school_vacations = '[
  {"name": "Winterferien", "from": "2026-02-16", "to": "2026-02-20"},
  {"name": "Osterferien", "from": "2026-03-30", "to": "2026-04-10"},
  {"name": "Pfingstferien", "from": "2026-05-26", "to": "2026-06-05"},
  {"name": "Sommerferien", "from": "2026-08-03", "to": "2026-09-14"},
  {"name": "Herbstferien", "from": "2026-11-02", "to": "2026-11-06"},
  {"name": "Weihnachtsferien", "from": "2026-12-22", "to": "2027-01-05"}
]'::jsonb
WHERE school_vacations = '[]'::jsonb;
