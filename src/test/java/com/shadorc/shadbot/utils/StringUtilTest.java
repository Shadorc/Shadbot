package com.shadorc.shadbot.utils;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StringUtilTest {

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
    public void testNormalizeSpace() {
        assertEquals("hello", StringUtil.normalizeSpace("   hello      "));
        assertEquals("he llo", StringUtil.normalizeSpace("   he   llo      "));
        assertEquals("", StringUtil.normalizeSpace(""));
        assertNull(StringUtil.normalizeSpace(null));
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
