package com.iitsm.servicecatalog.entity;

import com.iitsm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

@Entity
@Table(name = "it_services")
public class ItService extends BaseEntity {
    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "service_key", nullable = false, unique = true, length = 120)
    private String serviceKey;

    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String description = "";

    @Column(nullable = false)
    private boolean active = true;

    protected ItService() { }

    public ItService(String name, String description) {
        rename(name);
        setDescription(description);
    }

    public void rename(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 120)
            throw new IllegalArgumentException("Service name must contain 1 to 120 characters");
        this.name = value.trim();
        this.serviceKey = this.name.toLowerCase(Locale.ROOT);
    }

    public void setDescription(String value) {
        if (value != null && value.length() > 1000)
            throw new IllegalArgumentException("Service description cannot exceed 1000 characters");
        this.description = value == null ? "" : value.trim();
    }

    public void setActive(boolean active) { this.active = active; }
    public String getName() { return name; }
    public String getServiceKey() { return serviceKey; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
}
