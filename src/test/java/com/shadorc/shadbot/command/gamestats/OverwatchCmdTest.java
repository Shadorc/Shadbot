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
        assertFalse(result.getProfile().getUsername().isBlank());
        assertNotNull(result.getProfile().getGames().getQuickplayWon());
        assertFalse(result.getProfile().getLevel().isBlank());
        assertFalse(result.getProfile().getPortrait().isBlank());
        assertFalse(result.getProfile().getQuickplayPlaytime().isBlank());
        assertFalse(result.getQuickplay().getEliminationsPerLife().isBlank());
        assertFalse(result.getQuickplay().getPlayed().isBlank());
    }

    @Test
    public void testGetResponseWrongBattletag() {
        assertThrows(CommandException.class, () -> this.invoke(
                "getOverwatchProfile", "&~#{([-|`_\"'\\^@)]=}°+¨^$£¤%* µ,?;.:/!§<>+*-/", OverwatchCmd.Platform.PC));
    }

}
