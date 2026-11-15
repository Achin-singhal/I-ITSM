package com.iitsm.incident.entity;

public enum ImpactLevel { VERY_LOW(1), LOW(2), MODERATE(3), HIGH(4), EXTENSIVE(5);
    private final int score;
    ImpactLevel(int score) { this.score = score; }
    public int score() { return score; }
}
