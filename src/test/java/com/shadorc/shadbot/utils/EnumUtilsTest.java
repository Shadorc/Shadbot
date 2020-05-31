package com.shadorc.shadbot.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EnumUtilsTest {

    private enum FakeEnum {
        VALUE_1;
    }

    @Test
    public void testParseEnum() {
        assertEquals(FakeEnum.VALUE_1, EnumUtils.parseEnum(FakeEnum.class, "VALUE_1"));
        assertEquals(FakeEnum.VALUE_1, EnumUtils.parseEnum(FakeEnum.class, "value_1"));
        assertNull(EnumUtils.parseEnum(FakeEnum.class, null));
        assertNull(EnumUtils.parseEnum(FakeEnum.class, "alue_1"));
    }

    @Test
    public void testParseEnumException() {
        assertEquals(FakeEnum.VALUE_1, EnumUtils.parseEnum(FakeEnum.class, "VALUE_1"));
        assertEquals(FakeEnum.VALUE_1, EnumUtils.parseEnum(FakeEnum.class, "value_1"));
        assertThrows(Exception.class, () -> EnumUtils.parseEnum(FakeEnum.class, "alue_1", new Exception("Enum not found.")));
        assertThrows(Exception.class, () -> EnumUtils.parseEnum(FakeEnum.class, null, new Exception("Enum not found.")));
    }

}
