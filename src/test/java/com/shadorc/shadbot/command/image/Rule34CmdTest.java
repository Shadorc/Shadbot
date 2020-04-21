package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.image.r34.R34Post;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class Rule34CmdTest {

    private static Logger logger;
    private static Rule34Cmd cmd;
    private static Method method;

    @BeforeAll
    public static void init() throws NoSuchMethodException {
        logger = Loggers.getLogger(Rule34CmdTest.class);
        cmd = new Rule34Cmd();

        method = Rule34Cmd.class.getDeclaredMethod("getR34Post", String.class);
        method.setAccessible(true);
    }

    @Test
    public void testGetR34Post() throws InvocationTargetException, IllegalAccessException {
        final R34Post result = ((Mono<R34Post>) method.invoke(cmd, "dab")).block();
        logger.debug("testGetR34Post: {}", result);
        assertNotNull(result.getSource());
        assertNotNull(result.getFileUrl());
        assertNotNull(result.getTags());
        assertTrue(result.getHeight() > 0);
        assertTrue(result.getWidth() > 0);
    }

    @Test
    public void testGetR34PostSpecial() throws InvocationTargetException, IllegalAccessException {
        final R34Post result = ((Mono<R34Post>) method.invoke(cmd, "&~#{([-|`_\"'\\^@)]=}°+¨^ $£¤%*µ,?;.:/!§<>+-*/")).block();
        logger.debug("testGetR34PostSpecial: {}", result);
        assertNull(result);
    }

}
