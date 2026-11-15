# Intelligent IT Service Management & Incident Resolution System (I-ITSM)

## 1. Title page

<div align="center">

## Final OOP Project Report

**Course:** CS F213 / MAC F212 - Object-Oriented Programming  
**Semester:** I, Academic Year 2026-2027  
**Project:** Intelligent IT Service Management & Incident Resolution System (I-ITSM)

**Submitted by:** [Enter group number and student names / IDs]  
**Submitted to:** [Enter instructor name]  
**Submission date:** [Confirm date required by course]

</div>

> **Evidence convention.** This report distinguishes implementation visible in the current source from behavior exercised by automated tests and from behavior that still needs a live or browser demonstration. It does not claim a professor-approved SRS, a PostgreSQL production validation, or a recorded demonstration where none was found. The professor's assignment PDF is the source for the academic requirements; current code, Flyway migrations, tests, and existing engineering documents are used for implementation evidence.

## 2. Abstract

I-ITSM is a browser-based, incident-centered IT service management application. Employees report and track incidents; technicians investigate assigned work and provide resolutions; administrators manage users, technician profiles, service and category catalogs, assignment settings, SLA policies, audit history, feedback, and reports. The backend calculates priority, ranks eligible technicians, monitors response and resolution deadlines, stores incident activity and audit events, and provides local duplicate and related-incident suggestions. These intelligent features use explainable rules and text similarity rather than an external AI service.

The application uses a React 18, Vite, and Tailwind frontend with a Java 21 and Spring Boot 3.3.5 modular-monolith backend, Spring Security/JWT, Spring Data JPA, Flyway, and PostgreSQL as the persistent database target. The latest recorded backend suite contains 78 passing tests; the frontend production build succeeds. Those automated tests use H2 in PostgreSQL compatibility mode, so live PostgreSQL migration and runtime verification remain outstanding. No browser-based end-to-end test run or recorded project demonstration was found in the repository.

## 3. Introduction

Organizations depend on applications, databases, networks, cloud systems, identity platforms, and other IT services. An outage or degradation must be reported, classified, prioritized, assigned, investigated, resolved, verified, and closed. The professor's project statement frames the challenge as handling incidents at organizational scale while maintaining response speed, visibility, service health, and a usable history [1, pp. 1-3].

I-ITSM applies object-oriented design to this incident workflow. It combines user roles, incident lifecycle rules, assignment strategies, SLA monitoring, notifications, audit history, reporting, and local intelligent suggestions in one deployable backend. The project is an academic prototype, not a claim of a complete commercial ITIL platform.

## 4. Problem statement

When incident intake and support coordination are handled through disconnected or manual channels, support teams can experience delayed response, inconsistent priority, poor technician allocation, SLA violations, duplicate reports, incomplete history, and difficulty identifying recurring problems. The assignment's representative scenario is an employee unable to use a business application and complete work such as payment transactions [1, pp. 1, 3].

The project problem is to provide a persistent, role-aware workflow that records each incident and its important events, uses documented criteria to calculate priority and recommend technicians, tracks SLA targets, and gives employees and administrators useful status and reporting views.

### Stakeholders

| Stakeholder | Interest in the system |
|---|---|
| Employee / customer | Report a problem, follow its progress, respond to information requests, verify a resolution, and leave feedback. |
| Technician / support engineer | See eligible assigned work, investigate, record notes, request information, resolve incidents, and monitor SLA context. |
| Administrator / IT manager | Manage users, services, categories, technician profiles, assignment settings, SLA policies, audit history, and reports. |
| Support organization | Maintain a shared operational record and understand incident volume, workload, and SLA outcomes. |

### Assumptions and scope

- The implementation manages incidents and a basic managed IT service catalog; it does not implement a full service-request fulfillment suite, configuration management database, change management, or asset lifecycle.
- SLA targets are measured in elapsed time. Business calendars, holidays, and support hours are not modeled.
- The project has local, explainable heuristics for priority, similarity, technician ranking, and escalation; it does not use a trained machine-learning model.
- External email is simulated through backend logging. In-app notifications are persisted.
- PostgreSQL is the configured production-style persistence target; the tests described here use an isolated H2 test profile.

## 5. Objectives

The project objectives are to:

1. Support the incident lifecycle from report through closure with validation of valid transitions.
2. Provide employee, technician, and administrator workspaces with server-side authorization and ownership checks.
3. Classify and prioritize incidents using configurable, documented business inputs.
4. Recommend eligible technicians through interchangeable assignment strategies using skills and actual active workload.
5. Calculate and monitor configurable response and resolution deadlines.
6. Persist incident activity, assignment history, SLA history, notifications, and audit events.
7. Offer useful search, filtering, reporting, and CSV export.
8. Provide local duplicate and correlation suggestions for human review.
9. Demonstrate OOP principles and multiple design patterns using actual code rather than diagram-only abstractions.

## 6. Existing system and its limitations

The assignment describes the operational problems that can arise from manually managing incidents; it does not identify a specific legacy product or provide measured baseline data. This report therefore treats manual or fragmented coordination as the problem context, not as a verified description of a named organization.

