# I-ITSM Entity Relationship Diagram

This diagram is derived from JPA entities and Flyway migrations V1–V6. It shows persistent tables only. `BaseEntity` contributes `id`, `created_at`, and `updated_at` to entities that extend it; these common columns are noted in the table descriptions instead of repeated in every entity box.

```mermaid
erDiagram
    USERS {
      BIGINT id PK
      VARCHAR email UK
      VARCHAR password_hash
      VARCHAR full_name
      VARCHAR role
      BOOLEAN active
      TIMESTAMPTZ created_at
      TIMESTAMPTZ updated_at
    }
    INCIDENTS {
      BIGINT id PK
      VARCHAR ticket_number UK
      VARCHAR title
      TEXT description
      VARCHAR status
      VARCHAR priority
      VARCHAR category "legacy snapshot"
      BIGINT category_id FK
      BIGINT service_id FK
      BIGINT reporter_id FK
      BIGINT assignee_id FK
      VARCHAR affected_service
      TIMESTAMPTZ created_at
      TIMESTAMPTZ updated_at
    }
    INCIDENT_CATEGORIES {
      BIGINT id PK
      VARCHAR name
      VARCHAR category_key UK
      BOOLEAN active
      TIMESTAMPTZ created_at
      TIMESTAMPTZ updated_at
    }
    IT_SERVICES {
      BIGINT id PK
      VARCHAR name
      VARCHAR service_key UK
      VARCHAR description
      BOOLEAN active
      TIMESTAMPTZ created_at
      TIMESTAMPTZ updated_at
    }
    INCIDENT_ACTIVITIES {
      BIGINT id PK
      BIGINT incident_id FK
      BIGINT actor_id FK
      VARCHAR activity_type
      TEXT message
      TIMESTAMPTZ created_at
      TIMESTAMPTZ updated_at
    }
    INCIDENT_FEEDBACK {
      BIGINT id PK
      BIGINT incident_id UK "also FK to incidents"
      BIGINT employee_id FK
      INTEGER rating "CHECK 1..5"
      VARCHAR comments
      TIMESTAMPTZ created_at
      TIMESTAMPTZ updated_at
    }
    TECHNICIAN_PROFILES {
      BIGINT id PK
      BIGINT user_id UK "also FK to users"
      BOOLEAN available
      INTEGER experience_years
      INTEGER max_active_workload
      TIMESTAMPTZ created_at
      TIMESTAMPTZ updated_at
    }
    TECHNICIAN_SKILLS {
      BIGINT profile_id PK "also FK to technician_profiles"
      VARCHAR skill PK
    }
    ASSIGNMENT_RECORDS {
      BIGINT id PK
      BIGINT incident_id FK
      BIGINT assigned_to_user_id FK
      BIGINT assigned_by_user_id FK
      VARCHAR strategy_used
      VARCHAR reason
      TIMESTAMPTZ created_at
      TIMESTAMPTZ updated_at
    }
    ASSIGNMENT_CONFIGURATION {
      BIGINT id PK
      VARCHAR strategy_name
    }
    SLA_POLICIES {
      BIGINT id PK
      VARCHAR name UK
      VARCHAR priority UK
      INTEGER response_time_minutes
      INTEGER resolution_time_minutes
      BOOLEAN active
      TIMESTAMPTZ created_at
      TIMESTAMPTZ updated_at
    }
    SLA_TRACKING {
      BIGINT id PK
      BIGINT incident_id UK "also FK to incidents"
      BIGINT policy_id FK
      TIMESTAMPTZ response_deadline
      TIMESTAMPTZ resolution_deadline
      VARCHAR status
      BOOLEAN response_warning_sent
      BOOLEAN resolution_warning_sent
      BOOLEAN response_escalation_sent
      BOOLEAN resolution_escalation_sent
      BOOLEAN response_violation_recorded
      BOOLEAN resolution_violation_recorded
    }
    SLA_HISTORY {
      BIGINT id PK
      BIGINT incident_id FK
      BIGINT tracking_id FK
      VARCHAR action
      VARCHAR description
    }
    NOTIFICATIONS {
      BIGINT id PK
      BIGINT recipient_id FK
      BIGINT incident_id FK
      VARCHAR title
      TEXT message
      VARCHAR channel
      BOOLEAN is_read
      VARCHAR deduplication_key UK
      TIMESTAMPTZ created_at
      TIMESTAMPTZ updated_at
    }
    AUDIT_LOGS {
      BIGINT id PK
      BIGINT incident_id FK
      BIGINT actor_id FK
      VARCHAR action
      VARCHAR previous_state
      VARCHAR new_state
      VARCHAR target_entity
      BIGINT target_id
      TEXT description
      TIMESTAMPTZ created_at
      TIMESTAMPTZ updated_at
    }

    USERS ||--o{ INCIDENTS : reports
    USERS o|--o{ INCIDENTS : assigned_to
    INCIDENT_CATEGORIES o|--o{ INCIDENTS : classifies
    IT_SERVICES o|--o{ INCIDENTS : service_link
    INCIDENTS ||--o{ INCIDENT_ACTIVITIES : records
    USERS ||--o{ INCIDENT_ACTIVITIES : acts
    INCIDENTS ||--o| INCIDENT_FEEDBACK : receives
    USERS ||--o{ INCIDENT_FEEDBACK : submits
    USERS ||--o| TECHNICIAN_PROFILES : has_profile
    TECHNICIAN_PROFILES ||--o{ TECHNICIAN_SKILLS : has_skills
    INCIDENTS ||--o{ ASSIGNMENT_RECORDS : assignment_history
    USERS ||--o{ ASSIGNMENT_RECORDS : assigned_to
    USERS o|--o{ ASSIGNMENT_RECORDS : assigned_by
    INCIDENTS ||--o| SLA_TRACKING : tracked_by
    SLA_POLICIES ||--o{ SLA_TRACKING : governs
    INCIDENTS ||--o{ SLA_HISTORY : has_history
    SLA_TRACKING o|--o{ SLA_HISTORY : produces
    USERS ||--o{ NOTIFICATIONS : receives
    INCIDENTS o|--o{ NOTIFICATIONS : concerns
    INCIDENTS o|--o{ AUDIT_LOGS : subject
    USERS o|--o{ AUDIT_LOGS : actor
```

