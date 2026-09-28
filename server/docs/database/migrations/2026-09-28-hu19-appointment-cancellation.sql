-- HU-19: a cancelled appointment keeps its original interval and identifier.
-- A replacement appointment may use the same assignment and start after manual release.
-- PostgreSQL ddl-auto=update does not drop obsolete constraints automatically.
-- Run once per existing environment before enabling administrative cancellation.
ALTER TABLE public.appointment DROP CONSTRAINT IF EXISTS uk_appointment_assignment_start;