Manual processes may make it harder to consistently capture incident information, calculate priority, compare technician capacity, detect SLA risk, find related reports, or reconstruct a complete operational history. I-ITSM centralizes these activities. The project itself remains bounded: it does not provide external email delivery, business-hours SLA calendars, a CMDB, or live PostgreSQL validation evidence.

## 7. Proposed system

I-ITSM provides a React single-page client and a REST API under `/api/v1`. Spring Boot modules handle authentication, users, incident lifecycle, service catalog, category management, assignment, SLA monitoring, notifications, audit, local intelligence, and reporting. Spring Data JPA persists domain entities, while Flyway migrations version schema changes.

## 8. Functional requirements

| ID | Requirement | Implementation evidence / boundary |
|---|---|---|
| FR-01 | Register and authenticate users with role-aware access. | Public registration creates `EMPLOYEE`; signed JWT and BCrypt; `AuthController`, `AuthService`, `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`. |
| FR-02 | Create, view, update, search, assign, resolve, verify, and close incidents. | `IncidentController`, `IncidentService`, `IncidentSearchService`, `Incident` state rules, and `/incidents` APIs. Role/owner rules apply. |
| FR-03 | Capture incident category, affected service, impact, urgency, business criticality, affected-user count, priority, assignee, SLA, resolution and closure information. | Request/response DTOs and `Incident`; persistent category/service links coexist with legacy text fields. |
| FR-04 | Calculate priority automatically and explain its factors. | `WeightedPriorityCalculationStrategy`; impact, urgency, business criticality, affected-user band, service criticality, configured weights and thresholds. |
| FR-05 | Implement at least two interchangeable technician assignment strategies. | Three concrete strategies are present: `RoundRobinAssignmentStrategy`, `WorkloadAssignmentStrategy`, `SkillBasedAssignmentStrategy`; the factory resolves the configured strategy. |
| FR-06 | Consider skills, category, availability, active workload, capacity and experience in technician recommendation. | `AssignmentService`, `TechnicianCandidate`, strategy implementations, and ranked recommendation endpoint. Actual workload is counted from active incident assignments. |
| FR-07 | Define and monitor response and resolution SLAs, warnings and violations. | `SlaPolicy`, `SlaTracking`, `SlaService`, scheduler, SLA history, warning/escalation flags. Time is elapsed time, not business time. |
| FR-08 | Notify relevant users about incident events using at least two mechanisms. | `NotificationPublisher`, in-app observer and simulated email observer. In-app entries persist; email is log-only simulation. |
| FR-09 | Keep a history of important incident and administrative actions. | `AuditService`, `AuditLog`, `AuditQueryService`, admin-only `/audit`; includes incident/SLA and user/service/category/feedback actions. Database-level tamper proofing is not implemented. |
| FR-10 | Search/filter incidents using at least five meaningful criteria. | `/incidents/search`: ID, text, status, priority, category, technician/assignee, creation dates, SLA status, plus pagination and role scoping. |
| FR-11 | Provide useful reports and dashboards. | Report summary/analytics, technician performance, database aggregates, date filters, and authorized incident/SLA/technician CSV endpoints. |
| FR-12 | Implement at least two intelligent or automated features. | Duplicate detection, related-incident suggestions, ranked technician recommendation, automated SLA warning/escalation, weighted priority. All are deterministic/local. |
| FR-13 | Persist users, services, incidents, categories, technicians, assignments, SLA data, notifications, audit history and feedback. | JPA entities/repositories and Flyway V1-V6. PostgreSQL execution remains unverified in this report. |
| FR-14 | Let administrators manage users, technician profiles, services, categories, assignment strategy and SLA policies. | `AdminUserController`, `TechnicianDirectoryController`, `ItServiceController`, `IncidentCategoryController`, `AssignmentController`, `SlaController`; protected by admin authorization where applicable. |
| FR-15 | Let employees submit feedback after resolution. | `IncidentFeedbackController` and `IncidentFeedbackService`; reporter-only, post-resolution, one submission per incident, rating 1-5, optional comments. |
| FR-16 | Let technicians add investigation notes and request more information. | `IncidentActivityRequest`, activity endpoint, activity types including `INVESTIGATION_NOTE` and `INFORMATION_REQUEST`; employee response activity supported. |
| FR-17 | Provide active managed services and categories without breaking historical text values. | IT services and incident categories have persistent models; migrations preserve/backfill existing label values and keep nullable relationships for legacy incidents. |

## 9. Non-functional requirements

