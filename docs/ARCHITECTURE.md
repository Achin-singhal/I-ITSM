# I-ITSM Architecture Document
**System**: Intelligent IT Service Management and Incident Resolution System  
**Pattern**: Modular Monolith  
**Backend**: Java 21 + Spring Boot 3.3.x + Spring Data JPA + Spring Security + Maven  
**Frontend**: React (Vite) + Tailwind CSS + shadcn/ui Design Pattern  
**Database**: PostgreSQL (Hosted on Supabase)

---

## 1. Architectural Philosophy: Modular Monolith

I-ITSM is structured as a **Modular Monolith**. Rather than introducing distributed network overhead, distributed transactions, and operational complexity typical of microservices, the system groups domain capabilities into well-encapsulated Java packages within a single deployable artifact.

```
       +-------------------------------------------------------------+
       |                  React Frontend (Vite)                      |
       |  (Tailwind CSS components with application page state)       |
       +------------------------------+------------------------------+
                                      | HTTP / REST (JSON)
                                      v
       +-------------------------------------------------------------+
       |             Spring Boot Modular Monolith (Port 8080)        |
       |                                                             |
       |  +--------------------+   +-------------------------------+ |
       |  |     auth / user    |   |     incident / assignment     | |
       |  +--------------------+   +-------------------------------+ |
       |                                                             |
       |  +--------------------+   +-------------------------------+ |
       |  |      sla / audit   |   |   notification / intelligence | |
       |  +--------------------+   +-------------------------------+ |
       |                                                             |
       |  +--------------------------------------------------------+ |
       |  |                     report / common                    | |
       |  +--------------------------------------------------------+ |
       +------------------------------+------------------------------+
                                      | JDBC / JPA (Hibernate)
                                      v
       +-------------------------------------------------------------+
       |               PostgreSQL Database (Supabase)                |
       +-------------------------------------------------------------+
```

### Key Principles:
1. **Module Cohesion**: Capabilities are grouped by domain package in one process. Some services directly use entities/repositories from neighboring modules, so package boundaries are conventional rather than enforced by separate build modules.
2. **Encapsulation**: Internal business logic remains package-private where practical; public APIs are exposed via Services and REST Controllers.
3. **Auditability & Observability**: Every state-changing incident transition triggers domain events captured by audit logs and notification observers.
4. **Resilience**: Independent layer execution allows the frontend to run standalone in dev mode with mock/fallback health indicators, while the backend provides explicit health telemetry.

---

## 2. Package Structure & Module Ownership

The system is developed collaboratively by 4 students. Ownership is cleanly divided across cohesive domain boundaries:

| Module | Primary Responsibility | Key Design Patterns Used | Student Owner |
| :--- | :--- | :--- | :--- |
| **`auth` & `user`** | JWT authentication, role-based authorization (ADMIN, TECHNICIAN, EMPLOYEE), user profiles. Legacy MANAGER/AGENT values remain readable. | Security Filter Chain, Encapsulation | **Student 1 (Lead)** |
| **`incident` & `audit`** | Incident ticket lifecycle, state transitions and persisted audit history. | State/Lifecycle encapsulation, MappedSuperclass Inheritance | **Student 2** |
| **`assignment` & `sla`** | Dynamic ticket assignment algorithms, SLA threshold calculation, breach tracking, escalation policies. | **Strategy Pattern** (Routing Algorithms), Factory Pattern | **Student 3** |
| **`notification`, `intelligence`, `report`** | Event-triggered in-app alerts with simulated email logging, local rule/similarity features and metrics aggregation. | **Observer Pattern** (Event Notification), Strategy/Polymorphism | **Student 4** |

---

## 3. Object-Oriented Design (OOP) Application

I-ITSM actively demonstrates core Java OOP principles:

### A. Abstraction & Interfaces
- **`AssignmentStrategy`**: Defines a recommendation strategy over `TechnicianCandidate` entries, including ordered ranking and a selected profile.
- **`IncidentEventObserver`**: Defines contract `void onIncidentEvent(IncidentEvent event)`.
- **`IncidentTriageEngine`**: Defines contract `TriageSuggestion analyze(Incident incident)`.

### B. Encapsulation
- Domain entities (such as `Incident`, `User`, `SlaPolicy`) maintain private instance fields with validated mutators.
- State transitions occur through expressive domain methods (e.g., `incident.markResolved(agentUser, resolutionNotes)`) rather than arbitrary setter mutations.

### C. Inheritance
- **`BaseEntity`** (`@MappedSuperclass`): Provides common attributes (`id`, `createdAt`, and `updatedAt`) to its entities. It does not currently declare an optimistic-lock `version` field.

