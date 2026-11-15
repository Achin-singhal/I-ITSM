package com.iitsm.incident.feedback.dto;

import com.iitsm.incident.feedback.entity.IncidentFeedback;
import java.time.Instant;

public record IncidentFeedbackResponse(Long id, Long incidentId, String ticketNumber,
        String incidentTitle, Long employeeId, String employeeName, String employeeEmail,
        int rating, String comments, Instant createdAt) {
    public static IncidentFeedbackResponse from(IncidentFeedback feedback) {
        var incident = feedback.getIncident(); var employee = feedback.getEmployee();
        return new IncidentFeedbackResponse(feedback.getId(), incident.getId(), incident.getTicketNumber(),
                incident.getTitle(), employee.getId(), employee.getFullName(), employee.getEmail(),
                feedback.getRating(), feedback.getComments(), feedback.getCreatedAt());
    }
}