| Requirement | Design / evidence | Limitation / verification boundary |
|---|---|---|
| Security | JWT authentication, BCrypt, role authorization, ownership/assignment checks, validation, CORS configuration, controlled DTOs. | No external penetration test or deployment security assessment is documented. |
| Persistence and integrity | PostgreSQL target, JPA relationships, Flyway, constraints/indexes, transaction-scoped business changes. | Live PostgreSQL migration and schema validation remain pending. |
| Maintainability | Modular monolith, package-level responsibility boundaries, services, repositories, DTOs, strategy/observer interfaces. | Modules share a deployable process and database; boundaries are conventional, not separate builds. |
| Performance | Database-side report aggregations; pagination; bounded CSV reads; repository indexes. | No load test or capacity benchmark is available. |
| Usability | Role-specific React workspaces, loading/error/empty states, responsive styling and real API integration. | The production build is verified; browser-based usability and role workflows have not been evidenced here. |
| Portability | Maven wrapper, Node/Vite scripts, environment-driven runtime configuration. | Actual local setup still depends on correct database credentials and PostgreSQL-compatible DDL. |
| Reliability | Transactions, assignment/SLA locks, persisted one-time markers, notification deduplication. | No multi-node live PostgreSQL concurrency exercise is reported. |

## 10. User roles and use cases

| Role | Responsibilities and use cases |
|---|---|
| Employee / Customer | Register/login; report, classify through submitted details, view/search own incidents, edit permitted incidents, track status/activity/SLA, respond to information requests, verify resolution, view own notifications, review duplicate suggestions, and submit feedback after resolution. |
| Technician / Support Engineer | Login; view assigned work and eligible queue items; inspect SLA and related-incident suggestions; acknowledge and investigate; add notes or request information; resolve with details; view notifications and workload context. |
| Administrator / IT Manager | Manage users/roles/activity; configure technician profiles and assignment strategies; create/update/deactivate services and categories; configure SLA policies; view incidents; assign technicians; inspect audit history and feedback; access analytics and exports. |
| Scheduled SLA monitor | Evaluate unresolved tracking rows, persist warnings/escalations/violations, and publish corresponding audit and notification events. It is an automated actor, not a login role. |

**Figure 1.** Role interactions and scheduled monitor use cases are documented in [the use case diagram](diagrams/USE_CASE_DIAGRAM.md). The diagram is Mermaid source and has not been independently rendered in a GitHub page as part of this report.

## 11. System architecture

I-ITSM is a **modular monolith**: one Spring Boot backend deployment with cohesive Java packages. The React client is a separate frontend process/build consuming REST endpoints. Backend modules share the same process, dependency injection container, transaction manager, and relational database; the project is not decomposed into microservices.

```mermaid
flowchart LR
  Browser[React 18 + Vite + Tailwind]
  API[Spring MVC REST API]
  Security[Spring Security + JWT]
  Modules[Domain modules: auth, user, incident, catalog, category, assignment, SLA, notification, audit, intelligence, report]
  Persistence[Spring Data JPA / Hibernate]
  Migration[Flyway V1-V6]
  DB[(PostgreSQL target)]
  Browser -->|HTTPS/JSON bearer token| API
  API --> Security
  API --> Modules
  Modules --> Persistence
  Migration --> DB
  Persistence --> DB
```

**Figure 2.** Runtime architecture (derived from `IitsmApplication`, package structure, controllers, security configuration, JPA configuration, and migrations). Development verification uses the H2 test profile; the diagram's PostgreSQL node is the configured target, not proof of live PostgreSQL validation.

The main request flow is: frontend request -> controller validation and authorization -> domain service -> repository/entity operation -> transaction commit. Incident creation also calculates priority and creates SLA tracking, audit, and notification records. Assignment and SLA monitoring use transaction and locking safeguards. See [architecture documentation](ARCHITECTURE.md) and [technical design report](TRD.md) for additional flow details, noting that portions of those older documents require reconciliation with current code.

## 12. Technology stack and justification

| Technology | Actual project use | Rationale |
|---|---|---|
| Java 21 | Backend language and compiler target. | Static typing, classes/interfaces, access control, collections and exception handling support the OOP domain model. |
| Spring Boot 3.3.5 | Backend runtime and application configuration. | Provides the integrated web, dependency injection, security, persistence and scheduling ecosystem used by the code. |
| React 18 | Frontend component UI. | Supports reusable views and role-oriented workspaces consuming REST APIs. |
| Vite | Frontend development server and production bundler. | Provides the configured dev/build workflow. |
| Tailwind CSS | Frontend utility styling. | Supports composable styling in existing components. |
| PostgreSQL | Persistent relational database target. | Fits linked user, incident, assignment, SLA, notification, feedback and audit data. Live verification is pending. |
| Spring Data JPA / Hibernate | Entity mapping and repository persistence. | Connects domain entities with relational storage and repository queries. Hibernate uses schema validation after Flyway. |
| Spring Security | Authentication filter chain and role authorization. | Protects API routes and implements endpoint access rules. |
| JWT (JJWT) | Signed bearer tokens. | Carries identity/role claims; filter validates tokens and checks user status against persistence. |
| Maven | Backend dependency management, compile and test lifecycle. | Wrapper standardizes project commands. |
| Flyway | Versioned migrations V1-V6. | Gives schema changes an ordered, reviewable history. |

These are actual project choices. The technology report discusses alternatives such as Node.js, .NET, MySQL, Vue/Angular, and microservices as hypothetical design alternatives; there is no evidence they were prototyped or benchmarked [2].

## 13. Object-oriented design and principles

