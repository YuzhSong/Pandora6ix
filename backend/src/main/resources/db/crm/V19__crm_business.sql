CREATE TABLE crm_business (
    id VARCHAR(36) PRIMARY KEY,
    enterprise_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    owner_user_id VARCHAR(36) NOT NULL,
    title VARCHAR(160) NOT NULL,
    kind VARCHAR(24) NOT NULL CHECK(kind IN ('MALL','PERFORMANCE','PRIVATE_DINING','VISIT','SALON','OTHER')),
    status VARCHAR(16) NOT NULL CHECK(status IN ('PLANNED','IN_PROGRESS','COMPLETED','CANCELLED')),
    event_date DATE NOT NULL,
    event_time VARCHAR(5),
    notes VARCHAR(2000),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(enterprise_id,id),
    FOREIGN KEY(enterprise_id,customer_id) REFERENCES crm_customer(enterprise_id,id),
    FOREIGN KEY(enterprise_id,owner_user_id) REFERENCES app_user(enterprise_id,id)
);
CREATE INDEX crm_business_owner_idx ON crm_business(enterprise_id,owner_user_id);
CREATE INDEX crm_business_customer_idx ON crm_business(enterprise_id,customer_id);
CREATE TABLE crm_participant (
    enterprise_id VARCHAR(36) NOT NULL,
    business_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    responsibility VARCHAR(128) NOT NULL,
    PRIMARY KEY(enterprise_id,business_id,user_id),
    FOREIGN KEY(enterprise_id,business_id) REFERENCES crm_business(enterprise_id,id),
    FOREIGN KEY(enterprise_id,user_id) REFERENCES app_user(enterprise_id,id)
);
CREATE INDEX crm_participant_user_idx ON crm_participant(enterprise_id,user_id);
