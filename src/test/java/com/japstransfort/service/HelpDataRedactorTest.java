package com.japstransfort.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HelpDataRedactorTest {

    @Test
    void testNullAndEmpty() {
        assertEquals("", HelpDataRedactor.redact(null));
        assertEquals("", HelpDataRedactor.redact(""));
    }

    @Test
    void testCleanTextUnchanged() {
        String input = "How do I add a new product to my catalog?";
        assertEquals(input, HelpDataRedactor.redact(input));
    }

    @Test
    void testEmailRedaction() {
        String input = "Please contact me at admin@japstransfort.com or user.test+filter@sub.domain.org for info.";
        String expected = "Please contact me at [removed] or [removed] for info.";
        assertEquals(expected, HelpDataRedactor.redact(input));
    }

    @Test
    void testPhoneRedactionSeparated() {
        String input = "My number is +63 917 123 4567 or call (02) 8123-4567 or +1-800-555-0199.";
        String expected = "My number is [removed] or call [removed] or [removed].";
        assertEquals(expected, HelpDataRedactor.redact(input));
    }

    @Test
    void testPhoneOrDigitSequenceUnseparated() {
        String input = "My unseparated number is 09171234567 and tracking ref is 123456789012.";
        String expected = "My unseparated number is [removed] and tracking ref is [removed].";
        assertEquals(expected, HelpDataRedactor.redact(input));
    }

    @Test
    void testDigitSequenceEightOrMore() {
        String input = "7 digits 1234567 is ok, but 8 digits 12345678 and 10 digits 9876543210 must be stripped.";
        String expected = "7 digits 1234567 is ok, but 8 digits [removed] and 10 digits [removed] must be stripped.";
        assertEquals(expected, HelpDataRedactor.redact(input));
    }

    @Test
    void testUrlWithTokenRedaction() {
        String input = "Click https://example.com/api?token=secret12345 or https://test.org/auth#session=xyz to view.";
        String expected = "Click [removed] or [removed] to view.";
        assertEquals(expected, HelpDataRedactor.redact(input));
    }

    @Test
    void testUrlWithoutTokenPreserved() {
        String input = "Visit https://google.com for info.";
        assertEquals(input, HelpDataRedactor.redact(input));
    }

    @Test
    void testCombinedMessage() {
        String input = "Hi, my email is john@mail.com, phone 0919-876-5432, ref 88887777, auth link https://japs.com/reset?key=abc!";
        String expected = "Hi, my email is [removed], phone [removed], ref [removed], auth link [removed]!";
        assertEquals(expected, HelpDataRedactor.redact(input));
    }
}
