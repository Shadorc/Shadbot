package com.shadorc.shadbot.utils;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StringUtilTest {

    @Test
    public void testRemoveLastLetter() {
        assertEquals("", StringUtil.removeLastLetter(""));
        assertNull(StringUtil.removeLastLetter(null));
        assertEquals("a", StringUtil.removeLastLetter("a"));
        assertEquals("a", StringUtil.removeLastLetter("ab"));
        assertEquals("ab", StringUtil.removeLastLetter("abc"));
    }

    @Test
    public void testCountMatches() {
        assertEquals(0, StringUtil.countMatches("foo", 'b'));
        assertEquals(1, StringUtil.countMatches("foo", 'f'));
        assertEquals(2, StringUtil.countMatches("foo", 'o'));
        assertEquals(2, StringUtil.countMatches(" hello world", ' '));
        assertEquals(2, StringUtil.countMatches("hello \"hi\"", '"'));
    }

    @Test
    public void testSubstringsBetween() {
        assertEquals(List.of("o wo"), StringUtil.substringsBetween("hello world", "ll", "rl"));
        assertEquals(Collections.emptyList(), StringUtil.substringsBetween(null, "ll", "rl"));
        assertEquals(Collections.emptyList(), StringUtil.substringsBetween("", "ll", "rl"));
        assertEquals(Collections.emptyList(), StringUtil.substringsBetween("null", "ll", "rl"));
        assertEquals(Collections.emptyList(), StringUtil.substringsBetween("he\"llo", "\"", "\""));
        assertEquals(List.of("hello"), StringUtil.substringsBetween("\"hello\"", "\"", "\""));
    }

    @Test
    public void testAbbreviate() {
        assertNull(StringUtil.abbreviate(null, 3));
        assertThrows(IllegalArgumentException.class, () -> StringUtil.abbreviate("hi", 1));
        assertEquals("hi", StringUtil.abbreviate("hi", 3));
        assertEquals("hello...", StringUtil.abbreviate("hello world", 8));
    }

    @Test
    public void testCapitalize() {
        assertEquals("", StringUtil.capitalize(""));
        assertEquals("F", StringUtil.capitalize("f"));
        assertEquals("Fo", StringUtil.capitalize("fo"));
        assertEquals("Foo", StringUtil.capitalize("foo"));
        assertEquals("Foo", StringUtil.capitalize("Foo"));
        assertEquals("Foo", StringUtil.capitalize("foO"));
        assertNull(StringUtil.capitalize(null));
    }

    @Test
    public void testGetQuotedElements() {
        assertEquals(List.of("hello"), StringUtil.getQuotedElements("\"hello\""));
        assertEquals(List.of("hello", "hi"), StringUtil.getQuotedElements("\"hello\" \"hi\""));
        assertEquals(List.of("hello", "hi"), StringUtil.getQuotedElements("\"hello\" foo \"hi\""));
        assertEquals(List.of("hello foo "), StringUtil.getQuotedElements("\"hello foo \"hi\""));
        assertEquals(List.of("hello", "foo"), StringUtil.getQuotedElements("\"hello\" \"\" \"foo\""));
        assertEquals(Collections.emptyList(), StringUtil.getQuotedElements("hello \"\"foo hi"));
        assertEquals(Collections.emptyList(), StringUtil.getQuotedElements(null));
        assertEquals(Collections.emptyList(), StringUtil.getQuotedElements("    "));
    }

    @Test
    public void testNormalizeSpace() {
        assertEquals("hello", StringUtil.normalizeSpace("   hello      "));
        assertEquals("he llo", StringUtil.normalizeSpace("   he   llo      "));
        assertEquals("", StringUtil.normalizeSpace(""));
        assertNull(StringUtil.normalizeSpace(null));
    }

    @Test
    public void testPluralOf() {
        assertEquals("-2 coins", StringUtil.pluralOf(-2, "coin"));
        assertEquals("-1 coin", StringUtil.pluralOf(-1, "coin"));
        assertEquals("0 coin", StringUtil.pluralOf(0, "coin"));
        assertEquals("1 coin", StringUtil.pluralOf(1, "coin"));
        assertEquals("2 coins", StringUtil.pluralOf(2, "coin"));
        assertEquals("1,000 coins", StringUtil.pluralOf(1000, "coin"));
        assertNull(StringUtil.pluralOf(1, "   "));
        assertNull(StringUtil.pluralOf(2, "   "));
        assertNull(StringUtil.pluralOf(2, null));
    }

    @Test
    public void testRemove() {
        assertEquals("bar", StringUtil.remove("foo bar", List.of("foo ")));
        assertEquals(" ", StringUtil.remove("foo bar", "foo", "bar"));
        assertEquals("foo bar", StringUtil.remove("foo bar"));
        assertEquals("foo bar", StringUtil.remove("foo bar", "*"));
        assertEquals("foo bar", StringUtil.remove("foo bar", ""));
        assertNull(StringUtil.remove(null));
    }

    @Test
    public void testSplit() {
        assertEquals(List.of("foo", "bar", "hi"), StringUtil.split("foo bar hi"));
        assertEquals(List.of("foo", "bar", "hi"), StringUtil.split("foo bar hi "));
        assertEquals(List.of("foo"), StringUtil.split("foo"));
        assertEquals(Collections.emptyList(), StringUtil.split(""));
        assertEquals(Collections.emptyList(), StringUtil.split(null));
    }

    @Test
    public void testSplitLimit() {
        assertEquals(List.of("foo", "bar", "hi"), StringUtil.split("foo bar hi", -1));
        assertEquals(List.of("foo", "bar", "hi"), StringUtil.split("foo bar hi", 0));
        assertEquals(List.of("foo bar hi"), StringUtil.split("foo bar hi", 1));
        assertEquals(List.of("foo", "bar hi"), StringUtil.split("foo bar hi", 2));
        assertEquals(List.of("foo", "bar", "hi"), StringUtil.split("foo bar hi", 3));
        assertEquals(List.of("foo", "bar", "hi"), StringUtil.split("foo bar hi", 4));
        assertEquals(List.of("foo", "bar hi"), StringUtil.split("foo bar hi ", 2));
        assertEquals(List.of("foo"), StringUtil.split("foo", 2));
        assertEquals(Collections.emptyList(), StringUtil.split("", 2));
        assertEquals(Collections.emptyList(), StringUtil.split(null, 2));
    }

    @Test
    public void testSplitLimitAndDelimiter() {
        assertEquals(List.of("foo", "bar", "hi"), StringUtil.split("foo-bar-hi", -1, "-"));
        assertEquals(List.of("foo", "bar", "hi"), StringUtil.split("foo-bar-hi", 0, "-"));
        assertEquals(List.of("foo-bar-hi"), StringUtil.split("foo-bar-hi", 1, "-"));
        assertEquals(List.of("foo", "bar-hi"), StringUtil.split("foo-bar-hi", 2, "-"));
        assertEquals(List.of("foo", "bar", "hi"), StringUtil.split("foo-bar-hi", 3, "-"));
        assertEquals(List.of("foo", "bar", "hi"), StringUtil.split("foo-bar-hi", 4, "-"));
        assertEquals(List.of("foo", "bar-hi-"), StringUtil.split("foo-bar-hi-", 2, "-"));
        assertEquals(List.of("foo"), StringUtil.split("foo", 2, "-"));
        assertEquals(Collections.emptyList(), StringUtil.split("", 2, "-"));
        assertEquals(Collections.emptyList(), StringUtil.split(null, 2, "-"));
    }

    @Test
    public void testSplitDelimiter() {
        assertEquals(List.of("foo", "bar", "hi"), StringUtil.split("foo-bar-hi", "-"));
        assertEquals(List.of("foo", "bar", "hi"), StringUtil.split("foo-bar-hi-", "-"));
        assertEquals(List.of("foo"), StringUtil.split("foo", "-"));
        assertEquals(Collections.emptyList(), StringUtil.split("", "-"));
        assertEquals(Collections.emptyList(), StringUtil.split(null, "-"));
    }

}
