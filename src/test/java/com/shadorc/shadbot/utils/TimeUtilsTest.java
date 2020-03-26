package com.shadorc.shadbot.utils;

import com.shadorc.shadbot.utils.TimeUtils;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class TimeUtilsTest {

    @Test
    public void testGetMillisUntilInstant() {
        final int offset = 30;
        final int millis = new Random().nextInt(10_000);
        final long elapsed = TimeUtils.getMillisUntil(Instant.now().minusMillis(millis));
        assertTrue(elapsed >= millis - offset && elapsed <= millis + offset);
        assertThrows(NullPointerException.class, () -> TimeUtils.getMillisUntil(null));
    }

    @Test
    public void testGetMillisUntilLong() {
        final int offset = 30;
        final int millis = new Random().nextInt(10_000);
        final long elapsed = TimeUtils.getMillisUntil(Instant.now().minusMillis(millis).toEpochMilli());
        assertTrue(elapsed >= millis - offset && elapsed <= millis + offset);
        assertThrows(NullPointerException.class, () -> TimeUtils.getMillisUntil(null));
    }

    @Test
    public void testParseTime() {
        assertEquals(60, TimeUtils.parseTime("1m"));
        assertEquals(60, TimeUtils.parseTime("1m 00s"));
        assertEquals(3600 + 10 * 60 + 6, TimeUtils.parseTime("01h10m6s"));
        assertThrows(IllegalArgumentException.class, () -> TimeUtils.parseTime("1m00"));
        assertThrows(IllegalArgumentException.class, () -> TimeUtils.parseTime("26d01h10m6s"));
        assertThrows(IllegalArgumentException.class, () -> TimeUtils.parseTime("-10s"));
        assertThrows(NullPointerException.class, () -> TimeUtils.parseTime(null));
    }

    @Test
    public void testToLocalDate() {
        final Instant now = Instant.now();
        assertEquals(LocalDateTime.ofInstant(now, ZoneId.systemDefault()), TimeUtils.toLocalDate(now));
        assertThrows(NullPointerException.class, () -> TimeUtils.toLocalDate(null));
    }
}
