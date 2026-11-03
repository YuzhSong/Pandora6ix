CREATE TABLE work_log_comment (
 id VARCHAR(36) PRIMARY KEY, enterprise_id VARCHAR(36) NOT NULL, work_log_id VARCHAR(36) NOT NULL,
 author_user_id VARCHAR(36) NOT NULL, content VARCHAR(500) NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(enterprise_id,id),
 FOREIGN KEY(enterprise_id,work_log_id) REFERENCES work_log(enterprise_id,id),
 FOREIGN KEY(enterprise_id,author_user_id) REFERENCES app_user(enterprise_id,id)
);
CREATE INDEX idx_work_log_comment_log ON work_log_comment(enterprise_id,work_log_id,created_at);
