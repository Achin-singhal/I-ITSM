# I-ITSM REST API Contract

This document reflects the Spring MVC controllers currently present in the backend. Base path: `/api/v1`. Protected requests use `Authorization: Bearer <JWT>`.

## Response format

Successful controller responses use `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": {},
  "errors": null,
  "timestamp": "2026-11-15T00:00:00Z"
}
```

Authentication responses put `token`, `tokenType`, `userId`, `email`, `fullName`, `role`, and `expiresInMs` directly in `data`. Incident and other controller data is likewise available under `data`.

## Authentication

| Method | Endpoint | Access | Request |
|---|---|---|---|
| POST | `/auth/login` | Public | `{ "email": "...", "password": "..." }` |
| POST | `/auth/register` | Public | `{ "email": "...", "password": "...", "fullName": "..." }` |

Registration always creates an `EMPLOYEE`. The response returns a signed JWT and the authenticated user's basic information. The frontend stores the token in tab-scoped `sessionStorage` and sends it as a bearer token.

## Administrator user management

All `/admin/users` routes require `ADMIN`. Responses include only ID, email, full name, role, active state and creation timestamp; password hashes and credentials are never returned.

| Method | Endpoint | Access / behavior |
|---|---|---|
| GET | `/admin/users?query=&page=0&size=25` | Admin search by email or name; size is 1–100 and results are paginated. |
| GET | `/admin/users/{id}` | Admin safe detail view. |
| PATCH | `/admin/users/{id}/active` | Body `{ "active": true }`; self-deactivation and removing the last active administrator are rejected. |
| PATCH | `/admin/users/{id}/role` | Body `{ "role": "EMPLOYEE|TECHNICIAN|ADMIN" }`; self-role changes and removing the last active admin role are rejected. Public registration remains employee-only. |

## Incident categories

Authenticated users can list categories; mutations require `ADMIN`. Incident create/update continues to accept the selected category name and the backend resolves it to the persistent category record. Inactive categories cannot be newly selected; when editing an existing incident, its unchanged historical category can be retained. Migration V4 backfills existing category labels and links incident rows without rewriting their existing `category` text.

| Method | Endpoint | Access / behavior |
|---|---|---|
| GET | `/categories?activeOnly=true` | Authenticated; inactive categories are included when `activeOnly=false`. |
| POST | `/categories` | Admin; body `{ "name": "Payments" }`. |
| PUT | `/categories/{id}` | Admin; body `{ "name": "Payment Services" }`. Existing incident label snapshots are retained. |
| PATCH | `/categories/{id}/active` | Admin; body `{ "active": false }`. |

## Employee incident feedback

Only the reporting employee can submit one feedback entry after `RESOLVED` (also accepted after `VERIFIED` or `CLOSED`). Rating is 1–5 and comments are optional (maximum 2,000 characters). A unique incident constraint prevents concurrent duplicate submissions; no update route is provided. Employees can read only their own feedback, and administrators can view the paginated review list.

| Method | Endpoint | Access / behavior |
|---|---|---|
| POST | `/incidents/{id}/feedback` | Employee reporter; body `{ "rating": 5, "comments": "Optional" }`. Pre-resolution and duplicate submissions return conflict. |
| GET | `/incidents/{id}/feedback` | Reporting employee; returns their feedback or 404 if none exists. |
| GET | `/admin/feedback?page=0&size=25` | Admin-only feedback list. |

## Incidents and activities

| Method | Endpoint | Access / behavior |
|---|---|---|
| GET | `/incidents` | Authenticated. Employees receive their own incidents; technicians receive their assigned incidents and unassigned queue items; admins receive all incidents. |
| GET | `/incidents/{id}` | Authenticated; employees must own the incident; technicians may access assigned incidents and unassigned queue items; admins may access all. |
| POST | `/incidents` | Authenticated. Creates and classifies an incident. |
| PUT | `/incidents/{id}` | Authenticated. Employees may update their own; technicians may update incidents assigned to them; admins may update any incident. |
| PATCH | `/incidents/{id}/status` | Authenticated; lifecycle and actor relationship are validated. |
| GET | `/incidents/{id}/activities` | Authenticated with incident visibility checks. |
| POST | `/incidents/{id}/activities` | Authenticated; activity type and reporter/assignee permissions are validated. |

