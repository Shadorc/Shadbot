package com.shadorc.shadbot.utils;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShadbotUtilTest {

    @Test
    public void testCleanLavaplayerErr() {
        final FriendlyException errWithMsg = new FriendlyException("<url src=\"youtube\">Watch on YouTube</url>Error",
                FriendlyException.Severity.COMMON, null);
        assertEquals("Error", ShadbotUtil.cleanLavaplayerErr(errWithMsg));

        final FriendlyException errWithoutMsg = new FriendlyException(null, FriendlyException.Severity.COMMON, null);
        assertEquals("Error not specified.", ShadbotUtil.cleanLavaplayerErr(errWithoutMsg));
    }

}
