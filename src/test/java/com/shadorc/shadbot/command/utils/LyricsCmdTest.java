package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.api.html.musixmatch.Musixmatch;
import com.shadorc.shadbot.command.CmdTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LyricsCmdTest extends CmdTest<LyricsCmd> {

    @Test
    public void testGetMusixmatch() {
        final Musixmatch result = this.invoke("getMusixmatch", "21 guns");
        assertNotNull(result.getLyrics());
        assertNotNull(result.getArtist());
        assertNotNull(result.getImageUrl());
        assertNotNull(result.getTitle());
        assertNotNull(result.getUrl());
    }

    @Test
    public void testGetMusixmatchSpecial() {
        final Musixmatch result = this.invoke(
                "getMusixmatch", "&~#{([-|`_\"'\\^@)]=}°+¨^ $£¤%*µ,?;.:/!§<>+*-/");
        assertNull(result);
    }

}
