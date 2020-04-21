package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.api.json.urbandictionary.UrbanDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UrbanCmdTest {

    private static Logger logger;
    private static UrbanCmd cmd;
    private static Method method;

    @BeforeAll
    public static void init() throws NoSuchMethodException {
        logger = Loggers.getLogger(UrbanCmdTest.class);
        cmd = new UrbanCmd();

        method = UrbanCmd.class.getDeclaredMethod("getUrbanDefinition", String.class);
        method.setAccessible(true);
    }

    @Test
    public void testGetUrbanDefinition() throws InvocationTargetException, IllegalAccessException {
        final UrbanDefinition result = ((Mono<UrbanDefinition>) method.invoke(cmd, "dab")).block();
        logger.debug("testGetUrbanDefinition: {}", result);
        assertNotNull(result.getDefinition());
        assertNotNull(result.getExample());
        assertNotNull(result.getPermalink());
        assertNotNull(result.getWord());
    }

    @Test
    public void testGetUrbanDefinitionSpecial() throws InvocationTargetException, IllegalAccessException {
        final UrbanDefinition result =
                ((Mono<UrbanDefinition>) method.invoke(cmd, "&~#{([-|`_\"'\\^@)]=}°+¨^ $£¤%*µ,?;.:/!§<>+-*/")).block();
        logger.debug("testGetUrbanDefinitionSpecial: {}", result);
        assertNotNull(result);
    }

}
