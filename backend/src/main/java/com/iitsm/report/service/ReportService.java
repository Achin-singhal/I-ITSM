package com.iitsm.report.service;

import com.iitsm.assignment.repository.TechnicianProfileRepository;
import com.iitsm.incident.entity.IncidentPriority;
import com.iitsm.incident.entity.IncidentStatus;
import com.iitsm.incident.repository.IncidentRepository;
import com.iitsm.report.dto.*;
import com.iitsm.sla.repository.SlaTrackingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ReportService {
    private static final List<IncidentStatus> OPEN_STATUSES = List.of(IncidentStatus.NEW, IncidentStatus.OPEN,
            IncidentStatus.CLASSIFIED, IncidentStatus.ASSIGNED, IncidentStatus.ACKNOWLEDGED);
    private static final List<IncidentStatus> RESOLVED_STATUSES = List.of(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED);
    private static final List<IncidentStatus> TERMINAL_STATUSES = List.of(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED);
    private static final List<IncidentPriority> URGENT_PRIORITIES = List.of(IncidentPriority.CRITICAL, IncidentPriority.HIGH);

    private final IncidentRepository incidents;
    private final SlaTrackingRepository slaTracking;
    private final TechnicianProfileRepository technicians;

    public ReportService(IncidentRepository incidents, SlaTrackingRepository slaTracking, TechnicianProfileRepository technicians) {
        this.incidents = incidents; this.slaTracking = slaTracking; this.technicians = technicians;
    }

    public AnalyticsSummaryResponse getSummary(Instant from, Instant to) {
        validateRange(from, to);
        long total = incidents.countCreatedInRange(from, to);
        long open = incidents.countCreatedWithStatuses(OPEN_STATUSES, from, to);
        long inProgress = incidents.countCreatedWithStatuses(List.of(IncidentStatus.IN_PROGRESS), from, to);
        long resolved = incidents.countCreatedWithStatuses(RESOLVED_STATUSES, from, to);
        long closed = incidents.countCreatedWithStatuses(List.of(IncidentStatus.CLOSED), from, to);
        long urgent = incidents.countCreatedWithPriorities(URGENT_PRIORITIES, from, to);
        Object[] sla = slaAggregate(from, to);
        long targets = number(sla[0]) * 2;
        long warnings = number(sla[1]) + number(sla[2]);
        long breaches = number(sla[3]) + number(sla[4]);
        Double compliance = targets == 0 ? null : ((targets - breaches) * 100.0) / targets;
        Object averageValue=incidents.averageResolutionSeconds(from,to);
        Double average=averageValue==null?null:((Number)averageValue).doubleValue();
        return new AnalyticsSummaryResponse(total, open, inProgress, resolved, closed, urgent, warnings, breaches, compliance, average);
    }

    public ReportAnalyticsResponse getAnalytics(Instant from, Instant to) {
        validateRange(from, to);
        AnalyticsSummaryResponse summary = getSummary(from, to);
        Map<String,Long> byStatus = new LinkedHashMap<>();
        for (IncidentStatus value : IncidentStatus.values()) byStatus.put(value.name(), 0L);
        incidents.aggregateStatus(from, to).forEach(row -> byStatus.put(((IncidentStatus) row[0]).name(), number(row[1])));
        Map<String,Long> byPriority = new LinkedHashMap<>();
        for (IncidentPriority value : IncidentPriority.values()) byPriority.put(value.name(), 0L);
        incidents.aggregatePriority(from, to).forEach(row -> byPriority.put(((IncidentPriority) row[0]).name(), number(row[1])));
        Map<String,Long> byCategory = new TreeMap<>();
        incidents.aggregateCategory(from, to).forEach(row -> byCategory.put(String.valueOf(row[0]), number(row[1])));
        List<TrendPoint> trend = incidents.aggregateCreatedByDate(from, to).stream()
                .map(row -> new TrendPoint(toLocalDate(row[0]), number(row[1]))).toList();
        long resolved = incidents.countCreatedWithStatuses(TERMINAL_STATUSES, from, to);
        long unresolved = summary.totalIncidents() - resolved;
        long totalTargets = number(slaAggregate(from, to)[0]) * 2;
        return new ReportAnalyticsResponse(summary, byStatus, byPriority, byCategory, trend, resolved, unresolved,
                totalTargets, summary.slaBreaches());
    }

    public List<TechnicianMetricsResponse> getTechnicianReports(Instant from, Instant to) {
        validateRange(from,to);
        return technicianMetrics(from,to,null);
    }

    public Optional<TechnicianMetricsResponse> getTechnicianReport(Long userId, Instant from, Instant to) {
        validateRange(from,to);
        return technicianMetrics(from,to,userId).stream().findFirst();
    }

    public List<TechnicianDirectoryResponse> technicianDirectory() {
        return technicians.directoryWithWorkload().stream().map(row -> new TechnicianDirectoryResponse(
                number(row[0]), String.valueOf(row[1]), String.valueOf(row[2]), Boolean.TRUE.equals(row[3]),
                Math.toIntExact(number(row[4])), number(row[5]))).toList();
    }

    public static void validateRange(Instant from, Instant to) {
        if (from != null && to != null && !from.isBefore(to))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"from must be earlier than to; the to boundary is exclusive");
    }
    private static TechnicianMetricsResponse technicianMetric(Object[] row) {
        Double average = row[8] == null ? null : ((Number)row[8]).doubleValue();
        return new TechnicianMetricsResponse(number(row[0]),String.valueOf(row[1]),String.valueOf(row[2]),
                Boolean.TRUE.equals(row[3]),Math.toIntExact(number(row[4])),number(row[5]),number(row[6]),number(row[7]),average);
    }
    private List<TechnicianMetricsResponse> technicianMetrics(Instant from,Instant to,Long technicianId) {
        return technicians.aggregateTechnicianReports(from,to,technicianId).stream().map(ReportService::technicianMetric).toList();
    }
    private static long number(Object value) { return value == null ? 0L : ((Number)value).longValue(); }
    private Object[] slaAggregate(Instant from,Instant to) {
        Object[] result=slaTracking.aggregateReportData(from,to);
        return result.length==1 && result[0] instanceof Object[] nested ? nested : result;
    }
    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate date) return date;
        if (value instanceof Date date) return date.toLocalDate();
        if (value instanceof java.util.Date date) return date.toInstant().atZone(java.time.ZoneOffset.UTC).toLocalDate();
        return LocalDate.parse(String.valueOf(value));
    }
}