Create/update request fields: `title`, `description`, `category`, `impact` (`VERY_LOW`, `LOW`, `MODERATE`, `HIGH`, `EXTENSIVE`), `urgency` (`LOW`, `NORMAL`, `HIGH`, `IMMEDIATE`, `CRITICAL`), `businessCriticality` (1–5), `affectedUserCount` (0–1,000,000), `serviceCriticality` (1–5), and either `serviceId` for a managed active service or the compatibility field `affectedService` for legacy free text. When a managed service is selected, its name populates `affectedService`. Priority is calculated by the backend.

Status request: `{ "status": "...", "details": "..." }`. Valid lifecycle states include `NEW`, `OPEN` (legacy), `CLASSIFIED`, `ASSIGNED`, `ACKNOWLEDGED`, `IN_PROGRESS`, `RESOLVED`, `VERIFIED`, and `CLOSED`. Resolution requires nonblank details. Assignment is handled by the assignment API. Activity request: `{ "activityType": "INVESTIGATION_NOTE|INFORMATION_REQUEST|EMPLOYEE_RESPONSE", "message": "..." }`.

Incident response records include `id`, `ticketNumber`, `title`, `description`, `status`, `priority`, `priorityCode`, `category`, nullable `serviceId` and `serviceName` for managed service links, `affectedService`, `reporterId`, `reporterName`, `assigneeId`, `assigneeName`, `createdAt`, `updatedAt`, impact/urgency/criticality inputs, `priorityScore`, `priorityExplanation`, resolution details, and lifecycle timestamps.

## IT service catalog

| Method | Endpoint | Access / behavior |
|---|---|---|
| GET | `/services/active` | Authenticated employee, technician, or administrator; returns active selectable services. |
| GET | `/services` | Admin-only complete catalog, including inactive services. |
| GET | `/services/{id}` | Admin-only service details. |
| POST | `/services` | Admin-only; body `{ "name": "...", "description": "..." }`. |
| PUT | `/services/{id}` | Admin-only; update service name and description. |
| PATCH | `/services/{id}/active` | Admin-only; body `{ "active": true }` or `{ "active": false }`. |

Names are unique case-insensitively. New service associations require an active service. Flyway V5 imports distinct existing `affected_service` labels into the catalog, adds a nullable incident foreign key, and preserves the text column for older clients and historical incidents.

## Assignment

| Method | Endpoint | Access / behavior |
|---|---|---|
| GET | `/assignments/recommendations/{incidentId}?strategy=SKILL_BASED` | `TECHNICIAN`, `ADMIN`; optional `ROUND_ROBIN`, `WORKLOAD_BALANCED`, or `SKILL_BASED`. |
| GET | `/assignments/recommendations/{incidentId}/ranked?strategy=...` | `TECHNICIAN`, `ADMIN`; ordered eligible technicians with workload, experience, and match reasons. |
| POST | `/assignments/auto-route/{incidentId}?strategy=...` | `TECHNICIAN`, `ADMIN`; assigns or permits administrator reassignment in supported lifecycle states. |
| POST | `/assignments/{incidentId}/assign?strategy=...` | `TECHNICIAN`, `ADMIN`; `{ "technicianId": 123 }`; selected technician must be returned as eligible by the selected strategy at assignment time. |
| PUT | `/assignments/strategy` | `ADMIN`; body `{ "strategy": "WORKLOAD_BALANCED" }`. |
| PUT | `/assignments/technicians/{userId}/profile` | `ADMIN`; body includes `available`, `experienceYears`, `maxActiveWorkload`, and nonempty `skills`. |
| GET | `/assignments/{incidentId}/history` | `TECHNICIAN`, `ADMIN`. |

`GET /users/technicians` is available to administrators for technician selection. There is still no endpoint to read the saved assignment strategy or retrieve arbitrary full user profiles. Technician profile management accepts a known user ID.

## Intelligent incident suggestions

| Method | Endpoint | Access / behavior |
|---|---|---|
| POST | `/intelligence/duplicates` | `EMPLOYEE`, `TECHNICIAN`, `ADMIN`; body `{ "title", "description", "category", "affectedService" }`; returns up to 10 matching incident IDs/ticket numbers/statuses, scores from 0 to 1, and reasons. Suggestions are informational and never merge records. |
| GET | `/intelligence/incidents/{incidentId}/related` | `TECHNICIAN`, `ADMIN`; returns up to 10 related active incidents with score and signals. |

Duplicate similarity uses normalized token-set Jaccard similarity weighted across title (0.35), description (0.40), category equality (0.15), and affected-service token similarity (0.10). The default duplicate threshold is `0.62`. Related correlation scores text (0.50), category (0.20), affected service (0.20), and time proximity (0.10), and by default only compares incidents created within 14 days. The related threshold defaults to `0.35`. The service is deterministic and local; it is not a semantic-language model. Closed incidents are excluded. Results are ranked, capped at 10, and do not change incident data.

