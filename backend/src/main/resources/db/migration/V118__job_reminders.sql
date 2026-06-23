-- Reminder tracking: lets the scheduled JobReminderService fire each reminder
-- exactly once. overdue_* guard the "4 weeks after the scheduled date" wave;
-- year_end_reminder_sent_at guards the once-per-period "2 weeks before period end" wave.
ALTER TABLE job_assignments ADD COLUMN overdue_reminder_sent_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE jobs ADD COLUMN overdue_reminder_sent_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE billing_periods ADD COLUMN year_end_reminder_sent_at TIMESTAMP WITH TIME ZONE;
