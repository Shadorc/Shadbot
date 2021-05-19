package com.locibot.locibot.utils;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RandUtilTest {

    @Test
    public void testRandValueCollection() {
        assertNull(RandUtil.randValue(Collections.emptyList()));
        assertEquals(5, RandUtil.randValue(Set.of(5)));
        assertEquals(5, RandUtil.randValue(List.of(5)));
    }

    @Test
    public void testRandValueArray() {
        assertNull(RandUtil.randValue(new Integer[0]));
        assertEquals(5, RandUtil.randValue(new Integer[]{5}));
    }

}
