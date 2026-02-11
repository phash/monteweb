package com.monteweb.calendar.internal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "monteweb.modules", name = "calendar.enabled", havingValue = "true")
@ComponentScan(basePackages = "com.monteweb.calendar.internal")
public class CalendarModuleConfig {
}
