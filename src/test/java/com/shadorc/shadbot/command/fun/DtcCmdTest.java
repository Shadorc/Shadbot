package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.api.json.dtc.Quote;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DtcCmdTest {

    private static Logger logger;
    private static DtcCmd cmd;

    @BeforeAll
    public static void init() {
        logger = Loggers.getLogger(DtcCmdTest.class);
        cmd = new DtcCmd();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetRandomQuote() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Method method = DtcCmd.class.getDeclaredMethod("getRandomQuote");
        method.setAccessible(true);
        final Quote result = ((Mono<Quote>) method.invoke(cmd)).block();
        logger.debug("testGetRandomQuote: {}", result);
        assertNotNull(result.getContent());
        assertNotNull(result.getId());
    }

}
