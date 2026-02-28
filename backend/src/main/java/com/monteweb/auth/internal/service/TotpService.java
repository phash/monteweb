package com.monteweb.auth.internal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * TOTP (Time-based One-Time Password) service implementing RFC 6238.
 * Uses HMAC-SHA1 with 30-second time steps and 6-digit codes.
 * No external libraries — uses javax.crypto.Mac directly.
 */
@Service
public class TotpService {

    private static final Logger log = LoggerFactory.getLogger(TotpService.class);
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final int SECRET_BYTES = 20;
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final String ISSUER = "MonteWeb";
    private static final int RECOVERY_CODE_COUNT = 8;
    private static final int RECOVERY_CODE_LENGTH = 8;

    // Base32 alphabet (RFC 4648)
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final SecureRandom secureRandom = new SecureRandom();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Generates a random TOTP secret encoded in Base32.
     */
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /**
     * Generates an otpauth:// URI for QR code generation.
     */
    public String generateTotpUri(String secret, String email) {
        String encodedIssuer = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8);
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                encodedIssuer, encodedEmail, secret, encodedIssuer, CODE_DIGITS, TIME_STEP_SECONDS);
    }

    /**
     * Verifies a TOTP code against the given secret.
     * Checks current time step and +/- 1 window for clock drift tolerance.
     */
    public boolean verifyCode(String secret, String code) {
        if (code == null || code.length() != CODE_DIGITS) {
            return false;
        }

        int codeInt;
        try {
            codeInt = Integer.parseInt(code);
        } catch (NumberFormatException e) {
            return false;
        }

        byte[] secretBytes = base32Decode(secret);
        long currentTimeStep = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;

        // Check current and adjacent time steps (window of 1)
        for (int i = -1; i <= 1; i++) {
            int generatedCode = generateCode(secretBytes, currentTimeStep + i);
            if (generatedCode == codeInt) {
                return true;
            }
        }

        return false;
    }

    /**
     * Generates 8 random alphanumeric recovery codes.
     */
    public List<String> generateRecoveryCodes() {
        List<String> codes = new ArrayList<>();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < RECOVERY_CODE_LENGTH; j++) {
                code.append(chars.charAt(secureRandom.nextInt(chars.length())));
            }
            codes.add(code.toString());
        }
        return codes;
    }

    /**
     * Hashes a recovery code using BCrypt for secure storage.
     */
    public String hashRecoveryCode(String plainCode) {
        return passwordEncoder.encode(plainCode.toUpperCase());
    }

    /**
     * Verifies a recovery code against a stored value.
     * Supports both BCrypt-hashed codes and legacy plaintext codes.
     */
    public boolean verifyRecoveryCode(String input, String stored) {
        if (stored != null && (stored.startsWith("$2a$") || stored.startsWith("$2b$"))) {
            return passwordEncoder.matches(input.toUpperCase(), stored);
        }
        return stored != null && stored.equals(input.toUpperCase()); // legacy plaintext
    }

    /**
     * Generates a TOTP code for the given secret bytes and time counter.
     * Implements RFC 6238 / RFC 4226 HOTP algorithm with HMAC-SHA1.
     */
    private int generateCode(byte[] secretBytes, long timeCounter) {
        byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeCounter).array();

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(timeBytes);

            // Dynamic truncation (RFC 4226 section 5.4)
            int offset = hash[hash.length - 1] & 0x0F;
            int truncatedHash = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            return truncatedHash % (int) Math.pow(10, CODE_DIGITS);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("TOTP generation failed", e);
            throw new RuntimeException("TOTP generation failed", e);
        }
    }

    /**
     * Encodes bytes to Base32 (RFC 4648).
     */
    private String base32Encode(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;

        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                result.append(BASE32_ALPHABET.charAt((buffer >> bitsLeft) & 0x1F));
            }
        }
        if (bitsLeft > 0) {
            result.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return result.toString();
    }

    /**
     * Decodes a Base32 string to bytes.
     */
    private byte[] base32Decode(String encoded) {
        String upper = encoded.toUpperCase().replaceAll("[^A-Z2-7]", "");
        int buffer = 0;
        int bitsLeft = 0;
        List<Byte> result = new ArrayList<>();

        for (char c : upper.toCharArray()) {
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                result.add((byte) ((buffer >> bitsLeft) & 0xFF));
            }
        }

        byte[] bytes = new byte[result.size()];
        for (int i = 0; i < result.size(); i++) {
            bytes[i] = result.get(i);
        }
        return bytes;
    }
}
