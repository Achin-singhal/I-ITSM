# Test Cases and Verification Record

## Current code verification

The backend uses JUnit 5, Spring Boot integration tests, MockMvc and an H2 in-memory PostgreSQL compatibility database. The frontend package has no unit/E2E test script; production compilation is verified with Vite.

| Area | Test cases in repository |
|---|---|
| JWT | Signed token claims, validity and expiry behavior (`JwtServiceTest`) |
| Incident | State transition rules (`IncidentLifecycleTest`); HTTP validation, owner authorization, employee-only registration, role workflow, assignment, SLA visibility, activities and token rejection (`IncidentSecurityIntegrationTest`) |
| Assignment | Strategy behavior, candidate eligibility, workload/capacity and concurrent assignment protection (`AssignmentStrategyTest`, `IncidentSecurityIntegrationTest`) |
| SLA | Warning/breach behavior, escalation idempotency and concurrent monitor processing (`SlaServiceTest`, `IncidentSecurityIntegrationTest`) |
| Intelligence | Similarity scores and thresholds (`IncidentSimilarityServiceTest`); priority weight/threshold validation (`WeightedPriorityCalculationStrategyTest`) |
| Reports | Aggregation accuracy, date bounds, SQL search/pagination/ownership, technician workload, CSV formula neutralization and role restriction (`ReportServiceIntegrationTest`) |
| Administrative and feedback audit | User creation/state/role changes, IT service CRUD/state changes, category CRUD/state changes, feedback ownership/lifecycle, no audit on failed/no-op actions, sensitive detail exclusion, and employee denial of audit history (`IncidentSecurityIntegrationTest.administrativeAndFeedbackActionsAreAuditedWithoutLeakingSecretsOrLoggingFailures`) |
| End-to-end API | Authenticated employee → technician → admin HTTP workflow through lifecycle, assignment, notices, SLA details, audit, reports and CSV endpoint authorization (`IncidentSecurityIntegrationTest.employeeTechnicianAdministratorWorkflowRunsThroughAuthenticatedApis`) |

## Phase 6 verification run

Latest verification for the administrative and feedback audit update: from the `backend` directory, `.\mvnw.cmd test` on Java 21 executed **78 tests, 0 failures, 0 errors, 0 skipped**, `BUILD SUCCESS`. Frontend `npm run build` completed successfully with Vite. The audit integration coverage verifies all requested administrative/feedback event types, admin-only history access, no audit for failed operations, and exclusion of feedback comments and password data from audit responses. The prior service catalog verification ran 77 tests successfully.

PostgreSQL was not used for this feature verification. Flyway V1–V6 and Hibernate `validate` are exercised using H2 2.2 in PostgreSQL mode only. This does not establish PostgreSQL behavior or compatibility with an existing production schema.

No browser-driven frontend test suite is configured. The role workflow is exercised at the Spring MVC/API integration layer, not by clicking through a real browser session.
