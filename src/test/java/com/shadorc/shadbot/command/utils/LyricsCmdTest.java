package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.api.html.musixmatch.Musixmatch;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LyricsCmdTest {

    private static Logger logger;
    private static LyricsCmd cmd;
    private static Method method;

    @BeforeAll
    public static void init() throws NoSuchMethodException {
        logger = Loggers.getLogger(LyricsCmdTest.class);
        cmd = new LyricsCmd();

        method = LyricsCmd.class.getDeclaredMethod("getMusixmatch", String.class);
        method.setAccessible(true);
    }

    @Test
    public void testGetMusixmatch() throws InvocationTargetException, IllegalAccessException {
        final Musixmatch result = ((Mono<Musixmatch>) method.invoke(cmd, "21 guns")).block();
        logger.info("testGetMusixmatch: {\n\turl={},\n\ttitle={},\n\tartist={},\n\timageUrl={},\n\tlyrics={}\n}",
                result.getUrl(), result.getTitle(), result.getArtist(), result.getImageUrl(), result.getLyrics());
        assertNotNull(result.getLyrics());
        assertNotNull(result.getArtist());
        assertNotNull(result.getImageUrl());
        assertNotNull(result.getTitle());
        assertNotNull(result.getUrl());
    }

    @Test
    public void testGetMusixmatchNotFound() throws InvocationTargetException, IllegalAccessException {
        final Musixmatch result = ((Mono<Musixmatch>) method.invoke(cmd, "azertyuiopqsdfghjklmwxcvbn")).block();
        logger.info("testGetMusixmatchNotFound: {}", result);
        assertNull(result);
    }

}
