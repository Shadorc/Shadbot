package com.shadorc.shadbot.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class TimeUtilTest {

    @Test
    public void testIsLocalDateInTheSameWeek() {
        assertTrue(TimeUtil.isLocalDateInTheSameWeek(LocalDate.now(), LocalDate.now()));
        assertTrue(TimeUtil.isLocalDateInTheSameWeek(
                LocalDate.of(2020, 4, 11), LocalDate.of(2020, 4, 6)));
        assertFalse(TimeUtil.isLocalDateInTheSameWeek(
                LocalDate.of(2020, 4, 11), LocalDate.of(2020, 4, 13)));
    }

    @Test
    public void testGetMillisUntilInstant() {
        final int offset = 30;
        final int millis = new Random().nextInt(10_000);
        final long elapsed = TimeUtil.getMillisUntil(Instant.now().minusMillis(millis));
        assertTrue(elapsed >= millis - offset && elapsed <= millis + offset);
    }

    @Test
    public void testGetMillisUntilLong() {
        final int offset = 30;
        final int millis = new Random().nextInt(10_000);
        final long elapsed = TimeUtil.getMillisUntil(Instant.now().minusMillis(millis).toEpochMilli());
        assertTrue(elapsed >= millis - offset && elapsed <= millis + offset);
    }

    @Test
    public void testParseTime() {
        assertEquals(60, TimeUtil.parseTime("1m"));
        assertEquals(60, TimeUtil.parseTime("1m 00s"));
        assertEquals(3600 + 10 * 60 + 6, TimeUtil.parseTime("01h10m6s"));
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
