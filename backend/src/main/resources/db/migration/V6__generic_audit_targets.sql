-- Keep the existing incident/actor references and add a generic subject reference
-- for user, service, category, and feedback audit events.
ALTER TABLE audit_logs ADD COLUMN target_entity VARCHAR(64);
ALTER TABLE audit_logs ADD COLUMN target_id BIGINT;
CREATE INDEX ix_audit_target_created ON audit_logs(target_entity, target_id, created_at DESC);
