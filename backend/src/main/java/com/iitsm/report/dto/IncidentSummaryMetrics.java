package com.iitsm.report.dto;

import java.util.Map;

/**
 * Aggregated analytics and metrics for management reporting.
 */
public class IncidentSummaryMetrics {

    private long totalIncidents;
    private long openIncidents;
    private long inProgressIncidents;
    private long resolvedIncidents;
    private long closedIncidents;
    private double slaCompliancePercentage;
    private Map<String, Long> incidentsByPriority;

    public IncidentSummaryMetrics() {
    }

    public IncidentSummaryMetrics(long totalIncidents, long openIncidents, long inProgressIncidents,
                                  long resolvedIncidents, long closedIncidents,
                                  double slaCompliancePercentage, Map<String, Long> incidentsByPriority) {
        this.totalIncidents = totalIncidents;
        this.openIncidents = openIncidents;
        this.inProgressIncidents = inProgressIncidents;
        this.resolvedIncidents = resolvedIncidents;
        this.closedIncidents = closedIncidents;
        this.slaCompliancePercentage = slaCompliancePercentage;
        this.incidentsByPriority = incidentsByPriority;
    }

    public long getTotalIncidents() {
        return totalIncidents;
    }

    public void setTotalIncidents(long totalIncidents) {
        this.totalIncidents = totalIncidents;
    }

    public long getOpenIncidents() {
        return openIncidents;
    }

    public void setOpenIncidents(long openIncidents) {
        this.openIncidents = openIncidents;
    }

    public long getInProgressIncidents() {
        return inProgressIncidents;
    }

    public void setInProgressIncidents(long inProgressIncidents) {
        this.inProgressIncidents = inProgressIncidents;
    }

    public long getResolvedIncidents() {
        return resolvedIncidents;
    }

    public void setResolvedIncidents(long resolvedIncidents) {
        this.resolvedIncidents = resolvedIncidents;
    }

    public long getClosedIncidents() {
        return closedIncidents;
    }

    public void setClosedIncidents(long closedIncidents) {
        this.closedIncidents = closedIncidents;
    }

    public double getSlaCompliancePercentage() {
        return slaCompliancePercentage;
    }

    public void setSlaCompliancePercentage(double slaCompliancePercentage) {
        this.slaCompliancePercentage = slaCompliancePercentage;
    }

    public Map<String, Long> getIncidentsByPriority() {
        return incidentsByPriority;
    }

    public void setIncidentsByPriority(Map<String, Long> incidentsByPriority) {
        this.incidentsByPriority = incidentsByPriority;
    }
}
