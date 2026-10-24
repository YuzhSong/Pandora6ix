CREATE TABLE operation_log (
 id VARCHAR(36) PRIMARY KEY, enterprise_id VARCHAR(36) NOT NULL,
 actor_user_id VARCHAR(36), object_id VARCHAR(36), object_type VARCHAR(64) NOT NULL,
 module VARCHAR(64) NOT NULL, action VARCHAR(64) NOT NULL, source VARCHAR(64) NOT NULL,
 result VARCHAR(24) NOT NULL, summary VARCHAR(512) NOT NULL,
 occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 request_id VARCHAR(128), failure_reason VARCHAR(1000),
 before_values CLOB, after_values CLOB,
 FOREIGN KEY(enterprise_id,actor_user_id) REFERENCES app_user(enterprise_id,id)
);
CREATE INDEX ix_operation_log_page ON operation_log(enterprise_id,occurred_at,id);
CREATE INDEX ix_operation_log_filter ON operation_log(enterprise_id,module,action,result,occurred_at);
