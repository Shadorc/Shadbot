package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.api.html.musixmatch.Musixmatch;
import com.shadorc.shadbot.command.CmdTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LyricsCmdTest extends CmdTest<LyricsCmd> {

    @Test
    public void testGetMusixmatch() {
        final Musixmatch result = this.invoke("getMusixmatch", "21 guns");
        assertFalse(result.getLyrics().isBlank());
        assertFalse(result.getArtist().isBlank());
        assertFalse(result.getImageUrl().isBlank());
        assertFalse(result.getTitle().isBlank());
        assertFalse(result.getUrl().isBlank());
    }

    @Test
    public void testGetMusixmatchSpecial() {
        final Musixmatch result = this.invoke("getMusixmatch", SPECIAL_CHARS);
        assertNull(result);
    }

}
