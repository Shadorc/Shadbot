package com.shadorc.shadbot.command.fun;

import discord4j.common.util.Snowflake;
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
    private static Method method;

    @BeforeAll
    public static void init() throws NoSuchMethodException {
        logger = Loggers.getLogger(ChatCmdTest.class);
        cmd = new ChatCmd();

        method = ChatCmd.class.getDeclaredMethod("getResponse", Snowflake.class, String.class);
        method.setAccessible(true);
    }

    @Test
    public void testGetResponse() throws InvocationTargetException, IllegalAccessException {
        final String result = ((Mono<String>) method.invoke(cmd, Snowflake.of(1234L), "Hello World!")).block();
        logger.debug("testGetResponse: {}", result);
        assertNotNull(result);
    }

    @Test
    public void testGetResponseSpecial() throws InvocationTargetException, IllegalAccessException {
        final String result =
                ((Mono<String>) method.invoke(cmd, Snowflake.of(1234L), "&~#{([-|`_\"'\\^@)]=}°+¨^ $£¤%*µ,?;.:/!§<>+-*/")).block();
        logger.debug("testGetResponseSpecial: {}", result);
        assertNotNull(result);
    }

}
