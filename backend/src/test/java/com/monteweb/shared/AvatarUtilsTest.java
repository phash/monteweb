package com.monteweb.shared;

import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.util.AvatarUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class AvatarUtilsTest {

    @Test
    void validateAndConvert_validImage_shouldReturnDataUrl() {
        byte[] imageBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}; // PNG magic bytes
        var file = new MockMultipartFile("avatar", "photo.png", "image/png", imageBytes);

        String result = AvatarUtils.validateAndConvert(file);

        assertNotNull(result);
        assertTrue(result.startsWith("data:image/png;base64,"));
    }

    @Test
    void validateAndConvert_jpegImage_shouldWork() {
        byte[] imageBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        var file = new MockMultipartFile("avatar", "photo.jpg", "image/jpeg", imageBytes);

        String result = AvatarUtils.validateAndConvert(file);

        assertNotNull(result);
        assertTrue(result.startsWith("data:image/jpeg;base64,"));
    }

    @Test
    void validateAndConvert_emptyFile_shouldThrow() {
        var file = new MockMultipartFile("avatar", "empty.png", "image/png", new byte[0]);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> AvatarUtils.validateAndConvert(file));
        assertEquals("Avatar file is empty", ex.getMessage());
    }

    @Test
    void validateAndConvert_nonImageContentType_shouldThrow() {
        // Even with PDF content type, magic bytes don't match an image format
        var file = new MockMultipartFile("avatar", "doc.pdf", "application/pdf", "data".getBytes());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> AvatarUtils.validateAndConvert(file));
        assertEquals("File must be a valid image (JPEG, PNG, WebP, or GIF)", ex.getMessage());
    }

    @Test
    void validateAndConvert_spoofedContentType_shouldThrow() {
        // HTML file disguised as image via Content-Type header — magic bytes are not an image
        var file = new MockMultipartFile("avatar", "evil.png", "image/png", "<html>XSS</html>".getBytes());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> AvatarUtils.validateAndConvert(file));
        assertEquals("File must be a valid image (JPEG, PNG, WebP, or GIF)", ex.getMessage());
    }

    @Test
    void validateAndConvert_nullContentType_shouldThrow() {
        // No Content-Type and non-image magic bytes
        var file = new MockMultipartFile("avatar", "file", null, "data".getBytes());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> AvatarUtils.validateAndConvert(file));
        assertEquals("File must be a valid image (JPEG, PNG, WebP, or GIF)", ex.getMessage());
    }

    @Test
    void validateAndConvert_tooLargeFile_shouldThrow() {
        // Create a file larger than 2MB — size check happens before magic bytes
        byte[] largeContent = new byte[2 * 1024 * 1024 + 1];
        var file = new MockMultipartFile("avatar", "large.png", "image/png", largeContent);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> AvatarUtils.validateAndConvert(file));
        assertEquals("Avatar must be smaller than 2MB", ex.getMessage());
    }

    @Test
    void validateAndConvert_exactlyAtLimit_shouldWork() {
        // Exactly 2MB with valid PNG magic bytes should be fine
        byte[] content = new byte[2 * 1024 * 1024];
        content[0] = (byte) 0x89;
        content[1] = 0x50;
        content[2] = 0x4E;
        content[3] = 0x47;
        var file = new MockMultipartFile("avatar", "exact.png", "image/png", content);

        String result = AvatarUtils.validateAndConvert(file);
        assertNotNull(result);
        assertTrue(result.startsWith("data:image/png;base64,"));
    }

    @Test
    void validateAndConvert_webpImage_shouldWork() {
        byte[] imageBytes = new byte[]{
                0x52, 0x49, 0x46, 0x46, // RIFF
                0x00, 0x00, 0x00, 0x00, // file size (placeholder)
                0x57, 0x45, 0x42, 0x50  // WEBP
        };
        var file = new MockMultipartFile("avatar", "photo.webp", "image/webp", imageBytes);

        String result = AvatarUtils.validateAndConvert(file);

        assertNotNull(result);
        assertTrue(result.startsWith("data:image/webp;base64,"));
    }

    @Test
    void validateAndConvert_gifImage_shouldWork() {
        byte[] imageBytes = new byte[]{0x47, 0x49, 0x46, 0x38}; // GIF magic bytes
        var file = new MockMultipartFile("avatar", "photo.gif", "image/gif", imageBytes);

        String result = AvatarUtils.validateAndConvert(file);

        assertNotNull(result);
        assertTrue(result.startsWith("data:image/gif;base64,"));
    }
}
