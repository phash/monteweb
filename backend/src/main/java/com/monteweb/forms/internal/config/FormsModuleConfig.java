package com.monteweb.forms.internal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "monteweb.modules", name = "forms.enabled", havingValue = "true")
@ComponentScan(basePackages = "com.monteweb.forms.internal")
public class FormsModuleConfig {
}
