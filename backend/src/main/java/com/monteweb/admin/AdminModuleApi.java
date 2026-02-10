package com.monteweb.admin;

/**
 * Public API: Facade interface for the admin module.
 * Other modules can check tenant configuration through this interface.
 */
public interface AdminModuleApi {

    TenantConfigInfo getTenantConfig();

    boolean isModuleEnabled(String moduleName);
}
