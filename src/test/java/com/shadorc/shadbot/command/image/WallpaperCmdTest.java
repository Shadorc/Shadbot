package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.image.r34.R34Post;
import com.shadorc.shadbot.api.json.image.wallhaven.Wallpaper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class WallpaperCmdTest {

    private static Logger logger;
    private static WallpaperCmd cmd;
    private static Method method;

    @BeforeAll
    public static void init() throws NoSuchMethodException {
        logger = Loggers.getLogger(WallpaperCmdTest.class);
        cmd = new WallpaperCmd();

        method = WallpaperCmd.class.getDeclaredMethod("getWallpaper", String.class);
        method.setAccessible(true);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void TestGetWallpaper_Keyword() throws InvocationTargetException, IllegalAccessException {
        final Wallpaper result = ((Mono<Wallpaper>) method.invoke(cmd, "doom")).block();
        logger.debug("TestGetWallpaper_Keyword: {}", result);
        assertNotNull(result.getPath());
        assertEquals("sfw", result.getPurity());
        assertNotNull(result.getResolution());
        assertNotNull(result.getUrl());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void TestGetWallpaper_Keywords() throws InvocationTargetException, IllegalAccessException {
        final Wallpaper result = ((Mono<Wallpaper>) method.invoke(cmd, "doom, video game")).block();
        logger.debug("TestGetWallpaper_Keywords: {}", result);
        assertNotNull(result.getPath());
        assertEquals("sfw", result.getPurity());
        assertNotNull(result.getResolution());
        assertNotNull(result.getUrl());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void TestGetWallpaper_Random() throws InvocationTargetException, IllegalAccessException {
        final Wallpaper result = ((Mono<Wallpaper>) method.invoke(cmd, "")).block();
        logger.info("TestGetWallpaper_Random: {}", result);
        assertNotNull(result.getPath());
        assertEquals("sfw", result.getPurity());
        assertNotNull(result.getResolution());
        assertNotNull(result.getUrl());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void TestGetWallpaper_NotFound() throws InvocationTargetException, IllegalAccessException {
        final Wallpaper result = ((Mono<Wallpaper>) method.invoke(cmd, "this is not a keyword")).block();
        logger.debug("TestGetWallpaper_Random: {}", result);
        assertNull(result);
    }

}
