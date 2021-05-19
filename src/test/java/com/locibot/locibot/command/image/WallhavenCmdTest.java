package com.locibot.locibot.command.image;

import com.locibot.locibot.command.CmdTest;
import com.locibot.locibot.api.json.image.wallhaven.Wallpaper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WallhavenCmdTest extends CmdTest<WallhavenCmd> {

    @Test
    public void testGetWallpaperKeyword() {
        final Wallpaper result = this.invoke("getWallpaper", "doom");
        assertFalse(result.path().isBlank());
        assertEquals("sfw", result.purity());
        assertFalse(result.resolution().isBlank());
        assertFalse(result.url().isBlank());
    }

    @Test
    public void testGetWallpaperKeywords() {
        final Wallpaper result = this.invoke("getWallpaper", "doom, video game");
        assertFalse(result.path().isBlank());
        assertEquals("sfw", result.purity());
        assertFalse(result.resolution().isBlank());
        assertFalse(result.url().isBlank());
    }

    @Test
    public void testGetWallpaperRandom() {
        final Wallpaper result = this.invoke("getWallpaper", "");
        assertFalse(result.path().isBlank());
        assertEquals("sfw", result.purity());
        assertFalse(result.resolution().isBlank());
        assertFalse(result.url().isBlank());
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
