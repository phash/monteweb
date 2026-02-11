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
        byte[] imageBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
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
        var file = new MockMultipartFile("avatar", "doc.pdf", "application/pdf", "data".getBytes());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> AvatarUtils.validateAndConvert(file));
        assertEquals("File must be an image", ex.getMessage());
    }

    @Test
    void validateAndConvert_nullContentType_shouldThrow() {
        var file = new MockMultipartFile("avatar", "file", null, "data".getBytes());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> AvatarUtils.validateAndConvert(file));
        assertEquals("File must be an image", ex.getMessage());
    }

    @Test
    void validateAndConvert_tooLargeFile_shouldThrow() {
        // Create a file larger than 2MB
        byte[] largeContent = new byte[2 * 1024 * 1024 + 1];
        var file = new MockMultipartFile("avatar", "large.png", "image/png", largeContent);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> AvatarUtils.validateAndConvert(file));
        assertEquals("Avatar must be smaller than 2MB", ex.getMessage());
    }

    @Test
    void validateAndConvert_exactlyAtLimit_shouldWork() {
        // Exactly 2MB should be fine
        byte[] content = new byte[2 * 1024 * 1024];
        var file = new MockMultipartFile("avatar", "exact.png", "image/png", content);

        String result = AvatarUtils.validateAndConvert(file);
        assertNotNull(result);
        assertTrue(result.startsWith("data:image/png;base64,"));
    }
}
