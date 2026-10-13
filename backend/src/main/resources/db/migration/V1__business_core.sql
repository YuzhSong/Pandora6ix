CREATE TABLE enterprise (
 id VARCHAR(36) PRIMARY KEY, login_domain VARCHAR(128) NOT NULL UNIQUE,
 name VARCHAR(128) NOT NULL, status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
 business_timezone VARCHAR(64), version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE org_unit (
 id VARCHAR(36) PRIMARY KEY, enterprise_id VARCHAR(36) NOT NULL REFERENCES enterprise(id),
 name VARCHAR(128) NOT NULL, kind VARCHAR(24) NOT NULL CHECK(kind IN ('COMPANY','DEPARTMENT','TEAM')),
 parent_id VARCHAR(36), version BIGINT NOT NULL DEFAULT 0,
 UNIQUE(enterprise_id,id), FOREIGN KEY(enterprise_id,parent_id) REFERENCES org_unit(enterprise_id,id)
);
CREATE TABLE app_user (
 id VARCHAR(36) PRIMARY KEY, enterprise_id VARCHAR(36) NOT NULL REFERENCES enterprise(id),
 login_name VARCHAR(128) NOT NULL, password_hash VARCHAR(255) NOT NULL, display_name VARCHAR(128) NOT NULL,
 org_unit_id VARCHAR(36), supervisor_user_id VARCHAR(36), job_title VARCHAR(128),
 status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','DISABLED')), version BIGINT NOT NULL DEFAULT 0,
 UNIQUE(enterprise_id,id), UNIQUE(enterprise_id,login_name),
 FOREIGN KEY(enterprise_id,org_unit_id) REFERENCES org_unit(enterprise_id,id),
 FOREIGN KEY(enterprise_id,supervisor_user_id) REFERENCES app_user(enterprise_id,id)
);
CREATE TABLE role (code VARCHAR(32) PRIMARY KEY);
INSERT INTO role VALUES ('ADMIN'),('FOUNDER'),('DEPARTMENT_MANAGER'),('TEAM_LEAD'),('EMPLOYEE');
CREATE TABLE user_role (
 enterprise_id VARCHAR(36) NOT NULL, user_id VARCHAR(36) NOT NULL, role_code VARCHAR(32) NOT NULL REFERENCES role(code),
 PRIMARY KEY(enterprise_id,user_id,role_code), FOREIGN KEY(enterprise_id,user_id) REFERENCES app_user(enterprise_id,id)
);
CREATE TABLE auth_session (
 token_hash VARCHAR(64) PRIMARY KEY, enterprise_id VARCHAR(36) NOT NULL, user_id VARCHAR(36) NOT NULL,
 expires_at TIMESTAMP WITH TIME ZONE NOT NULL, revoked BOOLEAN NOT NULL DEFAULT FALSE,
 FOREIGN KEY(enterprise_id,user_id) REFERENCES app_user(enterprise_id,id)
);
CREATE TABLE enterprise_tag (
 id VARCHAR(36) PRIMARY KEY, enterprise_id VARCHAR(36) NOT NULL REFERENCES enterprise(id),
 name VARCHAR(64) NOT NULL, color_hex VARCHAR(7) NOT NULL, creator_user_id VARCHAR(36) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0, UNIQUE(enterprise_id,id),
 FOREIGN KEY(enterprise_id,creator_user_id) REFERENCES app_user(enterprise_id,id)
);
CREATE TABLE task (
 id VARCHAR(36) PRIMARY KEY, enterprise_id VARCHAR(36) NOT NULL, creator_user_id VARCHAR(36) NOT NULL,
 task_kind VARCHAR(24) NOT NULL CHECK(task_kind IN ('PERSONAL','DELEGATED')), title VARCHAR(255) NOT NULL,
 description CLOB, priority_level INT NOT NULL CHECK(priority_level BETWEEN 1 AND 5),
 start_date DATE NOT NULL, end_date DATE NOT NULL, time_kind VARCHAR(24) NOT NULL CHECK(time_kind IN ('ALL_DAY','TIMED')),
 start_minute INT, end_minute INT, review_status VARCHAR(24) NOT NULL DEFAULT 'PENDING' CHECK(review_status IN ('PENDING','APPROVED','REJECTED')),
 primary_tag_id VARCHAR(36), version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(enterprise_id,id), CHECK(end_date >= start_date),
 CHECK((time_kind='ALL_DAY' AND start_minute IS NULL AND end_minute IS NULL) OR (time_kind='TIMED' AND start_minute BETWEEN 0 AND 1439 AND end_minute BETWEEN 0 AND 1439)),
 FOREIGN KEY(enterprise_id,creator_user_id) REFERENCES app_user(enterprise_id,id),
 FOREIGN KEY(enterprise_id,primary_tag_id) REFERENCES enterprise_tag(enterprise_id,id)
);
CREATE TABLE task_tag (
 enterprise_id VARCHAR(36) NOT NULL, task_id VARCHAR(36) NOT NULL, tag_id VARCHAR(36) NOT NULL,
 PRIMARY KEY(enterprise_id,task_id,tag_id), FOREIGN KEY(enterprise_id,task_id) REFERENCES task(enterprise_id,id),
 FOREIGN KEY(enterprise_id,tag_id) REFERENCES enterprise_tag(enterprise_id,id)
);
CREATE TABLE task_review (
 id VARCHAR(36) PRIMARY KEY, enterprise_id VARCHAR(36) NOT NULL, task_id VARCHAR(36) NOT NULL,
 reviewer_user_id VARCHAR(36) NOT NULL, submitted_by_user_id VARCHAR(36) NOT NULL, task_version BIGINT NOT NULL,
 submitted_snapshot CLOB NOT NULL, decision VARCHAR(24) NOT NULL DEFAULT 'PENDING', attempt_no INT NOT NULL DEFAULT 1,
 submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 UNIQUE(enterprise_id,id), UNIQUE(enterprise_id,task_id,attempt_no),
 FOREIGN KEY(enterprise_id,task_id) REFERENCES task(enterprise_id,id),
 FOREIGN KEY(enterprise_id,reviewer_user_id) REFERENCES app_user(enterprise_id,id),
 FOREIGN KEY(enterprise_id,submitted_by_user_id) REFERENCES app_user(enterprise_id,id)
);
CREATE TABLE task_assignment (
 id VARCHAR(36) PRIMARY KEY, enterprise_id VARCHAR(36) NOT NULL, task_id VARCHAR(36) NOT NULL, recipient_user_id VARCHAR(36) NOT NULL,
 execution_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_APPROVAL' CHECK(execution_status IN ('PENDING_APPROVAL','ACTIVE','AWAITING_ACCEPTANCE','COMPLETED')),
 progress_percent INT NOT NULL DEFAULT 0 CHECK(progress_percent BETWEEN 0 AND 100),
 activated_at TIMESTAMP WITH TIME ZONE, completion_source VARCHAR(32), completed_by_user_id VARCHAR(36),
 completed_at TIMESTAMP WITH TIME ZONE, version BIGINT NOT NULL DEFAULT 0,
 UNIQUE(enterprise_id,id), UNIQUE(enterprise_id,task_id,recipient_user_id),
 CHECK((execution_status='COMPLETED' AND completion_source IS NOT NULL AND completed_by_user_id IS NOT NULL AND completed_at IS NOT NULL) OR
       (execution_status<>'COMPLETED' AND completion_source IS NULL AND completed_by_user_id IS NULL AND completed_at IS NULL)),
 FOREIGN KEY(enterprise_id,task_id) REFERENCES task(enterprise_id,id),
 FOREIGN KEY(enterprise_id,recipient_user_id) REFERENCES app_user(enterprise_id,id),
 FOREIGN KEY(enterprise_id,completed_by_user_id) REFERENCES app_user(enterprise_id,id)
);
CREATE TABLE task_progress_record (
 id VARCHAR(36) PRIMARY KEY, enterprise_id VARCHAR(36) NOT NULL, assignment_id VARCHAR(36) NOT NULL,
 actor_user_id VARCHAR(36) NOT NULL, progress_percent INT NOT NULL CHECK(progress_percent BETWEEN 0 AND 100), note CLOB,
 recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(enterprise_id,assignment_id) REFERENCES task_assignment(enterprise_id,id),
 FOREIGN KEY(enterprise_id,actor_user_id) REFERENCES app_user(enterprise_id,id)
);
CREATE TABLE task_completion_request (
 id VARCHAR(36) PRIMARY KEY, enterprise_id VARCHAR(36) NOT NULL, assignment_id VARCHAR(36) NOT NULL,
 requested_by_user_id VARCHAR(36) NOT NULL, submitted_note CLOB, requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 decision VARCHAR(24) NOT NULL DEFAULT 'PENDING', decided_by_user_id VARCHAR(36), decision_note CLOB,
 decided_at TIMESTAMP WITH TIME ZONE, version BIGINT NOT NULL DEFAULT 0,
 UNIQUE(enterprise_id,id), FOREIGN KEY(enterprise_id,assignment_id) REFERENCES task_assignment(enterprise_id,id),
 FOREIGN KEY(enterprise_id,requested_by_user_id) REFERENCES app_user(enterprise_id,id),
 FOREIGN KEY(enterprise_id,decided_by_user_id) REFERENCES app_user(enterprise_id,id)
);
CREATE TABLE work_log (
 id VARCHAR(36) PRIMARY KEY, enterprise_id VARCHAR(36) NOT NULL, author_user_id VARCHAR(36) NOT NULL, work_date DATE NOT NULL,
 primary_tag_id VARCHAR(36), published_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 UNIQUE(enterprise_id,id), UNIQUE(enterprise_id,author_user_id,id), UNIQUE(enterprise_id,author_user_id,work_date),
 FOREIGN KEY(enterprise_id,author_user_id) REFERENCES app_user(enterprise_id,id),
 FOREIGN KEY(enterprise_id,primary_tag_id) REFERENCES enterprise_tag(enterprise_id,id)
);
CREATE TABLE work_log_entry (
 id VARCHAR(36) PRIMARY KEY, enterprise_id VARCHAR(36) NOT NULL, author_user_id VARCHAR(36) NOT NULL, work_log_id VARCHAR(36) NOT NULL,
 client_entry_key VARCHAR(128) NOT NULL, content CLOB NOT NULL, display_order INT NOT NULL,
 recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 UNIQUE(enterprise_id,author_user_id,id), UNIQUE(enterprise_id,work_log_id,client_entry_key),
 FOREIGN KEY(enterprise_id,author_user_id,work_log_id) REFERENCES work_log(enterprise_id,author_user_id,id)
);
CREATE TABLE work_log_tag (
 enterprise_id VARCHAR(36) NOT NULL, work_log_id VARCHAR(36) NOT NULL, tag_id VARCHAR(36) NOT NULL,
 PRIMARY KEY(enterprise_id,work_log_id,tag_id), FOREIGN KEY(enterprise_id,work_log_id) REFERENCES work_log(enterprise_id,id),
 FOREIGN KEY(enterprise_id,tag_id) REFERENCES enterprise_tag(enterprise_id,id)
);
CREATE TABLE company_important_item (
 id VARCHAR(36) PRIMARY KEY, enterprise_id VARCHAR(36) NOT NULL, publisher_user_id VARCHAR(36) NOT NULL,
 title VARCHAR(255) NOT NULL, content CLOB NOT NULL, display_order INT NOT NULL DEFAULT 0, version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(enterprise_id,id), FOREIGN KEY(enterprise_id,publisher_user_id) REFERENCES app_user(enterprise_id,id)
);
CREATE TABLE personal_important_selection_state (
 enterprise_id VARCHAR(36) NOT NULL, owner_user_id VARCHAR(36) NOT NULL, has_manual_selection BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0,
 PRIMARY KEY(enterprise_id,owner_user_id), FOREIGN KEY(enterprise_id,owner_user_id) REFERENCES app_user(enterprise_id,id)
);
CREATE TABLE personal_important_selection (
 enterprise_id VARCHAR(36) NOT NULL, owner_user_id VARCHAR(36) NOT NULL, entry_id VARCHAR(36) NOT NULL, display_order INT NOT NULL,
 PRIMARY KEY(enterprise_id,owner_user_id,entry_id),
 FOREIGN KEY(enterprise_id,owner_user_id) REFERENCES personal_important_selection_state(enterprise_id,owner_user_id),
 FOREIGN KEY(enterprise_id,owner_user_id,entry_id) REFERENCES work_log_entry(enterprise_id,author_user_id,id)
);
CREATE TABLE api_idempotency (
 enterprise_id VARCHAR(36) NOT NULL, actor_user_id VARCHAR(36) NOT NULL, operation VARCHAR(200) NOT NULL, idempotency_key VARCHAR(128) NOT NULL,
 request_hash VARCHAR(64) NOT NULL, response_json CLOB NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(enterprise_id,actor_user_id,operation,idempotency_key),
 FOREIGN KEY(enterprise_id,actor_user_id) REFERENCES app_user(enterprise_id,id)
);
CREATE INDEX ix_task_range ON task(enterprise_id,start_date,end_date);
CREATE INDEX ix_assignment_recipient ON task_assignment(enterprise_id,recipient_user_id,task_id);
CREATE INDEX ix_journal_date ON work_log(enterprise_id,author_user_id,work_date);
CREATE INDEX ix_entry_journal ON work_log_entry(enterprise_id,work_log_id,display_order);
CREATE TABLE enterprise_initialization_lock (id INT PRIMARY KEY);
INSERT INTO enterprise_initialization_lock VALUES (1);
CREATE TABLE enterprise_initialization_replay (
 scope_hash VARCHAR(64) NOT NULL, idempotency_key VARCHAR(128) NOT NULL,
 request_hash VARCHAR(64) NOT NULL, response_json CLOB NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(scope_hash,idempotency_key)
);