### D. Polymorphism
- The assignment routing engine executes `strategy.assign(...)` at runtime, agnostic to whether the concrete class is `RoundRobinAssignmentStrategy` or `WorkloadAssignmentStrategy`.
- The notification publisher dispatches updates to a collection of `IncidentEventObserver` implementations (`EmailNotificationObserver`, `InAppNotificationObserver`) uniformly.

---

## 4. Key Design Patterns Detail

### 1. Strategy Pattern (`com.iitsm.assignment.strategy`)
Used for ticket distribution among available agents.

```
                    +---------------------------+
                    |    <<AssignmentStrategy>>  |
                    +---------------------------+
                    | + assign(incident, agents)|
                    +-------------+-------------+
                                  ^
         +------------------------+------------------------+
         |                                                 |
+----------------------------------+     +----------------------------------+
|   RoundRobinAssignmentStrategy   |     |    WorkloadAssignmentStrategy    |
+----------------------------------+     +----------------------------------+
| Selects next agent cyclically    |     | Selects agent with least active  |
+----------------------------------+     +----------------------------------+
```

### 2. Observer Pattern (`com.iitsm.notification.observer`)
Used for decoupled reactive updates when incident state changes occur.

```
+----------------------------+             +-------------------------------+
|   NotificationPublisher    |             |    <<IncidentEventObserver>>  |
+----------------------------+             +-------------------------------+
| - observers: List<Observer>| <>--------> | + onIncidentEvent(event)      |
| + subscribe(observer)      |             +---------------+---------------+
| + notifyObservers(event)   |                             ^
+----------------------------+                             |
                                  +------------------------+------------------------+
                                  |                                                 |
                  +--------------------------------+               +--------------------------------+
                  |    EmailNotificationObserver   |               |    InAppNotificationObserver   |
                  +--------------------------------+               +--------------------------------+
                  | Sends external dispatch logs   |               | Persists in-app alert entity   |
                  +--------------------------------+               +--------------------------------+
```

---

## 5. Security & Secret Management

- **Zero Hardcoded Secrets**: All sensitive values (DB host, user, password, JWT secret) are injected exclusively through environment variables (`DB_URL`, `DB_PASSWORD`, `JWT_SECRET`).
- **Spring Security 6.x**: Stateless JWT Bearer token authentication filter with RBAC endpoint protection.
- **CORS Management**: Configured via Spring Web MVC filter permitting only authorized frontend domains.

## 6. Phase 2 Core Business Logic

### Priority calculation

`PriorityCalculationStrategy` is resolved through `PriorityStrategyFactory`. The default `WEIGHTED` strategy scores each factor from 0 to 100 and combines impact (25%), urgency (25%), business criticality (25%), affected-user band (15%), and affected-service criticality (10%). The affected-user bands are 0, 20, 40, 60, 80 and 100 for 0, 1-5, 6-20, 21-100, 101-500 and more than 500 users. Thresholds are configurable with `PRIORITY_THRESHOLD_P1/P2/P3` (defaults 80/60/35); weights are configurable with `PRIORITY_WEIGHT_*` and must sum to 1. The score, selected priority and factor explanation are stored on the incident. Priority is recalculated when these inputs change.

Priority mapping is P1=CRITICAL, P2=HIGH, P3=MEDIUM and P4=LOW. These default weights and bands are an explicit MVP policy, not a learned classifier.

### Technician assignment

Technicians have persisted availability, skill tags, experience years and maximum active workload. `SKILL_BASED` filters to available technicians whose skills match the incident category (or `*`), then ranks by experience, active workload and user ID. `WORKLOAD_BALANCED` ranks eligible technicians by database-counted active workload, then experience and user ID. Both exclude inactive users and technicians at capacity. The selected strategy is stored in `assignment_configuration`; assignment records, audit rows and notifications are written with the assignment transaction.

### SLA tracking and audit

Flyway migration `V1__phase2_core_business_schema.sql` creates baseline tables and adds listed columns without dropping tables or rows. Flyway is configured to baseline non-empty schemas at version 0; `CREATE TABLE IF NOT EXISTS` does not retrofit all defined constraints onto a pre-existing table, so existing schemas require DDL/history review. Hibernate validates the migrated schema. New incidents receive the active policy matching their calculated priority. Response and resolution deadlines are elapsed-time targets; the scheduled monitor checks every 30 seconds by default, warns within the final 20% of a target, and records each warning and violation once. A separate SLA history table records policy selection/changes, acknowledgements, resolutions, warnings and violations.

Incident state changes and assignment, activity, audit, SLA and in-app notification writes share the transaction that performs the operation. Administrative user, IT service, incident category and employee feedback audit entries are also written in the transaction that performs each action. Each audit entry records the actor when available, action, target type and ID, timestamp, and safe change details. Administrators can filter by incident, actor, action, target type, target ID and time range. Audit descriptions deliberately omit credentials, password hashes, JWTs and feedback comments. The API is append-only, but the database does not enforce tamper-proof immutability.

