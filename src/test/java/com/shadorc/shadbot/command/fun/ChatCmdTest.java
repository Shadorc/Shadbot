package com.shadorc.shadbot.command.fun;

import discord4j.rest.util.Snowflake;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ChatCmdTest {

    private static Logger logger;
    private static ChatCmd cmd;

    @BeforeAll
    public static void init() {
        logger = Loggers.getLogger(ChatCmdTest.class);
        cmd = new ChatCmd();
    }

    @Test
    public void testGetResponse() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Method method = ChatCmd.class.getDeclaredMethod("getResponse", Snowflake.class, String.class);
        method.setAccessible(true);
        final String result = ((Mono<String>) method.invoke(cmd, Snowflake.of(1234L), "Hello World!")).block();
        logger.info("testGetResponse: {}", result);
        assertNotNull(result);
    }

}
