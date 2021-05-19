package com.locibot.locibot.utils;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ListUtilTest {

    @Test
    public void testPartition() {
        assertEquals(List.of(List.of(1), List.of(2), List.of(3)), ListUtil.partition(List.of(1, 2, 3), 1));
        assertEquals(List.of(List.of(1, 2, 3)), ListUtil.partition(List.of(1, 2, 3), 3));
        assertEquals(List.of(List.of(1, 2), List.of(3)), ListUtil.partition(List.of(1, 2, 3), 2));
        assertEquals(List.of(List.of(1)), ListUtil.partition(List.of(1), 2));
        assertThrows(IllegalArgumentException.class, () -> ListUtil.partition(List.of(1, 2, 3), 0));
        assertEquals(Collections.emptyList(), ListUtil.partition(Collections.emptyList(), 2));
    }

}
