package com.iitsm.intelligence.service;

import com.iitsm.incident.entity.IncidentPriority;
import com.iitsm.intelligence.dto.TriageSuggestion;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete triage engine analyzing keywords for priority and category estimation.
 * Demonstrates Polymorphism implementing IncidentTriageEngine.
 */
@Component
public class RuleBasedTriageEngine implements IncidentTriageEngine {

    @Override
    public String getEngineType() {
        return "RULE_BASED";
    }

    @Override
    public TriageSuggestion evaluate(String title, String description) {
        String combined = (title + " " + description).toLowerCase();
        List<String> rationale = new ArrayList<>();

        IncidentPriority priority = IncidentPriority.MEDIUM;
        String category = "SOFTWARE";
        double confidence = 0.70;

        if (combined.contains("down") || combined.contains("outage") || combined.contains("security breach") || combined.contains("critical")) {
            priority = IncidentPriority.CRITICAL;
            rationale.add("Detected high-severity keywords: 'outage' / 'down' / 'breach'");
            confidence = 0.95;
        } else if (combined.contains("slow") || combined.contains("error") || combined.contains("fails")) {
            priority = IncidentPriority.HIGH;
            rationale.add("Detected performance failure keywords: 'error' / 'slow'");
            confidence = 0.85;
        } else {
            rationale.add("Standard incident characteristics matched default profile");
        }

        if (combined.contains("network") || combined.contains("vpn") || combined.contains("dns") || combined.contains("wifi")) {
            category = "NETWORK";
            rationale.add("Matched network connectivity domain vocabulary");
        } else if (combined.contains("password") || combined.contains("login") || combined.contains("access") || combined.contains("permission")) {
            category = "ACCESS";
            rationale.add("Matched identity and access management domain vocabulary");
        } else if (combined.contains("laptop") || combined.contains("monitor") || combined.contains("hardware")) {
            category = "HARDWARE";
            rationale.add("Matched physical device domain vocabulary");
        }

        return new TriageSuggestion(priority, category, confidence, rationale);
    }
}
