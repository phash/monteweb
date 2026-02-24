package com.monteweb.auth.internal.service;

import com.monteweb.admin.AdminModuleApi;
import com.monteweb.admin.TenantConfigInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.Hashtable;

/**
 * LDAP/Active Directory authentication service.
 * Reads config from tenant_config via AdminModuleApi.
 * Uses JNDI (javax.naming) — no external dependencies needed.
 */
@Service
public class LdapAuthService {

    private static final Logger log = LoggerFactory.getLogger(LdapAuthService.class);

    private final AdminModuleApi adminModuleApi;

    public LdapAuthService(AdminModuleApi adminModuleApi) {
        this.adminModuleApi = adminModuleApi;
    }

    /**
     * Whether LDAP authentication is enabled and configured.
     */
    public boolean isLdapEnabled() {
        var config = adminModuleApi.getTenantConfig();
        return adminModuleApi.isModuleEnabled("ldap") && config.ldapConfigured();
    }

    /**
     * Attempts to authenticate a user against the configured LDAP server.
     *
     * @param username the username (typically email or uid)
     * @param password the user's password
     * @return LdapUserInfo with extracted attributes, or null if authentication failed
     */
    public LdapUserInfo authenticate(String username, String password) {
        var config = adminModuleApi.getTenantConfig();
        if (!adminModuleApi.isModuleEnabled("ldap") || !config.ldapConfigured()) {
            return null;
        }

        try {
            // Step 1: Bind as service account to search for the user
            DirContext bindCtx = createBindContext(config);
            try {
                // Step 2: Search for the user by the configured filter
                String userDn = searchForUser(bindCtx, config, username);
                if (userDn == null) {
                    log.debug("LDAP user not found for username: {}", username);
                    return null;
                }

                // Step 3: Extract user attributes before closing bind context
                Attributes userAttrs = getUserAttributes(bindCtx, userDn, config);

                // Step 4: Try to bind as the found user to verify password
                if (!verifyUserPassword(config, userDn, password)) {
                    log.debug("LDAP password verification failed for user: {}", username);
                    return null;
                }

                // Step 5: Extract attributes and return
                return extractUserInfo(userAttrs, config);
            } finally {
                bindCtx.close();
            }
        } catch (NamingException e) {
            log.warn("LDAP authentication error for username {}: {}", username, e.getMessage());
            return null;
        }
    }

    private DirContext createBindContext(TenantConfigInfo config) throws NamingException {
        Hashtable<String, String> env = buildBaseEnv(config);

        String bindDn = config.ldapBindDn();
        if (bindDn != null && !bindDn.isBlank()) {
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, bindDn);
            // Password is not in TenantConfigInfo (never exposed).
            // We need to get it from a separate method on AdminModuleApi.
            // For now, we'll use anonymous bind if no bind DN password is available.
            String bindPassword = getBindPassword();
            env.put(Context.SECURITY_CREDENTIALS, bindPassword != null ? bindPassword : "");
        }

        return new InitialDirContext(env);
    }

    private String getBindPassword() {
        // The bind password is stored in DB but not exposed in TenantConfigInfo.
        // We access it via a dedicated method on AdminModuleApi.
        return adminModuleApi.getLdapBindPassword();
    }

    private Hashtable<String, String> buildBaseEnv(TenantConfigInfo config) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, config.ldapUrl());
        if (config.ldapUseSsl()) {
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }
        env.put("com.sun.jndi.ldap.connect.timeout", "5000");
        env.put("com.sun.jndi.ldap.read.timeout", "5000");
        return env;
    }

    private String searchForUser(DirContext ctx, TenantConfigInfo config, String username) throws NamingException {
        String filter = config.ldapUserSearchFilter().replace("{0}", escapeForLdap(username));
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setCountLimit(1);

        NamingEnumeration<SearchResult> results = ctx.search(config.ldapBaseDn(), filter, controls);
        if (results.hasMore()) {
            SearchResult result = results.next();
            return result.getNameInNamespace();
        }
        return null;
    }

    private Attributes getUserAttributes(DirContext ctx, String userDn, TenantConfigInfo config) throws NamingException {
        String[] attrIds = {
                config.ldapAttrEmail(),
                config.ldapAttrFirstName(),
                config.ldapAttrLastName()
        };
        return ctx.getAttributes(userDn, attrIds);
    }

    private boolean verifyUserPassword(TenantConfigInfo config, String userDn, String password) {
        Hashtable<String, String> env = buildBaseEnv(config);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, userDn);
        env.put(Context.SECURITY_CREDENTIALS, password);

        try {
            DirContext userCtx = new InitialDirContext(env);
            userCtx.close();
            return true;
        } catch (NamingException e) {
            return false;
        }
    }

    private LdapUserInfo extractUserInfo(Attributes attrs, TenantConfigInfo config) throws NamingException {
        String email = getAttr(attrs, config.ldapAttrEmail());
        String firstName = getAttr(attrs, config.ldapAttrFirstName());
        String lastName = getAttr(attrs, config.ldapAttrLastName());

        if (email == null || email.isBlank()) {
            log.warn("LDAP user has no email attribute ({})", config.ldapAttrEmail());
            return null;
        }

        return new LdapUserInfo(email, firstName, lastName, config.ldapDefaultRole());
    }

    private String getAttr(Attributes attrs, String name) throws NamingException {
        Attribute attr = attrs.get(name);
        if (attr != null && attr.get() != null) {
            return attr.get().toString();
        }
        return null;
    }

    /**
     * Escapes special characters for LDAP filter values (RFC 4515).
     */
    private String escapeForLdap(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\5c");
                case '*' -> sb.append("\\2a");
                case '(' -> sb.append("\\28");
                case ')' -> sb.append("\\29");
                case '\0' -> sb.append("\\00");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Record holding user info extracted from LDAP.
     */
    public record LdapUserInfo(
            String email,
            String firstName,
            String lastName,
            String defaultRole
    ) {
    }
}
