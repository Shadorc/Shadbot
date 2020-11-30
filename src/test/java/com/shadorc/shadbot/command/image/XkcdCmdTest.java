package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.xkcd.XkcdResponse;
import com.shadorc.shadbot.command.CmdTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XkcdCmdTest extends CmdTest<XkcdCmd> {

    @Test
    public void testGetRandomXkcd() {
        final XkcdResponse result = this.invoke("getRandomXkcd");
        assertNotNull(result.getImg());
        assertTrue(result.getNum() > 0);
        assertNotNull(result.getTitle());
    }

}
