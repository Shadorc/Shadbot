package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.image.r34.R34Post;
import com.shadorc.shadbot.command.CmdTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Rule34CmdTest extends CmdTest<Rule34Cmd> {

    @Test
    public void testGetR34Post() {
        final R34Post result = this.invoke("getR34Post", "dab");
        assertNotNull(result.getSource());
        assertNotNull(result.getFileUrl());
        assertNotNull(result.getTags());
        assertTrue(result.getHeight() > 0);
        assertTrue(result.getWidth() > 0);
    }

    @Test
    public void testGetR34PostSpecial() {
        final R34Post result = this.invoke(
                "getR34Post", "&~#{([-|`_\"'\\^@)]=}°+¨^ $£¤%*µ,?;.:/!§<>+-*/");
        assertNull(result);
    }

}
