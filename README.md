# I-ITSM: Intelligent IT Service Management and Incident Resolution System

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://reactjs.org/)
[![Vite](https://img.shields.io/badge/Vite-5-purple.svg)](https://vitejs.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind-3.4-38B2AC.svg)](https://tailwindcss.com/)
[![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL%20(Supabase)-336791.svg)](https://supabase.com/)

---

## 1. Project Overview

**I-ITSM** is an enterprise-grade academic collaborative project designed to model an Intelligent IT Service Management and Incident Resolution platform. The system is engineered as a **Modular Monolith** using **Java 21**, **Spring Boot**, **Spring Data JPA**, **Spring Security**, and a modern **React (Vite) + Tailwind CSS** frontend.

### IT service catalog and Phase 5 reporting
The administrator workspace includes a persistent IT service catalog. Incident records may reference a managed service while retaining the legacy `affected_service` text for existing rows and clients. The workspace also includes database-backed incident analytics, date-filtered report charts, technician workload metrics, backend-filtered incident search with pagination, and authorized CSV exports. See [API_CONTRACT.md](docs/API_CONTRACT.md) and [DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md) for API and schema details. Analytics are computed by backend SQL projections; the UI does not substitute sample metrics.

### Key Highlights:
- **Modular Monolith**: High-cohesion domain modules (`auth`, `user`, `incident`, `servicecatalog`, `assignment`, `sla`, `notification`, `audit`, `intelligence`, `report`).
- **Object-Oriented Design (OOP)**: Clear demonstration of Encapsulation, Inheritance, Abstraction, and Polymorphism.
- **Design Patterns**: 
  - **Strategy Pattern** for dynamic incident assignment routing.
  - **Observer Pattern** for multi-channel decoupled notifications.
  - **Factory Pattern** for strategy resolution.
- **Security**: Stateless JWT authentication, role-based access control (`ADMIN`, `TECHNICIAN`, `EMPLOYEE`), and runtime signing/database secrets supplied through environment variables.
- **Cloud Database Ready**: Configured for Supabase PostgreSQL with SSL support.

---

## 2. Collaborative Team & Module Ownership

The following table records intended team ownership from project documentation; it does not imply every listed extension is complete:

| Student | Assigned Modules | Responsibilities & Design Focus |
| :--- | :--- | :--- |
| **Student 1 (Lead)** | `auth`, `user`, Common Infra | JWT authentication, role authorization, user profile CRUD, global security filters, base entity classes, Supabase DB connection. |
| **Student 2** | `incident`, `audit` | Ticket lifecycle management, domain state transitions, persisted audit history. Audit immutability is not enforced at the database layer. |
| **Student 3** | `assignment`, `sla` | Automated routing using **Strategy Pattern** (Round-Robin, Workload-Balanced, Skill-Based), elapsed-time SLA policies and monitoring. |
| **Student 4** | `notification`, `intelligence`, `report` | Observer-based in-app alerts (email is simulated), local rule/similarity analysis, SLA compliance and analytics. |

---

## 3. Directory Layout

```
I-ITSM/
├── README.md                      # Comprehensive project guide (this file)
├── .env.example                   # Master environment template
├── .gitignore                     # Git ignore rules for Java, Node, IDEs
├── docs/                          # Engineering documentation
│   ├── ARCHITECTURE.md            # Modular Monolith & OOP design pattern details
│   ├── API_CONTRACT.md            # REST API endpoint specs & JSON schemas
│   ├── DATABASE_DESIGN.md         # PostgreSQL schema and migration notes
│   ├── REQUIREMENT_TRACEABILITY.md # Repository-evidence checklist; professor PDF review pending
│   ├── SRS.md / TRD.md            # Implementation-aligned specifications
│   ├── USER_GUIDE.md              # Role workflows
│   ├── TEST_CASES.md              # Test coverage and latest run results
│   ├── DEMO_SCRIPT.md             # 18-minute academic demo plan
│   ├── TECHNOLOGY_SELECTION.md    # Rationale for the implemented technology stack
│   └── diagrams/                  # Source-aligned UML and database diagrams
│       ├── USE_CASE_DIAGRAM.md
│       ├── CLASS_DIAGRAM.md
│       └── ER_DIAGRAM.md
├── frontend/                      # React + Vite + Tailwind CSS Application
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js
│   ├── tailwind.config.js
│   ├── postcss.config.js
│   └── src/
│       ├── App.jsx                # Application shell and page navigation
│       ├── main.jsx               # React entry point
│       ├── index.css              # Tailwind and design system rules
│       ├── components/            # UI components (shadcn/ui style)
│       ├── pages/                 # Landing, Incidents, and Health Check pages
│       └── services/              # Fetch API client
└── backend/                       # Spring Boot Application (Java 21 + Maven)
    ├── pom.xml                    # Maven build descriptor with all starters
    ├── mvnw / mvnw.cmd            # Maven Wrapper scripts
    └── src/
        ├── main/
        │   ├── java/com/iitsm/
        │   │   ├── IitsmApplication.java
        │   │   ├── common/        # BaseEntity, ApiResponse, GlobalExceptionHandler
        │   │   ├── config/        # Security and CORS configuration
        │   │   ├── health/        # Health check endpoint (/api/v1/health)
│   │   ├── auth/          # Authentication & JWT
│   │   ├── user/          # User management
│   │   ├── incident/      # Incident lifecycle, categories & feedback
│   │   ├── servicecatalog/# Managed IT service catalog
        │   │   ├── assignment/    # Strategy Pattern routing
        │   │   ├── sla/           # SLA policies & breach tracking
        │   │   ├── notification/  # Observer Pattern alerts
        │   │   ├── audit/         # Audit trailing
        │   │   ├── intelligence/  # Triage suggestion engine
        │   │   └── report/        # Reporting & metrics
        │   └── resources/
        │       └── application.yml# Environment variable driven configuration
        └── test/                  # Unit and integration tests
```

---

## 4. Prerequisites (Windows)

1. **Java Development Kit (JDK 21)**:
   Verify with:
   ```powershell
   java -version
   javac -version
   ```
2. **Node.js (v18+) & npm (v9+)**:
   Verify with:
   ```powershell
   node -v
   npm -v
   ```
3. **Maven (v3.9+) or Maven Wrapper**:
   The backend includes `mvnw.cmd` so a global Maven installation is optional.

---

## 5. Run the Application (Windows PowerShell)

### Backend configuration

The backend reads configuration from process environment variables. Spring Boot does not automatically load the root `.env.example` file. For a local disposable H2 run, open PowerShell in `backend` and set:

```powershell
$env:SPRING_PROFILES_ACTIVE = "test"
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
.\mvnw.cmd spring-boot:run
```

The `test` profile uses an in-memory H2 database in PostgreSQL compatibility mode. Its data is temporary and is discarded when the process stops. For persistent PostgreSQL, set `SPRING_PROFILES_ACTIVE=dev`, `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` to values for a database you control. Flyway migrations run at startup and Hibernate validates the resulting schema. Never use production credentials in source files or commit them.

| Variable | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` (default) for PostgreSQL or `test` for H2 |
| `DB_URL` | JDBC URL, for example `jdbc:postgresql://localhost:5432/i_itsm_db` |
| `DB_USERNAME`, `DB_PASSWORD` | Database credentials |
| `JWT_SECRET` | Random signing secret, at least 32 bytes; required at startup |
| `JWT_EXPIRATION_MS` | Token lifetime in milliseconds (default 86,400,000) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed browser origins |
| `DUPLICATE_SIMILARITY_THRESHOLD` | Duplicate suggestion threshold from 0 (exclusive) to 1 (default `0.62`) |
| `RELATED_INCIDENT_THRESHOLD` | Related-incident score threshold from 0 (exclusive) to 1 (default `0.35`) |
| `RELATED_INCIDENT_WINDOW_DAYS` | Maximum created-time distance for correlation (default `14`) |
| `SLA_WARNING_THRESHOLD_PERCENT` | Remaining target fraction that triggers an SLA warning (default `0.20`) |
| `SLA_ESCALATION_THRESHOLD_PERCENT` | Remaining target fraction that triggers escalation (default `0.10`, must be below warning threshold) |
| `SLA_MONITOR_INTERVAL_MS` | Scheduled SLA monitor interval (default `30000`) |

Check the public health endpoint at `http://localhost:8080/api/v1/health`.

### Frontend setup

In a second PowerShell terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). The default Vite proxy forwards `/api` to `http://localhost:8080`. To use another API host, create `frontend/.env` and set `VITE_API_BASE_URL` to the `/api/v1` base path. Frontend environment variables are public build-time configuration: do not put credentials or signing secrets in `VITE_*` values.

### Accounts and roles

Use the registration screen to create an employee account; public registration always assigns `EMPLOYEE`. The backend does not seed demo accounts. Administrator role management is available only to authenticated administrators. Technician accounts must be provisioned by a trusted operator.

#### One-time first administrator bootstrap

The first administrator can be created through the opt-in backend bootstrap. It is disabled by default and must only be enabled for one interactive local startup when the database has no `ADMIN` account. Stop any existing backend instance first. From a PowerShell terminal in `backend`, set the non-secret account details and explicitly enable the bootstrap:

```powershell
$env:BOOTSTRAP_ADMIN_ENABLED = 'true'
$env:BOOTSTRAP_ADMIN_EMAIL = Read-Host 'First administrator email'
$env:BOOTSTRAP_ADMIN_FULL_NAME = Read-Host 'First administrator full name'
.\mvnw.cmd spring-boot:run
```

The backend prompts for the password using `Console.readPassword()`, so it is not echoed. Do not put the password in an environment variable, command, argument, or project file. The bootstrap refuses to create an account if any `ADMIN` already exists; it does not promote or modify an existing user. Run it in a real interactive terminal, not a background process or IDE console without `System.console()` support.

After the success message, stop the backend with **Ctrl+C**, clear the one-time settings, and start the backend normally:

```powershell
Remove-Item Env:BOOTSTRAP_ADMIN_ENABLED, Env:BOOTSTRAP_ADMIN_EMAIL, Env:BOOTSTRAP_ADMIN_FULL_NAME
.\mvnw.cmd spring-boot:run
```

Never leave `BOOTSTRAP_ADMIN_ENABLED=true` in persistent machine/user environment settings. The bootstrap is for initial local provisioning; normal registration remains employee-only.

---

## 6. Testing & Build Commands

### Frontend Production Build:
```powershell
cd frontend
npm run build
```

### Backend Build & Tests:
```powershell
cd backend
.\mvnw.cmd test
```

The frontend currently has no automated test script; `npm run build` is its available production verification.

## 8. Academic evaluation documents

- [Requirement checklist and source limits](docs/REQUIREMENT_TRACEABILITY.md)
- [Implementation-aligned SRS](docs/SRS.md)
- [Technical design report](docs/TRD.md)
- [Role-based user guide](docs/USER_GUIDE.md)
- [Test cases and verification record](docs/TEST_CASES.md)
- [18-minute demonstration plan](docs/DEMO_SCRIPT.md)
- [Use case diagram](docs/diagrams/USE_CASE_DIAGRAM.md)
- [Implementation class diagram](docs/diagrams/CLASS_DIAGRAM.md)
- [Entity relationship diagram](docs/diagrams/ER_DIAGRAM.md)
- [Technology selection report](docs/TECHNOLOGY_SELECTION.md)

The workspace contains the professor project-statement PDF, but its contents could not be read in this environment; no exact claim of professor-statement coverage is made. No separate PRS/TRD source files were found. See the traceability document before final academic submission.

## 7. Phase 4 intelligent features

All Phase 4 intelligence runs locally in the backend. Duplicate suggestions use normalized Unicode text and token-set Jaccard similarity: title (35%), description (40%), category (15%), and affected service (10%). Closed incidents are excluded. The score and match reasons are returned for human review; matches are never merged automatically. Related-incident suggestions combine text, category, affected service, and created-time proximity within the configured time window.

Technician ranking reuses the configured assignment `Strategy`. Unavailable, inactive, and at-capacity technicians are filtered by the existing candidate rule. The response explains category-skill match, active workload, and recorded experience; a selected eligible recommendation can be assigned through the authorized assignment API.

The scheduled SLA monitor records warning, escalation, and breach actions in SLA history, audit logs, and in-app notifications. Pessimistic locks on SLA tracking rows serialize processing across application instances, while persisted flags prevent duplicate warning/escalation processing. These targets are elapsed-time targets. The algorithm is explainable but heuristic; it is not a learned model. See [API_CONTRACT.md](docs/API_CONTRACT.md) for routes and [ARCHITECTURE.md](docs/ARCHITECTURE.md) for design and limitations.
