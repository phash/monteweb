package com.monteweb.jobboard.internal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "monteweb.modules.jobboard", name = "enabled", havingValue = "true")
public class JobboardModuleConfig {
}
