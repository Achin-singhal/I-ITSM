package com.iitsm.incident.entity;

public enum UrgencyLevel { LOW(1), NORMAL(2), HIGH(3), IMMEDIATE(4), CRITICAL(5);
    private final int score;
    UrgencyLevel(int score) { this.score = score; }
    public int score() { return score; }
}
