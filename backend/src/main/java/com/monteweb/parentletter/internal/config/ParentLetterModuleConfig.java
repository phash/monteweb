package com.monteweb.parentletter.internal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ConditionalOnProperty(prefix = "monteweb.modules", name = "parentletter.enabled", havingValue = "true")
@ComponentScan(basePackages = "com.monteweb.parentletter.internal")
@EnableScheduling
public class ParentLetterModuleConfig {
}