| OOP concept | Implementation example |
|---|---|
| Encapsulation | `Incident` has private state and domain operations such as assignment, acknowledgement, resolution and transition validation; services do not treat lifecycle fields as arbitrary UI state. |
| Abstraction | `AssignmentStrategy`, `PriorityCalculationStrategy`, `IncidentTriageEngine`, and `IncidentEventObserver` define behavior contracts. |
| Inheritance | Persistent domain entities inherit common ID and timestamps from `BaseEntity` (`@MappedSuperclass`). |
| Polymorphism | Assignment and observer implementations can be invoked through their interfaces; configured strategy selection does not require the caller to branch on each implementation. |
| Interfaces | The strategy, triage, and observer interfaces make behavior replaceable and testable. |
| Composition | Services receive repositories, strategies, audit and notification collaborators through constructor injection. |
| Association | JPA relationships connect incidents to reporters/assignees, activities, services, categories, feedback and SLA tracking. |
| Aggregation / collections | Technician profiles maintain a set of skills; publishers and service logic work with lists/sets of observers, candidates and results. The diagram indicates aggregation where supported. |
| Exception handling | Request validation, domain exceptions, `ResponseStatusException`, and `GlobalExceptionHandler` translate invalid input/state/access into controlled API responses. |
| Data structures | Collections and maps represent candidate rankings, sets of skills/statuses, observer lists, and report/result lists. |

Role values are represented by a `Role` enum rather than Employee/Technician/Admin subclasses. The class diagram depicts selected real source classes and explicitly marks its representative scope [3].

## 14. Design patterns used

1. **Strategy Pattern:** `AssignmentStrategy` has round-robin, workload-balanced, and skill-based implementations. A factory resolves a configured strategy. Priority calculation also uses `PriorityCalculationStrategy` with a weighted implementation.
2. **Factory Pattern:** `AssignmentStrategyFactory` and `PriorityStrategyFactory` resolve implementations behind an abstraction.
3. **Observer Pattern:** `NotificationPublisher` dispatches incident events to observer implementations. In-app notifications persist; email is simulated/log-only.

The code also encapsulates lifecycle transition rules in `Incident`; it does not implement a separate state-object class hierarchy, so this report does not claim the State Pattern. The assignment requires at least two patterns; the strategy, factory, and observer structures are present in source and tests [1, pp. 10-12].

## 15. Database design and ER diagram

PostgreSQL is the target persistent store. JPA entities and Flyway migrations define users, incidents, incident activities, incident categories, IT services, feedback, technician profiles/skills, assignments, SLA policies/tracking/history, notifications, audit logs, and assignment configuration. Common ID/timestamps are inherited from `BaseEntity` where mapped.

Flyway versions present in source are V1-V6. V6 additively adds nullable `audit_logs.target_entity` and `target_id` columns plus a target lookup index. It does not rewrite historical audit rows. Generic target ID is intentionally not an FK because its target may be in different tables; the separate incident reference and actor reference remain represented as associations. The service/category migrations preserve existing text labels and use nullable references where older records may lack a managed catalog association.

**Figure 3.** [Entity Relationship Diagram](diagrams/ER_DIAGRAM.md), derived from JPA entities and Flyway migrations. The migration SQL, not the Markdown diagram, is authoritative for exact constraints and indexes. Live PostgreSQL schema compatibility has not been demonstrated; H2 compatibility-mode migration success is not a substitute [4].

## 16. UML diagrams

- **Use case diagram:** [USE_CASE_DIAGRAM.md](diagrams/USE_CASE_DIAGRAM.md) - Employee, Technician, Administrator, and scheduled SLA monitor interactions.
- **Class diagram:** [CLASS_DIAGRAM.md](diagrams/CLASS_DIAGRAM.md) - selected Java entities, services, interfaces, pattern implementations and relationships.
- **ER diagram:** [ER_DIAGRAM.md](diagrams/ER_DIAGRAM.md) - persistent entities, keys, cardinalities, and inferred versus database-enforced relations.
- **Architecture diagram:** included in [ARCHITECTURE.md](ARCHITECTURE.md) and summarized in this report.

The use-case and class diagrams use Mermaid flowchart/class syntax; the ER diagram uses Mermaid ER notation. The diagram documents state that elements were compared with source. This report has not independently rendered them in GitHub's Mermaid renderer.

## 17. Module-wise implementation

