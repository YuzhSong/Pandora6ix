-- 提醒记录：企业隔离、本人可见，dedupe_key 承担事件去重（同一用户同一事件只保留一条）
CREATE TABLE notification (
 id VARCHAR(36) PRIMARY KEY,
 enterprise_id VARCHAR(36) NOT NULL,
 user_id VARCHAR(36) NOT NULL,
 kind VARCHAR(24) NOT NULL CHECK(kind IN ('NEW_TASK','DEADLINE','STATUS')),
 task_id VARCHAR(36) NOT NULL,
 assignment_id VARCHAR(36),
 title VARCHAR(200) NOT NULL,
 body VARCHAR(1000) NOT NULL,
 dedupe_key VARCHAR(200) NOT NULL,
 due_at TIMESTAMP WITH TIME ZONE,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 read_at TIMESTAMP WITH TIME ZONE,
 UNIQUE(enterprise_id,id),
 FOREIGN KEY(enterprise_id,user_id) REFERENCES app_user(enterprise_id,id),
 FOREIGN KEY(enterprise_id,task_id) REFERENCES task(enterprise_id,id)
);
CREATE UNIQUE INDEX ux_notification_dedupe ON notification(enterprise_id,user_id,dedupe_key);
CREATE INDEX ix_notification_user ON notification(enterprise_id,user_id,created_at DESC);
