package com.monteweb.shared;

import com.monteweb.shared.util.SsrfProtectionUtils;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

class SsrfProtectionUtilsTest {

    @Test
    void validateUrl_nullUrl_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> SsrfProtectionUtils.validateUrl(null));
    }

    @Test
    void validateUrl_blankUrl_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> SsrfProtectionUtils.validateUrl("  "));
    }

    @Test
    void validateUrl_ftpScheme_shouldThrow() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> SsrfProtectionUtils.validateUrl("ftp://example.com/file"));
        assertTrue(ex.getMessage().contains("HTTP and HTTPS"));
    }

    @Test
    void validateUrl_fileScheme_shouldThrow() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> SsrfProtectionUtils.validateUrl("file:///etc/passwd"));
        assertTrue(ex.getMessage().contains("HTTP and HTTPS"));
    }

    @Test
    void validateUrl_noHost_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> SsrfProtectionUtils.validateUrl("http://"));
    }

    @Test
    void validateUrl_localhost_shouldThrow() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> SsrfProtectionUtils.validateUrl("http://localhost/admin"));
        assertTrue(ex.getMessage().contains("internal"));
    }

    @Test
    void validateUrl_dotLocal_shouldThrow() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> SsrfProtectionUtils.validateUrl("http://myservice.local/api"));
        assertTrue(ex.getMessage().contains("internal"));
    }

    @Test
    void validateUrl_dotInternal_shouldThrow() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> SsrfProtectionUtils.validateUrl("http://backend.internal/api"));
        assertTrue(ex.getMessage().contains("internal"));
    }

    @Test
    void validateUrl_kubernetesService_shouldThrow() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> SsrfProtectionUtils.validateUrl("http://redis.default.svc.cluster.local:6379"));
        assertTrue(ex.getMessage().contains("internal"));
    }

    @Test
    void isPrivateAddress_loopback_shouldReturnTrue() throws Exception {
        assertTrue(SsrfProtectionUtils.isPrivateAddress(InetAddress.getByName("127.0.0.1")));
    }

    @Test
    void isPrivateAddress_10network_shouldReturnTrue() throws Exception {
        assertTrue(SsrfProtectionUtils.isPrivateAddress(InetAddress.getByName("10.0.0.1")));
    }

    @Test
    void isPrivateAddress_172_16_shouldReturnTrue() throws Exception {
        assertTrue(SsrfProtectionUtils.isPrivateAddress(InetAddress.getByName("172.16.0.1")));
    }

    @Test
    void isPrivateAddress_192_168_shouldReturnTrue() throws Exception {
        assertTrue(SsrfProtectionUtils.isPrivateAddress(InetAddress.getByName("192.168.1.1")));
    }

    @Test
    void isPrivateAddress_linkLocal_shouldReturnTrue() throws Exception {
        assertTrue(SsrfProtectionUtils.isPrivateAddress(InetAddress.getByName("169.254.1.1")));
    }

    @Test
    void isPrivateAddress_cloudMetadata_shouldReturnTrue() throws Exception {
        assertTrue(SsrfProtectionUtils.isPrivateAddress(InetAddress.getByName("169.254.169.254")));
    }

    @Test
    void isPrivateAddress_anyLocal_shouldReturnTrue() throws Exception {
        assertTrue(SsrfProtectionUtils.isPrivateAddress(InetAddress.getByName("0.0.0.0")));
    }

    @Test
    void isPrivateAddress_multicast_shouldReturnTrue() throws Exception {
        assertTrue(SsrfProtectionUtils.isPrivateAddress(InetAddress.getByName("224.0.0.1")));
    }

    @Test
    void isPrivateAddress_publicIp_shouldReturnFalse() throws Exception {
        assertFalse(SsrfProtectionUtils.isPrivateAddress(InetAddress.getByName("8.8.8.8")));
    }

    @Test
    void isPrivateAddress_anotherPublicIp_shouldReturnFalse() throws Exception {
        assertFalse(SsrfProtectionUtils.isPrivateAddress(InetAddress.getByName("1.1.1.1")));
    }

    @Test
    void isPrivateAddress_ipv6Loopback_shouldReturnTrue() throws Exception {
        assertTrue(SsrfProtectionUtils.isPrivateAddress(InetAddress.getByName("::1")));
    }

    @Test
    void readLimited_shouldTruncateAtMaxBytes() throws Exception {
        byte[] data = "Hello World! This is a longer string for testing.".getBytes();
        var stream = new java.io.ByteArrayInputStream(data);
        String result = SsrfProtectionUtils.readLimited(stream, 5);
        assertEquals("Hello", result);
    }

    @Test
    void readLimited_shouldReturnFullContentIfUnderLimit() throws Exception {
        byte[] data = "Short".getBytes();
        var stream = new java.io.ByteArrayInputStream(data);
        String result = SsrfProtectionUtils.readLimited(stream, 1024);
        assertEquals("Short", result);
    }

    @Test
    void readLimited_emptyStream_shouldReturnEmpty() throws Exception {
        var stream = new java.io.ByteArrayInputStream(new byte[0]);
        String result = SsrfProtectionUtils.readLimited(stream, 1024);
        assertEquals("", result);
    }
}
