ALTER TABLE task ADD COLUMN occurrence_date DATE;
UPDATE task SET occurrence_date=start_date WHERE series_id IS NOT NULL;
CREATE INDEX idx_task_series_occurrence ON task(enterprise_id,series_id,occurrence_date);