| Module | Main responsibility | Representative implementation |
|---|---|---|
| `auth` | Registration, login, token creation/validation. | `AuthController`, `AuthService`, `JwtService`, `JwtAuthenticationFilter`. |
| `user` | Role/user administration and technician directory support. | `UserService`, `UserAdministrationService`, `AdminUserController`, `TechnicianDirectoryController`. |
| `incident` | Incident details, lifecycle, search, activity, category and feedback. | `IncidentController`, `IncidentService`, `IncidentSearchService`, `Incident`, activity/category/feedback controllers and services. |
| `servicecatalog` | Persistent service catalog and incident service links. | `ItServiceController`, `ItServiceCatalog`, `ItService`, repository, DTOs. |
| `assignment` | Strategy configuration, eligibility, recommendations, assignment history. | `AssignmentService`, `AssignmentStrategy` implementations, factory, profile and record entities. |
| `sla` | Policy deadlines, scheduled monitoring, warnings, escalation and history. | `SlaService`, `SlaTracking`, `SlaPolicy`, scheduler configuration and repositories. |
| `notification` | Event-driven notifications and user notification history. | `NotificationPublisher`, `IncidentEventObserver`, in-app and simulated email observers. |
| `audit` | Persistent event records and admin query endpoint. | `AuditService`, `AuditLog`, `AuditQueryService`, `AuditController`. |
| `intelligence` | Priority scoring, duplicate and related suggestions, rule-based triage abstraction. | `WeightedPriorityCalculationStrategy`, `IncidentSimilarityService`, `RuleBasedTriageEngine`, `IntelligenceController`. |
| `report` | Analytics, metrics, search support and CSV export. | `ReportService`, `ReportExportService`, `ReportController`, report DTOs. |
| `common/config` | Shared API responses, base entity, error handling, and security/CORS configuration. | `ApiResponse`, `PagedResponse`, `BaseEntity`, `GlobalExceptionHandler`, `SecurityConfig`. |

Frontend implementation is under `frontend/src`: `App.jsx`, role pages, `AdminPage.jsx`, incident form/detail components, report panel, UI components, and `services/api.js`.

## 18. Intelligent features

### 15.1 Priority calculation

`WeightedPriorityCalculationStrategy` combines impact, urgency, business criticality, affected-user count bands, and affected-service criticality using configurable weights and thresholds. It stores a score, selected priority, and explanation. The documented defaults map P1/P2/P3/P4 to Critical/High/Medium/Low; the exact scoring rules are configuration and code policy, not a learned model.

### 15.2 Duplicate incident suggestions

`IncidentSimilarityService` normalizes text and compares token sets using Jaccard similarity. The duplicate score weights title (0.35), description (0.40), category equality (0.15), and affected-service similarity (0.10). Results are thresholded, ranked, capped, and omit closed incidents. The employee can review suggestions and continue creating a separate incident; the application does not merge or delete records.

### 15.3 Related incident correlation

Related incident scoring considers text similarity, category, affected service, and temporal proximity. Results are suggestions for technician/admin review; they are not persisted as a permanent correlation relationship and do not change incident status.

### 15.4 Technician recommendation and SLA escalation

Technician ranking reuses assignment strategies and applies active account, profile availability, workload capacity and skill eligibility rules, then considers workload and experience. Scheduled SLA monitoring records approaching deadlines and response/resolution breaches, with persistence flags/locks intended to avoid duplicate processing. These are deterministic business rules, not predictive AI. Automated integration tests exist, but a live PostgreSQL multi-instance test is not reported.

## 19. Security and exception handling

- Passwords are BCrypt-encoded; password hashes are not included in user DTOs.
- JWTs are signed with an environment-supplied secret and validated by a Spring Security filter. Current user activity/role is checked against persistence.
- Public registration creates an employee account only. Elevation and administrative actions require protected routes.
- Employee incident queries/actions apply ownership checks; technician actions are scoped to assignment/eligible queue rules; admin routes use role guards.
- DTO validation checks required and bounded fields. Incident state changes are validated in domain/service logic.
- Global exception handling returns structured API errors without returning stack traces or credentials.
- Audit API access is administrator-only. Audit records omit password/hash/token/secret and feedback comment content.
- CORS is configured for allowed frontend origins; operational deployments must set appropriate origins and secrets.

These controls have code and automated test evidence, not an independent penetration test. No claim is made about security review of a production deployment.

## 20. Testing methodology and test cases

### Methodology

Backend tests use JUnit 5, Spring Boot test support, MockMvc, and an H2 in-memory database in PostgreSQL compatibility mode with Flyway migrations and Hibernate schema validation. Unit tests exercise domain/strategy calculations; integration tests exercise HTTP routes, persistence, authorization, filtering and workflows. The frontend has a Vite production build but no configured browser automation test script.

The latest recorded complete backend test run in the workspace used Java 21 and `.\mvnw.cmd test` from `backend`: **78 tests, 78 passed, 0 failed, 0 errors, 0 skipped**. The frontend `npm run build` succeeded. This is automated verification against H2; it is not live PostgreSQL validation or browser-based UI verification. Surefire XML reports are located in `backend/target/surefire-reports/`.

### Representative meaningful tests

The cases below are existing automated test coverage, not a claim that each has been run manually in a browser.

