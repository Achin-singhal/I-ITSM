-- Persist one escalation-notice marker per SLA deadline. Existing SLA state and
-- history are preserved; the flags default to false for already tracked incidents.
ALTER TABLE sla_tracking ADD COLUMN IF NOT EXISTS response_escalation_sent BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sla_tracking ADD COLUMN IF NOT EXISTS resolution_escalation_sent BOOLEAN NOT NULL DEFAULT FALSE;
