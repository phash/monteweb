package com.monteweb.admin.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tenant_config")
@Getter
@Setter
@NoArgsConstructor
public class TenantConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_name", nullable = false)
    private String schoolName;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> theme;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Boolean> modules;

    @Column(name = "target_hours_per_family", nullable = false)
    private BigDecimal targetHoursPerFamily = new BigDecimal("30.0");

    @Column(name = "target_cleaning_hours", nullable = false)
    private BigDecimal targetCleaningHours = new BigDecimal("3.0");

    @Column(name = "bundesland", nullable = false, length = 5)
    private String bundesland = "BY";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "school_vacations", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, String>> schoolVacations = new ArrayList<>();

    @Column(name = "parent_to_parent_messaging", nullable = false)
    private boolean parentToParentMessaging = false;

    @Column(name = "student_to_student_messaging", nullable = false)
    private boolean studentToStudentMessaging = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
