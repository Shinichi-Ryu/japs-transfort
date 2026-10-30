package com.japstransfort.service;

import java.util.regex.Pattern;

/**
 * Utility to redact sensitive user data before sending messages to the Gemini API.
 * Replaces emails, phone numbers, URLs with tokens, and digit sequences >= 8 chars with [removed].
 */
public final class HelpDataRedactor {

    private HelpDataRedactor() {}

    // URLs with query/hash parameters containing token/key/auth/secret/session/code/sig/api
    // Excludes trailing punctuation like ., !, ?, ;, : from being swallowed
    private static final Pattern TOKEN_URL_PATTERN = Pattern.compile(
            "https?://[^\\s<>'\"{}|\\\\^`]+[?&#][^\\s<>'\"{}|\\\\^`]*(?:token|key|auth|secret|session|code|sig|api|credential|pwd|password|jwt)[a-zA-Z0-9_=%&/\\-]*",
            Pattern.CASE_INSENSITIVE
    );

    // Standard email addresses
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    );

    // Phone numbers (Philippine landline/mobile and international formats with separators)
    // Matches patterns like +63 917 123 4567, 0917-123-4567, (02) 8123-4567, +1-800-555-0199
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{2,4}\\)?[-.\\s]\\d{3,4}[-.\\s]\\d{3,4}"
    );

    // Sequences of 8 or more consecutive digits
    private static final Pattern DIGIT_SEQUENCE_PATTERN = Pattern.compile(
            "\\d{8,}"
    );

    /**
     * Redacts any email address, phone number, token URL, or 8+ digit sequence with [removed].
     */
    public static String redact(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String result = input;
        // 1. Redact URLs with tokens
        result = TOKEN_URL_PATTERN.matcher(result).replaceAll("[removed]");
        // 2. Redact Email addresses
        result = EMAIL_PATTERN.matcher(result).replaceAll("[removed]");
        // 3. Redact Separated phone numbers
        result = PHONE_PATTERN.matcher(result).replaceAll("[removed]");
        // 4. Redact 8+ digit sequences (e.g. unseparated phone numbers, account numbers, tokens)
        result = DIGIT_SEQUENCE_PATTERN.matcher(result).replaceAll("[removed]");

        return result;
    }
}
