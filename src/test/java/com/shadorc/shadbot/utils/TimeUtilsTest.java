package com.shadorc.shadbot.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class TimeUtilsTest {

    @Test
    public void testIsLocalDateInTheSameWeek() {
        assertTrue(TimeUtils.isLocalDateInTheSameWeek(LocalDate.now(), LocalDate.now()));
        assertTrue(TimeUtils.isLocalDateInTheSameWeek(
                LocalDate.of(2020, 04, 11), LocalDate.of(2020, 04, 6)));
        assertFalse(TimeUtils.isLocalDateInTheSameWeek(
                LocalDate.of(2020, 04, 11), LocalDate.of(2020, 04, 13)));
    }

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
    public void testToLocalDateTime() {
        final Instant now = Instant.now();
        assertEquals(LocalDateTime.ofInstant(now, ZoneId.systemDefault()), TimeUtils.toLocalDateTime(now));
        assertThrows(NullPointerException.class, () -> TimeUtils.toLocalDateTime(null));
    }
}
