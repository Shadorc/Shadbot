package com.shadorc.shadbot.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

public class FormatUtilsTest {

    @Test
    public void testCoins() {
        assertEquals("100 coins", FormatUtil.coins(100));
        assertEquals("0 coin", FormatUtil.coins(0));
        assertEquals("-1 coin", FormatUtil.coins(-1));
        assertEquals("150,000 coins", FormatUtil.coins(150000));
        assertEquals("-150,000 coins", FormatUtil.coins(-150000));
    }

    private enum TestEnum {
        TEST_ONE, test_Two, testThree
    }

    @Test
    public void testCapitalizeEnum() {
        assertEquals("Test one", FormatUtil.capitalizeEnum(TestEnum.TEST_ONE));
        assertEquals("Test two", FormatUtil.capitalizeEnum(TestEnum.test_Two));
        assertEquals("Testthree", FormatUtil.capitalizeEnum(TestEnum.testThree));
        assertNull(FormatUtil.capitalizeEnum(null));
    }

    @Test
    public void testFormatDurationWords() {
        assertEquals("5 days 3 hours 2 minutes 1 second", FormatUtil.formatDurationWords(
                Duration.ofSeconds(5 * 24 * 60 * 60 + 3 * 60 * 60 + 2 * 60 + 1)));
        assertEquals("5 days 3 hours 2 minutes", FormatUtil.formatDurationWords(Duration.ofMinutes(5 * 24 * 60 + 3 * 60 + 2)));
        assertEquals("5 days 3 hours", FormatUtil.formatDurationWords(Duration.ofHours(5 * 24 + 3)));
        assertEquals("3 hours 2 minutes", FormatUtil.formatDurationWords(Duration.ofMinutes(3 * 60 + 2)));
        assertEquals("5 days 2 minutes", FormatUtil.formatDurationWords(Duration.ofMinutes(5 * 24 * 60 + 2)));
        assertEquals("5 days 1 second", FormatUtil.formatDurationWords(Duration.ofSeconds(5 * 24 * 60 * 60 + 1)));
        assertEquals("5 days", FormatUtil.formatDurationWords(Duration.ofDays(5)));
        assertEquals("3 hours", FormatUtil.formatDurationWords(Duration.ofHours(3)));
        assertEquals("2 minutes", FormatUtil.formatDurationWords(Duration.ofMinutes(2)));
        assertEquals("2 seconds", FormatUtil.formatDurationWords(Duration.ofSeconds(2)));
        assertEquals("0 second", FormatUtil.formatDurationWords(Duration.ZERO));
        assertEquals("1 minute", FormatUtil.formatDurationWords(Duration.ofMinutes(1)));
        assertEquals("1 hour", FormatUtil.formatDurationWords(Duration.ofHours(1)));
        assertEquals("1 day", FormatUtil.formatDurationWords(Duration.ofDays(1)));
        assertEquals("1,000,000,000 days", FormatUtil.formatDurationWords(Duration.ofDays(1000000000L)));
    }

    @Test
    public void formatLongDuration() {
        final LocalDateTime localDateTime = LocalDateTime.now(ZoneId.systemDefault());
        assertEquals("0:01", FormatUtil.formatLongDuration(localDateTime.minusSeconds(1)));
        assertEquals("1:00:00", FormatUtil.formatLongDuration(localDateTime.minusHours(1)));
        assertEquals("1:01:15",
                FormatUtil.formatLongDuration(localDateTime.minusHours(1).minusMinutes(1).minusSeconds(15)));
        assertEquals("1 day", FormatUtil.formatLongDuration(localDateTime.minusDays(1)));
    }

    @Test
    public void testFormatDuration() {
        assertEquals("5:04:03", FormatUtil.formatDuration(5 * 60 * 60 * 1000 + 4 * 60 * 1000 + 3 * 1000));
        assertEquals("0:00", FormatUtil.formatDuration(0));
        assertEquals("0:01", FormatUtil.formatDuration(1000));
        assertEquals("1:00", FormatUtil.formatDuration(60_000));
        assertEquals("59:59", FormatUtil.formatDuration(59 * 60 * 1000 + 59 * 1000));
        assertEquals("1:00:00", FormatUtil.formatDuration(60 * 60 * 1000));
        assertThrows(IllegalArgumentException.class, () -> FormatUtil.formatDuration(-1000));
    }

    @Test
    public void testNumber() {
        assertEquals("0", FormatUtil.number(0));
        assertEquals("1", FormatUtil.number(1));
        assertEquals("-1", FormatUtil.number(-1));
        assertEquals("1,000", FormatUtil.number(1000));
        assertEquals("-1,000", FormatUtil.number(-1000));
        assertEquals("1.5", FormatUtil.number(1.5));
        assertEquals("1,500.5", FormatUtil.number(1500.5));
    }

    @Test
    public void testNumberedList() {
        assertEquals("1\n2\n3\n4\n5", FormatUtil.numberedList(5, 5, i -> Integer.toString(i)));
        assertEquals("1\n2", FormatUtil.numberedList(2, 5, i -> Integer.toString(i)));
        assertEquals("1\n2", FormatUtil.numberedList(5, 2, i -> Integer.toString(i)));
        assertEquals("", FormatUtil.numberedList(0, 2, i -> Integer.toString(i)));
        assertEquals("", FormatUtil.numberedList(2, 0, i -> Integer.toString(i)));
        assertEquals("", FormatUtil.numberedList(0, 0, i -> Integer.toString(i)));
        assertEquals("", FormatUtil.numberedList(-1, 0, i -> Integer.toString(i)));
    }

    private enum FakeEnum1 {VALUE_1, VALUE_2, VALUE_3}

    private enum FakeEnum2 {VALUE_1}

    private enum FakeEnum3 {}

    @Test
    public void testOptions() {
        assertEquals("Options: `value_1`, `value_2`, `value_3`", FormatUtil.options(FakeEnum1.class));
        assertThrows(IllegalArgumentException.class, () -> FormatUtil.options(FakeEnum2.class));
        assertThrows(IllegalArgumentException.class, () -> FormatUtil.options(FakeEnum3.class));
    }

    @Test
    public void testTrackName() {
        assertEquals("author - title (1:00)", FormatUtil.trackName(
                new AudioTrackInfo("title", "author", 60 * 1000,
                        "identifier", false, "uri")));
        assertEquals("title (1:00)", FormatUtil.trackName(
                new AudioTrackInfo("title", "Unknown artist", 60 * 1000,
                        "identifier", false, "uri")));
        assertEquals("author - title (Stream)", FormatUtil.trackName(
                new AudioTrackInfo("title", "author", 60 * 1000,
                        "identifier", true, "uri")));
        assertEquals("title (Stream)", FormatUtil.trackName(
                new AudioTrackInfo("title", "Unknown artist", 60 * 1000,
                        "identifier", true, "uri")));
        assertEquals("author - title (Stream)", FormatUtil.trackName(
                new AudioTrackInfo("   title  ", "  author    ", 60 * 1000,
                        "identifier", true, "uri")));
        assertEquals("author - title (1:00)", FormatUtil.trackName(
                new AudioTrackInfo("author - title", "author", 60 * 1000,
                        "identifier", false, "uri")));
        assertEquals("Unknown video name (1:00)", FormatUtil.trackName(
                new AudioTrackInfo(null, "author", 60 * 1000,
                        "identifier", false, "uri")));
    }

}
