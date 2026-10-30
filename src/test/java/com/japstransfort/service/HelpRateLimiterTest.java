package com.japstransfort.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HelpRateLimiterTest {

    private HelpRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new HelpRateLimiter(50); // Global cap 50 for testing
    }

    @Test
    void testUnderLimitAllowed() {
        for (int i = 0; i < 10; i++) {
            assertEquals(HelpRateLimiter.RateLimitResult.ALLOWED, rateLimiter.checkAndConsume("userA"));
        }
    }

    @Test
    void testEleventhQuestionHitsMinuteLimit() {
        for (int i = 0; i < 10; i++) {
            assertEquals(HelpRateLimiter.RateLimitResult.ALLOWED, rateLimiter.checkAndConsume("userB"));
        }
        // 11th request within 1 minute
        assertEquals(HelpRateLimiter.RateLimitResult.USER_MINUTE_LIMIT, rateLimiter.checkAndConsume("userB"));
        // Another user is not affected
        assertEquals(HelpRateLimiter.RateLimitResult.ALLOWED, rateLimiter.checkAndConsume("userC"));
    }

    @Test
    void testGlobalCapHit() {
        HelpRateLimiter smallGlobal = new HelpRateLimiter(5);
        for (int i = 0; i < 5; i++) {
            assertEquals(HelpRateLimiter.RateLimitResult.ALLOWED, smallGlobal.checkAndConsume("user" + i));
        }
        // 6th call hits global cap
        assertEquals(HelpRateLimiter.RateLimitResult.GLOBAL_DAILY_LIMIT, smallGlobal.checkAndConsume("user99"));
    }
}
