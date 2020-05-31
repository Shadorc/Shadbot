package com.shadorc.shadbot.utils;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RandUtilsTest {

    @Test
    public void testRandValueList() {
        assertNull(RandUtils.randValue(Collections.emptyList()));
        assertEquals(5, RandUtils.randValue(List.of(5)));
    }

    @Test
    public void testRandValueArray() {
        assertNull(RandUtils.randValue(new Integer[0]));
        assertEquals(5, RandUtils.randValue(List.of(5).toArray(new Integer[0])));
    }

}
