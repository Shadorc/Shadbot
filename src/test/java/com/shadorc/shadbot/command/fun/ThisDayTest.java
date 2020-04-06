package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.api.html.thisday.ThisDay;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ThisDayTest {

    private static Logger logger;
    private static ThisDayCmd cmd;

    @BeforeAll
    public static void init() {
        logger = Loggers.getLogger(ThisDayTest.class);
        cmd = new ThisDayCmd();
    }

    @Test
    public void testGetThisDay() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Method method = ThisDayCmd.class.getDeclaredMethod("getThisDay");
        method.setAccessible(true);
        final ThisDay result = ((Mono<ThisDay>) method.invoke(cmd)).block();
        logger.debug("testGetThisDay: date={}, events={}", result.getDate(), result.getEvents());
        assertNotNull(result.getDate());
        assertNotNull(result.getEvents());
    }

}
