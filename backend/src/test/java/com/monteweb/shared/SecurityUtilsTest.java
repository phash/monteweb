package com.monteweb.shared;

import com.monteweb.shared.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserId_whenAuthenticated_shouldReturnUserId() {
        UUID userId = UUID.randomUUID();
        var auth = new UsernamePasswordAuthenticationToken(
                userId.toString(), null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        var result = SecurityUtils.getCurrentUserId();
        assertThat(result).isPresent().contains(userId);
    }

    @Test
    void getCurrentUserId_whenNotAuthenticated_shouldReturnEmpty() {
        SecurityContextHolder.clearContext();

        var result = SecurityUtils.getCurrentUserId();
        assertThat(result).isEmpty();
    }

    @Test
    void requireCurrentUserId_whenNotAuthenticated_shouldThrow() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(SecurityUtils::requireCurrentUserId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No authenticated user");
    }

    @Test
    void requireCurrentUserId_whenAuthenticated_shouldReturnUserId() {
        UUID userId = UUID.randomUUID();
        var auth = new UsernamePasswordAuthenticationToken(
                userId.toString(), null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        UUID result = SecurityUtils.requireCurrentUserId();
        assertThat(result).isEqualTo(userId);
    }
}
