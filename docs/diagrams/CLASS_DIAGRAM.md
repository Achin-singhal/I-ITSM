# I-ITSM Implementation Class Diagram

The diagram includes representative domain, service, strategy, observer, and API classes verified in Java source. DTOs and repositories are omitted where they would obscure the domain relationships. Method signatures are abbreviated to the important public operations.

```mermaid
classDiagram
 direction LR

 class BaseEntity {
   <<abstract>>
   -Long id
   -Instant createdAt
   -Instant updatedAt
   +onCreate()
   +onUpdate()
 }
 class User {
   -String email
   -String passwordHash
   -String fullName
   -Role role
   -boolean active
 }
 class Role {
   <<enumeration>>
   EMPLOYEE
   TECHNICIAN
   ADMIN
 }
 class AuthService { +register(RegisterRequest) AuthResponse +login(LoginRequest) AuthResponse }
 class JwtService { +generateToken(Long,String,Role) String +parseAndValidate(String) Claims }
 class JwtAuthenticationFilter
 class SecurityConfig

 class Incident {
   -String ticketNumber
   -String title
   -String description
   -IncidentStatus status
   -IncidentPriority priority
   -String category
   -BigDecimal priorityScore
   -String affectedService
   +assignTo(User)
   +transitionTo(IncidentStatus)
   +acknowledge(User)
   +resolveWith(String)
   +updateDetails(...)
 }
 class IncidentStatus { <<enumeration>> }
 class IncidentPriority { <<enumeration>> }
 class IncidentActivity { -String activityType -String message }
 class IncidentService { +create(...) +update(...) +assign(...) +changeStatus(...) }
 class IncidentController
 class IncidentCategory { -String name -String key -boolean active +rename(String) }
 class ItService { -String name -String serviceKey -String description -boolean active +rename(String) +setActive(boolean) }
 class ItServiceCatalog { +listAll() List +listActive() List +create(ItServiceRequest) ItServiceResponse +update(Long,ItServiceRequest) ItServiceResponse +setActive(Long,boolean) ItServiceResponse }
 class ItServiceRepository { <<interface>> }
 class ItServiceController
 class IncidentFeedback { -int rating -String comments }

 class AssignmentStrategy { <<interface>> +getName() String +recommend(Incident,List) Optional +rank(Incident,List) List }
 class RoundRobinAssignmentStrategy
 class WorkloadAssignmentStrategy
 class SkillBasedAssignmentStrategy
 class AssignmentStrategyFactory { +getStrategy(String) AssignmentStrategy }
 class AssignmentService
 class TechnicianProfile { -boolean available -int experienceYears -int maxActiveWorkload -Set skills +configure(...) }
 class AssignmentRecord { -String strategyUsed -String reason }
 class AssignmentConfiguration { -String strategyName }

 class SlaService
 class SlaPolicy { -String name -IncidentPriority priority -int responseTimeMinutes -int resolutionTimeMinutes -boolean active +configure(...) }
 class SlaTracking { -Instant responseDeadline -Instant resolutionDeadline -SlaStatus status +recordResponseViolation() boolean +recordResolutionViolation() boolean }
 class SlaHistoryEntry { -String action -String description }

 class IncidentEventObserver { <<interface>> +getChannelName() String +onIncidentEvent(IncidentEvent) void }
 class InAppNotificationObserver
 class EmailNotificationObserver
 class IncidentEvent
 class NotificationPublisher
 class Notification { -String title -String message -String channel -boolean read -String deduplicationKey }
 class AuditService
 class AuditLog { -String action -String previousState -String newState -String description }

 class PriorityCalculationStrategy { <<interface>> +calculate(PriorityInput) PriorityDecision }
 class WeightedPriorityCalculationStrategy
 class PriorityStrategyFactory
 class IncidentTriageEngine { <<interface>> +evaluate(String,String) TriageSuggestion }
 class RuleBasedTriageEngine
 class IncidentSimilarityService { +duplicates(IncidentSimilarityRequest) List +related(Long) List }
 class ReportService
 class ReportExportService

 BaseEntity <|-- User
 BaseEntity <|-- Incident
 BaseEntity <|-- IncidentActivity
 BaseEntity <|-- IncidentCategory
 BaseEntity <|-- IncidentFeedback
 BaseEntity <|-- TechnicianProfile
 BaseEntity <|-- AssignmentRecord
 BaseEntity <|-- SlaPolicy
 BaseEntity <|-- SlaTracking
 BaseEntity <|-- SlaHistoryEntry
 BaseEntity <|-- Notification
 BaseEntity <|-- AuditLog

 AssignmentStrategy <|.. RoundRobinAssignmentStrategy
 AssignmentStrategy <|.. WorkloadAssignmentStrategy
 AssignmentStrategy <|.. SkillBasedAssignmentStrategy
 AssignmentStrategyFactory ..> AssignmentStrategy : resolves
 AssignmentService ..> AssignmentStrategy : uses
 PriorityCalculationStrategy <|.. WeightedPriorityCalculationStrategy
 PriorityStrategyFactory ..> PriorityCalculationStrategy : resolves
 IncidentTriageEngine <|.. RuleBasedTriageEngine
 IncidentEventObserver <|.. InAppNotificationObserver
 IncidentEventObserver <|.. EmailNotificationObserver
 NotificationPublisher o--> IncidentEventObserver : notifies

 AuthService ..> JwtService
 JwtAuthenticationFilter ..> JwtService : validates
 SecurityConfig ..> JwtAuthenticationFilter : installs
 IncidentController ..> IncidentService
 IncidentService ..> Incident
 IncidentService ..> SlaService
 IncidentService ..> AuditService
 IncidentService ..> NotificationPublisher
 IncidentService ..> PriorityStrategyFactory
 IncidentSimilarityService ..> Incident
 ReportService ..> Incident
 ReportExportService ..> ReportService

 User "1" <-- "0..*" Incident : reporter
 User "0..1" <-- "0..*" Incident : assignee
 IncidentCategory "0..1" <-- "0..*" Incident : categoryDefinition
 ItService "0..1" <-- "0..*" Incident : serviceDefinition
 ItServiceController ..> ItServiceCatalog
 ItServiceCatalog ..> ItServiceRepository
 Incident "1" <-- "0..*" IncidentActivity
 User "1" <-- "0..*" IncidentActivity : actor
 User "1" <-- "0..1" TechnicianProfile : profile
 TechnicianProfile "1" o-- "0..*" String : skills
 Incident "1" <-- "0..*" AssignmentRecord
 User "1" <-- "0..*" AssignmentRecord : assignedTo
 User "0..1" <-- "0..*" AssignmentRecord : assignedBy
 Incident "1" <-- "0..1" SlaTracking
 SlaPolicy "1" <-- "0..*" SlaTracking
 Incident "1" <-- "0..*" SlaHistoryEntry
 SlaTracking "0..1" <-- "0..*" SlaHistoryEntry
 Incident "1" <-- "0..*" Notification
 User "1" <-- "0..*" Notification : recipient
 Incident "0..1" <-- "0..*" AuditLog
 User "0..1" <-- "0..*" AuditLog : actor
 Incident "1" <-- "0..1" IncidentFeedback
 User "1" <-- "0..*" IncidentFeedback : employee
 User ..> Role : has
 Incident ..> IncidentStatus
 Incident ..> IncidentPriority
 SlaTracking ..> SlaStatus
 AssignmentConfiguration ..> AssignmentStrategy : selected strategy name
 IncidentEvent ..> Incident : event context
 WeightedPriorityCalculationStrategy ..> PriorityInput
 WeightedPriorityCalculationStrategy ..> PriorityDecision
 RuleBasedTriageEngine ..> TriageSuggestion
 IncidentSimilarityService ..> IncidentSimilarityMatch
 ReportService ..> ReportAnalyticsResponse
```

