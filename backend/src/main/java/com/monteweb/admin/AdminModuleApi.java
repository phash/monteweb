package com.monteweb.admin;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Public API: Facade interface for the admin module.
 * Other modules can check tenant configuration through this interface.
 */
public interface AdminModuleApi {

    TenantConfigInfo getTenantConfig();

    boolean isModuleEnabled(String moduleName);

    /**
     * Records a security-relevant action in the persistent, queryable audit log
     * (US-348). Intended for cross-module callers (e.g. role changes, account
     * activation/deactivation, impersonation start/stop). {@code details} and
     * {@code ipAddress} may be null.
     *
     * @param userId     the acting user (may be null for system actions)
     * @param action     a short action code, e.g. "ROLE_CHANGE", "IMPERSONATION_START"
     * @param entityType the type of affected entity, e.g. "USER"
     * @param entityId   the id of the affected entity (may be null)
     * @param details    optional structured details (stored as JSON)
     * @param ipAddress  optional originating IP address
     */
    void recordAuditEvent(UUID userId, String action, String entityType, UUID entityId,
                          Map<String, Object> details, String ipAddress);

    /**
     * Returns the stored LDAP bind password for authentication.
     * This is kept separate from TenantConfigInfo to avoid exposing it in API responses.
     */
    String getLdapBindPassword();

    boolean isMaintenanceEnabled();

    /**
     * DSGVO retention: deletes old error reports.
     * RESOLVED/IGNORED older than cutoff90, NEW/REPORTED older than cutoff365.
     * Returns total number of deleted records.
     */
    int cleanupOldErrorReports(Instant cutoff90, Instant cutoff365);
}
