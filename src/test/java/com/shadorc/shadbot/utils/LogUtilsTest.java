package com.shadorc.shadbot.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LogUtilsTest {

    @Test
    public void testGetLogger() {
        assertEquals("shadbot", LogUtils.getLogger().getName());
        assertEquals("shadbot.music", LogUtils.getLogger(LogUtils.Category.MUSIC).getName());
        assertEquals("shadbot.music.LogUtilsTest",
                LogUtils.getLogger(LogUtilsTest.class, LogUtils.Category.MUSIC).getName());
        assertEquals("shadbot.music.database",
                LogUtils.getLogger(LogUtils.Category.MUSIC, LogUtils.Category.DATABASE).getName());
        assertEquals("shadbot.music.database.LogUtilsTest",
                LogUtils.getLogger(LogUtilsTest.class, LogUtils.Category.MUSIC, LogUtils.Category.DATABASE).getName());
        assertEquals("shadbot.LogUtilsTest", LogUtils.getLogger(LogUtilsTest.class).getName());
    }

}