| # | Test case | Test evidence |
|---:|---|---|
| 1 | JWT contains identity/role claims and is signed. | `JwtServiceTest.createsSignedJwtWithIdentityAndRoleClaims` |
| 2 | JWT signed with a different key is rejected. | `JwtServiceTest.rejectsTokenSignedByDifferentKey` |
| 3 | Weak JWT signing secret is rejected. | `JwtServiceTest.rejectsWeakSecret` |
| 4 | Minimum/maximum priority inputs map to expected bands. | `WeightedPriorityCalculationStrategyTest.assignsP4AtMinimumAndP1AtMaximum` |
| 5 | Configured priority boundary behavior is checked. | `WeightedPriorityCalculationStrategyTest.mapsConfiguredBoundaryScoresToHigherBand` |
| 6 | Invalid priority weights/thresholds/configuration are rejected. | `WeightedPriorityCalculationStrategyTest` invalid configuration tests |
| 7 | Skill strategy selects a matching eligible technician. | `AssignmentStrategyTest.skillStrategyMatchesCategoryAndPrefersExperienceAfterEligibility` |
| 8 | Workload strategy uses active incident workload and excludes unavailable/capacity-bound candidates. | `AssignmentStrategyTest.loadStrategyUsesActualWorkloadFiltersAvailabilityAndCapacityAndBreaksTiesById` |
| 9 | Ranked recommendations include match/capacity explanations. | `AssignmentStrategyTest.rankedWorkloadRecommendationsPreferSkillMatchAndExplainCapacity` |
| 10 | Full incident lifecycle is permitted in order. | `IncidentLifecycleTest.permitsFullLifecycle` |
| 11 | Invalid skips and unauthorized acknowledgement are rejected. | `IncidentLifecycleTest.rejectsSkippingAndUnauthorizedAcknowledgement` |
| 12 | Closed incidents cannot be reassigned; legacy OPEN incidents can be assigned. | `IncidentLifecycleTest.rejectsAssignmentAfterClosure`, `assignsLegacyOpenIncidentDirectlyToAssigned` |
| 13 | Response and resolution deadlines are calculated from creation. | `SlaServiceTest.calculatesBothElapsedTimeDeadlinesFromCreation` |
| 14 | SLA warning and violation events are persisted once. | `SlaServiceTest.generatesOneWarningBeforeDeadlineAndPersistsAtRiskState`, `recordsSlaViolationOnceWhenDeadlineHasPassed` |
| 15 | Duplicate detection normalizes/ranks matches and excludes closed incidents. | `IncidentSimilarityServiceTest.normalizesTextAndReturnsRankedDuplicateSuggestionsButExcludesClosedIncidents` |
| 16 | Similar category/service alone does not create an excessive false positive. | `IncidentSimilarityServiceTest.categoryAndServiceAloneDoNotPassDuplicateThreshold` |
| 17 | Related signals and time-window rules are evaluated. | `IncidentSimilarityServiceTest.relatedSignalsIncludeCategoryServiceTextAndTimeWindow` |
| 18 | Employee registration cannot elevate role and incident ownership is enforced. | `IncidentSecurityIntegrationTest.publicRegistrationCannotElevateAndOwnershipIsEnforced` |
| 19 | Concurrent assignments respect technician capacity and avoid duplicate same-incident assignment. | `IncidentSecurityIntegrationTest.concurrentAssignmentsCannotExceedTechnicianCapacity`, `concurrentAssignmentOfSameIncidentCreatesOnlyOneAssignment` |
| 20 | Concurrent SLA monitoring records one warning/violation per target. | `IncidentSecurityIntegrationTest.concurrentSlaMonitoringPersistsOneWarningAndViolationPerTarget` |
| 21 | User search supports null/blank query and paginated counts; user management hides credentials. | `IncidentSecurityIntegrationTest.adminUserSearchSupportsNullableBlankMatchingAndPaginatedCount`, `administratorsManageUsersWithoutExposingCredentials` |
| 22 | Category, service and feedback role/lifecycle behavior is enforced. | `IncidentSecurityIntegrationTest.administratorsManageCategoriesAndEmployeesCannot`, `itServiceCatalogEnforcesRoleAccessValidationAndLifecycle`, `employeeCanSubmitOneOwnedFeedbackOnlyAfterResolutionAndAdminCanReadIt` |
| 23 | Administrative and feedback audit events persist; unauthorized access, failed operations, duplicates, and sensitive audit details are checked. | `IncidentSecurityIntegrationTest.administrativeAndFeedbackActionsAreAuditedWithoutLeakingSecretsOrLoggingFailures` |
| 24 | Reports, filters, date bounds, pagination, CSV and authorization behave as expected. | `ReportServiceIntegrationTest` integration cases |

The professor's minimum of 15 meaningful test cases is exceeded by the automated suite [1, p. 15]. These tests do not demonstrate a real-browser workflow or PostgreSQL compatibility.

## 21. Results and screenshots

### Verified results available in repository

| Result | Evidence | What it establishes |
|---|---|---|
| Backend test suite | Latest Surefire XML and console record: 78 run, 0 failures/errors/skips. | Unit and HTTP/persistence integration behavior under the test profile. |
| Frontend production build | Vite build output: 1,590 modules transformed; successful output generated. | Frontend source compiles/bundles for production. |
| Migrations in automated integration profile | Flyway V1-V6 applied in the H2 test context; Hibernate context starts. | Test-schema migration/validation in H2 compatibility mode. |

### Application screenshots

