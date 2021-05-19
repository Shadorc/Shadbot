package com.locibot.locibot.command.image;

import com.locibot.locibot.command.CmdTest;
import com.locibot.locibot.api.json.xkcd.XkcdResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XkcdCmdTest extends CmdTest<XkcdCmd> {

    @Test
    public void testGetRandomXkcd() {
        final XkcdResponse result = this.invoke("getRandomXkcd");
        assertFalse(result.img().isBlank());
        assertTrue(result.num() > 0);
        assertFalse(result.title().isBlank());
    }

    @Test
    public void testGetLatestXkcd() {
        final XkcdResponse result = this.invoke("getLatestXkcd");
        assertFalse(result.img().isBlank());
        assertTrue(result.num() > 0);
        assertFalse(result.title().isBlank());
    }

}
