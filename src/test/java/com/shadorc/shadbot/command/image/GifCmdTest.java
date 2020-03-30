package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.utils.NetUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GifCmdTest {

    private static Logger logger;
    private static GifCmd cmd;
    private static Method method;

    @BeforeAll
    public static void init() throws NoSuchMethodException {
        logger = Loggers.getLogger(GifCmdTest.class);
        cmd = new GifCmd();

        method = GifCmd.class.getDeclaredMethod("getGif", String.class);
        method.setAccessible(true);
    }

    @Test
    public void testGetGifRandom() throws InvocationTargetException, IllegalAccessException {
        final String result = ((Mono<String>) method.invoke(cmd, "")).block();
        logger.info("testGetGifRandom: {}", result);
        assertNotNull(result);
    }

    @Test
    public void testGetGifSearch() throws InvocationTargetException, IllegalAccessException {
        final String result = ((Mono<String>) method.invoke(cmd, "doom")).block();
        logger.info("testGetGifSearch: {}", result);
        assertNotNull(result);
    }

    @Test
    public void testGetGifSearchSpecial() throws InvocationTargetException, IllegalAccessException {
        final String result = ((Mono<String>) method.invoke(cmd,
                NetUtils.encode("&~#{([-|`_\"'\\^@)]=}°+¨^$£¤%*µ,?;.:/!§<>+-*/"))).block();
        logger.info("testGetGifSearchSpecial: {}", result);
        assertNull(result);
    }

}
