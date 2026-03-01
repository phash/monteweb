package com.monteweb.shared.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AesEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String ENC_PREFIX = "ENC(";
    private static final String ENC_SUFFIX = ")";

    private final SecretKey secretKey;

    public AesEncryptionService(
            @Value("${monteweb.encryption.secret:}") String encryptionSecret,
            @Value("${monteweb.jwt.secret}") String jwtSecret) {
        try {
            // Use dedicated encryption secret; fall back to JWT secret for backward compatibility.
            // Log a warning when falling back so operators know to set ENCRYPTION_SECRET.
            String keySource;
            if (encryptionSecret != null && encryptionSecret.length() >= 32) {
                keySource = encryptionSecret;
            } else {
                org.slf4j.LoggerFactory.getLogger(AesEncryptionService.class)
                        .warn("SECURITY: monteweb.encryption.secret is not set or too short (<32 chars). " +
                              "Falling back to JWT secret for AES key derivation. " +
                              "Set ENCRYPTION_SECRET to a separate 64-char+ secret.");
                keySource = jwtSecret;
            }
            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(keySource.getBytes());
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize AES encryption key", e);
        }
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

            byte[] combined = ByteBuffer.allocate(iv.length + ciphertext.length)
                    .put(iv)
                    .put(ciphertext)
                    .array();

            return ENC_PREFIX + Base64.getEncoder().encodeToString(combined) + ENC_SUFFIX;
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public String decrypt(String value) {
        if (value == null) return null;
        if (!value.startsWith(ENC_PREFIX) || !value.endsWith(ENC_SUFFIX)) {
            return value; // Legacy plaintext passthrough
        }
        try {
            String base64 = value.substring(ENC_PREFIX.length(), value.length() - ENC_SUFFIX.length());
            byte[] combined = Base64.getDecoder().decode(base64);

            byte[] iv = ByteBuffer.wrap(combined, 0, GCM_IV_LENGTH).array();
            byte[] ciphertext = ByteBuffer.wrap(combined, GCM_IV_LENGTH, combined.length - GCM_IV_LENGTH).array();

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext));
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENC_PREFIX) && value.endsWith(ENC_SUFFIX);
    }
}
