package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.json.gamestats.overwatch.OverwatchProfile;
import com.shadorc.shadbot.command.CmdTest;
import com.shadorc.shadbot.command.CommandException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OverwatchCmdTest extends CmdTest<OverwatchCmd> {

    @Test
    public void testGetResponse() {
        final OverwatchProfile result = this.invoke(
                "getOverwatchProfile", "Shadorc#2503", OverwatchCmd.Platform.PC);
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

    @Test
    public void testGetResponseWrongBattletag() {
        assertThrows(CommandException.class, () -> this.invoke(
                "getOverwatchProfile", "&~#{([-|`_\"'\\^@)]=}°+¨^$£¤%* µ,?;.:/!§<>+-*/", OverwatchCmd.Platform.PC));
    }

}
