package com.iitsm.incident.feedback.service;

import com.iitsm.incident.entity.IncidentStatus;
import com.iitsm.incident.feedback.dto.IncidentFeedbackRequest;
import com.iitsm.incident.feedback.dto.IncidentFeedbackResponse;
import com.iitsm.incident.feedback.entity.IncidentFeedback;
import com.iitsm.incident.feedback.repository.IncidentFeedbackRepository;
import com.iitsm.incident.repository.IncidentRepository;
import com.iitsm.audit.service.AuditService;
import com.iitsm.user.entity.Role;
import com.iitsm.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;

@Service
@Transactional
public class IncidentFeedbackService {
    private static final Set<IncidentStatus> FEEDBACK_STATUSES = Set.of(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED);
    private final IncidentFeedbackRepository feedback;
    private final IncidentRepository incidents;
    private final UserRepository users;
    private final AuditService audit;

    public IncidentFeedbackService(IncidentFeedbackRepository feedback, IncidentRepository incidents, UserRepository users, AuditService audit) {
        this.feedback = feedback; this.incidents = incidents; this.users = users; this.audit = audit;
    }

    public IncidentFeedbackResponse submit(Long incidentId, String email, IncidentFeedbackRequest request) {
        var employee = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        var incident = incidents.findByIdForUpdate(incidentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));
        if (employee.getRole() != Role.EMPLOYEE)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the reporting employee may submit feedback");
        if (!incident.getReporter().getId().equals(employee.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the reporting employee may submit feedback");
        if (!FEEDBACK_STATUSES.contains(incident.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Feedback is available after the incident is resolved");
        if (feedback.existsByIncidentId(incidentId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Feedback has already been submitted for this incident");
        var saved = feedback.saveAndFlush(new IncidentFeedback(incident, employee, request.rating(), request.comments()));
        audit.recordEntityAction(incident, "INCIDENT_FEEDBACK", saved.getId(), employee, "FEEDBACK_SUBMITTED",
                null, "rating=" + saved.getRating(), "Employee feedback submitted; comments were omitted from the audit record");
        return IncidentFeedbackResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public IncidentFeedbackResponse mine(Long incidentId, String email) {
        var employee = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        var incident = incidents.findById(incidentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));
        if (employee.getRole() != Role.EMPLOYEE || !incident.getReporter().getId().equals(employee.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the reporting employee may view this feedback");
        return feedback.findByIncidentId(incidentId).map(IncidentFeedbackResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feedback not found"));
    }

    @Transactional(readOnly = true)
    public Page<IncidentFeedbackResponse> all(int page, int size) {
        return feedback.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)).map(IncidentFeedbackResponse::from);
    }
}
