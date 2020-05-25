package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.utils.NetUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    @SuppressWarnings("unchecked")
    public void testGetGifRandom() throws InvocationTargetException, IllegalAccessException {
        final String result = ((Mono<String>) method.invoke(cmd, "")).block();
        logger.debug("testGetGifRandom: {}", result);
        assertNotNull(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetGifSearch() throws InvocationTargetException, IllegalAccessException {
        final String result = ((Mono<String>) method.invoke(cmd, "doom")).block();
        logger.debug("testGetGifSearch: {}", result);
        assertNotNull(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetGifSearchSpecial() {
        assertDoesNotThrow(() -> {
            final String result = ((Mono<String>) method.invoke(cmd,
                    NetUtils.encode("&~#{([-|`_\"'\\^@)]=}°+¨^ $£¤%*µ,?;.:/!§<>+-*/"))).block();
            logger.debug("testGetGifSearchSpecial: {}", result);
        });
    }

}
