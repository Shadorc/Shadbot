package com.shadorc.shadbot.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EnumUtilsTest {

    private enum FakeEnum {
        VALUE_1;

        @Override
        public String toString() {
            return "not value 1";
        }
    }

    @Test
    public void testParseEnum() {
        assertEquals(FakeEnum.VALUE_1, EnumUtils.parseEnum(FakeEnum.class, "VALUE_1"));
        assertEquals(FakeEnum.VALUE_1, EnumUtils.parseEnum(FakeEnum.class, "value_1"));
        assertEquals(FakeEnum.VALUE_1, EnumUtils.parseEnum(FakeEnum.class, "vAluE_1"));
        assertNull(EnumUtils.parseEnum(FakeEnum.class, null));
        assertNull(EnumUtils.parseEnum(FakeEnum.class, "alue_1"));
    }

    @Test
    public void testParseEnumException() {
        final RuntimeException err = new RuntimeException("Enum not found.");
        assertEquals(FakeEnum.VALUE_1, EnumUtils.parseEnum(FakeEnum.class, "VALUE_1", err));
        assertEquals(FakeEnum.VALUE_1, EnumUtils.parseEnum(FakeEnum.class, "value_1", err));
        assertEquals(FakeEnum.VALUE_1, EnumUtils.parseEnum(FakeEnum.class, "vAluE_1", err));
        assertThrows(err.getClass(), () -> EnumUtils.parseEnum(FakeEnum.class, null, err));
        assertThrows(err.getClass(), () -> EnumUtils.parseEnum(FakeEnum.class, "alue_1", err));
    }

}
