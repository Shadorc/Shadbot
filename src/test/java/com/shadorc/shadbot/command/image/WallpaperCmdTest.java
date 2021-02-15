package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.image.wallhaven.Wallpaper;
import com.shadorc.shadbot.command.CmdTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WallpaperCmdTest extends CmdTest<WallpaperCmd> {

    @Test
    public void TestGetWallpaper_Keyword() {
        final Wallpaper result = this.invoke("getWallpaper", "doom");
        assertNotNull(result.getPath());
        assertEquals("sfw", result.getPurity());
        assertNotNull(result.getResolution());
        assertNotNull(result.getUrl());
    }

    @Test
    public void TestGetWallpaper_Keywords() {
        final Wallpaper result = this.invoke("getWallpaper", "doom, video game");
        assertNotNull(result.getPath());
        assertEquals("sfw", result.getPurity());
        assertNotNull(result.getResolution());
        assertNotNull(result.getUrl());
    }

    @Test
    public void TestGetWallpaper_Random() {
        final Wallpaper result = this.invoke("getWallpaper", "");
        assertNotNull(result.getPath());
        assertEquals("sfw", result.getPurity());
        assertNotNull(result.getResolution());
        assertNotNull(result.getUrl());
    }

    @Test
    public void TestGetWallpaper_NotFound() {
        final Wallpaper result = this.invoke("getWallpaper", "this is not a keyword");
        assertNull(result);
    }

}