No application screenshots were found in the project media inventory. Do not treat the rendered assignment PDF pages as application screenshots. Capture actual UI images from the running application and insert them before PDF submission.

| Suggested figure | Screen to capture | Evidence status |
|---|---|---|
| Figure 4 | Employee incident creation with active service/category selection and duplicate suggestions. | Missing; capture manually. |
| Figure 5 | Incident detail showing priority explanation, assignment, lifecycle and SLA information. | Missing; capture manually. |
| Figure 6 | Technician investigation/activity and resolution workflow. | Missing; capture manually. |
| Figure 7 | Administration audit history showing target/action filters and recent records. | Missing; capture manually after V6 is applied to the local database. |
| Figure 8 | Admin reports dashboard with real database results and export controls. | Missing; capture manually using non-sensitive demonstration data. |

Use test/demo data and redact email addresses or other identifying information before including images in a report shared outside the group.

## 22. Three demonstration workflows

The professor requires at least three complete workflows and a recorded video [1, p. 15]. The project contains an 18-minute demo script and one authenticated API-level employee-technician-admin workflow test. The workflows below are prepared as a screen plan; they have **not** been verified through browser automation or a recorded video in this report.

### Workflow 1 - Employee reports and tracks an incident

1. Sign in as an Employee and open **Dashboard** or **Incidents**.
2. Choose **Report incident**; enter a meaningful title, description, category, active service, impact, urgency, business criticality and affected-user count.
3. Review the possible duplicate list (ticket number, title, status and score); continue creating a separate incident only after review.
4. Submit and open the incident detail. Show generated ticket number, calculated priority explanation, current state, assigned technician (if assigned), and response/resolution SLA deadlines.
5. Open **Notifications** and show the persisted incident event notification.
6. As an administrator, show that the incident and its creation/assignment event appear in the incident list and audit history.

**Expected evidence:** Incident is stored; priority is produced by backend logic; duplicate suggestions are advisory; role-limited views and notifications are visible.

### Workflow 2 - Technician investigates, resolves, and employee verifies with feedback

1. Sign in as an Administrator and open **Administration → Assignment**. Show the active strategy and technician profile availability, skills, experience, and workload capacity.
2. Open an eligible incident, request ranked recommendations, explain reasons, and assign a suitable technician.
3. Sign in as that Technician; open **Dashboard → assigned incident**. Acknowledge, start investigation, add an investigation note, and request additional information if the Employee response will be shown.
4. Review SLA deadline/history and related incident suggestions; enter resolution details and resolve the incident.
5. Sign in as the reporting Employee, open the incident, verify resolution, then submit one rating and optional feedback.
6. As an Administrator, open **Administration → Feedback** and **Audit history** to show the feedback and audit event. Confirm feedback comment text is not copied into audit detail.

**Expected evidence:** Assigned-technician authorization, investigation records, resolution transition, employee verification/feedback ownership, notifications and audit entries.

### Workflow 3 - Similar reports, correlation, SLA escalation, and operational review

1. Use an Employee account to create two related incidents using distinct ticket records but similar outage details/service/category. Do not merge them.
2. Review duplicate suggestions during incident creation; as a Technician, open one incident and review related incident suggestions.
3. Use a dedicated non-production test database and an appropriately short, documented SLA policy/monitor interval. Wait for the actual scheduler to create a warning or breach event; do not claim escalation until the corresponding state/history/notification is visible.
4. As an Administrator, open **Administration → Audit history**, filter for incident/SLA action and target. Open **Reports** and show database-backed status, priority, technician workload, SLA compliance, and an authorized CSV export.

**Expected evidence:** Similarity/correlation remains advisory; SLA history, audit and in-app notification are linked to actual monitor processing; reports use persisted data. The test database and configuration must be prepared safely before recording.

## 23. Limitations

- Live PostgreSQL V1-V6 migration, Hibernate validation, API query behavior, and concurrency have not been independently verified for this report. H2 compatibility mode is not proof of PostgreSQL behavior.
- No browser-driven end-to-end test harness/run or application screenshots are present in the inspected repository.
- No recorded three-workflow video or shared upload link was found.
- Email notifications are simulated; no SMTP or external mail delivery is configured.
- SLA timing uses elapsed time and does not account for business hours, holidays, or service calendars.
- Duplicate/correlation and technician/priority recommendations use explainable heuristics; they are not trained AI models. Rule-based triage exists as an abstraction/implementation but is not exposed as an API workflow.
- Correlation suggestions are computed on demand and are not persisted as incident relationships.
- Audit records are append-only by application convention, not protected by a database tamper-evidence mechanism. Older audit records are preserved and are not backfilled with the V6 generic target columns.
- The managed IT service catalog is not a full service request, CMDB, asset, or change-management implementation.
- No load/performance benchmark, external penetration test, or production deployment assessment is available.
- Team names, individual contributions, final repository submission link, and instructor/submission metadata were not verified from source files.

## 24. Future scope

Future work can be considered only after core requirements and live verification are completed:

