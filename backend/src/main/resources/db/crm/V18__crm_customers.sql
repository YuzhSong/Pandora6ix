CREATE TABLE crm_customer (
    id VARCHAR(36) PRIMARY KEY,
    enterprise_id VARCHAR(36) NOT NULL,
    owner_user_id VARCHAR(36) NOT NULL,
    name VARCHAR(128) NOT NULL,
    kind VARCHAR(16) NOT NULL CHECK(kind IN ('PERSON','COMPANY')),
    category VARCHAR(64),
    contact VARCHAR(255),
    notes VARCHAR(2000),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(enterprise_id,id),
    FOREIGN KEY(enterprise_id,owner_user_id) REFERENCES app_user(enterprise_id,id)
);
CREATE INDEX crm_customer_owner_idx ON crm_customer(enterprise_id,owner_user_id);
