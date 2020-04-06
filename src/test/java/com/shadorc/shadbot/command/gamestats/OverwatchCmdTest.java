package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.json.gamestats.overwatch.OverwatchProfile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class OverwatchCmdTest {

    private static Logger logger;
    private static OverwatchCmd cmd;

    @BeforeAll
    public static void init() {
        logger = Loggers.getLogger(OverwatchCmdTest.class);
        cmd = new OverwatchCmd();
    }

    @Test
    public void testGetResponse() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Method method = OverwatchCmd.class.getDeclaredMethod("getOverwatchProfile", String.class, OverwatchCmd.Platform.class);
        method.setAccessible(true);

        final OverwatchProfile result = ((Mono<OverwatchProfile>) method.invoke(cmd, "Shadorc#2503", OverwatchCmd.Platform.PC)).block();
        logger.debug("testGetResponse: {}", result);
        assertEquals(OverwatchCmd.Platform.PC, result.getPlatform());
        assertNull(result.getProfile().getMessage().orElse(null));
        assertFalse(result.getProfile().isPrivate());
        assertNotNull(result.getProfile().getUsername());
        assertNotNull(result.getProfile().getGames().getQuickplayWon());
        assertNotNull(result.getProfile().getLevel());
        assertNotNull(result.getProfile().getPortrait());
        assertNotNull(result.getProfile().getQuickplayPlaytime());
        assertNotNull(result.getQuickplay().getEliminationsPerLife());
        assertNotNull(result.getQuickplay().getPlayed());
    }

}
