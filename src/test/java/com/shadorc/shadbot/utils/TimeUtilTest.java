package com.shadorc.shadbot.utils;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public class TimeUtilTest {

    @Test
    public void testElapsedInstant() {
        final int offset = 30;
        final int millis = ThreadLocalRandom.current().nextInt(10_000);
        final Duration elapsed = TimeUtil.elapsed(Instant.now().minusMillis(millis));
        assertTrue(elapsed.toMillis() >= millis - offset && elapsed.toMillis() <= millis + offset);
    }

    @Test
    public void testElapsedLong() {
        final int offset = 30;
        final int millis = ThreadLocalRandom.current().nextInt(10_000);
        final Duration elapsed = TimeUtil.elapsed(Instant.now().minusMillis(millis).toEpochMilli());
        assertTrue(elapsed.toMillis() >= millis - offset && elapsed.toMillis() <= millis + offset);
    }

    @Test
    public void testParseTime() {
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.parseTime("-123"));
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.parseTime("12345678910111213141516"));
        assertEquals(Duration.ofSeconds(123), TimeUtil.parseTime("123"));
        assertEquals(Duration.ZERO, TimeUtil.parseTime("0"));
        assertEquals(Duration.ZERO, TimeUtil.parseTime("0s"));
        assertEquals(Duration.ZERO, TimeUtil.parseTime("0m0s"));
        assertEquals(Duration.ZERO, TimeUtil.parseTime("0m"));
        assertEquals(Duration.ofSeconds(84), TimeUtil.parseTime("84s"));
        assertEquals(Duration.ofMinutes(1), TimeUtil.parseTime("1m"));
        assertEquals(Duration.ofMinutes(1), TimeUtil.parseTime("60s"));
        assertEquals(Duration.ofMinutes(1), TimeUtil.parseTime("1m00s"));
        assertEquals(Duration.ofMinutes(1), TimeUtil.parseTime("1m 00s"));
        assertEquals(Duration.ofSeconds(3600 + 10 * 60 + 6), TimeUtil.parseTime("01h10m6s"));
        assertEquals(Duration.ofMinutes(1), TimeUtil.parseTime("1m00"));
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.parseTime("26d01h10m6s"));
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.parseTime("-10s"));
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.parseTime("abcd"));
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.parseTime("s"));
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.parseTime("1ms"));
    }

}
