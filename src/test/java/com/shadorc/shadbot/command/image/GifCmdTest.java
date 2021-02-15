package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.command.CmdTest;
import com.shadorc.shadbot.utils.NetUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GifCmdTest extends CmdTest<GifCmd> {

    @Test
    public void testGetGifRandom() {
        final String result = this.invoke("getGifUrl", "");
        assertNotNull(result);
    }

    @Test
    public void testGetGifSearch() {
        final String result = this.invoke("getGifUrl", "doom");
        assertNotNull(result);
    }

    @Test
    public void testGetGifSearchSpecial() {
        assertDoesNotThrow(() -> {
            this.invoke("getGifUrl", NetUtil.encode("&~#{([-|`_\"'\\^@)]=}°+¨^ $£¤%*µ,?;.:/!§<>+*-/"));
        });
    }

}
