package com.monteweb.profilefields.internal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "monteweb.modules", name = "profilefields.enabled", havingValue = "true")
@ComponentScan(basePackages = "com.monteweb.profilefields.internal")
public class ProfileFieldsModuleConfig {
}
