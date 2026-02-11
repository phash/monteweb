package com.monteweb.user;

import com.monteweb.TestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainerConfig.class)
class UserServiceIntegrationTest {

    @Autowired
    private UserModuleApi userModuleApi;

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
}
