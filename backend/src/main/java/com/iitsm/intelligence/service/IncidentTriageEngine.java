package com.iitsm.intelligence.service;

import com.iitsm.intelligence.dto.TriageSuggestion;

/**
 * Strategy/Engine interface for intelligent triage of incoming tickets.
 * Demonstrates Abstraction for AI / rule-based triage providers.
 */
public interface IncidentTriageEngine {

    /**
     * Engine identifier (e.g. RULE_BASED, LLM_BASED).
     */
    String getEngineType();

    /**
     * Evaluates ticket text and returns recommended priority and category.
     *
     * @param title Ticket title
     * @param description Ticket description
     * @return Triage recommendation
     */
    TriageSuggestion evaluate(String title, String description);
}
