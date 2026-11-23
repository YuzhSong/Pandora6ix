CREATE TABLE crm_employment_contract (
    id VARCHAR(36) PRIMARY KEY,
    enterprise_id VARCHAR(36) NOT NULL,
    employee_user_id VARCHAR(36) NOT NULL,
    title VARCHAR(128) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    notes VARCHAR(2000),
    attachment_id VARCHAR(36),
    attachment_name VARCHAR(180),
    version BIGINT NOT NULL DEFAULT 0,
    CHECK(end_date >= start_date),
    CHECK((attachment_id IS NULL AND attachment_name IS NULL)
        OR (attachment_id IS NOT NULL AND attachment_name IS NOT NULL)),
    UNIQUE(enterprise_id,id),
    FOREIGN KEY(enterprise_id,employee_user_id) REFERENCES app_user(enterprise_id,id)
);
CREATE INDEX crm_contract_expiry_idx ON crm_employment_contract(enterprise_id,end_date);
CREATE INDEX crm_contract_employee_idx ON crm_employment_contract(enterprise_id,employee_user_id);