## OOP and pattern evidence

- **Encapsulation:** entity state is private; behavior such as incident transitions, category renaming, technician configuration, and SLA flag updates is implemented on domain objects.
- **Inheritance:** JPA domain entities extend the mapped superclass `BaseEntity` for IDs and timestamps.
- **Abstraction and interfaces:** `AssignmentStrategy`, `PriorityCalculationStrategy`, `IncidentTriageEngine`, and `IncidentEventObserver` describe replaceable behavior.
- **Polymorphism / Strategy:** round-robin, workload, and skill-based assignment strategies implement one interface; the configured strategy is selected through `AssignmentStrategyFactory`. `WeightedPriorityCalculationStrategy` implements priority calculation.
- **Factory:** assignment and priority strategy factories resolve strategy implementations.
- **Observer:** `NotificationPublisher` notifies `IncidentEventObserver` implementations, including the persisted in-app observer and simulated email observer.
- **Composition and association:** services compose collaborators through dependency injection; entity relationships shown are JPA associations. `TechnicianProfile` holds an element collection of skill strings.
- Role is an enum rather than a class hierarchy; no inheritance between Employee, Technician, and Administrator Java classes was found.

## Diagram limits

The diagram shows selected classes, not every controller, repository, DTO, or method. Relationships from JPA fields are concrete; service dependency arrows summarize constructor-injected/use relationships and are not database relationships. `AssignmentConfiguration` persists a strategy name, not a direct JPA link to a strategy object. Email delivery is simulated.