## Keys, constraints, and interpretation

- Primary and foreign keys follow V1–V4. `users.email`, `incidents.ticket_number`, `incident_categories.category_key`, `technician_profiles.user_id`, `sla_policies.name`, `sla_policies.priority`, `sla_tracking.incident_id`, and `incident_feedback.incident_id` are unique in the migration/entity mapping.
- `technician_skills` has composite primary key `(profile_id, skill)` and a foreign key to `technician_profiles`. This is JPA `@ElementCollection`, not a separate domain entity.
- `incident_feedback.rating` is constrained to 1–5 in V4. Feedback’s one-per-incident rule is database-enforced by a unique incident FK.
- V4 keeps `incidents.category` as the historical/display string, adds nullable `category_id`, links matching legacy values to categories, and adds an index. The category FK is nullable, so an incident can retain an unmatched legacy category without a linked category record.
- V5 adds `it_services`, imports distinct existing `affected_service` labels, and adds nullable `incidents.service_id` plus a foreign key and index. The original text remains for backward compatibility; the association is nullable for legacy incidents.
- V2 adds query indexes; V3 adds SLA escalation state flags. The diagram focuses on relational keys and representative columns; migration files remain authoritative for all columns and indexes.
- `assignment_configuration` stores the selected strategy name, not a foreign key to a strategy row. There is currently one seeded configuration row with `id=1`.
- ER cardinalities mirror unique constraints and nullable columns where present. Relationship lines are database-enforced only where the corresponding FK/unique constraint exists in Flyway. JPA associations or application semantics without a migration FK are not assumed to be enforced.
- No live PostgreSQL migration or Hibernate validation was performed for this documentation task. PostgreSQL compatibility is inferred from PostgreSQL-targeted SQL (`BIGINT GENERATED BY DEFAULT AS IDENTITY`, `TIMESTAMP WITH TIME ZONE`, `UPDATE ... FROM`, and `CREATE INDEX IF NOT EXISTS`); live migration verification remains pending.
- V6 adds nullable `audit_logs.target_entity` and `audit_logs.target_id` plus a target lookup index. These generic target columns intentionally have no foreign key because they can refer to users, IT services, incident categories, feedback, or incidents; the existing `incident_id` association remains separately constrained. Existing audit rows are retained without rewriting or backfilling.
