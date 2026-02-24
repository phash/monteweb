package com.monteweb.bookmark.internal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "monteweb.modules", name = "bookmarks.enabled", havingValue = "true")
@ComponentScan(basePackages = "com.monteweb.bookmark.internal")
public class BookmarkModuleConfig {
}
