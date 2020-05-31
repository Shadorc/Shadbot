package com.shadorc.shadbot.utils;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ListUtilsTest {

    @Test
    public void testPartition() {
        assertEquals(List.of(List.of(1), List.of(2), List.of(3)), ListUtils.partition(List.of(1, 2, 3), 1));
        assertEquals(List.of(List.of(1, 2, 3)), ListUtils.partition(List.of(1, 2, 3), 3));
        assertEquals(List.of(List.of(1, 2), List.of(3)), ListUtils.partition(List.of(1, 2, 3), 2));
        assertEquals(List.of(List.of(1)), ListUtils.partition(List.of(1), 2));
        assertThrows(IllegalArgumentException.class, () -> ListUtils.partition(List.of(1, 2, 3), 0));
        assertEquals(Collections.emptyList(), ListUtils.partition(Collections.emptyList(), 2));
    }

}
