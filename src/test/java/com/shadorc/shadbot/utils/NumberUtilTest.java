package com.shadorc.shadbot.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NumberUtilTest {

    @Test
    public void testToIntOrNull() {
        assertEquals(Integer.valueOf(14), NumberUtil.toIntOrNull("14"));
        assertEquals(Integer.valueOf(14), NumberUtil.toIntOrNull("  14   "));
        assertEquals(Integer.valueOf(0), NumberUtil.toIntOrNull("0"));
        assertEquals(Integer.valueOf(-14), NumberUtil.toIntOrNull("-14"));
        assertEquals(Integer.valueOf(-14), NumberUtil.toIntOrNull("  -14   "));
        assertNull(NumberUtil.toIntOrNull("fourteen"));
        assertNull(NumberUtil.toIntOrNull(null));
        assertNull(NumberUtil.toIntOrNull("9223372036854775807"));
    }

    @Test
    public void testToPositiveIntOrNull() {
        assertEquals(Integer.valueOf(14), NumberUtil.toPositiveIntOrNull("14"));
        assertEquals(Integer.valueOf(14), NumberUtil.toPositiveIntOrNull("  14   "));
        assertNull(NumberUtil.toPositiveIntOrNull("0"));
        assertNull(NumberUtil.toPositiveIntOrNull("-14"));
        assertNull(NumberUtil.toPositiveIntOrNull("  -14   "));
        assertNull(NumberUtil.toPositiveIntOrNull("fourteen"));
        assertNull(NumberUtil.toPositiveIntOrNull(null));
        assertNull(NumberUtil.toPositiveIntOrNull("9223372036854775807"));
    }

    @Test
    public void testToIntBetweenOrNull() {
        assertEquals(14, NumberUtil.toIntBetweenOrNull(" 14   ", 10, 20));
        assertEquals(14, NumberUtil.toIntBetweenOrNull("14", 10, 20));
        assertEquals(10, NumberUtil.toIntBetweenOrNull("10", 10, 20));
        assertEquals(20, NumberUtil.toIntBetweenOrNull("20", 10, 20));
        assertNull(NumberUtil.toIntBetweenOrNull("9", 10, 20));
        assertNull(NumberUtil.toIntBetweenOrNull("21", 10, 20));
        assertNull(NumberUtil.toIntBetweenOrNull("-12", 10, 20));
        assertNull(NumberUtil.toIntBetweenOrNull(null, 10, 20));
        assertNull(NumberUtil.toIntBetweenOrNull("9223372036854775807", 10, 20));
    }

    @Test
    public void testToLongOrNull() {
        assertEquals(Long.valueOf(14), NumberUtil.toLongOrNull("14"));
        assertEquals(Long.valueOf(14), NumberUtil.toLongOrNull("  14   "));
        assertEquals(Long.valueOf(0), NumberUtil.toLongOrNull("0"));
        assertEquals(Long.valueOf(-14), NumberUtil.toLongOrNull("-14"));
        assertEquals(Long.valueOf(-14), NumberUtil.toLongOrNull("  -14   "));
        assertEquals(Long.MAX_VALUE, NumberUtil.toLongOrNull("9223372036854775807"));
        assertNull(NumberUtil.toLongOrNull("19223372036854775807"));
        assertNull(NumberUtil.toLongOrNull("fourteen"));
        assertNull(NumberUtil.toLongOrNull(null));
    }

    @Test
    public void testToPositiveLongOrNull() {
        assertEquals(Long.valueOf(14), NumberUtil.toPositiveLongOrNull("14"));
        assertEquals(Long.valueOf(14), NumberUtil.toPositiveLongOrNull("  14   "));
        assertNull(NumberUtil.toPositiveLongOrNull("0"));
        assertNull(NumberUtil.toPositiveLongOrNull("-14"));
        assertNull(NumberUtil.toPositiveLongOrNull("  -14   "));
        assertEquals(Long.MAX_VALUE, NumberUtil.toPositiveLongOrNull("9223372036854775807"));
        assertNull(NumberUtil.toPositiveLongOrNull("19223372036854775807"));
        assertNull(NumberUtil.toPositiveLongOrNull("fourteen"));
        assertNull(NumberUtil.toPositiveLongOrNull(null));
    }

    @Test
    public void testIsPositiveLong() {
        assertTrue(NumberUtil.isPositiveLong("14"));
        assertTrue(NumberUtil.isPositiveLong("  14   "));
        assertFalse(NumberUtil.isPositiveLong("0"));
        assertFalse(NumberUtil.isPositiveLong("-14"));
        assertFalse(NumberUtil.isPositiveLong("  -14   "));
        assertTrue(NumberUtil.isPositiveLong("9223372036854775807"));
        assertFalse(NumberUtil.isPositiveLong("19223372036854775807"));
        assertFalse(NumberUtil.isPositiveLong("fourteen"));
        assertFalse(NumberUtil.isPositiveLong(null));
    }

    @Test
    public void testTruncateBetween() {
        assertEquals(14, NumberUtil.truncateBetween(14, 10, 20));
        assertEquals(10, NumberUtil.truncateBetween(4, 10, 20));
        assertEquals(20, NumberUtil.truncateBetween(24, 10, 20));
        assertEquals(10, NumberUtil.truncateBetween(-12, 10, 20));
        assertEquals(10, NumberUtil.truncateBetween(10, 10, 20));
        assertEquals(20, NumberUtil.truncateBetween(20, 10, 20));
    }

    @Test
    public void testIsBetween() {
        assertTrue(NumberUtil.isBetween(14, 10, 20));
        assertFalse(NumberUtil.isBetween(4, 10, 20));
        assertFalse(NumberUtil.isBetween(24, 10, 20));
        assertFalse(NumberUtil.isBetween(-12, 10, 20));
        assertTrue(NumberUtil.isBetween(10, 10, 20));
        assertTrue(NumberUtil.isBetween(20, 10, 20));
    }

}
