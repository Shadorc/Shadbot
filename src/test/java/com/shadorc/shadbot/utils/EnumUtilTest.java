package com.shadorc.shadbot.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class EnumUtilTest {

    private enum FakeEnum {
        VALUE_1;

        @Override
        public String toString() {
            return "toString() should not be used for parsing";
        }
    }

    @Test
    public void testParseEnum() {
        assertEquals(FakeEnum.VALUE_1, EnumUtil.parseEnum(FakeEnum.class, "VALUE_1"));
        assertEquals(FakeEnum.VALUE_1, EnumUtil.parseEnum(FakeEnum.class, "value_1"));
        assertEquals(FakeEnum.VALUE_1, EnumUtil.parseEnum(FakeEnum.class, "vAluE_1"));
        assertNull(EnumUtil.parseEnum(FakeEnum.class, null));
        assertNull(EnumUtil.parseEnum(FakeEnum.class, ""));
        assertNull(EnumUtil.parseEnum(FakeEnum.class, "alue_1"));
        assertNull(EnumUtil.parseEnum(FakeEnum.class, "value"));
    }

}
