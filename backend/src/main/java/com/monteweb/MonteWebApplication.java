package com.monteweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableScheduling;

@Modulithic(
    systemName = "MonteWeb",
    sharedModules = "shared"
)
@EnableScheduling
@SpringBootApplication
public class MonteWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonteWebApplication.class, args);
    }
}
