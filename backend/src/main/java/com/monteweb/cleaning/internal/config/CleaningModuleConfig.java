package com.monteweb.cleaning.internal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "monteweb.modules", name = "cleaning.enabled", havingValue = "true")
@ComponentScan(basePackages = "com.monteweb.cleaning.internal")
public class CleaningModuleConfig {
}