## SLA, notifications, audit, and health

| Method | Endpoint | Access / behavior |
|---|---|---|
| GET | `/sla/policies` | Authenticated. |
| POST | `/sla/policies` | `ADMIN`; creates priority response/resolution targets in minutes. |
| PUT | `/sla/policies/{id}` | `ADMIN`; updates a policy. |
| DELETE | `/sla/policies/{id}` | `ADMIN`; deactivates a policy. |
| GET | `/sla/incidents/{incidentId}/status` | Authenticated, incident visibility applies. |
| GET | `/sla/incidents/{incidentId}/history` | Authenticated, incident visibility applies. |
| GET | `/notifications` | Authenticated; current user's notification history. |
| PATCH | `/notifications/{id}/read` | Authenticated; recipient only. |
| GET | `/audit?incidentId=&actorId=&action=&targetEntity=&targetId=&from=&to=` | `ADMIN`; optional filters. Target entity values include `USER`, `IT_SERVICE`, `INCIDENT_CATEGORY`, and `INCIDENT_FEEDBACK`. `from` and `to` are ISO date-time values. |
| GET | `/health` | Public. |

SLA status data includes policy, response/resolution targets and deadlines, acknowledgement/resolution times, status, and response/resolution violation flags. Notification records include read state. Email delivery is simulated by the backend.

The SLA history endpoint also returns escalation actions (`SLA_ESCALATED_RESPONSE_WARNING`, `SLA_ESCALATED_RESOLUTION_WARNING`, `SLA_ESCALATED_RESPONSE_BREACH`, and `SLA_ESCALATED_RESOLUTION_BREACH`). These actions are persisted to SLA history and audit logs and publish an in-app notification. Defaults are a warning at 20% of target remaining and escalation at 10%; the escalation percentage must be positive and less than the warning percentage. `SLA_MONITOR_INTERVAL_MS` defaults to 30 seconds. Tracking-row pessimistic locks and database-persisted one-time markers prevent duplicate escalation work across monitor instances.

User creation, user activation/deactivation and role changes; IT service creation/updates/activation/deactivation; incident category creation/updates/activation/deactivation; and employee feedback submissions append audit entries in the same transaction as the action. Entries include the acting user when available, action, target type/ID, timestamp and safe change values. Feedback comments, passwords, password hashes, JWTs and secrets are not copied into audit details. Existing audit fields and response fields remain available; generic target fields are additive. Audit history access is restricted to administrators.

## Reports, analytics, search and directories

| Method | Endpoint | Access / behavior |
|---|---|---|
| GET | `/incidents/search` | Authenticated, server-side filters `id`, `query`, `status`, `priority`, `category`, `assigneeId`, `createdFrom`, `createdTo`, `slaStatus`; `page` starts at 0 and `size` is 1–100. Employees are always scoped to their own reports; technicians to their assigned incidents and unassigned queue items. Returns `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`. Date bounds are inclusive `createdFrom` and exclusive `createdTo`. |
| GET | `/reports/summary?from=&to=` | `ADMIN`; incident KPIs, SLA warning/breach counts, compliance and average resolution seconds. Dates are optional ISO instants; `to` is exclusive. Averages are null when no resolved samples exist. |
| GET | `/reports/analytics?from=&to=` | `ADMIN`; database-aggregated counts by status, priority, category and creation date, resolved/unresolved totals and SLA statistics. |
| GET | `/reports/technicians?from=&to=` | `ADMIN`; current active workload and date-filtered assigned/resolved counts and mean resolution duration by technician. |
| GET | `/reports/technicians/me?from=&to=` | `TECHNICIAN`; the caller's metrics only. |
| GET | `/users/technicians` | `ADMIN`; technician directory with availability, capacity and current workload, including technicians without a profile. |
| GET | `/reports/exports/incidents.csv` | `ADMIN`; accepts incident search filters, streams matching rows. |
| GET | `/reports/exports/sla.csv?from=&to=` | `ADMIN`; streams SLA tracking rows for the date range. |
| GET | `/reports/exports/technicians.csv?from=&to=` | `ADMIN`; streams technician workload report. |

CSV fields are escaped and formula-prefixed values are neutralized. Report counts are created-date scoped; technician active workload is a current snapshot. Aggregations run in the database; incident CSV uses bounded pages.
