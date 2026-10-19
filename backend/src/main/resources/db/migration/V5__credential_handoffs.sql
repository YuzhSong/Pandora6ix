CREATE TABLE credential_handoff (
 enterprise_id VARCHAR(36) NOT NULL,
 user_id VARCHAR(36) NOT NULL,
 state VARCHAR(16) NOT NULL CHECK(state IN ('AVAILABLE','CHANGED','EXPIRED')),
 encrypted_password VARCHAR(2048),
 hash_fingerprint VARCHAR(64),
 expires_at TIMESTAMP WITH TIME ZONE,
 PRIMARY KEY(enterprise_id,user_id),
 FOREIGN KEY(enterprise_id,user_id) REFERENCES app_user(enterprise_id,id),
 CHECK((state='AVAILABLE' AND encrypted_password IS NOT NULL AND hash_fingerprint IS NOT NULL AND expires_at IS NOT NULL)
    OR (state<>'AVAILABLE' AND encrypted_password IS NULL AND hash_fingerprint IS NULL AND expires_at IS NULL))
);
