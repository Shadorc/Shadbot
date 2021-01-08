/*
package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.image.deviantart.Image;
import com.shadorc.shadbot.command.CmdTest;
import com.shadorc.shadbot.utils.NetUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ImageCmdTest extends CmdTest<ImageCmd> {

    @Test
    public void testGetPopularImage() {
        final Image result = this.invoke("getPopularImage", "dab");
        assertNotNull(result.getContent());
        assertNotNull(result.getAuthor());
        assertNotNull(result.getCategoryPath());
        assertNotNull(result.getTitle());
        assertNotNull(result.getUrl());
    }

    @Test
    public void testGetPopularImageSpecial() {
        final Image result = this.invoke(
                "getPopularImage", NetUtils.encode("&~#{([-|`_\"'\\^@)]=}°+¨^ $£¤%*µ,?;.:/!§<>+*-/"));
        assertNull(result);
    }

}
*/
