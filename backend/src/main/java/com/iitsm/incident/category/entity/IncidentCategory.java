package com.iitsm.incident.category.entity;

import com.iitsm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

@Entity
@Table(name = "incident_categories")
public class IncidentCategory extends BaseEntity {
    @NotBlank @Size(max = 64)
    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "category_key", nullable = false, unique = true, length = 64)
    private String key;

    @Column(nullable = false)
    private boolean active = true;

    protected IncidentCategory() { }
    public IncidentCategory(String name) { rename(name); }

    public void rename(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 64)
            throw new IllegalArgumentException("Category name must contain 1 to 64 characters");
        name = value.trim();
        key = name.toLowerCase(Locale.ROOT);
    }
    public void setActive(boolean active) { this.active = active; }
    public String getName() { return name; }
    public String getKey() { return key; }
    public boolean isActive() { return active; }
}
