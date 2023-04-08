CREATE INDEX press_contact_time_idx
ON press (contact_id, time)
WHERE contact_id IS NOT NULL;
