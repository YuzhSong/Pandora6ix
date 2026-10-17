ALTER TABLE app_user ADD COLUMN gender VARCHAR(6) CHECK (gender IN ('MALE','FEMALE'));
ALTER TABLE app_user ADD COLUMN id_number VARCHAR(18);
ALTER TABLE app_user ADD COLUMN phone_number VARCHAR(24);
