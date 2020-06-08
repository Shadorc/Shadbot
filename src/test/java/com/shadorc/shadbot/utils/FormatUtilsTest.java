package com.shadorc.shadbot.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class FormatUtilsTest {

    @Test
    public void testCoins() {
        assertEquals("100 coins", FormatUtils.coins(100));
        assertEquals("0 coin", FormatUtils.coins(0));
        assertEquals("-1 coin", FormatUtils.coins(-1));
        assertEquals("150,000 coins", FormatUtils.coins(150000));
        assertEquals("-150,000 coins", FormatUtils.coins(-150000));
        assertEquals("9,223,372,036,854,775,807 coins", FormatUtils.coins(Long.MAX_VALUE));
    }

    private enum TestEnum {
        TEST_ONE, test_Two, testThree;
    }

    @Test
    public void testCapitalizeEnum() {
        assertEquals("Test one", FormatUtils.capitalizeEnum(TestEnum.TEST_ONE));
        assertEquals("Test two", FormatUtils.capitalizeEnum(TestEnum.test_Two));
        assertEquals("Testthree", FormatUtils.capitalizeEnum(TestEnum.testThree));
        assertNull(FormatUtils.capitalizeEnum(null));
    }

    @Test
    public void testFormatDurationWords() {
        assertEquals("5 days 3 hours 2 minutes 1 second", FormatUtils.formatDurationWords(
                Duration.ofSeconds(5 * 24 * 60 * 60 + 3 * 60 * 60 + 2 * 60 + 1)));
        assertEquals("5 days 3 hours 2 minutes", FormatUtils.formatDurationWords(Duration.ofMinutes(5 * 24 * 60 + 3 * 60 + 2)));
        assertEquals("5 days 3 hours", FormatUtils.formatDurationWords(Duration.ofHours(5 * 24 + 3)));
        assertEquals("3 hours 2 minutes", FormatUtils.formatDurationWords(Duration.ofMinutes(3 * 60 + 2)));
        assertEquals("5 days 2 minutes", FormatUtils.formatDurationWords(Duration.ofMinutes(5 * 24 * 60 + 2)));
        assertEquals("5 days 1 second", FormatUtils.formatDurationWords(Duration.ofSeconds(5 * 24 * 60 * 60 + 1)));
        assertEquals("5 days", FormatUtils.formatDurationWords(Duration.ofDays(5)));
        assertEquals("3 hours", FormatUtils.formatDurationWords(Duration.ofHours(3)));
        assertEquals("2 minutes", FormatUtils.formatDurationWords(Duration.ofMinutes(2)));
        assertEquals("2 seconds", FormatUtils.formatDurationWords(Duration.ofSeconds(2)));
        assertEquals("0 second", FormatUtils.formatDurationWords(Duration.ZERO));
        assertEquals("1 minute", FormatUtils.formatDurationWords(Duration.ofMinutes(1)));
        assertEquals("1 hour", FormatUtils.formatDurationWords(Duration.ofHours(1)));
        assertEquals("1 day", FormatUtils.formatDurationWords(Duration.ofDays(1)));
        assertEquals("1,000,000,000 days", FormatUtils.formatDurationWords(Duration.ofDays(1000000000L)));
    }

    @Test
    public void formatLongDuration() {
        final int second = 1;
        final int minute = 60 * second;
        final int hour = 60 * minute;
        final int day = 24 * hour;
        final int month = 31 * day;
        final int year = 12 * month;
        assertEquals("0:01", FormatUtils.formatLongDuration(Instant.now().minusSeconds(second)));
        assertEquals("1:00:00", FormatUtils.formatLongDuration(Instant.now().minusSeconds(hour)));
        assertEquals("1:01:15", FormatUtils.formatLongDuration(Instant.now().minusSeconds(hour + minute + 15 * second)));
        assertEquals("1 day", FormatUtils.formatLongDuration(Instant.now().minusSeconds(day)));
        assertEquals("1 month", FormatUtils.formatLongDuration(Instant.now().minusSeconds(month)));
        assertEquals("1 month, 7 days", FormatUtils.formatLongDuration(Instant.now().minusSeconds(month + 6 * day)));
        assertEquals("2 years, 1 month, 20 days", FormatUtils.formatLongDuration(Instant.now().minusSeconds(2 * year + month + 6 * day)));
    }

    @Test
    public void testFormatDuration() {
        assertEquals("5:04:03", FormatUtils.formatDuration(5 * 60 * 60 * 1000 + 4 * 60 * 1000 + 3 * 1000));
        assertEquals("0:00", FormatUtils.formatDuration(0));
        assertEquals("0:01", FormatUtils.formatDuration(1000));
        assertEquals("1:00", FormatUtils.formatDuration(60_000));
        assertEquals("59:59", FormatUtils.formatDuration(59 * 60 * 1000 + 59 * 1000));
        assertEquals("1:00:00", FormatUtils.formatDuration(60 * 60 * 1000));
        assertThrows(IllegalArgumentException.class, () -> FormatUtils.formatDuration(-1000));
    }

    @Test
    public void testNumber() {
        assertEquals("0", FormatUtils.number(0));
        assertEquals("1", FormatUtils.number(1));
        assertEquals("-1", FormatUtils.number(-1));
        assertEquals("1,000", FormatUtils.number(1000));
        assertEquals("-1,000", FormatUtils.number(-1000));
        assertEquals("1.5", FormatUtils.number(1.5));
        assertEquals("1,500.5", FormatUtils.number(1500.5));
    }

    @Test
    public void testNumberedList() {
        assertEquals("1\n2\n3\n4\n5", FormatUtils.numberedList(5, 5, i -> Integer.toString(i)));
        assertEquals("1\n2", FormatUtils.numberedList(2, 5, i -> Integer.toString(i)));
        assertEquals("1\n2", FormatUtils.numberedList(5, 2, i -> Integer.toString(i)));
        assertEquals("", FormatUtils.numberedList(0, 2, i -> Integer.toString(i)));
        assertEquals("", FormatUtils.numberedList(2, 0, i -> Integer.toString(i)));
        assertEquals("", FormatUtils.numberedList(0, 0, i -> Integer.toString(i)));
        assertEquals("", FormatUtils.numberedList(-1, 0, i -> Integer.toString(i)));
    }

    private enum FakeEnum1 {VALUE_1, VALUE_2, VALUE_3;}

    private enum FakeEnum2 {VALUE_1}

    private enum FakeEnum3 {}

    @Test
    public void testOptions() {
        assertEquals("Options: `value_1`, `value_2`, `value_3`", FormatUtils.options(FakeEnum1.class));
        assertThrows(IllegalArgumentException.class, () -> FormatUtils.options(FakeEnum2.class));
        assertThrows(IllegalArgumentException.class, () -> FormatUtils.options(FakeEnum3.class));
    }

    @Test
    public void testTrackName() {
        assertEquals("author - title (1:00)", FormatUtils.trackName(
                new AudioTrackInfo("title", "author", 60 * 1000,
                        "identifier", false, "uri")));
        assertEquals("title (1:00)", FormatUtils.trackName(
                new AudioTrackInfo("title", "Unknown artist", 60 * 1000,
                        "identifier", false, "uri")));
        assertEquals("author - title (Stream)", FormatUtils.trackName(
                new AudioTrackInfo("title", "author", 60 * 1000,
                        "identifier", true, "uri")));
        assertEquals("title (Stream)", FormatUtils.trackName(
                new AudioTrackInfo("title", "Unknown artist", 60 * 1000,
                        "identifier", true, "uri")));
        assertEquals("author - title (Stream)", FormatUtils.trackName(
                new AudioTrackInfo("   title  ", "  author    ", 60 * 1000,
                        "identifier", true, "uri")));
        assertEquals("author - title (1:00)", FormatUtils.trackName(
                new AudioTrackInfo("author - title", "author", 60 * 1000,
                        "identifier", false, "uri")));
    }

}
