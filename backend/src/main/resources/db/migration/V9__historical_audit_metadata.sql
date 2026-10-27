ALTER TABLE operation_log ADD COLUMN actor_display_name VARCHAR(128);
ALTER TABLE operation_log ADD COLUMN actor_login_name VARCHAR(128);
ALTER TABLE operation_log ADD COLUMN object_label VARCHAR(512);
ALTER TABLE operation_log ADD COLUMN before_version BIGINT;
ALTER TABLE operation_log ADD COLUMN after_version BIGINT;

ALTER TABLE personnel_change ADD COLUMN actor_display_name VARCHAR(128);
ALTER TABLE personnel_change ADD COLUMN actor_login_name VARCHAR(128);
