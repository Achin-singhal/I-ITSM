# I-ITSM Technology Selection Report

## Implemented system and architecture

The repository implements a **modular monolith**: one Spring Boot application and deployable backend, organized into cohesive Java packages (`auth`, `user`, `incident`, `assignment`, `sla`, `notification`, `audit`, `intelligence`, `report`, and shared infrastructure). Modules collaborate through injected services and domain abstractions. The React/Vite frontend is a separate client application that calls REST APIs. Persistence is centralized through Spring Data JPA and Flyway-managed PostgreSQL schema migrations. The project is not currently split into independently deployed microservices.

The project files establish the stack below. The explanations of why it fits are design rationale; the repository does not provide an experiment comparing frameworks or databases.

## Professor's nine technology questions

### 1. Why did you choose this programming language?

**Implemented choice: Java 21.** Java is the backend language used in `backend/pom.xml` and the Spring source. Its static typing, mature JVM ecosystem, class/interface model, access control, and exception model support a domain-oriented OOP application. Java 21 supplies a current LTS baseline for the Spring Boot 3.3.5 application. No alternative language was experimentally benchmarked.

### 2. Why did you choose this frontend technology?

**Implemented choice: React 18, Vite, and Tailwind CSS.** React components support reusable page and UI composition; Vite provides the development server and production bundling configured by `frontend/vite.config.js`; Tailwind utilities are used by the existing UI styling. This combination supports a separately developed single-page client that consumes the backend API. No frontend alternative was experimentally tested.

### 3. Why did you choose this backend technology?

**Implemented choice: Spring Boot 3.3.5 with Spring MVC, Spring Security, Spring Data JPA, and Maven.** Spring Boot supplies application configuration and dependency management; MVC exposes REST controllers; Security integrates JWT authentication and role checks; Spring Data JPA maps domain entities and repositories; Maven declares dependencies, plugins, and Java version. These are actual dependencies and source conventions, not merely proposed tools.

### 4. Why did you select this database?

**Implemented target: PostgreSQL.** The dev configuration and V1–V4 Flyway scripts target PostgreSQL; the schema uses identity columns, timezone-aware timestamps, foreign keys, checks, unique constraints, and indexes. PostgreSQL is a suitable relational store for the incident/user/history relationships and transactional persistence. Automated tests also use H2 in PostgreSQL compatibility mode; H2 is not proof of PostgreSQL behavior. **Live PostgreSQL migration and Hibernate schema validation remain pending** because no live database was used for this documentation task.

### 5. Why did you choose this architecture?

**Implemented choice: modular monolith.** It keeps deployment and local operation straightforward for an academic project while grouping related responsibilities into domain modules. Modules retain clear responsibilities but share one application process, security context, transaction manager, and database. This avoids the operational overhead of service discovery, distributed transactions, and multiple deployments while the project is of this scope. These are architecture reasons, not evidence of a measured comparison.

### 6. What alternatives did you consider?

The repository documents no verified record of alternatives being prototyped or tested. Plausible alternatives for a design discussion include Python or C# for the backend; Angular or Vue for the frontend; Node.js for API implementation; MySQL for relational persistence; and independently deployed microservices. These are **hypothetical comparison alternatives**, not claimed implementation decisions or experimentally evaluated candidates. Java/Spring, React/Vite, PostgreSQL, and the modular monolith are the choices actually present in the codebase.

### 7. What are the advantages and limitations of your technology stack?

**Advantages evidenced by the stack and code:** Java/Spring offer mature OOP and security/persistence libraries; Spring Data JPA reduces repetitive persistence plumbing; PostgreSQL enforces relational constraints; Flyway versions schema changes; React components and Tailwind utilities support reusable UI; Maven standardizes the backend build; the modular monolith simplifies local execution and transactions.

**Limitations:** the backend and database are a shared deployment and scaling unit; JPA mappings and migrations require compatibility discipline; H2 tests cannot fully validate PostgreSQL-specific behavior; a single application can become tightly coupled if module boundaries are not maintained; React client and API contracts must evolve together. No performance/load comparison is supplied by the repository.

### 8. How does your technology support object-oriented design?

Java classes, interfaces, access modifiers, enums, inheritance, composition, and exceptions are used directly. Domain entities encapsulate state transitions and validation; entities inherit common identity/timestamps from `BaseEntity`; strategy interfaces enable polymorphic assignment and priority calculation; observer interfaces decouple incident events from notification channels; factories resolve strategies. Spring dependency injection composes services, strategies, and observers. React uses component composition, but the core OOP requirement is demonstrated in the Java domain and service code. See [class diagram](diagrams/CLASS_DIAGRAM.md) for source-aligned examples.

### 9. How would your system evolve if the number of users increased significantly?

The current modular monolith can first be scaled horizontally as a stateless Spring application behind a load balancer, with PostgreSQL connection pooling and monitoring, while shared state remains in PostgreSQL. Query plans, indexes, pagination, aggregation queries, caching of suitable read-heavy data, and background processing can be evaluated based on measured bottlenecks. If independent scaling or team ownership later justifies it, modules with stable boundaries could be extracted selectively behind APIs or messaging. That would introduce distributed-systems complexity and is a **future option**, not implemented architecture. No load test or capacity claim has been made.

## Technology inventory

| Technology | Actual use in repository |
|---|---|
| Java 21 | Backend language / Maven compiler target |
| Spring Boot 3.3.5 | Backend runtime and dependency platform |
| React 18 | Frontend UI |
| Vite | Frontend dev server and build |
| Tailwind CSS | Frontend styling utilities |
| PostgreSQL | Persistent database target in dev configuration and migrations |
| Spring Data JPA | ORM and repository persistence |
| Spring Security | Authentication and role authorization |
| JWT (JJWT) | Signed bearer-token authentication |
| Maven | Backend dependency management, compilation, and tests |
| Flyway | Versioned database migrations V1–V4 |

## Verification boundary

This report was checked against `backend/pom.xml`, backend source and configuration, migration files, and frontend package/configuration files. It describes the implementation available in this repository. No external provider, framework alternative, architecture alternative, or database has been represented as experimentally tested. **Live PostgreSQL migration and Hibernate validation remain pending.**
