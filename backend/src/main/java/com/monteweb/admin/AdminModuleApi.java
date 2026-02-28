package com.monteweb.admin;

import java.time.Instant;

/**
 * Public API: Facade interface for the admin module.
 * Other modules can check tenant configuration through this interface.
 */
public interface AdminModuleApi {

    TenantConfigInfo getTenantConfig();

    boolean isModuleEnabled(String moduleName);

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
