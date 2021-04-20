package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.json.gamestats.steam.player.PlayerSummary;
import com.shadorc.shadbot.command.CmdTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CounterStrikeCmdTest extends CmdTest<CounterStrikeCmd> {

    @Test
    public void testGetIdentificator() {
        final String method = "getIdentificator";
        assertEquals("76561198040113951", this.invoke(method, "76561198040113951"));
        assertEquals("shadorc", this.invoke(method, "shadorc"));
        assertEquals("shadorc", this.invoke(method, "http://steamcommunity.com/id/shadorc"));
        assertEquals("shadorc", this.invoke(method, "http://steamcommunity.com/id/shadorc/"));
        assertEquals("shadorc", this.invoke(method, "/shadorc"));
        assertEquals("shadorc", this.invoke(method, "shadorc/"));
        assertEquals("*", this.invoke(method, "*"));
        assertThrows(IllegalArgumentException.class, () -> this.invoke(method, "/"));
    }

    @Test
    public void testGetSteamId() {
        final String method = "getSteamId";
        assertEquals("76561198040113951", this.invoke(method, "76561198040113951"));
        assertEquals("76561198040113951", this.invoke(method, "shadorc"));
        assertNull(this.invoke(method, "-76561198040113951"));
        assertNull(this.invoke(method, "thisuser_does_not_exist"));
        assertNull(this.invoke(method, SPECIAL_CHARS));
        assertNull(this.invoke(method, "*"));
    }

    @Test
    public void testGetPlayerSummary() {
        final String method = "getPlayerSummary";
        final PlayerSummary result = this.invoke(method, "76561198040113951");
        assertEquals("76561198040113951", result.steamId());
        assertFalse(result.avatarFull().isBlank());
        assertFalse(result.personaName().isBlank());
        assertNotNull(result.getCommunityVisibilityState());
    }

}
