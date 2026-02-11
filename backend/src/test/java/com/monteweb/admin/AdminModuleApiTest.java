package com.monteweb.admin;

import com.monteweb.TestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestContainerConfig.class)
class AdminModuleApiTest {

    @Autowired
    private AdminModuleApi adminModule;

    @Test
    void getTenantConfig_shouldReturnNonNullConfig() {
        TenantConfigInfo config = adminModule.getTenantConfig();

        assertNotNull(config);
        assertNotNull(config.id());
        assertNotNull(config.schoolName());
        assertNotNull(config.modules());
    }

    @Test
    void getTenantConfig_shouldHaveDefaultModules() {
        TenantConfigInfo config = adminModule.getTenantConfig();

        assertNotNull(config.modules());
        // Default modules should be present
        assertTrue(config.modules().containsKey("messaging"));
        assertTrue(config.modules().containsKey("files"));
        assertTrue(config.modules().containsKey("jobboard"));
        assertTrue(config.modules().containsKey("cleaning"));
        assertTrue(config.modules().containsKey("calendar"));
    }

    @Test
    void isModuleEnabled_existingModule_shouldReturnTrue() {
        // Default modules are enabled
        assertTrue(adminModule.isModuleEnabled("messaging"));
        assertTrue(adminModule.isModuleEnabled("files"));
        assertTrue(adminModule.isModuleEnabled("jobboard"));
        assertTrue(adminModule.isModuleEnabled("cleaning"));
        assertTrue(adminModule.isModuleEnabled("calendar"));
    }

    @Test
    void isModuleEnabled_nonExistentModule_shouldReturnFalse() {
        assertFalse(adminModule.isModuleEnabled("nonexistent_module"));
    }

    @Test
    void getTenantConfig_shouldHaveHoursTargets() {
        TenantConfigInfo config = adminModule.getTenantConfig();

        assertNotNull(config.targetHoursPerFamily());
        assertNotNull(config.targetCleaningHours());
        assertTrue(config.targetHoursPerFamily().doubleValue() >= 0);
        assertTrue(config.targetCleaningHours().doubleValue() >= 0);
    }

    @Test
    void tenantConfigInfo_shouldBeRecord() {
        TenantConfigInfo config = adminModule.getTenantConfig();

        // Records have proper equals/hashCode
        TenantConfigInfo config2 = adminModule.getTenantConfig();
        assertEquals(config.id(), config2.id());
        assertEquals(config.schoolName(), config2.schoolName());
    }
}
