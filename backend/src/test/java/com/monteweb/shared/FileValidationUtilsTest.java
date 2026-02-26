package com.monteweb.shared;

import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.util.FileValidationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class FileValidationUtilsTest {

    // ---- validateImage ----

    @Test
    void validateImage_validPng_shouldPass() {
        byte[] data = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x00, 0x00};
        var file = new MockMultipartFile("file", "image.png", "image/png", data);

        assertDoesNotThrow(() -> FileValidationUtils.validateImage(file));
    }

    @Test
    void validateImage_validJpeg_shouldPass() {
        byte[] data = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", data);

        assertDoesNotThrow(() -> FileValidationUtils.validateImage(file));
    }

    @Test
    void validateImage_validGif89a_shouldPass() {
        byte[] data = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x00, 0x00};
        var file = new MockMultipartFile("file", "anim.gif", "image/gif", data);

        assertDoesNotThrow(() -> FileValidationUtils.validateImage(file));
    }

    @Test
    void validateImage_validGif87a_shouldPass() {
        byte[] data = {0x47, 0x49, 0x46, 0x38, 0x37, 0x61, 0x00, 0x00};
        var file = new MockMultipartFile("file", "old.gif", "image/gif", data);

        assertDoesNotThrow(() -> FileValidationUtils.validateImage(file));
    }

    @Test
    void validateImage_validWebp_shouldPass() {
        byte[] data = {0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00};
        var file = new MockMultipartFile("file", "photo.webp", "image/webp", data);

        assertDoesNotThrow(() -> FileValidationUtils.validateImage(file));
    }

    @Test
    void validateImage_emptyFile_shouldThrow() {
        var file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> FileValidationUtils.validateImage(file));
        assertEquals("File is empty", ex.getMessage());
    }

    @Test
    void validateImage_nonImageContentType_shouldThrow() {
        var file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "data".getBytes());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> FileValidationUtils.validateImage(file));
        assertEquals("File must be an image", ex.getMessage());
    }

    @Test
    void validateImage_spoofedContentType_shouldThrow() {
        byte[] notImage = "This is text, not an image".getBytes();
        var file = new MockMultipartFile("file", "fake.png", "image/png", notImage);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> FileValidationUtils.validateImage(file));
        assertEquals("File content does not match an image format", ex.getMessage());
    }

    // ---- validateUpload ----

    @Test
    void validateUpload_normalPdf_shouldPass() {
        byte[] data = {0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-
        var file = new MockMultipartFile("file", "report.pdf", "application/pdf", data);

        assertDoesNotThrow(() -> FileValidationUtils.validateUpload(file));
    }

    @Test
    void validateUpload_normalDocx_shouldPass() {
        byte[] data = {0x50, 0x4B, 0x03, 0x04}; // ZIP magic (DOCX is a ZIP)
        var file = new MockMultipartFile("file", "document.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", data);

        assertDoesNotThrow(() -> FileValidationUtils.validateUpload(file));
    }

    @ParameterizedTest
    @ValueSource(strings = {"virus.exe", "script.bat", "hack.cmd", "shell.sh", "attack.php", "xss.html", "evil.svg"})
    void validateUpload_dangerousExtension_shouldThrow(String filename) {
        var file = new MockMultipartFile("file", filename, "application/octet-stream", "data".getBytes());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> FileValidationUtils.validateUpload(file));
        assertTrue(ex.getMessage().contains("is not allowed"));
    }

    @Test
    void validateUpload_imageWithSpoofedType_shouldThrow() {
        // Claims to be image/png but contains no valid image magic bytes
        byte[] notImage = "Not actually an image".getBytes();
        var file = new MockMultipartFile("file", "spoofed.png", "image/png", notImage);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> FileValidationUtils.validateUpload(file));
        assertEquals("File content does not match claimed image type", ex.getMessage());
    }

    @Test
    void validateUpload_imageWithValidMagicBytes_shouldPass() {
        byte[] data = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x00, 0x00};
        var file = new MockMultipartFile("file", "photo.png", "image/png", data);

        assertDoesNotThrow(() -> FileValidationUtils.validateUpload(file));
    }

    @Test
    void validateUpload_emptyFile_shouldThrow() {
        var file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> FileValidationUtils.validateUpload(file));
        assertEquals("File is empty", ex.getMessage());
    }

    // ---- isImageMagicBytes ----

    @Test
    void isImageMagicBytes_tooShort_shouldReturnFalse() {
        assertFalse(FileValidationUtils.isImageMagicBytes(new byte[]{0x00, 0x01}));
    }

    @Test
    void isImageMagicBytes_randomData_shouldReturnFalse() {
        assertFalse(FileValidationUtils.isImageMagicBytes(new byte[]{0x00, 0x01, 0x02, 0x03}));
    }

    @Test
    void isImageMagicBytes_pngMagic_shouldReturnTrue() {
        assertTrue(FileValidationUtils.isImageMagicBytes(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}));
    }

    @Test
    void isImageMagicBytes_jpegMagic_shouldReturnTrue() {
        assertTrue(FileValidationUtils.isImageMagicBytes(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}));
    }
}
