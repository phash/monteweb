package com.monteweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableScheduling;

@Modulithic(
    systemName = "MonteWeb",
    sharedModules = "shared"
)
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication(exclude = {OAuth2ClientAutoConfiguration.class})
public class MonteWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonteWebApplication.class, args);
    }
}
