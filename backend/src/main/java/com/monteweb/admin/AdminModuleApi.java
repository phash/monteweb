package com.monteweb.admin;

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
}
