package com.iitsm.intelligence;

import com.iitsm.incident.entity.Incident;
import com.iitsm.incident.entity.IncidentPriority;
import com.iitsm.incident.entity.IncidentStatus;
import com.iitsm.incident.repository.IncidentRepository;
import com.iitsm.intelligence.dto.IncidentSimilarityRequest;
import com.iitsm.intelligence.service.IncidentSimilarityService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;

class IncidentSimilarityServiceTest {
    private static final IncidentSimilarityRequest REQUEST = new IncidentSimilarityRequest(
            "VPN connection drops", "VPN connection drops after login for remote staff", "NETWORK", "Corporate VPN");

    @Test void normalizesTextAndReturnsRankedDuplicateSuggestionsButExcludesClosedIncidents() {
        Incident closeMatch = incident("VPN connection drops", "VPN connection drops after login for remote staff", "network", "Corporate VPN", 1L);
        Incident closedMatch = incident("VPN connection drops", "VPN connection drops after login for remote staff", "NETWORK", "Corporate VPN", 2L);
        closeMatch.setTicketNumber("INC-MATCH");
        closedMatch.setTicketNumber("INC-CLOSED");
        close(closedMatch);
        IncidentRepository repository = repository(List.of(closeMatch, closedMatch), Optional.empty());

        var matches = new IncidentSimilarityService(repository, .62, .35, 14).duplicates(REQUEST);

        assertEquals(1, matches.size());
        assertEquals(1L, matches.get(0).incidentId());
        assertEquals("INC-MATCH", matches.get(0).ticketNumber());
        assertEquals("VPN connection drops", matches.get(0).title());
        assertEquals(1.0, matches.get(0).similarity());
        assertTrue(matches.get(0).reasons().contains("same category"));
    }

    @Test void categoryAndServiceAloneDoNotPassDuplicateThreshold() {
        Incident unrelated = incident("Printer paper jam", "Replace tray rollers in the north office printer", "NETWORK", "Corporate VPN", 3L);
        IncidentRepository repository = repository(List.of(unrelated), Optional.empty());

        assertTrue(new IncidentSimilarityService(repository, .62, .35, 14).duplicates(REQUEST).isEmpty());
        assertEquals(1, new IncidentSimilarityService(repository, .20, .35, 14).duplicates(REQUEST).size(),
                "A lower configured threshold may surface weak matches for human review");
    }

    @Test void relatedSignalsIncludeCategoryServiceTextAndTimeWindow() {
        Incident source = incident("VPN connection issue", "Remote staff lose the corporate VPN connection", "NETWORK", "Corporate VPN", 4L);
        Incident related = incident("VPN disconnect", "Remote staff lose connection to the corporate VPN", "NETWORK", "Corporate VPN", 5L);
        source.setCreatedAt(Instant.parse("2026-11-01T10:00:00Z"));
        related.setCreatedAt(Instant.parse("2026-11-03T10:00:00Z"));
        IncidentRepository repository = repository(List.of(source, related), Optional.of(source));

        var matches = new IncidentSimilarityService(repository, .62, .35, 14).related(4L);

        assertEquals(1, matches.size());
        assertEquals(5L, matches.get(0).incidentId());
        assertTrue(matches.get(0).reasons().contains("reported within 3 days"));
    }

    @Test void rejectsInvalidSimilarityConfiguration() {
        IncidentRepository repository = repository(List.of(), Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> new IncidentSimilarityService(repository, 0, .35, 14));
        assertThrows(IllegalArgumentException.class, () -> new IncidentSimilarityService(repository, .7, .35, 0));
    }

    private static Incident incident(String title, String description, String category, String service, long id) {
        Incident incident = new Incident(title, description, IncidentPriority.MEDIUM, category, null);
        incident.setId(id);
        incident.updateDetails(title, description, category, incident.getImpact(), incident.getUrgency(), 3, 1, service, 3);
        incident.setCreatedAt(Instant.parse("2026-11-02T10:00:00Z"));
        return incident;
    }
    private static void close(Incident incident) {
        incident.transitionTo(IncidentStatus.CLASSIFIED); incident.transitionTo(IncidentStatus.ASSIGNED);
        incident.transitionTo(IncidentStatus.ACKNOWLEDGED); incident.transitionTo(IncidentStatus.IN_PROGRESS);
        incident.transitionTo(IncidentStatus.RESOLVED); incident.transitionTo(IncidentStatus.VERIFIED); incident.transitionTo(IncidentStatus.CLOSED);
    }
    private static IncidentRepository repository(List<Incident> values, Optional<Incident> byId) {
        return (IncidentRepository) Proxy.newProxyInstance(IncidentRepository.class.getClassLoader(), new Class<?>[]{IncidentRepository.class}, (proxy, method, args) -> {
            if (method.getName().equals("findAll")) return values;
            if (method.getName().equals("findById")) return byId;
            if (method.getName().equals("toString")) return "IncidentRepository test double";
            if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
            if (method.getName().equals("equals")) return proxy == args[0];
            return null;
        });
    }
}
