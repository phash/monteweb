package com.monteweb.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "monteweb.email")
public record EmailProperties(
        boolean enabled,
        String from,
        String baseUrl
) {
    public EmailProperties {
        if (from == null) from = "noreply@monteweb.local";
        if (baseUrl == null) baseUrl = "http://localhost:5173";
    }
}
