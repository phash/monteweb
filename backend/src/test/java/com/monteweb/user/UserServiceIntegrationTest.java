package com.monteweb.user;

import com.monteweb.TestContainerConfig;
import com.monteweb.user.internal.repository.UserRepository;
import com.monteweb.user.internal.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestContainerConfig.class)
class UserServiceIntegrationTest {

    @Autowired
    private UserModuleApi userModuleApi;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createUser_shouldPersistAndReturn() {
        var user = userModuleApi.createUser(
                "service-test@example.com", "hashedpw",
                "Service", "Test", null, UserRole.PARENT);

        assertThat(user.id()).isNotNull();
        assertThat(user.email()).isEqualTo("service-test@example.com");
        assertThat(user.displayName()).isEqualTo("Service Test");
        assertThat(user.role()).isEqualTo(UserRole.PARENT);
    }

    @Test
    void findById_shouldReturnUser() {
        var created = userModuleApi.createUser(
                "findbyid@example.com", "hash",
                "Find", "ById", null, UserRole.TEACHER);

        var found = userModuleApi.findById(created.id());
        assertThat(found).isPresent();
        assertThat(found.get().email()).isEqualTo("findbyid@example.com");
    }

    @Test
    void findByEmail_shouldReturnUser() {
        userModuleApi.createUser(
                "findbyemail@example.com", "hash",
                "Find", "Email", null, UserRole.PARENT);

        var found = userModuleApi.findByEmail("findbyemail@example.com");
        assertThat(found).isPresent();
    }

    @Test
    void searchUsers_shouldFindByDisplayName() {
        userModuleApi.createUser(
                "searchable@example.com", "hash",
                "Searchable", "User", null, UserRole.PARENT);

        var results = userModuleApi.searchUsers("Searchable", PageRequest.of(0, 10));
        assertThat(results.getContent()).isNotEmpty();
        assertThat(results.getContent().get(0).displayName()).contains("Searchable");
    }

    @Test
    void existsByEmail_shouldReturnCorrectly() {
        userModuleApi.createUser(
                "exists-check@example.com", "hash",
                "Exists", "Check", null, UserRole.PARENT);

        assertThat(userModuleApi.existsByEmail("exists-check@example.com")).isTrue();
        assertThat(userModuleApi.existsByEmail("nonexistent@example.com")).isFalse();
    }

    @Test
    void createOidcUser_shouldPersistWithoutPassword() {
        var user = userModuleApi.createOidcUser(
                "oidc-user@example.com", "Oidc", "User",
                "google", "google-sub-123", UserRole.PARENT);

        assertThat(user.id()).isNotNull();
        assertThat(user.email()).isEqualTo("oidc-user@example.com");

        // Verify OIDC lookup works
        var found = userModuleApi.findByOidcProviderAndSubject("google", "google-sub-123");
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(user.id());
    }

    @Test
    void linkOidcProvider_shouldLinkExistingUser() {
        var user = userModuleApi.createUser(
                "link-oidc@example.com", "hash",
                "Link", "Oidc", null, UserRole.PARENT);

        userModuleApi.linkOidcProvider(user.id(), "keycloak", "kc-sub-456");

        var found = userModuleApi.findByOidcProviderAndSubject("keycloak", "kc-sub-456");
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(user.id());
    }

    @Test
    void updateProfile_shouldTrimAndDeriveDisplayName() {
        var user = userModuleApi.createUser(
                "update-profile@example.com", "hash",
                "Old", "Name", null, UserRole.PARENT);

        var updated = userModuleApi.updateProfile(user.id(), "  New  ", "  Surname  ", "  123  ");

        assertThat(updated.firstName()).isEqualTo("New");
        assertThat(updated.lastName()).isEqualTo("Surname");
        assertThat(updated.displayName()).isEqualTo("New Surname");
        assertThat(updated.phone()).isEqualTo("123");
    }

    @Test
    void updateProfile_blankFirstName_shouldThrow() {
        var user = userModuleApi.createUser(
                "update-blank-first-svc@example.com", "hash",
                "Old", "Name", null, UserRole.PARENT);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> userModuleApi.updateProfile(user.id(), "   ", "Surname", null))
                .isInstanceOf(com.monteweb.shared.exception.BadRequestException.class);
    }

    @Test
    void updateProfile_nullLastName_shouldThrow() {
        var user = userModuleApi.createUser(
                "update-null-last-svc@example.com", "hash",
                "Old", "Name", null, UserRole.PARENT);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> userModuleApi.updateProfile(user.id(), "First", null, null))
                .isInstanceOf(com.monteweb.shared.exception.BadRequestException.class);
    }

    // --- US-341/342: SUPERADMIN availability safeguards ---

    /**
     * Drives the SUPERADMIN active-count down to exactly one (the freshly created test
     * SUPERADMIN) by deactivating every other active SUPERADMIN directly via the
     * repository (bypassing the service-level guard). Returns the id of the last
     * remaining active SUPERADMIN.
     */
    private UUID makeLastActiveSuperadmin(String email) {
        // Deactivate all currently-active SUPERADMINs (e.g. the seeded admin) directly.
        userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.SUPERADMIN && u.isActive())
                .forEach(u -> {
                    u.setActive(false);
                    userRepository.save(u);
                });
        var created = userModuleApi.createUser(email, "hash", "Last", "Admin", null, UserRole.SUPERADMIN);
        // createUser persists as PARENT-less SUPERADMIN with active=true by default.
        assertThat(userRepository.countByRoleAndActive(UserRole.SUPERADMIN, true)).isEqualTo(1);
        return created.id();
    }

    @Test
    void updateRole_demotingLastActiveSuperadmin_shouldThrow() {
        UUID lastAdmin = makeLastActiveSuperadmin("last-superadmin-demote@example.com");

        assertThatThrownBy(() -> userService.updateRole(lastAdmin, UserRole.PARENT))
                .isInstanceOf(com.monteweb.shared.exception.BusinessException.class)
                .hasMessageContaining("last active SUPERADMIN");

        // Role unchanged.
        assertThat(userRepository.findById(lastAdmin).orElseThrow().getRole())
                .isEqualTo(UserRole.SUPERADMIN);
    }

    @Test
    void updateRole_demotingSuperadminWhenAnotherActiveExists_shouldSucceed() {
        // Ensure two active SUPERADMINs exist, then demoting one must be allowed.
        makeLastActiveSuperadmin("two-admins-base@example.com");
        var second = userModuleApi.createUser(
                "two-admins-second@example.com", "hash", "Second", "Admin", null, UserRole.SUPERADMIN);
        assertThat(userRepository.countByRoleAndActive(UserRole.SUPERADMIN, true)).isEqualTo(2);

        var updated = userService.updateRole(second.id(), UserRole.PARENT);
        assertThat(updated.role()).isEqualTo(UserRole.PARENT);
    }

    @Test
    void setActive_deactivatingLastActiveSuperadmin_shouldThrow() {
        UUID lastAdmin = makeLastActiveSuperadmin("last-superadmin-deactivate@example.com");

        assertThatThrownBy(() -> userService.setActive(lastAdmin, false))
                .isInstanceOf(com.monteweb.shared.exception.BusinessException.class)
                .hasMessageContaining("last active SUPERADMIN");

        assertThat(userRepository.findById(lastAdmin).orElseThrow().isActive()).isTrue();
    }
}
