package com.shadorc.shadbot.command.fun;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JokeCmdTest {

    private static Logger logger;
    private static JokeCmd cmd;

    @BeforeAll
    public static void init() {
        logger = Loggers.getLogger(JokeCmdTest.class);
        cmd = new JokeCmd();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetRandomJoke() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Method method = JokeCmd.class.getDeclaredMethod("getRandomJoke");
        method.setAccessible(true);
        final String result = ((Mono<String>) method.invoke(cmd)).block();
        logger.debug("testGetRandomJoke: {}", result);
        assertNotNull(result);
    }
}
