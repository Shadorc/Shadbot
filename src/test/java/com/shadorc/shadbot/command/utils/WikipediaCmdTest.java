package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.api.json.wikipedia.WikipediaPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class WikipediaCmdTest {

    private static Logger logger;
    private static WikiCmd cmd;
    private static Method method;

    @BeforeAll
    public static void init() throws NoSuchMethodException {
        logger = Loggers.getLogger(WikipediaCmdTest.class);
        cmd = new WikiCmd();

        method = WikiCmd.class.getDeclaredMethod("getWikipediaPage", String.class);
        method.setAccessible(true);
    }

    @Test
    public void testGetWikipediaPage() throws InvocationTargetException, IllegalAccessException {
        final WikipediaPage result = ((Mono<WikipediaPage>) method.invoke(cmd, "21 guns")).block();
        logger.debug("testGetWikipediaPage: {}", result);
        assertNotNull(result.getExtract());
        assertNotNull(result.getTitle());
    }

    @Test
    public void testGetWikipediaPageSpecial() throws InvocationTargetException, IllegalAccessException {
        final WikipediaPage result = ((Mono<WikipediaPage>) method.invoke(cmd, "&~#{([-|`_\"'\\^@)]=}°+¨^ $£¤%*µ,?;.:/!§<>+-*/")).block();
        logger.debug("testGetWikipediaPageSpecial: {}", result);
        assertNull(result);
    }

}
