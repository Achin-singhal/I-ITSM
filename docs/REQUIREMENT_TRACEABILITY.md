# Requirement-to-Implementation Checklist

## Source and scope

The workspace contains `OOP_S1_2026-2027_Project.pdf`, but a local text extractor is unavailable and the browser's file-URL policy rejected opening it. Its contents therefore have not been read or used to infer requirements. No PRS, TRD, or SRS source document is present in the repository. The checklist below is explicitly derived from claims in the current README and API/documentation; it is not a professor-statement traceability matrix. Exact academic traceability remains pending review of the PDF or an accessible text copy.

**Evidence basis:** README key highlights, collaborative module responsibilities and documented current API behavior. “Implemented” means the repository contains connected code and tests; “Partial” marks a narrower behavior than the README wording or an implementation gap.

| Repository-documented requirement | Implementation evidence | Status / caveat |
|---|---|---|
| JWT authentication; EMPLOYEE, TECHNICIAN and ADMIN roles | `auth/JwtService`, `JwtAuthenticationFilter`, `config/SecurityConfig`, `@PreAuthorize` controller rules; `JwtServiceTest`, `IncidentSecurityIntegrationTest` | Implemented. Public registration is employee-only. |
| Incident reporting, editing, ownership and lifecycle | `incident/IncidentController`, `IncidentService`, lifecycle domain methods and DTO validation; `IncidentSecurityIntegrationTest` | Implemented. Employee access is ownership-scoped; assigned technicians drive technician transitions. |
| Priority classification | `WeightedPriorityCalculationStrategy`, priority configuration properties, incident create/update path | Implemented. The triage engine described below is separate from this score. |
| Strategy-based technician assignment | `AssignmentStrategy`, `RoundRobinAssignmentStrategy`, `WorkloadAssignmentStrategy`, `SkillBasedAssignmentStrategy`, `AssignmentStrategyFactory`, `AssignmentService` | Implemented and tested for workload, skill and capacity cases. |
| SLA targets and breach monitoring | `SlaPolicy`, `SlaTracking`, `SlaService`, scheduled monitor, warning/escalation flags and history | Implemented as elapsed-time targets. No business-hours calendar is implemented. |
| Observer-based notifications | `NotificationPublisher`, incident observers, notification persistence and deduplication | Partial: in-app notices persist; email observer is simulated/log-only, not delivered externally. |
| Incident, SLA, administrative and feedback audit history | `AuditService`, `AuditLog`, `AuditQueryService`, `AuditController`, `UserService`, `UserAdministrationService`, `ItServiceCatalog`, `IncidentCategoryService`, `IncidentFeedbackService`; additive Flyway V6; `IncidentSecurityIntegrationTest.administrativeAndFeedbackActionsAreAuditedWithoutLeakingSecretsOrLoggingFailures` | Implemented for the listed operations, with actor/action/target/time/safe details and admin-only retrieval. Historical records remain unchanged. Database-level tamper-proof immutability is not enforced, so “tamper-evident” remains partial. |
| Rule/AI triage classification | `RuleBasedTriageEngine` implements `IncidentTriageEngine` | Partial: a local keyword implementation exists, but there is no exposed triage REST endpoint or frontend workflow. It is not an AI model. |
| Duplicate and related-incident suggestions | `IncidentSimilarityService`, `IntelligenceController`, incident form/detail UI | Implemented as explainable, human-reviewed suggestions; no automatic merging. |
| Analytics, reporting and CSV export | `ReportController`, database aggregate queries, `ReportExportService`, admin Reports panel | Implemented; reports are admin-only except technician self metrics. |
| General user profile/role administration | Public registration, `UserService`, technician directory/profile configuration | Partial: no general user directory, deactivation or role-management UI/API. |

This checklist must be reconciled against the original professor statement before asserting official requirement coverage.
