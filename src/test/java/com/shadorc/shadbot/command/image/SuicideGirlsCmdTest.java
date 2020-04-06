package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.html.suicidegirl.SuicideGirl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SuicideGirlsCmdTest {

    private static Logger logger;
    private static SuicideGirlsCmd cmd;

    @BeforeAll
    public static void init() {
        logger = Loggers.getLogger(SuicideGirlsCmdTest.class);
        cmd = new SuicideGirlsCmd();
    }

    @Test
    public void testGetRandomSuicideGirl() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Method method = SuicideGirlsCmd.class.getDeclaredMethod("getRandomSuicideGirl");
        method.setAccessible(true);
        final SuicideGirl result = ((Mono<SuicideGirl>) method.invoke(cmd)).block();
        logger.debug("testGetRandomSuicideGirl: name={}, url={}, imageUrl={}", result.getName(), result.getUrl(), result.getImageUrl());
        assertNotNull(result.getName());
        assertNotNull(result.getImageUrl());
        assertNotNull(result.getUrl());
    }

}
