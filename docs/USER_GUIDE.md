# I-ITSM User Guide

## Sign in and navigation

1. Start the backend and frontend using the commands in [README.md](../README.md).
2. Open `http://localhost:5173` and choose **Sign in**.
3. Employees can choose **Need an account? Register**. Public registration always creates an EMPLOYEE account.
4. Technician and administrator accounts must already be provisioned by a trusted operator; this application has no public role-elevation flow.
5. The top navigation provides Dashboard, Incidents, Notifications and System health. Administration appears only for an ADMIN session.

## Employee workflow

1. On Dashboard or Incidents, choose **Report incident**.
2. Enter title, description, category, select an active IT service and provide impact inputs; submit. The application first displays any duplicate suggestions for review. You may continue with a separate incident.
3. Open a row to see its priority explanation, status, activity, SLA deadlines and current assignment.
4. Use **Edit** while permitted to update your report. Add an employee response in the activity form.
5. After resolution, submit a one-time 1–5 rating and optional comments from the incident detail panel. Only the reporting employee can submit it.
6. When a technician resolves the incident, choose **Verify resolution**.
7. Open **Notifications** to review updates and mark them read.

## Technician workflow

1. Sign in with the provisioned technician account. Open **Dashboard** or **Incidents** to view operational work; filter by state, priority, category, dates or SLA status.
2. Open an assigned incident and review its SLA targets/history and related incident suggestions.
3. Choose **Acknowledge**, then **Start investigation**. Add an investigation note or request information.
4. Enter resolution details and choose **Resolve incident**.
5. Review assignment and notification history as needed.

## Administrator workflow

1. Sign in with the provisioned ADMIN account and open **Administration**.
2. **Overview** shows persisted aggregate KPIs. **Reports** accepts a date range and shows grouped charts, workload rows and CSV controls.
3. **Assignment** changes strategy and technician availability, experience, skill and capacity profile.
4. In the incident detail panel, rank recommendations and assign an eligible technician.
5. **SLA policies** manages per-priority elapsed-time response and resolution targets.
6. **Audit history** accepts incident, actor, action, target type, target ID and time-range filters. User, service, category and feedback actions appear alongside incident/SLA history. Feedback comments and credentials are not copied into audit details. Report exports are available in the Reports panel.
7. **Users** searches registered accounts, displays safe user details, activates/deactivates accounts and changes roles. The active administrator cannot change their own role or deactivate their account; the final active administrator cannot be removed.
8. **Categories** creates, renames and activates/deactivates incident categories. Existing incident category labels are retained; inactive categories are unavailable for new and updated reports.
9. **Feedback** reviews employee ratings/comments after incident resolution.
10. **IT services** creates, edits, activates, and deactivates catalog services. Deactivation prevents new selections but does not remove existing incident references.

## Search, empty/error states and data

Incident search and filters are evaluated by the backend. Results are paginated; date end filters are inclusive in the date picker and sent as an exclusive next-day boundary. Dashboard/report data comes from database queries. If a section is empty, check the selected date range and whether incidents/profiles exist. If an error appears, verify that the backend and its database are reachable, then retry.

## Account provisioning and privacy

Account role changes are available only in the administrator workspace and are enforced by the backend. Public registration always creates an employee account. Passwords and database secrets must not be shared in reports or demo materials. User responses never include password hashes. In-app notification history is limited to its recipient; employee incident records and feedback are ownership-scoped.
