package com.locibot.locibot.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LogUtilTest {

    @Test
    public void testGetLogger() {
        assertEquals("shadbot", LogUtil.getLogger().getName());
        assertEquals("shadbot.music", LogUtil.getLogger(LogUtil.Category.MUSIC).getName());
        assertEquals("shadbot.music.LogUtilTest",
                LogUtil.getLogger(LogUtilTest.class, LogUtil.Category.MUSIC).getName());
        assertEquals("shadbot.music.database",
                LogUtil.getLogger(LogUtil.Category.MUSIC, LogUtil.Category.DATABASE).getName());
        assertEquals("shadbot.music.database.LogUtilTest",
                LogUtil.getLogger(LogUtilTest.class, LogUtil.Category.MUSIC, LogUtil.Category.DATABASE).getName());
        assertEquals("shadbot.LogUtilTest", LogUtil.getLogger(LogUtilTest.class).getName());
    }

}
