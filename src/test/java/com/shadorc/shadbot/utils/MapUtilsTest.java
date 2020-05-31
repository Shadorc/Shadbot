package com.shadorc.shadbot.utils;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapUtilsTest {

    @Test
    public void testInverse() {
        assertEquals(Map.of("1", 1), MapUtils.inverse(Map.of(1, "1")));
        assertEquals(Map.of("1", 1, "2", 2, "3", 3),
                MapUtils.inverse(Map.of(1, "1", 2, "2", 3, "3")));
        assertEquals(Collections.emptyMap(), MapUtils.inverse(Collections.emptyMap()));
    }

    @Test
    public void testSort() {
        final Map<String, Integer> expected = Map.of("1", 1, "2", 2, "3", 3);
        final Map<String, Integer> unsorted = Map.of("2", 2, "1", 1, "3", 3);
        final Map<String, Integer> singleton = Map.of("3", 3);
        final Map<String, Integer> empty = new HashMap<>();
        assertEquals(expected, MapUtils.sort(unsorted, Comparator.comparingInt(Map.Entry::getValue)));
        assertEquals(empty, MapUtils.sort(empty, Comparator.comparingInt(Map.Entry::getValue)));
        assertEquals(singleton, MapUtils.sort(singleton, Comparator.comparingInt(Map.Entry::getValue)));
    }

}