1. Run migrations and integration checks against an isolated PostgreSQL test database, then record actual evidence.
2. Add browser-based tests and capture the required demo workflows/screenshots.
3. Integrate real email/SMS/push providers if permitted, using secrets management and delivery observability.
4. Add business-hours calendars and service-specific SLA calendars.
5. Add a service request workflow, CMDB/service dependency model, and related service health view if project scope requires them.
6. Add a database-level retention/immutability or tamper-evidence strategy for audit, with access and retention policy.
7. Bound/index similarity candidate evaluation and assess it with measured workload data.
8. Add capacity/load testing before deciding whether to scale the monolith or extract modules.
9. If significant scale requires it, horizontally scale the stateless backend and evaluate PostgreSQL pooling, indexes, caching, and background jobs from measured bottlenecks before considering selective service extraction.

These are future possibilities, not implemented features or measured performance claims.

## 25. Conclusion

I-ITSM implements a role-aware incident workflow using a Java/Spring modular monolith and a React frontend. Current source includes incident creation and lifecycle management, priority calculation, multiple technician assignment strategies, SLA monitoring, persistent in-app notifications, audit history, search/reporting, a managed service catalog, categories, feedback, and local intelligent suggestions. The code and automated suite demonstrate use of OOP concepts and Strategy, Factory, and Observer patterns.

The latest recorded automated verification is 78 passing backend tests and a successful frontend production build. The test evidence is limited to the H2 test profile and API/unit-level execution. The final academic submission still requires manual completion of team/submission metadata, real UI screenshots, a three-workflow recorded demonstration, and live PostgreSQL validation if the group intends to claim PostgreSQL operation as demonstrated.

## 26. References

1. **Course project specification:** *CS F213 / MAC F212 OOP, Semester-I 2026-2027, Project: Intelligent IT Service Management & Incident Resolution System*, repository file `OOP_S1_2026-2027_Project.pdf`, 17 pages. Requirements referenced in this report include the project overview (pp. 1-3), roles and MVP (pp. 3-5), incident/priority/assignment/SLA/lifecycle/notification/audit/search/intelligence (pp. 5-10), OOP and patterns (pp. 10-12), technology-selection questions (p. 12), and submission/testing/demo requirements (pp. 14-17).
2. `docs/TECHNOLOGY_SELECTION.md`, technology inventory, nine-question rationale, explicitly hypothetical alternatives, and unverified PostgreSQL boundary.
3. `docs/diagrams/CLASS_DIAGRAM.md`, source-aligned representative class diagram and documented limits.
4. `docs/diagrams/ER_DIAGRAM.md`, `docs/DATABASE_DESIGN.md`, and Flyway migrations `backend/src/main/resources/db/migration/V1__phase2_core_business_schema.sql` through `V6__generic_audit_targets.sql`.
5. `docs/API_CONTRACT.md`, current documented REST routes and access rules; verify the corresponding current controller source for any API claim.
6. `docs/ARCHITECTURE.md`, modular-monolith description, OOP/design-pattern rationale, and documented technical limits.
7. `docs/SRS.md` and `docs/TRD.md`, implementation-aligned scope/design summaries; portions predate the latest audit and service catalog changes and should be reconciled before final PDF publication.
8. `docs/USER_GUIDE.md` and `docs/DEMO_SCRIPT.md`, role workflow instructions and an 18-minute demonstration plan.
9. `docs/TEST_CASES.md` and Surefire XML files under `backend/target/surefire-reports/`, automated test descriptions and recorded test results.
10. `docs/REQUIREMENT_TRACEABILITY.md` and `docs/FINAL_REQUIREMENT_AUDIT.md`, prior audit/traceability snapshots. Some claims in these historical documents are stale relative to current code (including audit integration and administrator management); use current source, current migration V6, and current Surefire output as the implementation evidence.

---

## 27. Pre-submission manual confirmation checklist

- [ ] Replace title-page placeholders with the official group number, all student names/IDs, instructor name, and confirmed submission date.
- [ ] Review each student's actual contribution; repository documentation uses generic student labels and is not evidence of named ownership.
- [ ] Capture and insert real application screenshots from a safe local/demo dataset; the repository currently contains no app screenshots.
- [ ] Run and record three complete workflows in the browser; the API-level workflow test is not the required recorded UI demonstration.
- [ ] Record the required 15-20-minute project video and obtain the shared folder/upload link required by the professor.
- [ ] Verify Flyway V1-V6 and Hibernate schema validation against a separate, disposable PostgreSQL test database; do not use production/live data without authorization.
- [ ] Confirm admin, technician and employee accounts exist in the demo environment without placing credentials in the report/video.
- [ ] Reconcile stale claims in `SRS.md`, `TRD.md`, `TECHNOLOGY_SELECTION.md`, `REQUIREMENT_TRACEABILITY.md`, `FINAL_REQUIREMENT_AUDIT.md`, and README with current source and migration history before exporting the final PDF.
- [ ] Confirm the repository remote is accessible and submit its link through the professor's requested channel/spreadsheet.
- [ ] Export this Markdown report to the professor-required PDF format and inspect page breaks, Mermaid rendering, tables, and screenshot captions before submission.
