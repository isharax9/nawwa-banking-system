package lk.banking.security;

import java.security.SecureRandom;
import java.util.Base64;

public class SecurityUtils {
    // SecureRandom is thread-safe and expensive to initialize, so keep it as a static final field.
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generates a cryptographically strong, URL-safe random token.
     * The token's length is determined by the number of random bytes generated,
     * which is then Base64 encoded.
     *
     * @param numBytes The number of random bytes to generate. More bytes mean a longer,
     *                 stronger token (e.g., 16 bytes for a good-length token).
     * @return A URL-safe Base64 encoded string.
     */
    public static String generateRandomToken(int numBytes) {
        if (numBytes <= 0) {
            throw new IllegalArgumentException("Number of bytes for token must be positive.");
        }
        byte[] bytes = new byte[numBytes];
        random.nextBytes(bytes); // Fills the byte array with cryptographically strong random bytes
        // URL-safe Base64 encoding without padding characters
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Masks an email address for privacy display.
     * Examples:
     * - "john.doe@example.com" -> "j***e@example.com"
     * - "jo@example.com" -> "j***@example.com"
     * - "j@example.com" -> "j***@example.com"
     * - invalid/null emails are returned as-is.
     *
     * @param email The email address to mask.
     * @return The masked email string, or the original string if invalid/null.
     */
    public static String maskEmail(String email) {
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            return email; // Return as-is if null, empty, or not a basic email format
        }

        String[] parts = email.split("@");
        if (parts.length != 2) {
            return email; // Not a valid "local@domain" format
        }

        String localPart = parts[0];
        String domainPart = parts[1];

        // Masking logic for the local part
        String maskedLocalPart;
        if (localPart.length() <= 2) {
            // For very short local parts (e.g., "j", "jo"), show first char, then mask
            maskedLocalPart = localPart.charAt(0) + "***";
        } else {
            // For longer local parts, show first char, mask middle, show last char
            maskedLocalPart = localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1);
        }

        return maskedLocalPart + "@" + domainPart;
    }
}