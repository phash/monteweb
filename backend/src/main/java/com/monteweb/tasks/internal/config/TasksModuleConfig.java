package com.monteweb.tasks.internal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "monteweb.modules", name = "tasks.enabled", havingValue = "true")
@ComponentScan(basePackages = "com.monteweb.tasks.internal")
public class TasksModuleConfig {
}
