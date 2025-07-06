package lk.banking.security;

import java.security.SecureRandom;
import java.util.Base64;

public class SecurityUtils {
    private static final SecureRandom random = new SecureRandom();

    public static String generateRandomToken(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String masked = parts[0].charAt(0) + "***" + parts[0].charAt(parts[0].length() - 1);
        return masked + "@" + parts[1];
    }
}