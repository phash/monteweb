package com.monteweb.shared;

import com.monteweb.shared.util.AesEncryptionService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AesEncryptionService covering encrypt/decrypt roundtrip,
 * legacy plaintext passthrough, null/empty handling, and IV randomness.
 */
class AesEncryptionServiceTest {

    private static AesEncryptionService service;

    @BeforeAll
    static void setUp() {
        // Construct with a dedicated encryption secret (>= 32 chars) and a JWT secret fallback.
        // The service uses the encryption secret when it's >= 32 chars.
        String encryptionSecret = "a-very-secure-encryption-secret-that-is-at-least-32-chars-long";
        String jwtSecret = "fallback-jwt-secret-not-used-when-encryption-secret-is-set";
        service = new AesEncryptionService(encryptionSecret, jwtSecret);
    }

    @Test
    void encrypt_decrypt_roundtrip() {
        String plaintext = "Hello, MonteWeb! Umlaute: aou";

        String encrypted = service.encrypt(plaintext);
        String decrypted = service.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    void encrypt_decrypt_roundtrip_withSpecialCharacters() {
        String plaintext = "P@ssw0rd!#$%^&*()_+-={}[]|\\:\";<>?,./~`";

        String encrypted = service.encrypt(plaintext);
        String decrypted = service.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    void encrypt_decrypt_roundtrip_withEmoji() {
        String plaintext = "Test with emoji: \uD83D\uDE00\uD83C\uDF1F\uD83D\uDCA1";

        String encrypted = service.encrypt(plaintext);
        String decrypted = service.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    void decrypt_legacy_plaintext_passthrough() {
        // A value not wrapped in ENC(...) should be returned as-is (legacy plaintext)
        String legacyPlaintext = "my-old-github-pat-token-12345";

        String result = service.decrypt(legacyPlaintext);

        assertEquals(legacyPlaintext, result);
    }

    @Test
    void decrypt_legacy_plaintext_passthrough_withSpecialChars() {
        String legacyValue = "ghp_abc123XYZ!@#$%";

        String result = service.decrypt(legacyValue);

        assertEquals(legacyValue, result);
    }

    @Test
    void encrypt_produces_ENC_prefix() {
        String encrypted = service.encrypt("test-value");

        assertTrue(encrypted.startsWith("ENC("), "Encrypted value should start with 'ENC('");
        assertTrue(encrypted.endsWith(")"), "Encrypted value should end with ')'");
    }

    @Test
    void decrypt_null_returns_null() {
        assertNull(service.decrypt(null));
    }

    @Test
    void encrypt_null_returns_null() {
        assertNull(service.encrypt(null));
    }

    @Test
    void decrypt_empty_returns_empty() {
        // Empty string does not start with "ENC(" so it's treated as legacy plaintext passthrough
        assertEquals("", service.decrypt(""));
    }

    @Test
    void encrypt_empty_string_roundtrip() {
        String encrypted = service.encrypt("");
        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("ENC("));
        assertEquals("", service.decrypt(encrypted));
    }

    @Test
    void encrypt_different_calls_produce_different_output() {
        // Due to random IV, encrypting the same plaintext should produce different ciphertext
        String plaintext = "same-input-every-time";

        String encrypted1 = service.encrypt(plaintext);
        String encrypted2 = service.encrypt(plaintext);

        assertNotEquals(encrypted1, encrypted2,
                "Two encryptions of the same plaintext should differ due to random IV");

        // But both should decrypt to the same plaintext
        assertEquals(plaintext, service.decrypt(encrypted1));
        assertEquals(plaintext, service.decrypt(encrypted2));
    }

    @Test
    void isEncrypted_returns_true_for_encrypted_value() {
        String encrypted = service.encrypt("test");

        assertTrue(service.isEncrypted(encrypted));
    }

    @Test
    void isEncrypted_returns_false_for_plaintext() {
        assertFalse(service.isEncrypted("plain-value"));
    }

    @Test
    void isEncrypted_returns_false_for_null() {
        assertFalse(service.isEncrypted(null));
    }

    @Test
    void isEncrypted_returns_false_for_partial_prefix() {
        assertFalse(service.isEncrypted("ENC(no-closing-paren"));
    }

    @Test
    void decrypt_tampered_ciphertext_throws() {
        String encrypted = service.encrypt("sensitive-data");

        // Tamper with the base64 content inside ENC(...)
        String base64Part = encrypted.substring(4, encrypted.length() - 1);
        char[] chars = base64Part.toCharArray();
        // Flip a character in the middle of the ciphertext
        int midpoint = chars.length / 2;
        chars[midpoint] = (chars[midpoint] == 'A') ? 'B' : 'A';
        String tampered = "ENC(" + new String(chars) + ")";

        assertThrows(IllegalStateException.class, () -> service.decrypt(tampered),
                "Decrypting tampered ciphertext should throw");
    }

    @Test
    void decrypt_invalid_base64_throws() {
        assertThrows(IllegalStateException.class,
                () -> service.decrypt("ENC(not-valid-base64!!!)"),
                "Decrypting invalid base64 should throw");
    }

    @Test
    void constructor_falls_back_to_jwt_secret_when_encryption_secret_too_short() {
        // Encryption secret too short (< 32 chars), so it falls back to JWT secret
        AesEncryptionService fallbackService = new AesEncryptionService(
                "short",
                "a-jwt-secret-used-as-fallback-for-aes-key-derivation"
        );

        // Service should still work with the fallback key
        String encrypted = fallbackService.encrypt("test-data");
        assertEquals("test-data", fallbackService.decrypt(encrypted));
    }

    @Test
    void constructor_falls_back_to_jwt_secret_when_encryption_secret_null() {
        AesEncryptionService fallbackService = new AesEncryptionService(
                null,
                "a-jwt-secret-used-as-fallback-for-aes-key-derivation"
        );

        String encrypted = fallbackService.encrypt("test-data");
        assertEquals("test-data", fallbackService.decrypt(encrypted));
    }

    @Test
    void encrypt_long_plaintext_roundtrip() {
        // Test with a longer plaintext to ensure multi-block encryption works
        String longPlaintext = "A".repeat(10_000);

        String encrypted = service.encrypt(longPlaintext);
        String decrypted = service.decrypt(encrypted);

        assertEquals(longPlaintext, decrypted);
    }

    @Test
    void different_keys_cannot_decrypt_each_others_ciphertext() {
        AesEncryptionService otherService = new AesEncryptionService(
                "a-completely-different-encryption-secret-with-32-chars-minimum",
                "unused-jwt-secret"
        );

        String encrypted = service.encrypt("secret-message");

        // Trying to decrypt with a different key should fail
        assertThrows(IllegalStateException.class, () -> otherService.decrypt(encrypted),
                "Decrypting with a different key should throw");
    }
}
