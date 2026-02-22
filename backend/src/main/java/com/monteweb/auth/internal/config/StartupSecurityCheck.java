package com.monteweb.auth.internal.config;

import com.monteweb.user.UserModuleApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Checks for insecure default passwords at application startup.
 * Logs prominent warnings if seed/default accounts still use known default passwords.
 */
@Component
public class StartupSecurityCheck {

    private static final Logger log = LoggerFactory.getLogger(StartupSecurityCheck.class);

    private static final String[] DEFAULT_PASSWORDS = {"admin123", "test1234"};
    private static final String[] CHECK_EMAILS = {"admin@monteweb.local"};

    private final UserModuleApi userModuleApi;
    private final PasswordEncoder passwordEncoder;

    public StartupSecurityCheck(UserModuleApi userModuleApi, PasswordEncoder passwordEncoder) {
        this.userModuleApi = userModuleApi;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkDefaultPasswords() {
        for (String email : CHECK_EMAILS) {
            var hashOpt = userModuleApi.getPasswordHash(email);
            if (hashOpt.isEmpty()) continue;

            String hash = hashOpt.get();
            for (String defaultPassword : DEFAULT_PASSWORDS) {
                if (passwordEncoder.matches(defaultPassword, hash)) {
                    log.warn("##################################################################");
                    log.warn("# SECURITY WARNING: Account '{}' uses a default password!", email);
                    log.warn("# Change it immediately in production environments!");
                    log.warn("##################################################################");
                    break;
                }
            }
        }
    }
}
