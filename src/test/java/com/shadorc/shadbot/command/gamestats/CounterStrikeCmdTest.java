package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.json.gamestats.steam.player.PlayerSummary;
import com.shadorc.shadbot.command.MissingArgumentException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class CounterStrikeCmdTest {

    private static Logger logger;
    private static CounterStrikeCmd cmd;

    @BeforeAll
    public static void init() {
        logger = Loggers.getLogger(CounterStrikeCmdTest.class);
        cmd = new CounterStrikeCmd();
    }

    @Test
    public void testGetIdentificator() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        final Method method = CounterStrikeCmd.class.getDeclaredMethod("getIdentificator", String.class);
        method.setAccessible(true);

        // Argument cannot be null nor empty
        assertEquals("76561198040113951", method.invoke(cmd, "76561198040113951"));
        assertEquals("shadorc", method.invoke(cmd, "shadorc"));
        assertEquals("shadorc", method.invoke(cmd, "http://steamcommunity.com/id/shadorc"));
        assertEquals("shadorc", method.invoke(cmd, "http://steamcommunity.com/id/shadorc/"));
        assertEquals("shadorc", method.invoke(cmd, "/shadorc"));
        assertEquals("shadorc", method.invoke(cmd, "shadorc/"));
        assertEquals("*", method.invoke(cmd, "*"));
        // MissingArgumentException
        assertThrows(InvocationTargetException.class, () -> method.invoke(cmd, "/"));
    }

    @Test
    public void testGetSteamId() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        final Method method = CounterStrikeCmd.class.getDeclaredMethod("getSteamId", String.class);
        method.setAccessible(true);

        assertEquals("76561198040113951", ((Mono<String>) method.invoke(cmd, "76561198040113951")).block());
        assertEquals("76561198040113951", ((Mono<String>) method.invoke(cmd, "shadorc")).block());
        assertNull(((Mono<String>) method.invoke(cmd, "-76561198040113951")).block());
        assertNull(((Mono<String>) method.invoke(cmd, "thisuser_does_not_exist")).block());
        assertNull(((Mono<String>) method.invoke(cmd, "&~#{([-|`_\"'\\^@)]=}°+¨^$£¤%*µ,?;.:/!§<>+-*/")).block());
        assertNull(((Mono<String>) method.invoke(cmd, "*")).block());
    }

    @Test
    public void testGetPlayerSummary() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        final Method method = CounterStrikeCmd.class.getDeclaredMethod("getPlayerSummary", String.class);
        method.setAccessible(true);

        final PlayerSummary result = ((Mono<PlayerSummary>) method.invoke(cmd, "76561198040113951")).block();
        logger.info("testGetPlayerSummary: {}", result);
        assertEquals("76561198040113951", result.getSteamId());
        assertNotNull(result.getAvatarFull());
        assertNotNull(result.getPersonaName());
        assertNotNull(result.getCommunityVisibilityState());
    }

}
