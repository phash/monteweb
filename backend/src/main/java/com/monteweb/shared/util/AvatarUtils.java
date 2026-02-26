package com.monteweb.shared.util;

import com.monteweb.shared.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

public final class AvatarUtils {

    private static final long MAX_SIZE = 2 * 1024 * 1024; // 2 MB

    private AvatarUtils() {}

    /**
     * Validates the uploaded file is an image under 2MB and converts it to a base64 data URL.
     */
    public static String validateAndConvert(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("Avatar file is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("File must be an image");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("Avatar must be smaller than 2MB");
        }
        // Validate magic bytes to prevent content-type spoofing
        FileValidationUtils.validateImage(file);
        try {
            byte[] bytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            return "data:" + contentType + ";base64," + base64;
        } catch (IOException e) {
            throw new BusinessException("Failed to read avatar file");
        }
    }
}
