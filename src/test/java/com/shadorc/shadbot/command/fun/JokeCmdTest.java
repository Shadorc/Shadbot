package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.utils.LogUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JokeCmdTest {

    private static Logger logger;
    private static JokeCmd cmd;

    @BeforeAll
    public static void init() {
        logger = LogUtils.getLogger(JokeCmdTest.class, LogUtils.Category.TEST);
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
