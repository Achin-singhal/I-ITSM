-- Additive indexes for the repository query paths used by Phase 2.
-- These statements do not alter or remove existing rows.
CREATE INDEX IF NOT EXISTS ix_incidents_reporter_created
    ON incidents (reporter_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_incidents_assignee_status
    ON incidents (assignee_id, status);
CREATE INDEX IF NOT EXISTS ix_assignment_records_incident_created
    ON assignment_records (incident_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_sla_tracking_resolved_response_deadline
    ON sla_tracking (resolved_at, response_deadline);
CREATE INDEX IF NOT EXISTS ix_sla_tracking_resolved_resolution_deadline
    ON sla_tracking (resolved_at, resolution_deadline);
CREATE INDEX IF NOT EXISTS ix_sla_history_incident_created
    ON sla_history (incident_id, created_at);
CREATE INDEX IF NOT EXISTS ix_audit_logs_incident_created
    ON audit_logs (incident_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_audit_logs_actor_created
    ON audit_logs (actor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_notifications_recipient_created
    ON notifications (recipient_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_incident_activities_incident_created
    ON incident_activities (incident_id, created_at);
