package com.iitsm.intelligence.priority;

import com.iitsm.incident.entity.IncidentPriority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Weighted score from 0 to 100 using the configured business-impact dimensions. */
@Component
public class WeightedPriorityCalculationStrategy implements PriorityCalculationStrategy {
    private static final double WEIGHT_SUM_TOLERANCE = 0.000000001;
    private final double p1;
    private final double p2;
    private final double p3;
    private final double impactWeight;
    private final double urgencyWeight;
    private final double businessWeight;
    private final double usersWeight;
    private final double serviceWeight;

    @Autowired
    public WeightedPriorityCalculationStrategy(
            @Value("${app.priority.thresholds.p1:80}") double p1,
            @Value("${app.priority.thresholds.p2:60}") double p2,
            @Value("${app.priority.thresholds.p3:35}") double p3,
            @Value("${app.priority.weights.impact:0.25}") double impactWeight,
            @Value("${app.priority.weights.urgency:0.25}") double urgencyWeight,
            @Value("${app.priority.weights.business-criticality:0.25}") double businessWeight,
            @Value("${app.priority.weights.affected-users:0.15}") double usersWeight,
            @Value("${app.priority.weights.service-criticality:0.10}") double serviceWeight) {
        validateThresholds(p1, p2, p3);
        validateWeights(impactWeight, urgencyWeight, businessWeight, usersWeight, serviceWeight);
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.impactWeight = impactWeight;
        this.urgencyWeight = urgencyWeight;
        this.businessWeight = businessWeight;
        this.usersWeight = usersWeight;
        this.serviceWeight = serviceWeight;
    }

    public WeightedPriorityCalculationStrategy(double p1, double p2, double p3) {
        this(p1, p2, p3, .25, .25, .25, .15, .10);
    }

    @Override public String getName() { return "WEIGHTED"; }

    @Override public PriorityDecision calculate(PriorityInput in) {
        double impact = normalize(in.impact().score());
        double urgency = normalize(in.urgency().score());
        double business = normalize(in.businessCriticality());
        double service = normalize(in.serviceCriticality());
        int userScore = userBand(in.affectedUsers());
        double score = impact*impactWeight + urgency*urgencyWeight + business*businessWeight
                + userScore*usersWeight + service*serviceWeight;
        score = BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP).doubleValue();
        IncidentPriority priority = score >= p1 ? IncidentPriority.CRITICAL
                : score >= p2 ? IncidentPriority.HIGH
                : score >= p3 ? IncidentPriority.MEDIUM : IncidentPriority.LOW;
        String explanation = String.format("%s score %.2f/100 from impact %.0f%% at %.0f%% weight, urgency %.0f%% at %.0f%% weight, business criticality %.0f%% at %.0f%% weight, affected-user band %.0f%% at %.0f%% weight, and service '%s' criticality %.0f%% at %.0f%% weight. Thresholds: P1 >= %.2f, P2 >= %.2f, P3 >= %.2f.",
                priority, score, impact,impactWeight*100, urgency,urgencyWeight*100, business,businessWeight*100,
                (double)userScore,usersWeight*100, in.affectedService(), service,serviceWeight*100, p1, p2, p3);
        return new PriorityDecision(score, priority, explanation);
    }

    private static void validateThresholds(double p1, double p2, double p3) {
        if (!Double.isFinite(p1) || !Double.isFinite(p2) || !Double.isFinite(p3)
                || p3 <= 0 || p3 >= p2 || p2 >= p1 || p1 > 100) {
            throw new IllegalArgumentException("Priority thresholds must satisfy 0 < P3 < P2 < P1 <= 100 (received P1="
                    + p1 + ", P2=" + p2 + ", P3=" + p3 + ")");
        }
    }

    private static void validateWeights(double... weights) {
        double sum = 0;
        for (double weight : weights) {
            if (!Double.isFinite(weight) || weight < 0) {
                throw new IllegalArgumentException("Priority weights must be finite, nonnegative values");
            }
            sum += weight;
        }
        if (Math.abs(sum - 1.0) > WEIGHT_SUM_TOLERANCE) {
            throw new IllegalArgumentException("Priority weights must sum to 1.0 (received " + sum + ")");
        }
    }

    private double normalize(int value) { return (value - 1) * 25.0; }
    private int userBand(int count) { return count == 0 ? 0 : count <= 5 ? 20 : count <= 20 ? 40 : count <= 100 ? 60 : count <= 500 ? 80 : 100; }
}
