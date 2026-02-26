package com.monteweb.shared.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * SSRF protection utility for validating outgoing HTTP requests.
 * Blocks requests to private/internal IP ranges to prevent Server-Side Request Forgery.
 */
public final class SsrfProtectionUtils {

    private static final Logger log = LoggerFactory.getLogger(SsrfProtectionUtils.class);

    private SsrfProtectionUtils() {}

    /**
     * Validates that a URL is safe to fetch (not targeting internal/private networks).
     *
     * @param url the URL to validate
     * @throws IllegalArgumentException if the URL targets a private/internal address
     */
    public static void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank");
        }

        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URL: " + e.getMessage());
        }

        // Only allow HTTP(S)
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Only HTTP and HTTPS URLs are allowed");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL must have a valid host");
        }

        // Block well-known internal hostnames
        String lowerHost = host.toLowerCase();
        if (lowerHost.equals("localhost") || lowerHost.endsWith(".local")
                || lowerHost.endsWith(".internal") || lowerHost.endsWith(".svc.cluster.local")) {
            throw new IllegalArgumentException("Requests to internal hostnames are not allowed");
        }

        // Resolve DNS and check IP address
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (isPrivateAddress(addr)) {
                    throw new IllegalArgumentException(
                            "Requests to private/internal IP addresses are not allowed: " + host);
                }
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Cannot resolve host: " + host);
        }
    }

    /**
     * Checks whether an IP address is private, loopback, link-local, or otherwise internal.
     */
    public static boolean isPrivateAddress(InetAddress addr) {
        return addr.isLoopbackAddress()        // 127.0.0.0/8, ::1
                || addr.isSiteLocalAddress()   // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, fd00::/8
                || addr.isLinkLocalAddress()   // 169.254.0.0/16, fe80::/10
                || addr.isAnyLocalAddress()    // 0.0.0.0, ::
                || addr.isMulticastAddress()   // 224.0.0.0/4, ff00::/8
                || isCloudMetadata(addr);
    }

    /**
     * Checks for cloud metadata service addresses (AWS, GCP, Azure).
     */
    private static boolean isCloudMetadata(InetAddress addr) {
        byte[] bytes = addr.getAddress();
        if (bytes.length == 4) {
            // 169.254.169.254 (AWS/GCP metadata)
            return (bytes[0] & 0xFF) == 169 && (bytes[1] & 0xFF) == 254
                    && (bytes[2] & 0xFF) == 169 && (bytes[3] & 0xFF) == 254;
        }
        return false;
    }

    /**
     * Reads up to maxBytes from an InputStream, returning the result as a String.
     * Prevents OOM from unbounded response bodies.
     */
    public static String readLimited(java.io.InputStream inputStream, int maxBytes) throws java.io.IOException {
        byte[] buffer = new byte[Math.min(maxBytes, 8192)];
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int totalRead = 0;
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer, 0, Math.min(buffer.length, maxBytes - totalRead))) != -1) {
            out.write(buffer, 0, bytesRead);
            totalRead += bytesRead;
            if (totalRead >= maxBytes) {
                break;
            }
        }
        return out.toString(java.nio.charset.StandardCharsets.UTF_8);
    }
}
