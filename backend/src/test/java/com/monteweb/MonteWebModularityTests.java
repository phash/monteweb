package com.monteweb;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class MonteWebModularityTests {

    @Test
    void verifyModuleStructure() {
        var modules = ApplicationModules.of(MonteWebApplication.class);
        modules.verify();
    }

    @Test
    void printModuleArrangement() {
        var modules = ApplicationModules.of(MonteWebApplication.class);
        modules.forEach(System.out::println);
    }
}
