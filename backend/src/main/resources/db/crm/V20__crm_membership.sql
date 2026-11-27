CREATE TABLE crm_membership (
    id VARCHAR(36) PRIMARY KEY,
    enterprise_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    membership_type VARCHAR(80) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CHECK(end_date >= start_date),
    UNIQUE(enterprise_id,id),
    FOREIGN KEY(enterprise_id,customer_id) REFERENCES crm_customer(enterprise_id,id)
);
CREATE INDEX crm_membership_customer_idx ON crm_membership(enterprise_id,customer_id);
CREATE TABLE crm_benefit (
    id VARCHAR(36) PRIMARY KEY,
    enterprise_id VARCHAR(36) NOT NULL,
    membership_id VARCHAR(36) NOT NULL,
    name VARCHAR(128) NOT NULL,
    notes VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(enterprise_id,id),
    FOREIGN KEY(enterprise_id,membership_id) REFERENCES crm_membership(enterprise_id,id)
);
CREATE INDEX crm_benefit_member_idx ON crm_benefit(enterprise_id,membership_id);
CREATE TABLE crm_benefit_use (
    id VARCHAR(36) PRIMARY KEY,
    enterprise_id VARCHAR(36) NOT NULL,
    benefit_id VARCHAR(36) NOT NULL,
    service_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL CHECK(status IN ('SCHEDULED','COMPLETED','CANCELLED')),
    notes VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(enterprise_id,id),
    FOREIGN KEY(enterprise_id,benefit_id) REFERENCES crm_benefit(enterprise_id,id)
);
CREATE INDEX crm_benefit_use_service_idx ON crm_benefit_use(enterprise_id,benefit_id,service_date);
