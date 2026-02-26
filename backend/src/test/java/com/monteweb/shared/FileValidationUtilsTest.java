package com.monteweb.shared;

import com.monteweb.shared.util.FileValidationUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class FileValidationUtilsTest {

    // --- detectContentType ---

    @Test
    void detectContentType_png_shouldDetectFromMagicBytes() {
        byte[] bytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A};
        var file = new MockMultipartFile("file", "test.png", "image/png", bytes);

        assertEquals("image/png", FileValidationUtils.detectContentType(file));
    }

    @Test
    void detectContentType_jpeg_shouldDetectFromMagicBytes() {
        byte[] bytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        var file = new MockMultipartFile("file", "test.jpg", "image/jpeg", bytes);

        assertEquals("image/jpeg", FileValidationUtils.detectContentType(file));
    }

    @Test
    void detectContentType_gif_shouldDetectFromMagicBytes() {
        byte[] bytes = new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
        var file = new MockMultipartFile("file", "test.gif", "image/gif", bytes);

        assertEquals("image/gif", FileValidationUtils.detectContentType(file));
    }

    @Test
    void detectContentType_webp_shouldDetectFromMagicBytes() {
        byte[] bytes = new byte[]{
                0x52, 0x49, 0x46, 0x46,
                0x00, 0x00, 0x00, 0x00,
                0x57, 0x45, 0x42, 0x50
        };
        var file = new MockMultipartFile("file", "test.webp", "image/webp", bytes);

        assertEquals("image/webp", FileValidationUtils.detectContentType(file));
    }

    @Test
    void detectContentType_pdf_shouldDetectFromMagicBytes() {
        byte[] bytes = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-
        var file = new MockMultipartFile("file", "test.pdf", "application/pdf", bytes);

        assertEquals("application/pdf", FileValidationUtils.detectContentType(file));
    }

    @Test
    void detectContentType_htmlSpoofedAsImage_shouldReturnOctetStream() {
        var file = new MockMultipartFile("file", "evil.png", "text/html", "<html>XSS</html>".getBytes());

        assertEquals("application/octet-stream", FileValidationUtils.detectContentType(file));
    }

    @Test
    void detectContentType_svgSpoofedAsImage_shouldReturnOctetStream() {
        var file = new MockMultipartFile("file", "evil.svg", "image/svg+xml", "<svg>".getBytes());

        assertEquals("application/octet-stream", FileValidationUtils.detectContentType(file));
    }

    @Test
    void detectContentType_unknownBinaryWithSafeType_shouldUseDeclaredType() {
        byte[] bytes = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04};
        var file = new MockMultipartFile("file", "doc.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", bytes);

        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                FileValidationUtils.detectContentType(file));
    }

    @Test
    void detectContentType_tooSmallFile_shouldReturnOctetStream() {
        byte[] bytes = new byte[]{0x01, 0x02};
        var file = new MockMultipartFile("file", "tiny.bin", "application/octet-stream", bytes);

        assertEquals("application/octet-stream", FileValidationUtils.detectContentType(file));
    }

    // --- validateImageContentType ---

    @Test
    void validateImageContentType_validPng_shouldReturnDetectedType() {
        byte[] bytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        var file = new MockMultipartFile("file", "test.png", "image/png", bytes);

        assertEquals("image/png", FileValidationUtils.validateImageContentType(file));
    }

    @Test
    void validateImageContentType_nonImage_shouldThrow() {
        var file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        assertThrows(IllegalArgumentException.class,
                () -> FileValidationUtils.validateImageContentType(file));
    }

    @Test
    void validateImageContentType_spoofedPngHeader_shouldThrow() {
        // File claims image/png but has PDF magic bytes
        byte[] bytes = new byte[]{0x25, 0x50, 0x44, 0x46};
        var file = new MockMultipartFile("file", "evil.png", "image/png", bytes);

        assertThrows(IllegalArgumentException.class,
                () -> FileValidationUtils.validateImageContentType(file));
    }

    // --- isSafeContentType ---

    @Test
    void isSafeContentType_applicationJson_shouldBeTrue() {
        assertTrue(FileValidationUtils.isSafeContentType("application/json"));
    }

    @Test
    void isSafeContentType_textHtml_shouldBeFalse() {
        assertFalse(FileValidationUtils.isSafeContentType("text/html"));
    }

    @Test
    void isSafeContentType_svgXml_shouldBeFalse() {
        assertFalse(FileValidationUtils.isSafeContentType("image/svg+xml"));
    }

    @Test
    void isSafeContentType_javascript_shouldBeFalse() {
        assertFalse(FileValidationUtils.isSafeContentType("application/javascript"));
    }

    @Test
    void isSafeContentType_null_shouldBeFalse() {
        assertFalse(FileValidationUtils.isSafeContentType(null));
    }

    // --- sanitizeExtension ---

    @Test
    void sanitizeExtension_normal_shouldReturnAsIs() {
        assertEquals("pdf", FileValidationUtils.sanitizeExtension("pdf"));
    }

    @Test
    void sanitizeExtension_withPathTraversal_shouldStrip() {
        assertEquals("etcpasswd", FileValidationUtils.sanitizeExtension("../../etc/passwd"));
    }

    @Test
    void sanitizeExtension_null_shouldReturnBin() {
        assertEquals("bin", FileValidationUtils.sanitizeExtension(null));
    }

    @Test
    void sanitizeExtension_tooLong_shouldTruncate() {
        String result = FileValidationUtils.sanitizeExtension("averylongextension");
        assertEquals(10, result.length());
    }

    // --- getExtensionFromFilename ---

    @Test
    void getExtensionFromFilename_normal_shouldExtract() {
        assertEquals("pdf", FileValidationUtils.getExtensionFromFilename("document.pdf"));
    }

    @Test
    void getExtensionFromFilename_noExtension_shouldReturnBin() {
        assertEquals("bin", FileValidationUtils.getExtensionFromFilename("document"));
    }

    @Test
    void getExtensionFromFilename_pathTraversal_shouldSanitize() {
        String result = FileValidationUtils.getExtensionFromFilename("file.../../etc/passwd");
        // After last dot: "../../etc/passwd" -> sanitized to "etcpasswd" (10 chars truncated)
        assertEquals("etcpasswd", result);
    }

    // --- sanitizeContentDispositionFilename ---

    @Test
    void sanitizeFilename_normal_shouldReturnAsIs() {
        assertEquals("document.pdf",
                FileValidationUtils.sanitizeContentDispositionFilename("document.pdf"));
    }

    @Test
    void sanitizeFilename_withQuotes_shouldReplace() {
        String result = FileValidationUtils.sanitizeContentDispositionFilename("file\"name.pdf");
        assertFalse(result.contains("\""));
    }

    @Test
    void sanitizeFilename_withNewlines_shouldReplace() {
        String result = FileValidationUtils.sanitizeContentDispositionFilename("file\r\nname.pdf");
        assertFalse(result.contains("\r"));
        assertFalse(result.contains("\n"));
    }

    @Test
    void sanitizeFilename_withPathSeparators_shouldReplace() {
        String result = FileValidationUtils.sanitizeContentDispositionFilename("../../../etc/passwd");
        assertFalse(result.contains("/"));
    }

    @Test
    void sanitizeFilename_null_shouldReturnDefault() {
        assertEquals("download", FileValidationUtils.sanitizeContentDispositionFilename(null));
    }

    @Test
    void sanitizeFilename_tooLong_shouldTruncate() {
        String longName = "a".repeat(300) + ".pdf";
        String result = FileValidationUtils.sanitizeContentDispositionFilename(longName);
        assertTrue(result.length() <= 200);
    }

    // --- extensionFromContentType ---

    @Test
    void extensionFromContentType_knownTypes_shouldMap() {
        assertEquals("jpg", FileValidationUtils.extensionFromContentType("image/jpeg"));
        assertEquals("png", FileValidationUtils.extensionFromContentType("image/png"));
        assertEquals("pdf", FileValidationUtils.extensionFromContentType("application/pdf"));
    }

    @Test
    void extensionFromContentType_unknown_shouldReturnBin() {
        assertEquals("bin", FileValidationUtils.extensionFromContentType("application/octet-stream"));
    }
}
