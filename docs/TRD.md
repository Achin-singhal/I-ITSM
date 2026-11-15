# Technical Design Report — I-ITSM

**Status:** repository implementation summary. It is not an official professor-issued TRD. See [requirement traceability](REQUIREMENT_TRACEABILITY.md).

## System structure

The system is a modular monolith. The React 18/Vite 5/Tailwind client calls `/api/v1` through Fetch. Spring Boot 3.3.5 on Java 21 hosts cohesive `auth`, `user`, `incident`, `assignment`, `sla`, `notification`, `audit`, `intelligence`, `report`, and `common` packages. PostgreSQL is the configured persistent database; Flyway owns schema changes and Hibernate runs in `validate` mode.

## Main request flows

### Authentication and authorization

Registration creates an EMPLOYEE and stores a BCrypt hash. Login issues an HMAC-signed JWT with identity and role claims. `JwtAuthenticationFilter` verifies the token and checks current account identity/role/activity against the database. Controller/method rules gate administrator, technician and employee operations. Incident queries enforce reporter ownership for EMPLOYEE requests and limit technicians to assigned incidents plus unassigned queue items; technicians cannot inspect incidents assigned to a different technician.

### Incident and assignment

`IncidentController` validates request DTOs and delegates to `IncidentService`. The service calculates priority, persists the incident, starts SLA tracking and writes audit/notification events. `AssignmentService` resolves a strategy through a factory, calculates current workload from incident records, selects an eligible profile and persists assignment history. Assignment transactions lock incident/profile rows to serialize competing writes.

### SLA, audit and notification

`SlaService` creates tracking from the active priority policy. A scheduled transaction locks unresolved tracking rows, evaluates each target and persists one-time flags, SLA history, audit entries and notifications. Incident-event observers receive lifecycle events; in-app records are persisted and the email implementation only logs simulated delivery.

### Intelligence

`IncidentSimilarityService` normalizes text and calculates weighted Jaccard/equality/time scores for duplicate and correlation suggestions. It returns candidates for human review; no records are merged. `RuleBasedTriageEngine` exists as an implementation of the triage abstraction but is not exposed as an API.

### Reporting

`ReportService` uses repository count/group/average queries for summaries and distributions. Search applies role filters and `Pageable`; entity graphs load reporter/assignee data for rows. `ReportExportService` streams CSV, pages incident results in batches of 500, and escapes cells/formula prefixes.

## Database and migrations

Migrations are V1 baseline/additive schema, V2 query indexes and V3 SLA escalation flags. No Phase 5/6 schema migration was added. Blank H2 PostgreSQL-mode tests apply all migrations and Hibernate validates the test schema. A migration’s `CREATE TABLE IF NOT EXISTS` does not retrofit every omitted constraint onto a pre-existing table; existing production schemas still require a DDL/history comparison. Live PostgreSQL validation was not performed.

## Security and operational notes

- `JWT_SECRET` is required and must be at least 32 bytes; production credentials are process environment values.
- CORS is limited to configured origins, with localhost defaults intended for development.
- CSV exports and reports are admin-restricted; technician self-report is scoped to the caller.
- Audit records are append-only by application convention, but DB permissions/triggers do not guarantee tamper evidence.
- SLA time is elapsed time, not a working-hours calendar.
- No network AI, external email, rate limiting, token revocation list or browser automated test harness is included.
- Database session timezone affects the displayed calendar day in trend grouping.
