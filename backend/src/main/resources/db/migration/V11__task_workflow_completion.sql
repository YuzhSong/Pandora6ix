ALTER TABLE task_review ADD COLUMN review_kind VARCHAR(8) NOT NULL DEFAULT 'CREATE' CHECK(review_kind IN ('CREATE','EDIT'));
ALTER TABLE task_assignment ADD COLUMN work_status VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS' CHECK(work_status IN ('NOT_STARTED','IN_PROGRESS','COMPLETED'));
UPDATE task_assignment SET work_status='COMPLETED' WHERE execution_status='COMPLETED';
