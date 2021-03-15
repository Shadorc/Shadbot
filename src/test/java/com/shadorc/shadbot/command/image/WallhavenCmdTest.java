package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.image.wallhaven.Wallpaper;
import com.shadorc.shadbot.command.CmdTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WallhavenCmdTest extends CmdTest<WallhavenCmd> {

    @Test
    public void testGetWallpaperKeyword() {
        final Wallpaper result = this.invoke("getWallpaper", "doom");
        assertFalse(result.getPath().isBlank());
        assertEquals("sfw", result.getPurity());
        assertFalse(result.getResolution().isBlank());
        assertFalse(result.getUrl().isBlank());
    }

    @Test
    public void testGetWallpaperKeywords() {
        final Wallpaper result = this.invoke("getWallpaper", "doom, video game");
        assertFalse(result.getPath().isBlank());
        assertEquals("sfw", result.getPurity());
        assertFalse(result.getResolution().isBlank());
        assertFalse(result.getUrl().isBlank());
    }

    @Test
    public void testGetWallpaperRandom() {
        final Wallpaper result = this.invoke("getWallpaper", "");
        assertFalse(result.getPath().isBlank());
        assertEquals("sfw", result.getPurity());
        assertFalse(result.getResolution().isBlank());
        assertFalse(result.getUrl().isBlank());
    }

    @Test
    public void testGetWallpaperNotFound() {
        final Wallpaper result = this.invoke("getWallpaper", "this is not a keyword");
        assertNull(result);
    }

    @Test
    public void testGetWallpaperFuzzy() {
        final Wallpaper result = this.invoke("getWallpaper", SPECIAL_CHARS);
        assertNull(result);
    }

}
