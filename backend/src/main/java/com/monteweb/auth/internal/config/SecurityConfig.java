package com.monteweb.auth.internal.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CorsConfigurationSource corsConfigurationSource;
    private final OidcAuthenticationSuccessHandler oidcSuccessHandler;
    private final MaintenanceModeFilter maintenanceModeFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          CorsConfigurationSource corsConfigurationSource,
                          @Autowired(required = false) OidcAuthenticationSuccessHandler oidcSuccessHandler,
                          MaintenanceModeFilter maintenanceModeFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.corsConfigurationSource = corsConfigurationSource;
        this.oidcSuccessHandler = oidcSuccessHandler;
        this.maintenanceModeFilter = maintenanceModeFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var builder = http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // CSRF protection disabled: stateless JWT auth with no cookies/sessions (lgtm [java/spring-disabled-csrf-protection])
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .headers(h -> h
                        .frameOptions(f -> f.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true))
                        .referrerPolicy(r -> r.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicy(p -> p.policy("camera=(), microphone=(), geolocation()"))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/actuator/prometheus").hasRole("SUPERADMIN")
                        .requestMatchers("/actuator/**").hasRole("SUPERADMIN")
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/wopi/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/error-reports").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/config").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/privacy/policy").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/privacy/terms").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("SUPERADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(maintenanceModeFilter, JwtAuthenticationFilter.class);

        // Register OIDC success handler when OIDC is enabled
        if (oidcSuccessHandler != null) {
            builder.oauth2Login(oauth2 -> oauth2
                    .successHandler(oidcSuccessHandler)
            );
        }

        return builder.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
