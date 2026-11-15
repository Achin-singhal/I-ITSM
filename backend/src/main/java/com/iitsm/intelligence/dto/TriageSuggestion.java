package com.iitsm.intelligence.dto;

import com.iitsm.incident.entity.IncidentPriority;

import java.util.List;

/**
 * Data Transfer Object containing AI/rule recommendation for ticket classification.
 */
public class TriageSuggestion {

    private IncidentPriority recommendedPriority;
    private String recommendedCategory;
    private double confidenceScore;
    private List<String> rationale;

    public TriageSuggestion() {
    }

    public TriageSuggestion(IncidentPriority recommendedPriority, String recommendedCategory, double confidenceScore, List<String> rationale) {
        this.recommendedPriority = recommendedPriority;
        this.recommendedCategory = recommendedCategory;
        this.confidenceScore = confidenceScore;
        this.rationale = rationale;
    }

    public IncidentPriority getRecommendedPriority() {
        return recommendedPriority;
    }

    public void setRecommendedPriority(IncidentPriority recommendedPriority) {
        this.recommendedPriority = recommendedPriority;
    }

    public String getRecommendedCategory() {
        return recommendedCategory;
    }

    public void setRecommendedCategory(String recommendedCategory) {
        this.recommendedCategory = recommendedCategory;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public List<String> getRationale() {
        return rationale;
    }

    public void setRationale(List<String> rationale) {
        this.rationale = rationale;
    }
}
