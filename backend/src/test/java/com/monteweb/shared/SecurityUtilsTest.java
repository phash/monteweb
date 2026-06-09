package com.monteweb.shared;

import com.monteweb.shared.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private static void bindRequest(MockHttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
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

    @Test
    void getClientIpAddress_shouldPreferXForwardedForFirstHop() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.7, 70.41.3.18, 150.172.238.178");
        request.addHeader("X-Real-IP", "10.0.0.1");
        request.setRemoteAddr("127.0.0.1");
        bindRequest(request);

        assertThat(SecurityUtils.getClientIpAddress()).isEqualTo("203.0.113.7");
    }

    @Test
    void getClientIpAddress_shouldFallBackToXRealIp() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "198.51.100.42");
        request.setRemoteAddr("127.0.0.1");
        bindRequest(request);

        assertThat(SecurityUtils.getClientIpAddress()).isEqualTo("198.51.100.42");
    }

    @Test
    void getClientIpAddress_shouldFallBackToRemoteAddr() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.55");
        bindRequest(request);

        assertThat(SecurityUtils.getClientIpAddress()).isEqualTo("192.0.2.55");
    }

    @Test
    void getClientIpAddress_whenNoRequest_shouldReturnNull() {
        RequestContextHolder.resetRequestAttributes();

        assertThat(SecurityUtils.getClientIpAddress()).isNull();
    }

    @Test
    void getImpersonatorId_whenAttributePresent_shouldReturnAdminId() {
        UUID adminId = UUID.randomUUID();
        var request = new MockHttpServletRequest();
        request.setAttribute(SecurityUtils.IMPERSONATED_BY_ATTRIBUTE, adminId.toString());
        bindRequest(request);

        assertThat(SecurityUtils.getImpersonatorId()).isPresent().contains(adminId);
    }

    @Test
    void getImpersonatorId_whenAttributeAbsent_shouldReturnEmpty() {
        bindRequest(new MockHttpServletRequest());

        assertThat(SecurityUtils.getImpersonatorId()).isEmpty();
    }

    @Test
    void getImpersonatorId_whenAttributeNotAUuid_shouldReturnEmpty() {
        var request = new MockHttpServletRequest();
        request.setAttribute(SecurityUtils.IMPERSONATED_BY_ATTRIBUTE, "not-a-uuid");
        bindRequest(request);

        assertThat(SecurityUtils.getImpersonatorId()).isEmpty();
    }

    @Test
    void requireAuditActorId_whenImpersonating_shouldReturnAdminNotTarget() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        var auth = new UsernamePasswordAuthenticationToken(
                targetId.toString(), null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        var request = new MockHttpServletRequest();
        request.setAttribute(SecurityUtils.IMPERSONATED_BY_ATTRIBUTE, adminId.toString());
        bindRequest(request);

        assertThat(SecurityUtils.requireAuditActorId()).isEqualTo(adminId);
    }

    @Test
    void requireAuditActorId_whenNotImpersonating_shouldReturnCurrentUser() {
        UUID userId = UUID.randomUUID();
        var auth = new UsernamePasswordAuthenticationToken(
                userId.toString(), null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
        bindRequest(new MockHttpServletRequest());

        assertThat(SecurityUtils.requireAuditActorId()).isEqualTo(userId);
    }
}
