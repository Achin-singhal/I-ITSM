package com.iitsm.sla;

import com.iitsm.audit.entity.AuditLog;
import com.iitsm.audit.repository.AuditLogRepository;
import com.iitsm.audit.service.AuditService;
import com.iitsm.incident.entity.Incident;
import com.iitsm.incident.entity.IncidentPriority;
import com.iitsm.incident.repository.IncidentRepository;
import com.iitsm.notification.observer.IncidentEvent;
import com.iitsm.notification.observer.IncidentEventObserver;
import com.iitsm.notification.service.NotificationPublisher;
import com.iitsm.sla.entity.SlaHistoryEntry;
import com.iitsm.sla.entity.SlaPolicy;
import com.iitsm.sla.entity.SlaStatus;
import com.iitsm.sla.entity.SlaTracking;
import com.iitsm.sla.repository.SlaHistoryRepository;
import com.iitsm.sla.repository.SlaPolicyRepository;
import com.iitsm.sla.repository.SlaTrackingRepository;
import com.iitsm.sla.service.SlaService;
import com.iitsm.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SlaServiceTest {
    @Test void rejectsInvalidWarningAndEscalationThresholds() {
        assertThrows(IllegalArgumentException.class, () -> new SlaService(null,null,null,null,null,null,null,1.2,.1));
        assertThrows(IllegalArgumentException.class, () -> new SlaService(null,null,null,null,null,null,null,.2,.2));
    }

    @Test void calculatesBothElapsedTimeDeadlinesFromCreation() {
        Incident incident = new Incident("x", "y", IncidentPriority.CRITICAL, "SOFTWARE", null);
        Instant created = Instant.parse("2026-11-14T10:00:00Z");
        incident.setCreatedAt(created);
        SlaTracking tracking = new SlaTracking(incident, new SlaPolicy("P1", IncidentPriority.CRITICAL, 5, 30));

        assertEquals(created.plusSeconds(300), tracking.getResponseDeadline());
        assertEquals(created.plusSeconds(1800), tracking.getResolutionDeadline());
    }

    @Test void generatesOneWarningBeforeDeadlineAndPersistsAtRiskState() {
        Incident incident = incidentCreatedSecondsAgo(270);
        SlaTracking tracking = tracking(incident);
        RecordingCollaborators collaborators = new RecordingCollaborators(tracking);

        collaborators.service.monitorDeadlines();
        collaborators.service.monitorDeadlines();

        assertTrue(tracking.isResponseWarningSent());
        assertEquals(SlaStatus.AT_RISK, tracking.getStatus());
        assertEquals(1, collaborators.audits.stream().filter(a -> a.getAction().equals("SLA_RESPONSE_WARNING")).count());
        assertEquals(1, collaborators.audits.stream().filter(a -> a.getAction().equals("SLA_ESCALATED_RESPONSE_WARNING")).count());
        assertEquals(1, collaborators.events.stream().filter(e -> e.getEventType() == IncidentEvent.EventType.SLA_WARNING).count());
        assertEquals(1, collaborators.events.stream().filter(e -> e.getEventType() == IncidentEvent.EventType.SLA_ESCALATED).count());
        assertEquals(2, collaborators.history.size());
        assertEquals(2, collaborators.trackingReads.get());
    }

    @Test void recordsSlaViolationOnceWhenDeadlineHasPassed() {
        Incident incident = incidentCreatedSecondsAgo(600);
        SlaTracking tracking = tracking(incident);
        RecordingCollaborators collaborators = new RecordingCollaborators(tracking);

        collaborators.service.monitorDeadlines();
        collaborators.service.monitorDeadlines();

        assertTrue(tracking.isResponseViolationRecorded());
        assertEquals(SlaStatus.BREACHED, tracking.getStatus());
        assertEquals(1, collaborators.audits.stream().filter(a -> a.getAction().equals("SLA_RESPONSE_VIOLATED")).count());
        assertEquals(1, collaborators.audits.stream().filter(a -> a.getAction().equals("SLA_ESCALATED_RESPONSE_BREACH")).count());
        assertEquals(1, collaborators.events.stream().filter(e -> e.getEventType() == IncidentEvent.EventType.SLA_VIOLATED).count());
        assertEquals(1, collaborators.events.stream().filter(e -> e.getEventType() == IncidentEvent.EventType.SLA_ESCALATED).count());
        assertEquals(2, collaborators.history.size());
    }

    private static Incident incidentCreatedSecondsAgo(long seconds) {
        Incident incident = new Incident("x", "y", IncidentPriority.CRITICAL, "SOFTWARE", null);
        incident.setCreatedAt(Instant.now().minusSeconds(seconds));
        return incident;
    }

    private static SlaTracking tracking(Incident incident) {
        return new SlaTracking(incident, new SlaPolicy("P1", IncidentPriority.CRITICAL, 5, 30));
    }

    private static final class RecordingCollaborators {
        private final List<AuditLog> audits = new ArrayList<>();
        private final List<SlaHistoryEntry> history = new ArrayList<>();
        private final List<IncidentEvent> events = new ArrayList<>();
        private final AtomicInteger trackingReads = new AtomicInteger();
        private final SlaService service;

        private RecordingCollaborators(SlaTracking tracking) {
            SlaPolicyRepository policies = proxy(SlaPolicyRepository.class, (method, args) -> defaultValue(method));
            SlaTrackingRepository trackings = proxy(SlaTrackingRepository.class, (method, args) -> {
                if (method.getName().equals("lockUnresolved")) {
                    trackingReads.incrementAndGet();
                    return List.of(tracking);
                }
                if (method.getName().equals("findByIncidentId") || method.getName().equals("findByIncidentIdForUpdate")) return Optional.of(tracking);
                if (method.getName().equals("save")) return args[0];
                return defaultValue(method);
            });
            IncidentRepository incidents = proxy(IncidentRepository.class, (method, args) -> defaultValue(method));
            UserRepository users = proxy(UserRepository.class, (method, args) -> defaultValue(method));
            SlaHistoryRepository histories = proxy(SlaHistoryRepository.class, (method, args) -> {
                if (method.getName().equals("save")) {
                    history.add((SlaHistoryEntry) args[0]);
                    return args[0];
                }
                return defaultValue(method);
            });
            AuditLogRepository auditRepository = proxy(AuditLogRepository.class, (method, args) -> {
                if (method.getName().equals("save")) {
                    audits.add((AuditLog) args[0]);
                    return args[0];
                }
                return defaultValue(method);
            });
            AuditService audit = new AuditService(auditRepository);
            IncidentEventObserver observer = new IncidentEventObserver() {
                @Override public String getChannelName() { return "TEST"; }
                @Override public void onIncidentEvent(IncidentEvent event) { events.add(event); }
            };
            NotificationPublisher notifications = new NotificationPublisher(List.of(observer));
            service = new SlaService(policies, trackings, incidents, users, audit, notifications, histories);
        }
    }

    private static <T> T proxy(Class<T> type, RepositoryCall call) {
        InvocationHandler handler = (Object proxy, Method method, Object[] rawArgs) -> {
            Object[] args = rawArgs == null ? new Object[0] : rawArgs;
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> type.getSimpleName() + " test double";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                };
            }
            return call.invoke(method, args);
        };
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    private static Object defaultValue(Method method) {
        Class<?> type = method.getReturnType();
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (Optional.class.isAssignableFrom(type)) return Optional.empty();
        if (List.class.isAssignableFrom(type)) return List.of();
        return null;
    }

    @FunctionalInterface
    private interface RepositoryCall {
        Object invoke(Method method, Object[] args) throws Throwable;
    }
}
