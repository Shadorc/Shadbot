package com.shadorc.shadbot.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LogUtilTest {

    @Test
    public void testGetLogger() {
        assertEquals("shadbot", LogUtil.getLogger().getName());
        assertEquals("shadbot.music", LogUtil.getLogger(LogUtil.Category.MUSIC).getName());
        assertEquals("shadbot.music.LogUtilsTest",
                LogUtil.getLogger(LogUtilTest.class, LogUtil.Category.MUSIC).getName());
        assertEquals("shadbot.music.database",
                LogUtil.getLogger(LogUtil.Category.MUSIC, LogUtil.Category.DATABASE).getName());
        assertEquals("shadbot.music.database.LogUtilsTest",
                LogUtil.getLogger(LogUtilTest.class, LogUtil.Category.MUSIC, LogUtil.Category.DATABASE).getName());
        assertEquals("shadbot.LogUtilsTest", LogUtil.getLogger(LogUtilTest.class).getName());
    }

}
