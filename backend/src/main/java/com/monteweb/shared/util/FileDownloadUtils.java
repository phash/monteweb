package com.monteweb.shared.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Utility for safe file download headers.
 */
public final class FileDownloadUtils {

    private FileDownloadUtils() {
    }

    /**
     * Builds a safe Content-Disposition header value with RFC 5987 encoding.
     * Prevents HTTP header injection via malicious filenames.
     */
    public static String buildContentDisposition(String type, String filename) {
        String sanitized = sanitizeFilename(filename);
        String encoded = URLEncoder.encode(sanitized, StandardCharsets.UTF_8).replace("+", "%20");
        return type + "; filename=\"" + sanitized + "\"; filename*=UTF-8''" + encoded;
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "download";
        }
        return filename.replaceAll("[\\r\\n\"\\\\/;]", "_");
    }
}
