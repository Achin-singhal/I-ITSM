package com.iitsm.incident.feedback.entity;

import com.iitsm.common.BaseEntity;
import com.iitsm.incident.entity.Incident;
import com.iitsm.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "incident_feedback")
public class IncidentFeedback extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false, unique = true)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @Column(nullable = false)
    private int rating;

    @Column(length = 2000)
    private String comments;

    protected IncidentFeedback() { }
    public IncidentFeedback(Incident incident, User employee, int rating, String comments) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be from 1 to 5");
        this.incident = incident; this.employee = employee; this.rating = rating;
        this.comments = comments == null || comments.isBlank() ? null : comments.trim();
    }
    public Incident getIncident() { return incident; }
    public User getEmployee() { return employee; }
    public int getRating() { return rating; }
    public String getComments() { return comments; }
}
