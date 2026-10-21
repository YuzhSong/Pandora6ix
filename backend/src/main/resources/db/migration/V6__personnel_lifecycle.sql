ALTER TABLE app_user ADD COLUMN employment_status VARCHAR(24) NOT NULL DEFAULT 'IN_SERVICE';
ALTER TABLE app_user ADD COLUMN departure_date DATE;
ALTER TABLE app_user ADD CONSTRAINT employment_consistency CHECK (
 (employment_status='IN_SERVICE' AND departure_date IS NULL) OR
 (employment_status='DEPARTED' AND departure_date IS NOT NULL AND status='DISABLED')
);
CREATE TABLE personnel_change (
 id VARCHAR(36) PRIMARY KEY, enterprise_id VARCHAR(36) NOT NULL,
 user_id VARCHAR(36) NOT NULL, actor_user_id VARCHAR(36) NOT NULL,
 kind VARCHAR(24) NOT NULL CHECK(kind IN ('DEPARTURE','JOB_CHANGE')),
 effective_date DATE NOT NULL, recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 before_values CLOB NOT NULL, after_values CLOB NOT NULL,
 FOREIGN KEY(enterprise_id,user_id) REFERENCES app_user(enterprise_id,id),
 FOREIGN KEY(enterprise_id,actor_user_id) REFERENCES app_user(enterprise_id,id)
);
CREATE INDEX personnel_change_user ON personnel_change(enterprise_id,user_id,recorded_at);
