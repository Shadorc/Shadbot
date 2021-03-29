package com.shadorc.shadbot.utils;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
        assertEquals(Duration.ofMinutes(1), TimeUtil.parseTime("1m"));
        assertEquals(Duration.ofMinutes(1), TimeUtil.parseTime("1m 00s"));
        assertEquals(Duration.ofSeconds(3600 + 10 * 60 + 6), TimeUtil.parseTime("01h10m6s"));
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.parseTime("1m00"));
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.parseTime("26d01h10m6s"));
        assertThrows(IllegalArgumentException.class, () -> TimeUtil.parseTime("-10s"));
    }

    @Test
    public void testToLocalDateTime() {
        final Instant now = Instant.now();
        assertEquals(LocalDateTime.ofInstant(now, ZoneId.systemDefault()), TimeUtil.toLocalDateTime(now));
    }
}
