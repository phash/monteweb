package com.monteweb.shared;

import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.util.AvatarUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class AvatarUtilsTest {

    private static byte[] pngBytes(int size) {
        byte[] data = new byte[Math.max(size, 4)];
        // PNG magic bytes
        data[0] = (byte) 0x89;
        data[1] = 0x50;
        data[2] = 0x4E;
        data[3] = 0x47;
        return data;
    }

    private static byte[] jpegBytes(int size) {
        byte[] data = new byte[Math.max(size, 3)];
        data[0] = (byte) 0xFF;
        data[1] = (byte) 0xD8;
        data[2] = (byte) 0xFF;
        return data;
    }

    @Test
    void validateAndConvert_validImage_shouldReturnDataUrl() {
        var file = new MockMultipartFile("avatar", "photo.png", "image/png", pngBytes(100));

        String result = AvatarUtils.validateAndConvert(file);

        assertNotNull(result);
        assertTrue(result.startsWith("data:image/png;base64,"));
    }

    @Test
    void validateAndConvert_jpegImage_shouldWork() {
        var file = new MockMultipartFile("avatar", "photo.jpg", "image/jpeg", jpegBytes(100));

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
        byte[] largeContent = new byte[2 * 1024 * 1024 + 1];
        // Add PNG magic bytes even though it's too large
        largeContent[0] = (byte) 0x89;
        largeContent[1] = 0x50;
        largeContent[2] = 0x4E;
        largeContent[3] = 0x47;
        var file = new MockMultipartFile("avatar", "large.png", "image/png", largeContent);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> AvatarUtils.validateAndConvert(file));
        assertEquals("Avatar must be smaller than 2MB", ex.getMessage());
    }

    @Test
    void validateAndConvert_exactlyAtLimit_shouldWork() {
        byte[] content = pngBytes(2 * 1024 * 1024);
        var file = new MockMultipartFile("avatar", "exact.png", "image/png", content);

        String result = AvatarUtils.validateAndConvert(file);
        assertNotNull(result);
        assertTrue(result.startsWith("data:image/png;base64,"));
    }

    @Test
    void validateAndConvert_spoofedContentType_shouldThrow() {
        // File claims to be image/png but has no valid magic bytes
        byte[] fakeData = "This is not an image".getBytes();
        var file = new MockMultipartFile("avatar", "fake.png", "image/png", fakeData);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> AvatarUtils.validateAndConvert(file));
        assertEquals("File content does not match an image format", ex.getMessage());
    }
}
