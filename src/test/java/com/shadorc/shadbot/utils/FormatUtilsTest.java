package com.shadorc.shadbot.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FormatUtilsTest {

    @Test
    public void testCoins() {
        assertEquals("100 coins", FormatUtils.coins(100));
        assertEquals("0 coin", FormatUtils.coins(0));
        assertEquals("-1 coin", FormatUtils.coins(-1));
        assertEquals("150,000 coins", FormatUtils.coins(150000));
        assertEquals("-150,000 coins", FormatUtils.coins(-150000));
    }

    @Test
    public void testCustomDate() {
        assertEquals("5 days 3 hours 2 minutes", FormatUtils.customDate(Duration.ofMinutes(5 * 24 * 60 + 3 * 60 + 2)));
        assertEquals("5 days 3 hours", FormatUtils.customDate(Duration.ofHours(5 * 24 + 3)));
        assertEquals("3 hours 2 minutes", FormatUtils.customDate(Duration.ofMinutes(3 * 60 + 2)));
        assertEquals("5 days 2 minutes", FormatUtils.customDate(Duration.ofMinutes(5 * 24 * 60 + 2)));
        assertEquals("5 days", FormatUtils.customDate(Duration.ofDays(5)));
        assertEquals("3 hours", FormatUtils.customDate(Duration.ofHours(3)));
        assertEquals("2 minutes", FormatUtils.customDate(Duration.ofMinutes(2)));
        assertEquals("0 minute", FormatUtils.customDate(Duration.ZERO));
        assertEquals("1 minute", FormatUtils.customDate(Duration.ofMinutes(1)));
        assertEquals("1 hour", FormatUtils.customDate(Duration.ofHours(1)));
        assertEquals("1 day", FormatUtils.customDate(Duration.ofDays(1)));
        assertEquals("1,000,000,000 days", FormatUtils.customDate(Duration.ofDays(1000000000L)));
    }

    @Test
    public void testLongDuration() {
        // TODO
    }

    @Test
    public void testShortDuration() {
        assertEquals("5:04:03", FormatUtils.shortDuration(5 * 60 * 60 * 1000 + 4 * 60 * 1000 + 3 * 1000));
        assertEquals("0:00", FormatUtils.shortDuration(0));
        assertEquals("0:01", FormatUtils.shortDuration(1000));
        assertEquals("1:00", FormatUtils.shortDuration(60_000));
        assertEquals("59:59", FormatUtils.shortDuration(59 * 60 * 1000 + 59 * 1000));
        assertEquals("1:00:00", FormatUtils.shortDuration(60 * 60 * 1000));
        assertThrows(IllegalArgumentException.class, () -> FormatUtils.shortDuration(-1000));
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
                new AudioTrackInfo("title", "author", 60 * 1000, "identifier", false, "uri")));
        assertEquals("title (1:00)", FormatUtils.trackName(
                new AudioTrackInfo("title", "Unknown artist", 60 * 1000, "identifier", false, "uri")));
        assertEquals("author - title (Stream)", FormatUtils.trackName(
                new AudioTrackInfo("title", "author", 60 * 1000, "identifier", true, "uri")));
        assertEquals("title (Stream)", FormatUtils.trackName(
                new AudioTrackInfo("title", "Unknown artist", 60 * 1000, "identifier", true, "uri")));
        assertEquals("author - title (Stream)", FormatUtils.trackName(
                new AudioTrackInfo("   title  ", "  author    ", 60 * 1000, "identifier", true, "uri")));
    }

}
