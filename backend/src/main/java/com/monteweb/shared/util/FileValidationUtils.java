package com.monteweb.shared.util;

import com.monteweb.shared.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Utility for validating file uploads using magic bytes (file signatures)
 * to prevent content-type spoofing attacks.
 */
public final class FileValidationUtils {

    private FileValidationUtils() {}

    // Magic byte signatures for common file types
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] GIF87_MAGIC = {0x47, 0x49, 0x46, 0x38, 0x37, 0x61};
    private static final byte[] GIF89_MAGIC = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
    private static final byte[] WEBP_MAGIC_RIFF = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};
    private static final byte[] ZIP_MAGIC = {0x50, 0x4B, 0x03, 0x04};

    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "com", "msi", "scr", "pif", "vbs", "js",
            "ws", "wsf", "wsc", "wsh", "ps1", "psm1", "psd1",
            "sh", "bash", "csh", "ksh", "php", "jsp", "asp", "aspx",
            "htm", "html", "svg", "xml", "xhtml"
    );

    /**
     * Validates that the file is a genuine image by checking magic bytes.
     *
     * @param file the uploaded file
     * @throws BusinessException if the file is not a valid image
     */
    public static void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("File must be an image");
        }

        try {
            byte[] header = readHeader(file, 12);
            if (!isImageMagicBytes(header)) {
                throw new BusinessException("File content does not match an image format");
            }
        } catch (IOException e) {
            throw new BusinessException("Failed to read file for validation");
        }
    }

    /**
     * Validates a general file upload: blocks dangerous extensions
     * and checks that content-type aligns with magic bytes where possible.
     *
     * @param file the uploaded file
     * @throws BusinessException if the file fails validation
     */
    public static void validateUpload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("File is empty");
        }

        String filename = file.getOriginalFilename();
        if (filename != null) {
            String extension = getExtension(filename).toLowerCase();
            if (DANGEROUS_EXTENSIONS.contains(extension)) {
                throw new BusinessException("File type '" + extension + "' is not allowed");
            }
        }

        // If content-type claims to be an image, verify magic bytes
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image/")) {
            try {
                byte[] header = readHeader(file, 12);
                if (!isImageMagicBytes(header)) {
                    throw new BusinessException("File content does not match claimed image type");
                }
            } catch (IOException e) {
                throw new BusinessException("Failed to read file for validation");
            }
        }
    }

    public static boolean isImageMagicBytes(byte[] header) {
        if (header.length < 3) return false;
        return startsWith(header, JPEG_MAGIC)
                || startsWith(header, PNG_MAGIC)
                || startsWith(header, GIF87_MAGIC)
                || startsWith(header, GIF89_MAGIC)
                || startsWith(header, WEBP_MAGIC_RIFF);
    }

    private static byte[] readHeader(MultipartFile file, int length) throws IOException {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[length];
            int read = is.read(header);
            if (read < length) {
                byte[] trimmed = new byte[read];
                System.arraycopy(header, 0, trimmed, 0, read);
                return trimmed;
            }
            return header;
        }
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : "";
    }
}
