package com.monteweb.user.internal.model;

import com.monteweb.user.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "oidc_provider", length = 50)
    private String oidcProvider;

    @Column(name = "oidc_subject", length = 255)
    private String oidcSubject;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(length = 50)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role = UserRole.PARENT;

    @Column(name = "special_roles", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] specialRoles = {};

    @Column(name = "assigned_roles", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] assignedRoles = {};

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deletion_reason")
    private String deletionReason;

    @Column(name = "deletion_requested_at")
    private Instant deletionRequestedAt;

    @Column(name = "scheduled_deletion_at")
    private Instant scheduledDeletionAt;

    @Column(name = "totp_secret", length = 64)
    private String totpSecret;

    @Column(name = "totp_enabled", nullable = false)
    private boolean totpEnabled = false;

    @Column(name = "totp_recovery_codes", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] totpRecoveryCodes;

    @Column(name = "digest_frequency", nullable = false, length = 20)
    private String digestFrequency = "NONE";

    @Column(name = "dark_mode", nullable = false, length = 10)
    private String darkMode = "SYSTEM";

    @Column(name = "digest_last_sent_at")
    private Instant digestLastSentAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Set<String> getSpecialRolesAsSet() {
        if (specialRoles == null) return Set.of();
        return new HashSet<>(Set.of(specialRoles));
    }

    public Set<String> getAssignedRolesAsSet() {
        if (assignedRoles == null) return Set.of();
        return new HashSet<>(Set.of(assignedRoles));
    }
}
