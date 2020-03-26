package com.shadorc.shadbot.utils;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class UtilsTest {

    private enum FakeEnum {
        VALUE_1;
    }

    @Test
    public void testParseEnum() {
        assertEquals(FakeEnum.VALUE_1, Utils.parseEnum(FakeEnum.class, "VALUE_1"));
        assertEquals(FakeEnum.VALUE_1, Utils.parseEnum(FakeEnum.class, "value_1"));
        assertNull(Utils.parseEnum(FakeEnum.class, null));
        assertNull(Utils.parseEnum(FakeEnum.class, "alue_1"));
    }

    @Test
    public void testParseEnumException() {
        assertEquals(FakeEnum.VALUE_1, Utils.parseEnum(FakeEnum.class, "VALUE_1"));
        assertEquals(FakeEnum.VALUE_1, Utils.parseEnum(FakeEnum.class, "value_1"));
        assertThrows(Exception.class, () -> Utils.parseEnum(FakeEnum.class, "alue_1", new Exception("Enum not found.")));
        assertThrows(Exception.class, () -> Utils.parseEnum(FakeEnum.class, null, new Exception("Enum not found.")));
    }

    @Test
    public void testRandValueList() {
        assertNull(Utils.randValue(Collections.emptyList()));
        assertEquals(5, Utils.randValue(List.of(5)));
    }

    @Test
    public void testRandValueArray() {
        assertNull(Utils.randValue(new Integer[0]));
        assertEquals(5, Utils.randValue(List.of(5).toArray(new Integer[0])));
    }

    @Test
    public void testSortMap() {
        final Map<String, Integer> expected = Map.of("1", 1, "2", 2, "3", 3);
        final Map<String, Integer> unsorted = Map.of("2", 2, "1", 1, "3", 3);
        final Map<String, Integer> singleton = Map.of("3", 3);
        final Map<String, Integer> empty = new HashMap<>();
        assertEquals(expected, Utils.sortMap(unsorted, Comparator.comparingInt(Map.Entry::getValue)));
        assertEquals(empty, Utils.sortMap(empty, Comparator.comparingInt(Map.Entry::getValue)));
        assertEquals(singleton, Utils.sortMap(singleton, Comparator.comparingInt(Map.Entry::getValue)));
    }

}
