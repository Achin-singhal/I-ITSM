# Academic Demonstration Script (18 minutes)

## Before the demo

- Start backend with a disposable local PostgreSQL database, or the test H2 profile for a non-persistent demo; start the Vite dev server.
- Prepare one employee, one available technician with `APPLICATION` skill, and one administrator account. Provision non-employee roles through the trusted test setup; no credentials are included in this document.
- Ensure the selected priority has an active SLA policy. For a visible warning within the demo, set a short response target and warning/escalation thresholds in the local environment, restart, and create the incident after startup. The scheduled checker runs at its configured interval; a breach may occur only after the target expires.
- Keep a second similar incident available for duplicate/correlation review, or create it during setup.

| Time | Screen and navigation | Demonstration / narration |
|---|---|---|
| 0:00–2:00 | Public overview, then sign in | Introduce the service desk problem and the React/Vite → Spring Boot modular monolith → PostgreSQL architecture. Explain JWT bearer security and the three roles. |
| 2:00–5:00 | Employee: Dashboard → Report incident | Enter a payment/application outage. Review the duplicate suggestion, continue only after human review, submit. Open detail to show generated ticket, priority score/explanation, status and SLA targets. |
| 5:00–6:00 | Employee: Notifications | Show persisted creation/assignment notices. Open incident activity and add an employee response if the form is available. |
| 6:00–9:00 | Administrator: Administration → Assignment; return to incident | Show strategy configuration, directory/profile data. Open an unassigned incident, request ranked recommendations, narrate skill/workload/availability/capacity reasons, select a technician. |
| 9:00–12:00 | Technician: Dashboard → assigned incident | Sign in as the technician. Acknowledge, start investigation, add a note, review SLA deadline/history and related incidents, then enter resolution details and resolve. |
| 12:00–14:00 | Administrator: Administration → SLA policies; incident SLA history | Show response/resolution targets. Return to incident SLA history for warning/escalation/violation events if the configured monitor has reached the thresholds. Explain that email delivery is simulated and the in-app notification is persisted. |
| 14:00–17:00 | Employee verification; administrator → Audit history and Reports | Employee verifies resolution. Admin filters audit entries and shows analytics/date range, technician workload, incident/SLA/technician CSV exports. Mention that reports are database aggregates, not demo constants. |
| 17:00–18:00 | Architecture explanation | Explain Strategy + Factory for assignment, Observer for incident notifications, interfaces/polymorphism, encapsulated lifecycle transitions, entity inheritance, transaction/locking safeguards and local Jaccard similarity. State known limits: rule heuristics, simulated email, elapsed-time SLA and no live PostgreSQL validation in this workspace. |

## Demo truthfulness

Do not claim real email delivery, model-based AI, guaranteed live SLA escalation before the scheduled threshold, or PostgreSQL validation unless separately performed. Do not expose real credentials or production data on screen.
