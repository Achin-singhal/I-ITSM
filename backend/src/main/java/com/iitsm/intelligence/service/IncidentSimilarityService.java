package com.iitsm.intelligence.service;

import com.iitsm.incident.entity.Incident;
import com.iitsm.incident.entity.IncidentStatus;
import com.iitsm.incident.repository.IncidentRepository;
import com.iitsm.intelligence.dto.IncidentSimilarityMatch;
import com.iitsm.intelligence.dto.IncidentSimilarityRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** Local, explainable token-Jaccard matching for duplicates and related incidents. */
@Service
public class IncidentSimilarityService {
    private static final Set<IncidentStatus> CLOSED = Set.of(IncidentStatus.CLOSED);
    private final IncidentRepository incidents;
    private final double duplicateThreshold;
    private final double relatedThreshold;
    private final long relatedWindowDays;

    public IncidentSimilarityService(IncidentRepository incidents,
            @Value("${app.intelligence.duplicate-threshold:0.62}") double duplicateThreshold,
            @Value("${app.intelligence.related-threshold:0.35}") double relatedThreshold,
            @Value("${app.intelligence.related-window-days:14}") long relatedWindowDays) {
        if (!Double.isFinite(duplicateThreshold) || duplicateThreshold <= 0 || duplicateThreshold > 1) throw new IllegalArgumentException("app.intelligence.duplicate-threshold must be greater than 0 and at most 1");
        if (!Double.isFinite(relatedThreshold) || relatedThreshold <= 0 || relatedThreshold > 1) throw new IllegalArgumentException("app.intelligence.related-threshold must be greater than 0 and at most 1");
        if (relatedWindowDays < 1) throw new IllegalArgumentException("app.intelligence.related-window-days must be at least 1");
        this.incidents = incidents;
        this.duplicateThreshold = duplicateThreshold;
        this.relatedThreshold = relatedThreshold;
        this.relatedWindowDays = relatedWindowDays;
    }

    @Transactional(readOnly = true)
    public List<IncidentSimilarityMatch> duplicates(IncidentSimilarityRequest request) {
        return incidents.findAll().stream().filter(i -> !CLOSED.contains(i.getStatus()))
                .map(i -> match(request, i, duplicateScore(request, i)))
                .filter(m -> m.similarity() >= duplicateThreshold)
                .sorted(Comparator.comparingDouble(IncidentSimilarityMatch::similarity).reversed().thenComparing(IncidentSimilarityMatch::incidentId))
                .limit(10).toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentSimilarityMatch> related(Long incidentId) {
        Incident source = incidents.findById(incidentId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Incident not found"));
        Instant sourceTime = source.getCreatedAt();
        return incidents.findAll().stream().filter(i -> !Objects.equals(i.getId(), source.getId()))
                .filter(i -> !CLOSED.contains(i.getStatus()))
                .filter(i -> sourceTime != null && i.getCreatedAt() != null && withinDays(Duration.between(sourceTime, i.getCreatedAt()).abs(), relatedWindowDays))
                .map(i -> match(source, i, relatedScore(source, i)))
                .filter(m -> m.similarity() >= relatedThreshold)
                .sorted(Comparator.comparingDouble(IncidentSimilarityMatch::similarity).reversed().thenComparing(IncidentSimilarityMatch::incidentId))
                .limit(10).toList();
    }

    public double duplicateScore(IncidentSimilarityRequest a, Incident b) {
        double title = jaccard(tokens(a.title()), tokens(b.getTitle()));
        double description = jaccard(tokens(a.description()), tokens(b.getDescription()));
        double category = normalized(a.category()).equals(normalized(b.getCategory())) ? 1 : 0;
        double service = jaccard(tokens(a.affectedService()), tokens(b.getAffectedService()));
        return round(.35 * title + .40 * description + .15 * category + .10 * service);
    }

    private double relatedScore(Incident a, Incident b) {
        double description = jaccard(tokens(a.getTitle() + " " + a.getDescription()), tokens(b.getTitle() + " " + b.getDescription()));
        double category = normalized(a.getCategory()).equals(normalized(b.getCategory())) ? 1 : 0;
        double service = jaccard(tokens(a.getAffectedService()), tokens(b.getAffectedService()));
        double days = Duration.between(a.getCreatedAt(), b.getCreatedAt()).abs().toMillis() / 86_400_000.0;
        double temporal = 1 - Math.min(1, days / relatedWindowDays);
        return round(.50 * description + .20 * category + .20 * service + .10 * temporal);
    }

    private IncidentSimilarityMatch match(IncidentSimilarityRequest request, Incident candidate, double score) {
        List<String> reasons = new ArrayList<>();
        if (normalized(request.category()).equals(normalized(candidate.getCategory()))) reasons.add("same category");
        if (jaccard(tokens(request.affectedService()), tokens(candidate.getAffectedService())) >= .5) reasons.add("similar affected service");
        if (jaccard(tokens(request.title()), tokens(candidate.getTitle())) >= .3) reasons.add("similar title");
        if (jaccard(tokens(request.description()), tokens(candidate.getDescription())) >= .25) reasons.add("similar description");
        return new IncidentSimilarityMatch(candidate.getId(), candidate.getTicketNumber(), candidate.getTitle(), candidate.getStatus(), score, List.copyOf(reasons));
    }

    private IncidentSimilarityMatch match(Incident source, Incident candidate, double score) {
        List<String> reasons = new ArrayList<>();
        if (normalized(source.getCategory()).equals(normalized(candidate.getCategory()))) reasons.add("same category");
        if (jaccard(tokens(source.getAffectedService()), tokens(candidate.getAffectedService())) >= .5) reasons.add("similar affected service");
        if (withinDays(Duration.between(source.getCreatedAt(), candidate.getCreatedAt()).abs(), 3)) reasons.add("reported within 3 days");
        if (jaccard(tokens(source.getTitle() + " " + source.getDescription()), tokens(candidate.getTitle() + " " + candidate.getDescription())) >= .25) reasons.add("similar incident text");
        return new IncidentSimilarityMatch(candidate.getId(), candidate.getTicketNumber(), candidate.getTitle(), candidate.getStatus(), score, List.copyOf(reasons));
    }

    private static Set<String> tokens(String value) {
        String normalized = normalized(value);
        return Arrays.stream(normalized.split("\\s+" )).filter(token -> token.length() > 1).collect(Collectors.toSet());
    }
    private static String normalized(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ").trim().replaceAll("\\s+", " ");
    }
    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 0;
        Set<String> union = new HashSet<>(a); union.addAll(b);
        Set<String> intersection = new HashSet<>(a); intersection.retainAll(b);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }
    private static double round(double value) { return Math.round(value * 1000.0) / 1000.0; }
    private static boolean withinDays(Duration duration,long days) { return duration.compareTo(Duration.ofDays(days)) <= 0; }
}
