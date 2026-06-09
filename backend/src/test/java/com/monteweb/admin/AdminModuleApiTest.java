package com.monteweb.admin;

import com.monteweb.TestContainerConfig;
import com.monteweb.admin.internal.model.AuditLogEntry;
import com.monteweb.admin.internal.service.AuditService;
import com.monteweb.user.UserModuleApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestContainerConfig.class)
class AdminModuleApiTest {

    @Autowired
    private AdminModuleApi adminModule;

    @Autowired
    private AuditService auditService;

    @Autowired
    private UserModuleApi userModule;

    /**
     * The audit_log.user_id column has a FK to users(id), so the actor must be a real
     * user. We use the seeded SUPERADMIN (V032: admin@monteweb.local) as a stand-in actor.
     */
    private UUID seededActorId() {
        return userModule.findByEmail("admin@monteweb.local")
                .orElseThrow(() -> new IllegalStateException("Seeded admin user not found"))
                .id();
    }

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

    // ── Audit log (US-348) ───────────────────────────────────────────

    @Test
    void recordAuditEvent_shouldPersistAndBeQueryable() {
        UUID actor = seededActorId();
        UUID target = UUID.randomUUID();
        String action = "TEST_ACTION_" + UUID.randomUUID();

        adminModule.recordAuditEvent(actor, action, "USER", target,
                Map.of("note", "unit test"), "127.0.0.1");

        Page<AuditLogEntry> result = auditService.find(action, null, null, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        AuditLogEntry entry = result.getContent().get(0);
        assertEquals(actor, entry.getUserId());
        assertEquals(action, entry.getAction());
        assertEquals("USER", entry.getEntityType());
        assertEquals(target, entry.getEntityId());
        assertNotNull(entry.getCreatedAt());
    }

    @Test
    void auditFind_filterByAction_shouldExcludeOtherActions() {
        String actionA = "ACTION_A_" + UUID.randomUUID();
        String actionB = "ACTION_B_" + UUID.randomUUID();
        UUID actor = seededActorId();
        adminModule.recordAuditEvent(actor, actionA, "USER", null, null, null);
        adminModule.recordAuditEvent(actor, actionB, "USER", null, null, null);

        Page<AuditLogEntry> onlyA = auditService.find(actionA, null, null, PageRequest.of(0, 20));

        assertEquals(1, onlyA.getTotalElements());
        assertEquals(actionA, onlyA.getContent().get(0).getAction());
    }

    @Test
    void auditFind_filterByTimeRange_shouldRespectBounds() {
        String action = "TIMED_" + UUID.randomUUID();
        adminModule.recordAuditEvent(seededActorId(), action, "USER", null, null, null);

        Instant from = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant to = Instant.now().plus(1, ChronoUnit.HOURS);

        Page<AuditLogEntry> inRange = auditService.find(action, from, to, PageRequest.of(0, 20));
        assertEquals(1, inRange.getTotalElements());

        // Window entirely in the past must not match the just-created entry
        Page<AuditLogEntry> outOfRange = auditService.find(action,
                Instant.now().minus(2, ChronoUnit.HOURS),
                Instant.now().minus(1, ChronoUnit.HOURS),
                PageRequest.of(0, 20));
        assertEquals(0, outOfRange.getTotalElements());
    }
}
