package com.monteweb;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class MonteWebModularityTests {

    @Test
    @Disabled("Known cycle: user <-> family (AdminUserController uses FamilyModuleApi). " +
              "TODO: Move AdminUserController to admin module and expose admin methods via UserModuleApi.")
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
