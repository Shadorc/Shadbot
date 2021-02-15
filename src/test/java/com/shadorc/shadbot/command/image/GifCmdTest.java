package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.command.CmdTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GifCmdTest extends CmdTest<GifCmd> {

    @Test
    public void testGetGifRandom() {
        final String result = this.invoke("getGifUrl", "");
        assertFalse(result.isBlank());
    }

    @Test
    public void testGetGifSearch() {
        final String result = this.invoke("getGifUrl", "doom");
        assertFalse(result.isBlank());
    }

    @Test
    public void testGetGifSearchSpecialChars() {
        final String result = this.invoke("getGifUrl", SPECIAL_CHARS);
        assertNull(result);
    }

}
