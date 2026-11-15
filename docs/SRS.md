# Software Requirements Specification — I-ITSM

**Status:** implementation-aligned project snapshot, not a substitute for the unavailable professor-approved SRS. See [requirement traceability](REQUIREMENT_TRACEABILITY.md) for the source limitation.

## Purpose and scope

I-ITSM is a browser-based IT incident workflow application. It supports role-authenticated reporting, a managed IT service catalog, incident lifecycle processing, technician routing, SLA tracking, notifications, audit review, local incident similarity suggestions and administrator reports. It is a single-deployment modular monolith.

## Actors

- **Employee:** registers as an employee, reports and updates owned incidents, views their status/activity/SLA information, responds to activities, verifies resolution, and reads personal notifications.
- **Technician:** views operational incidents, receives assignments, acknowledges and investigates assigned incidents, records notes, resolves incidents, and views assignment/SLA/correlation information.
- **Administrator:** manages assignment settings and technician profiles, configures SLA policies, reviews incidents and audit entries, and accesses analytics and exports.
- **System monitor:** periodically evaluates unresolved SLA deadlines and persists warning, escalation and breach events.

## Functional capabilities present in code

1. Register and authenticate users with signed JWTs; role assignment during public registration is fixed to EMPLOYEE.
2. Create, update, search, view, assign and transition incident records with server-side validation, employee ownership enforcement and technician scoping to assigned work/unassigned queue items.
3. Calculate priority from impact, urgency, business criticality, affected-user band and service criticality.
4. Recommend/rank technicians using selected assignment strategies and eligible profile skills, current workload, availability and capacity.
5. Track per-priority response/resolution deadlines, warnings, escalations, violations and SLA history.
6. Persist in-app notifications and audit events from incident activity; email delivery is simulated.
7. Calculate local duplicate and related-incident suggestions for human review.
8. Provide administrator analytics, technician workload reports, filtered/paged incident search, and authorization-aware CSV endpoints.
9. Manage persistent IT services through administrator-only catalog operations; allow authenticated users to select active services for incidents while preserving historical free-text service values.

## Nonfunctional properties observable in implementation

- **Security:** BCrypt password storage, environment-supplied JWT secret, stateless bearer authentication, role restrictions and employee ownership checks.
- **Persistence:** Spring Data JPA over PostgreSQL; Flyway migrations; Hibernate schema validation. Tests use H2 in PostgreSQL compatibility mode.
- **Architecture:** React/Vite/Tailwind frontend and Spring Boot 3.3.5/Java 21 modular monolith.
- **Performance:** analytics use database aggregates; incident report export uses bounded pages; report tables and incident results are pageable.
- **Availability/operation:** no external AI or notification provider is required; external email is not implemented.

## Constraints and known limits

- Exact professor-statement requirements are unverified until the supplied PDF can be read.
- No PRS/TRD source documents were found; this SRS records behavior evidenced by code and repository documentation.
- No general role/user administration API, push notification transport, or external email delivery.
- PostgreSQL migrations have not been executed against a PostgreSQL instance in this environment.
- Full feature and role acceptance coverage is provided by backend integration tests, not a browser automation suite.
