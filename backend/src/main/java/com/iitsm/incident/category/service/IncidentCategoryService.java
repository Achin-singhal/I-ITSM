package com.iitsm.incident.category.service;

import com.iitsm.incident.category.dto.IncidentCategoryRequest;
import com.iitsm.incident.category.dto.IncidentCategoryResponse;
import com.iitsm.incident.category.entity.IncidentCategory;
import com.iitsm.incident.category.repository.IncidentCategoryRepository;
import com.iitsm.audit.service.AuditService;
import com.iitsm.user.entity.User;
import com.iitsm.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class IncidentCategoryService {
    private final IncidentCategoryRepository categories;
    private final UserRepository users;
    private final AuditService audit;
    public IncidentCategoryService(IncidentCategoryRepository categories, UserRepository users, AuditService audit) {
        this.categories = categories; this.users = users; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<IncidentCategoryResponse> list(boolean activeOnly) {
        return categories.findAllByOrderByNameAsc().stream().filter(c -> !activeOnly || c.isActive())
                .map(IncidentCategoryResponse::from).toList();
    }

    public IncidentCategoryResponse create(IncidentCategoryRequest request, String actorEmail) {
        String key = key(request.name());
        if (categories.existsByKey(key)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists");
        IncidentCategory created = categories.saveAndFlush(new IncidentCategory(request.name()));
        audit.recordEntityAction("INCIDENT_CATEGORY", created.getId(), actor(actorEmail), "INCIDENT_CATEGORY_CREATED",
                null, created.getName(), "Incident category created");
        return IncidentCategoryResponse.from(created);
    }

    public IncidentCategoryResponse update(Long id, IncidentCategoryRequest request, String actorEmail) {
        var category = category(id); String key = key(request.name());
        if (!category.getKey().equals(key) && categories.existsByKey(key))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists");
        String previousName = category.getName();
        category.rename(request.name());
        var saved = categories.save(category);
        if (!previousName.equals(saved.getName()))
            audit.recordEntityAction("INCIDENT_CATEGORY", saved.getId(), actor(actorEmail), "INCIDENT_CATEGORY_UPDATED",
                    previousName, saved.getName(), "Incident category renamed");
        return IncidentCategoryResponse.from(saved);
    }

    public IncidentCategoryResponse setActive(Long id, boolean active, String actorEmail) {
        var category = category(id);
        boolean previous = category.isActive();
        if (previous != active) {
            category.setActive(active);
            category = categories.save(category);
            audit.recordEntityAction("INCIDENT_CATEGORY", category.getId(), actor(actorEmail),
                    active ? "INCIDENT_CATEGORY_ACTIVATED" : "INCIDENT_CATEGORY_DEACTIVATED",
                    Boolean.toString(previous), Boolean.toString(active), active ? "Incident category activated" : "Incident category deactivated");
        }
        return IncidentCategoryResponse.from(category);
    }

    @Transactional(readOnly = true)
    public IncidentCategory requireActive(String name) {
        return categories.findByKey(key(name)).filter(IncidentCategory::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select an active incident category"));
    }

    @Transactional(readOnly = true)
    public IncidentCategory requireActiveForUpdate(String name, com.iitsm.incident.entity.Incident incident) {
        var selected = categories.findByKey(key(name)).filter(IncidentCategory::isActive);
        if (selected.isPresent()) return selected.get();
        var existing = incident.getCategoryDefinition();
        if (existing != null && incident.getCategory() != null && incident.getCategory().equalsIgnoreCase(name == null ? "" : name.trim()))
            return existing;
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select an active incident category");
    }

    private IncidentCategory category(Long id) {
        return categories.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }
    private User actor(String email) {
        return users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    }
    private String key(String name) { return name == null ? "" : name.trim().toLowerCase(Locale.ROOT); }
}
