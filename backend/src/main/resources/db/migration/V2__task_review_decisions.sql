ALTER TABLE task_review ADD COLUMN decided_by_user_id VARCHAR(36);
ALTER TABLE task_review ADD COLUMN decision_note CLOB;
ALTER TABLE task_review ADD COLUMN decided_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE task_review ADD CONSTRAINT fk_review_decider
 FOREIGN KEY(enterprise_id,decided_by_user_id) REFERENCES app_user(enterprise_id,id);
CREATE INDEX ix_review_pending ON task_review(enterprise_id,reviewer_user_id,decision,submitted_at,id);
CREATE INDEX ix_review_submitted ON task_review(enterprise_id,submitted_by_user_id,submitted_at,id);
