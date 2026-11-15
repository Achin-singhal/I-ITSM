package com.iitsm.servicecatalog.service;

import com.iitsm.servicecatalog.dto.ItServiceRequest;
import com.iitsm.servicecatalog.dto.ItServiceResponse;
import com.iitsm.servicecatalog.entity.ItService;
import com.iitsm.servicecatalog.repository.ItServiceRepository;
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
public class ItServiceCatalog {
    private final ItServiceRepository services;
    private final UserRepository users;
    private final AuditService audit;

    public ItServiceCatalog(ItServiceRepository services, UserRepository users, AuditService audit) {
        this.services = services; this.users = users; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ItServiceResponse> listAll() {
        return services.findAllByOrderByNameAsc().stream().map(ItServiceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ItServiceResponse> listActive() {
        return services.findAllByActiveTrueOrderByNameAsc().stream().map(ItServiceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ItServiceResponse get(Long id) { return ItServiceResponse.from(service(id)); }

    public ItServiceResponse create(ItServiceRequest request, String actorEmail) {
        String key = key(request.name());
        if (services.existsByServiceKey(key))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Service already exists");
        ItService created = services.saveAndFlush(new ItService(request.name(), request.description()));
        audit.recordEntityAction("IT_SERVICE", created.getId(), actor(actorEmail), "IT_SERVICE_CREATED", null,
                created.getName(), "IT service created");
        return ItServiceResponse.from(created);
    }

    public ItServiceResponse update(Long id, ItServiceRequest request, String actorEmail) {
        ItService service = service(id);
        String key = key(request.name());
        if (!service.getServiceKey().equals(key) && services.existsByServiceKey(key))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Service already exists");
        String previousName = service.getName();
        String previousDescription = service.getDescription();
        service.rename(request.name());
        service.setDescription(request.description());
        ItService saved = services.save(service);
        if (!previousName.equals(saved.getName()) || !previousDescription.equals(saved.getDescription())) {
            audit.recordEntityAction("IT_SERVICE", saved.getId(), actor(actorEmail), "IT_SERVICE_UPDATED",
                    previousName, saved.getName(), "IT service details updated; description changed=" + !previousDescription.equals(saved.getDescription()));
        }
        return ItServiceResponse.from(saved);
    }

    public ItServiceResponse setActive(Long id, boolean active, String actorEmail) {
        ItService service = service(id);
        boolean previous = service.isActive();
        if (previous != active) {
            service.setActive(active);
            service = services.save(service);
            audit.recordEntityAction("IT_SERVICE", service.getId(), actor(actorEmail), active ? "IT_SERVICE_ACTIVATED" : "IT_SERVICE_DEACTIVATED",
                    Boolean.toString(previous), Boolean.toString(active), active ? "IT service activated" : "IT service deactivated");
        }
        return ItServiceResponse.from(service);
    }

    @Transactional(readOnly = true)
    public ItService requireActive(Long id) {
        return services.findById(id).filter(ItService::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select an active IT service"));
    }

    private ItService service(Long id) {
        return services.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
    }

    private User actor(String email) {
        return users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    }

    private String key(String name) { return name == null ? "" : name.trim().toLowerCase(Locale.ROOT); }
}
