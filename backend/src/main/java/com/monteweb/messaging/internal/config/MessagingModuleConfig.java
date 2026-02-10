package com.monteweb.messaging.internal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "monteweb.modules.messaging", name = "enabled", havingValue = "true")
public class MessagingModuleConfig {
}
