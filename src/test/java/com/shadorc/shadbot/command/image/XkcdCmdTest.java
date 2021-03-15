package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.xkcd.XkcdResponse;
import com.shadorc.shadbot.command.CmdTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XkcdCmdTest extends CmdTest<XkcdCmd> {

    @Test
    public void testGetRandomXkcd() {
        final XkcdResponse result = this.invoke("getRandomXkcd");
        assertFalse(result.getImg().isBlank());
        assertTrue(result.getNum() > 0);
        assertFalse(result.getTitle().isBlank());
    }

    @Test
    public void testGetLatestXkcd() {
        final XkcdResponse result = this.invoke("getLatestXkcd");
        assertFalse(result.getImg().isBlank());
        assertTrue(result.getNum() > 0);
        assertFalse(result.getTitle().isBlank());
    }

}
