package com.monteweb.shared.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Shared file validation utilities for upload security.
 * Validates file content via magic bytes rather than trusting client-declared Content-Type.
 */
public final class FileValidationUtils {

    private FileValidationUtils() {}

    /**
     * Detects the actual content type of a file by reading magic bytes.
     * For recognized binary formats (images, PDF), returns the detected type.
     * For unrecognized content, falls back to the declared Content-Type if it is
     * safe (not HTML/SVG/JS/XML), or "application/octet-stream" otherwise.
     */
    public static String detectContentType(MultipartFile file) {
        try {
            byte[] header = new byte[12];
            try (InputStream is = file.getInputStream()) {
                int read = is.read(header);
                if (read < 4) {
                    return "application/octet-stream";
                }
            }

            // Images
            if (isJpeg(header)) return "image/jpeg";
            if (isPng(header)) return "image/png";
            if (isGif(header)) return "image/gif";
            if (isWebP(header)) return "image/webp";
            // Documents
            if (isPdf(header)) return "application/pdf";

            // For unrecognized types, use declared type only if safe
            String declared = file.getContentType();
            if (declared != null && isSafeContentType(declared)) {
                return declared;
            }
            return "application/octet-stream";
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }

    /**
     * Validates that a file is actually an image by checking magic bytes.
     * Returns the detected image content type.
     *
     * @throws IllegalArgumentException if the file is not a valid image
     */
    public static String validateImageContentType(MultipartFile file) {
        try {
            byte[] header = new byte[12];
            try (InputStream is = file.getInputStream()) {
                int read = is.read(header);
                if (read < 4) {
                    throw new IllegalArgumentException("File too small to be a valid image");
                }
            }

            if (isJpeg(header)) return "image/jpeg";
            if (isPng(header)) return "image/png";
            if (isGif(header)) return "image/gif";
            if (isWebP(header)) return "image/webp";

            throw new IllegalArgumentException(
                    "File content does not match a valid image format. Allowed: JPEG, PNG, WebP, GIF");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not validate file content: " + e.getMessage());
        }
    }

    /**
     * Checks whether a Content-Type is safe to serve (not interpretable as executable content).
     */
    public static boolean isSafeContentType(String contentType) {
        if (contentType == null) return false;
        String lower = contentType.toLowerCase();
        return !lower.contains("html")
                && !lower.contains("javascript")
                && !lower.contains("svg")
                && !lower.endsWith("+xml")
                && !lower.equals("text/xml")
                && !lower.equals("application/xml")
                && !lower.equals("text/css");
    }

    /**
     * Sanitizes a file extension to prevent path traversal or injection.
     * Returns only alphanumeric characters, max 10 chars.
     */
    public static String sanitizeExtension(String extension) {
        if (extension == null || extension.isEmpty()) return "bin";
        String clean = extension.replaceAll("[^a-zA-Z0-9]", "");
        if (clean.isEmpty()) return "bin";
        return clean.length() > 10 ? clean.substring(0, 10) : clean;
    }

    /**
     * Extracts and sanitizes the file extension from a filename.
     */
    public static String getExtensionFromFilename(String filename) {
        if (filename == null) return "bin";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "bin";
        return sanitizeExtension(filename.substring(dot + 1));
    }

    /**
     * Maps a content type to a safe file extension.
     */
    public static String extensionFromContentType(String contentType) {
        if (contentType == null) return "bin";
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            case "application/pdf" -> "pdf";
            default -> "bin";
        };
    }

    /**
     * Sanitizes a filename for use in a Content-Disposition HTTP header.
     * Removes characters that could enable HTTP header injection (quotes, newlines, control chars).
     */
    public static String sanitizeContentDispositionFilename(String filename) {
        if (filename == null || filename.isEmpty()) return "download";
        // Remove path separators, null bytes, control characters, quotes
        String clean = filename.replaceAll("[/\\\\\"'\\x00-\\x1f\\x7f]", "_");
        return clean.length() > 200 ? clean.substring(0, 200) : clean;
    }

    // ---- Magic byte detection ----

    private static boolean isJpeg(byte[] h) {
        return h[0] == (byte) 0xFF && h[1] == (byte) 0xD8 && h[2] == (byte) 0xFF;
    }

    private static boolean isPng(byte[] h) {
        return h[0] == (byte) 0x89 && h[1] == 0x50 && h[2] == 0x4E && h[3] == 0x47;
    }

    private static boolean isGif(byte[] h) {
        return h[0] == 0x47 && h[1] == 0x49 && h[2] == 0x46;
    }

    private static boolean isWebP(byte[] h) {
        return h.length >= 12
                && h[0] == 0x52 && h[1] == 0x49 && h[2] == 0x46 && h[3] == 0x46
                && h[8] == 0x57 && h[9] == 0x45 && h[10] == 0x42 && h[11] == 0x50;
    }

    private static boolean isPdf(byte[] h) {
        return h[0] == 0x25 && h[1] == 0x50 && h[2] == 0x44 && h[3] == 0x46;
    }
}