### Notifications and lifecycle

The existing Observer Pattern publisher dispatches persisted in-app notifications and simulated email events. Simulated email is log output only; no SMTP or external mail delivery is configured. In-app notifications are stored with a unique deduplication key and users can list their own history and mark their own notifications read.

The current lifecycle is `NEW -> CLASSIFIED -> ASSIGNED -> ACKNOWLEDGED -> IN_PROGRESS -> RESOLVED -> VERIFIED -> CLOSED`. Creation classifies immediately; assignment selects a technician through the configured strategy. Only the assigned technician can acknowledge and investigate/resolve, the reporting employee can verify, and an administrator closes. Legacy `OPEN` database rows remain readable and can proceed through the lifecycle rules.

## Phase 4: Local intelligence and automatic escalation

### Duplicate and related-incident signals

`IncidentSimilarityService` is a local application service; there is no external AI dependency. It normalizes Unicode text with NFKC, lowercases it, converts punctuation to token separators, and compares distinct token sets with Jaccard similarity. Duplicate matching weights title 35%, description 40%, category equality 15%, and affected-service token overlap 10%. The configurable default threshold is `0.62`; the engine returns score and matching reasons and omits `CLOSED` incidents. An employee can request suggestions but receives only the requested match metadata (incident ID, ticket number, state, score, reasons). The creation form asks the user to review suggestions and explicitly continue if they want to create a separate incident.

Related incidents use an independent score: combined title/description 50%, category 20%, affected service 20%, and temporal proximity 10%, restricted to a configurable default 14-day window and threshold `0.35`. These results are ephemeral suggestions and are not stored as relationships. This avoids altering incident data and keeps the first version free of a correlation table; all candidate incidents are currently read from the incident repository, which is suitable for the project dataset but should be bounded or indexed for large deployments.

### Ranked technician assignment

The existing `AssignmentStrategy` abstraction now also returns an ordered ranking. Workload-balanced ranking first prefers a matching category skill (or wildcard), then lower database-counted active workload, recorded experience, and stable user ID. Skill-based ranking requires a category or wildcard skill and orders eligible technicians by experience, workload, and user ID. Round robin exposes its rotated order. All strategies apply the shared availability, active-account, and workload-capacity eligibility rule. The ranked endpoint adds human-readable reasons. The selected-technician API rechecks eligibility under the same incident and candidate row locks before persisting assignment, history, audit, and observer notifications.

### SLA escalation

The existing scheduled `SlaService.monitorDeadlines` path now records warning and escalation levels. Defaults are warning at 20% of the target remaining and escalation at 10%; both are environment-configurable and validated at startup. A missed response or resolution target is a breach escalation. SLA history actions, audit rows, and Observer-published in-app notifications are written within the monitor transaction. Existing pessimistic locks serialize tracking events across application instances, and V3 adds persisted once-only escalation-warning markers. Existing violation markers guard the breach events. The current target clock measures elapsed time, not business hours, holidays, or support calendars.

Configuration variables: `DUPLICATE_SIMILARITY_THRESHOLD`, `RELATED_INCIDENT_THRESHOLD`, `RELATED_INCIDENT_WINDOW_DAYS`, `SLA_WARNING_THRESHOLD_PERCENT`, `SLA_ESCALATION_THRESHOLD_PERCENT`, and `SLA_MONITOR_INTERVAL_MS`. Values are mapped under `app.intelligence` and `app.sla` in `application.yml`.

Phase 4 APIs are documented in [API_CONTRACT.md](API_CONTRACT.md). `V3__phase4_sla_escalation_flags.sql` is additive: it appends two non-null boolean markers with false defaults to `sla_tracking`; it does not drop tables or rewrite incident rows. Related-suggestion results are computed on read and are not persisted.

## Phase 5: Reporting and search

`ReportController` restricts dashboard, aggregate, technician directory and CSV endpoints to administrators; a technician can access only the self-report route. Database-side grouped counts, daily trends, SLA aggregates and average resolution durations avoid loading every incident for the dashboard. Technician workload aggregates current active assignments plus date-filtered assigned/resolved totals. Incident search applies optional ID/text/status/priority/category/assignee/date/SLA filters and page bounds; employee queries are restricted to their reporter ID. CSV incident output reads bounded pages and protects against spreadsheet formula execution. No new table or migration was required.

Daily trend groupings use the database's date cast for timezone-aware timestamps. Use a consistent database session timezone between deployments. PostgreSQL itself has not been exercised in this workspace; only H2's PostgreSQL compatibility mode was available for integration tests.
