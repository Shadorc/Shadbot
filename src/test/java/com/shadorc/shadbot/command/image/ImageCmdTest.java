package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.image.deviantart.Content;
import com.shadorc.shadbot.api.json.image.deviantart.Image;
import com.shadorc.shadbot.command.CmdTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ImageCmdTest extends CmdTest<ImageCmd> {

    @Test
    public void testGetPopularImage() {
        final Image result = this.invoke("getPopularImage", "dab");
        assertFalse(result.getContent().map(Content::getSource).orElseThrow().isBlank());
        assertFalse(result.getAuthor().getUsername().isBlank());
        assertFalse(result.getCategoryPath().isBlank());
        assertFalse(result.getTitle().isBlank());
        assertFalse(result.getUrl().isBlank());
    }

    @Test
    public void testGetPopularImageSpecialChars() {
        final Image result = this.invoke("getPopularImage", SPECIAL_CHARS);
        assertNull(result);
    }

}
