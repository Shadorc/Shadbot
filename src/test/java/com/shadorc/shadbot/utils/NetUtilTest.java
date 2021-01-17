package com.shadorc.shadbot.utils;

import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class NetUtilTest {

    @Test
    public void testIsUrl() {
        assertTrue(NetUtil.isUrl("http://www.youtube.com"));
        assertTrue(NetUtil.isUrl("https://www.youtube.com"));
        assertTrue(NetUtil.isUrl("https://www.you%20tube.com"));
        assertTrue(NetUtil.isUrl("https://www.(you)%20tube.com"));
        assertFalse(NetUtil.isUrl("https://www.you tube.com"));
        assertFalse(NetUtil.isUrl("youtube"));
        assertFalse(NetUtil.isUrl("youtube.com"));
        assertFalse(NetUtil.isUrl("www.youtube.com"));
    }

    @Test
    public void testCleanWithLinebreaks() {
        assertEquals("Test", NetUtil.cleanWithLinebreaks("<html>Test</html>"));
        assertEquals("", NetUtil.cleanWithLinebreaks("<html></html>"));
        assertEquals("", NetUtil.cleanWithLinebreaks(""));
        assertEquals("Hello\\nWorld", NetUtil.cleanWithLinebreaks("Hello<br>World"));
        assertEquals("\\n\\nHello\\nWorld", NetUtil.cleanWithLinebreaks("<p>Hello</p><br>World"));
        assertEquals("\\n\\n", NetUtil.cleanWithLinebreaks("<br><br>"));
        assertEquals("\\n\\n", NetUtil.cleanWithLinebreaks("<p></p>"));
        assertEquals("\\n\\n", NetUtil.cleanWithLinebreaks("<p>"));
        assertEquals("&lt;1&gt;", NetUtil.cleanWithLinebreaks("<1>"));
        assertNull(NetUtil.cleanWithLinebreaks(null));
    }

    @Test
    public void testEncode() {
        assertNull(NetUtil.encode(null));
        assertEquals("", NetUtil.encode(""));
        final String input = "&é'(-è_çà)=az %µ¨£^$ù*,;:!?./§ertyuiop^1234567890é\\#\"";
        assertEquals(URLEncoder.encode(input, StandardCharsets.UTF_8), NetUtil.encode(input));
    }

}
