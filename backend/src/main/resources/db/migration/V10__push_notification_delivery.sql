CREATE TABLE push_device (
 id VARCHAR(36) PRIMARY KEY,
 enterprise_id VARCHAR(36) NOT NULL,
 user_id VARCHAR(36) NOT NULL,
 installation_id VARCHAR(36) NOT NULL,
 registration_id VARCHAR(256) NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(installation_id),
 FOREIGN KEY(enterprise_id,user_id) REFERENCES app_user(enterprise_id,id)
);
CREATE INDEX ix_push_device_recipient ON push_device(enterprise_id,user_id,active);

ALTER TABLE notification ADD COLUMN event_type VARCHAR(24);
UPDATE notification SET event_type=kind WHERE event_type IS NULL;

CREATE TABLE notification_push_delivery (
 id VARCHAR(36) PRIMARY KEY,
 enterprise_id VARCHAR(36) NOT NULL,
 notification_id VARCHAR(36) NOT NULL,
 push_device_id VARCHAR(36) NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','SENDING','DELIVERED','FAILED','CANCELLED')),
 attempt_count INTEGER NOT NULL DEFAULT 0,
 next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 locked_until TIMESTAMP WITH TIME ZONE,
 delivered_at TIMESTAMP WITH TIME ZONE,
 last_http_status INTEGER,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(notification_id,push_device_id),
 FOREIGN KEY(enterprise_id,notification_id) REFERENCES notification(enterprise_id,id),
 FOREIGN KEY(push_device_id) REFERENCES push_device(id)
);
CREATE INDEX ix_push_delivery_due ON notification_push_delivery(status,next_attempt_at,locked_until);
