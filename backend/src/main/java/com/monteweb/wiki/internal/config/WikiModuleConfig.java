package com.monteweb.wiki.internal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "monteweb.modules", name = "wiki.enabled", havingValue = "true")
@ComponentScan(basePackages = "com.monteweb.wiki.internal")
public class WikiModuleConfig {
}
