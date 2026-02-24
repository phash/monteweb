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

    @Column(name = "github_repo", length = 200)
    private String githubRepo;

    @Column(name = "github_pat", length = 500)
    private String githubPat;

    @Column(name = "require_assignment_confirmation", nullable = false)
    private boolean requireAssignmentConfirmation = true;

    @Column(name = "multilanguage_enabled", nullable = false)
    private boolean multilanguageEnabled = true;

    @Column(name = "default_language", nullable = false, length = 5)
    private String defaultLanguage = "de";

    @Column(name = "available_languages", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> availableLanguages = List.of("de", "en");

    @Column(name = "require_user_approval", nullable = false)
    private boolean requireUserApproval = true;

    @Column(name = "privacy_policy_text", columnDefinition = "TEXT")
    private String privacyPolicyText;

    @Column(name = "privacy_policy_version", length = 20)
    private String privacyPolicyVersion = "1.0";

    @Column(name = "terms_text", columnDefinition = "TEXT")
    private String termsText;

    @Column(name = "terms_version", length = 20)
    private String termsVersion = "1.0";

    @Column(name = "data_retention_days_notifications")
    private Integer dataRetentionDaysNotifications = 90;

    @Column(name = "data_retention_days_audit")
    private Integer dataRetentionDaysAudit = 1095;


    @Column(name = "max_upload_size_mb", nullable = false)
    private int maxUploadSizeMb = 50;

    @Column(name = "school_full_name", length = 300)
    private String schoolFullName;

    @Column(name = "school_address", columnDefinition = "TEXT")
    private String schoolAddress;

    @Column(name = "school_principal", length = 200)
    private String schoolPrincipal;

    @Column(name = "tech_contact_name", length = 200)
    private String techContactName;

    @Column(name = "tech_contact_email", length = 200)
    private String techContactEmail;

    @Column(name = "two_factor_mode", nullable = false, length = 20)
    private String twoFactorMode = "DISABLED";

    @Column(name = "two_factor_grace_deadline")
    private Instant twoFactorGraceDeadline;

    // --- LDAP/AD Integration ---

    // directoryAdminOnly enabled via modules map

    // Maintenance mode (enabled via modules map)
    @Column(name = "maintenance_message", columnDefinition = "TEXT")
    private String maintenanceMessage;

    @Column(name = "ldap_url", length = 255)
    private String ldapUrl;

    @Column(name = "ldap_base_dn", length = 255)
    private String ldapBaseDn;

    @Column(name = "ldap_bind_dn", length = 255)
    private String ldapBindDn;

    @Column(name = "ldap_bind_password", length = 512)
    private String ldapBindPassword;

    @Column(name = "ldap_user_search_filter", length = 255)
    private String ldapUserSearchFilter = "(uid={0})";

    @Column(name = "ldap_attr_email", length = 64)
    private String ldapAttrEmail = "mail";

    @Column(name = "ldap_attr_first_name", length = 64)
    private String ldapAttrFirstName = "givenName";

    @Column(name = "ldap_attr_last_name", length = 64)
    private String ldapAttrLastName = "sn";

    @Column(name = "ldap_default_role", length = 30)
    private String ldapDefaultRole = "PARENT";

    @Column(name = "ldap_use_ssl", nullable = false)
    private boolean ldapUseSsl = false;

    // --- WOPI / ONLYOFFICE (enabled via modules map) ---

    @Column(name = "wopi_office_url", length = 300)
    private String wopiOfficeUrl;

    // --- ClamAV Virus Scanner (enabled via modules map) ---

    @Column(name = "clamav_host", length = 200)
    private String clamavHost = "clamav";

    @Column(name = "clamav_port")
    private int clamavPort = 3310;

    // --- Jitsi Video Conferencing (enabled via modules map) ---

    @Column(name = "jitsi_server_url", length = 300)
    private String jitsiServerUrl = "https://meet.jit.si";

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
