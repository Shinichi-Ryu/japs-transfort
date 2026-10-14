package com.japstransfort.validator;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable validator enforcing standard password complexity rules:
 * - At least 8 and at most 64 characters
 * - At least one uppercase letter (A-Z)
 * - At least one lowercase letter (a-z)
 * - At least one digit (0-9)
 * - At least one special character (any printable ASCII symbol)
 * - No spaces or other whitespace
 * - Printable ASCII characters only (BCrypt limit)
 */
public class PasswordValidator {

    public static List<String> validate(String password) {
        if (password == null || password.isEmpty()) {
            return List.of("Password is required.");
        }

        List<String> errors = new ArrayList<>();

        if (password.length() < 8 || password.length() > 64) {
            errors.add("Password must be at least 8 and at most 64 characters.");
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        boolean hasWhitespace = false;
        boolean hasNonAscii = false;

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isWhitespace(c)) {
                hasWhitespace = true;
            }
            if (c < 32 || c > 126) {
                hasNonAscii = true;
            }
            if (c >= 'A' && c <= 'Z') {
                hasUpper = true;
            } else if (c >= 'a' && c <= 'z') {
                hasLower = true;
            } else if (c >= '0' && c <= '9') {
                hasDigit = true;
            } else if (c >= 33 && c <= 126) {
                hasSpecial = true;
            }
        }

        if (!hasUpper) {
            errors.add("Password must include at least one uppercase letter.");
        }
        if (!hasLower) {
            errors.add("Password must include at least one lowercase letter.");
        }
        if (!hasDigit) {
            errors.add("Password must include at least one digit.");
        }
        if (!hasSpecial) {
            errors.add("Password must include at least one special character.");
        }
        if (hasWhitespace) {
            errors.add("Password must not contain spaces or other whitespace.");
        }
        if (hasNonAscii) {
            errors.add("Password must contain printable ASCII characters only.");
        }

        return errors;
    }

    public static boolean isValid(String password) {
        return validate(password).isEmpty();
    }
}
