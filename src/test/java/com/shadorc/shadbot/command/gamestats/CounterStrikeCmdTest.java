/*
package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.json.gamestats.steam.player.PlayerSummary;
import com.shadorc.shadbot.command.CmdTest;
import com.shadorc.shadbot.command.CommandException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CounterStrikeCmdTest extends CmdTest<CounterStrikeCmd> {

    @Test
    public void testGetIdentificator() {
        assertEquals("76561198040113951", this.invoke("getIdentificator", "76561198040113951"));
        assertEquals("shadorc", this.invoke("getIdentificator", "shadorc"));
        assertEquals("shadorc", this.invoke("getIdentificator", "http://steamcommunity.com/id/shadorc"));
        assertEquals("shadorc", this.invoke("getIdentificator", "http://steamcommunity.com/id/shadorc/"));
        assertEquals("shadorc", this.invoke("getIdentificator", "/shadorc"));
        assertEquals("shadorc", this.invoke("getIdentificator", "shadorc/"));
        assertEquals("*", this.invoke("getIdentificator", "*"));
        assertThrows(CommandException.class, () -> this.invoke("getIdentificator", "/"));
    }

    @Test
    public void testGetSteamId() {
        assertEquals("76561198040113951", this.invoke("getSteamId", "76561198040113951"));
        assertEquals("76561198040113951", this.invoke("getSteamId", "shadorc"));
        assertNull(this.invoke("getSteamId", "-76561198040113951"));
        assertNull(this.invoke("getSteamId", "thisuser_does_not_exist"));
        assertNull(this.invoke("getSteamId", "&~#{([-|`_\"'\\^@)]=}°+¨^ $£¤%*µ,?;.:/!§<>+*-/"));
        assertNull(this.invoke("getSteamId", "*"));
    }

    @Test
    public void testGetPlayerSummary() {
        final PlayerSummary result = this.invoke("getPlayerSummary", "76561198040113951");
        assertEquals("76561198040113951", result.getSteamId());
        assertNotNull(result.getAvatarFull());
        assertNotNull(result.getPersonaName());
        assertNotNull(result.getCommunityVisibilityState());
    }

}
*/
