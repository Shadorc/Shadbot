package com.locibot.locibot.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.locibot.locibot.data.Config;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class FormatUtilsTest {

    private enum TestEnum1 {
        TEST_ONE, test_Two, testThree
    }

    private enum TestEnum2 {
        Test;


        @Override
        public String toString() {
            return "%s_toString".formatted(this.name());
        }
    }

    @Test
    public void testCapitalizeEnum() {
        assertNull(FormatUtil.capitalizeEnum(null));
        assertEquals("Test one", FormatUtil.capitalizeEnum(TestEnum1.TEST_ONE));
        assertEquals("Test two", FormatUtil.capitalizeEnum(TestEnum1.test_Two));
        assertEquals("Testthree", FormatUtil.capitalizeEnum(TestEnum1.testThree));
        assertEquals("Test_toString", FormatUtil.capitalizeEnum(TestEnum2.Test));
    }

    @Test
    public void testCreateColumns() {
        final Function<Integer, List<String>> createList = size -> IntStream.range(0, size)
                .boxed()
                .map(Object::toString)
                .toList();

        assertEquals(1, FormatUtil.createColumns(createList.apply(12), 20).size());
        assertEquals(1, FormatUtil.createColumns(createList.apply(12), 12).size());
        assertEquals(2, FormatUtil.createColumns(createList.apply(12), 10).size());
        assertEquals(12, FormatUtil.createColumns(createList.apply(12), 1).size());
        assertEquals(1, FormatUtil.createColumns(createList.apply(1), 1).size());
        assertEquals(0, FormatUtil.createColumns(createList.apply(0), 1).size());
        assertThrows(IllegalArgumentException.class, () -> FormatUtil.createColumns(createList.apply(12), 0));
    }

    @Test
    public void testFormatDurationWords() {
        final Locale locale = Config.DEFAULT_LOCALE;
        assertEquals("5 days 3 hours 2 minutes 1 second", FormatUtil.formatDurationWords(locale,
                Duration.ofSeconds(5 * 24 * 60 * 60 + 3 * 60 * 60 + 2 * 60 + 1)));
        assertEquals("5 days 3 hours 2 minutes", FormatUtil.formatDurationWords(locale,
                Duration.ofMinutes(5 * 24 * 60 + 3 * 60 + 2)));
        assertEquals("5 days 3 hours", FormatUtil.formatDurationWords(locale,
                Duration.ofHours(5 * 24 + 3)));
        assertEquals("3 hours 2 minutes", FormatUtil.formatDurationWords(locale,
                Duration.ofMinutes(3 * 60 + 2)));
        assertEquals("5 days 2 minutes", FormatUtil.formatDurationWords(locale,
                Duration.ofMinutes(5 * 24 * 60 + 2)));
        assertEquals("5 days 1 second", FormatUtil.formatDurationWords(locale,
                Duration.ofSeconds(5 * 24 * 60 * 60 + 1)));
        assertEquals("5 days", FormatUtil.formatDurationWords(locale,
                Duration.ofDays(5)));
        assertEquals("3 hours", FormatUtil.formatDurationWords(locale,
                Duration.ofHours(3)));
        assertEquals("2 minutes", FormatUtil.formatDurationWords(locale,
                Duration.ofMinutes(2)));
        assertEquals("2 seconds", FormatUtil.formatDurationWords(locale,
                Duration.ofSeconds(2)));
        assertEquals("0 second", FormatUtil.formatDurationWords(locale,
                Duration.ZERO));
        assertEquals("1 minute", FormatUtil.formatDurationWords(locale,
                Duration.ofMinutes(1)));
        assertEquals("1 hour", FormatUtil.formatDurationWords(locale,
                Duration.ofHours(1)));
        assertEquals("1 day", FormatUtil.formatDurationWords(locale,
                Duration.ofDays(1)));
        assertEquals("1,000,000,000 days", FormatUtil.formatDurationWords(locale,
                Duration.ofDays(1000000000L)));
    }

    @Test
    public void formatLongDuration() {
        final Locale locale = Config.DEFAULT_LOCALE;
        final LocalDateTime localDateTime = LocalDateTime.now(ZoneId.systemDefault());
        assertEquals("0:01", FormatUtil.formatLongDuration(locale, localDateTime.minusSeconds(1)));
        assertEquals("1:00:00", FormatUtil.formatLongDuration(locale, localDateTime.minusHours(1)));
        assertEquals("1:01:15",
                FormatUtil.formatLongDuration(locale, localDateTime.minusHours(1).minusMinutes(1).minusSeconds(15)));
        assertEquals("1 day", FormatUtil.formatLongDuration(locale, localDateTime.minusDays(1)));
        /* TODO Bug: These tests are inconsistent
        assertEquals("1 month", FormatUtil.formatLongDuration(locale, localDateTime.minusMonths(1)));
        assertEquals("1 year", FormatUtil.formatLongDuration(locale, localDateTime.minusYears(1)));
        assertEquals("3 months, 4 days",
                FormatUtil.formatLongDuration(locale, localDateTime.minusMonths(3).minusDays(4)));
        assertEquals("2 years, 4 days",
                FormatUtil.formatLongDuration(locale, localDateTime.minusYears(2).minusDays(4)));
        assertEquals("2 years, 3 months, 4 days",
                FormatUtil.formatLongDuration(locale, localDateTime.minusYears(2).minusMonths(3).minusDays(4)));
         */
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
    public void testNumberedList() {
        assertEquals("1\n2\n3\n4\n5", FormatUtil.numberedList(5, 5, i -> Integer.toString(i)));
        assertEquals("1\n2", FormatUtil.numberedList(2, 5, i -> Integer.toString(i)));
        assertEquals("1\n2", FormatUtil.numberedList(5, 2, i -> Integer.toString(i)));
        assertEquals("", FormatUtil.numberedList(0, 2, i -> Integer.toString(i)));
        assertEquals("", FormatUtil.numberedList(2, 0, i -> Integer.toString(i)));
        assertEquals("", FormatUtil.numberedList(0, 0, i -> Integer.toString(i)));
        assertEquals("", FormatUtil.numberedList(-1, 0, i -> Integer.toString(i)));
    }

    @Test
    public void testTrackName() {
        final Locale locale = Config.DEFAULT_LOCALE;
        assertEquals("author - title (1:00)", FormatUtil.trackName(locale,
                new AudioTrackInfo("title", "author", 60 * 1000,
                        "identifier", false, "uri")));
        assertEquals("title (1:00)", FormatUtil.trackName(locale,
                new AudioTrackInfo("title", "Unknown artist", 60 * 1000,
                        "identifier", false, "uri")));
        assertEquals("author - title (Stream)", FormatUtil.trackName(locale,
                new AudioTrackInfo("title", "author", 60 * 1000,
                        "identifier", true, "uri")));
        assertEquals("title (Stream)", FormatUtil.trackName(locale,
                new AudioTrackInfo("title", "Unknown artist", 60 * 1000,
                        "identifier", true, "uri")));
        assertEquals("author - title (Stream)", FormatUtil.trackName(locale,
                new AudioTrackInfo("   title  ", "  author    ", 60 * 1000,
                        "identifier", true, "uri")));
        assertEquals("author - title (1:00)", FormatUtil.trackName(locale,
                new AudioTrackInfo("author - title", "author", 60 * 1000,
                        "identifier", false, "uri")));
        assertEquals("Unknown video name (1:00)", FormatUtil.trackName(locale,
                new AudioTrackInfo(null, "author", 60 * 1000,
                        "identifier", false, "uri")));
    }

}
