package com.japstransfort.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory rate limiter for Jappy AI help questions.
 * Limits:
 * - 10 questions per minute per user
 * - 100 questions per day per user
 * - Global daily cap (configurable, default 1000)
 * Uses no database tables. Reclaims memory when calendar days roll over.
 */
@Service
public class HelpRateLimiter {

    public enum RateLimitResult {
        ALLOWED,
        USER_MINUTE_LIMIT,
        USER_DAILY_LIMIT,
        GLOBAL_DAILY_LIMIT
    }

    private static final ZoneId TIMEZONE = ZoneId.of("Asia/Manila");
    private static final int USER_MINUTE_MAX = 10;
    private static final int USER_DAILY_MAX = 100;

    private final int globalDailyCap;
    private final ConcurrentHashMap<String, UserUsage> userMap = new ConcurrentHashMap<>();
    private final AtomicInteger globalDailyCounter = new AtomicInteger(0);
    private volatile LocalDate currentGlobalDay = LocalDate.now(TIMEZONE);

    public HelpRateLimiter() {
        this(parseGlobalCap());
    }

    public HelpRateLimiter(int globalDailyCap) {
        this.globalDailyCap = globalDailyCap > 0 ? globalDailyCap : 1000;
    }

    private static int parseGlobalCap() {
        String env = System.getenv("GEMINI_DAILY_CAP");
        if (env == null || env.isBlank()) {
            env = System.getProperty("GEMINI_DAILY_CAP");
        }
        if (env != null && !env.isBlank()) {
            try {
                return Integer.parseInt(env.trim());
            } catch (NumberFormatException ignored) {}
        }
        return 1000;
    }

    /**
     * Checks rate limits and consumes 1 quota token if allowed.
     */
    public RateLimitResult checkAndConsume(String userId) {
        if (userId == null || userId.isBlank()) {
            userId = "anonymous";
        }

        LocalDate today = LocalDate.now(TIMEZONE);
        // Reset global counter if day changed
        if (!today.equals(currentGlobalDay)) {
            synchronized (this) {
                if (!today.equals(currentGlobalDay)) {
                    currentGlobalDay = today;
                    globalDailyCounter.set(0);
                    userMap.clear();
                }
            }
        }

        // Check global daily cap
        if (globalDailyCounter.get() >= globalDailyCap) {
            return RateLimitResult.GLOBAL_DAILY_LIMIT;
        }

        UserUsage usage = userMap.computeIfAbsent(userId, k -> new UserUsage(today));
        synchronized (usage) {
            long now = System.currentTimeMillis();

            // Check if user day changed
            if (!today.equals(usage.day)) {
                usage.day = today;
                usage.dailyCount = 0;
                usage.minuteTimestamps.clear();
            }

            // Prune timestamps older than 60 seconds (60,000 ms)
            while (!usage.minuteTimestamps.isEmpty() && now - usage.minuteTimestamps.peekFirst() > 60_000L) {
                usage.minuteTimestamps.pollFirst();
            }

            if (usage.minuteTimestamps.size() >= USER_MINUTE_MAX) {
                return RateLimitResult.USER_MINUTE_LIMIT;
            }

            if (usage.dailyCount >= USER_DAILY_MAX) {
                return RateLimitResult.USER_DAILY_LIMIT;
            }

            // Allowed: record consumption
            usage.minuteTimestamps.addLast(now);
            usage.dailyCount++;
            globalDailyCounter.incrementAndGet();
            return RateLimitResult.ALLOWED;
        }
    }

    /**
     * Resets all counters (useful for unit testing).
     */
    public void reset() {
        globalDailyCounter.set(0);
        userMap.clear();
        currentGlobalDay = LocalDate.now(TIMEZONE);
    }

    private static class UserUsage {
        private LocalDate day;
        private int dailyCount = 0;
        private final Deque<Long> minuteTimestamps = new ArrayDeque<>();

        public UserUsage(LocalDate day) {
            this.day = day;
        }
    }
}
