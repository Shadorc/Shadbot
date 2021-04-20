package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.json.gamestats.overwatch.OverwatchProfile;
import com.shadorc.shadbot.command.CmdTest;
import com.shadorc.shadbot.command.CommandException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OverwatchCmdTest extends CmdTest<OverwatchCmd> {

    @Test
    public void testGetResponse() {
        final String method = "getOverwatchProfile";
        final OverwatchProfile result = this.invoke(method, "Shadorc#2503", OverwatchCmd.Platform.PC);
        assertEquals(OverwatchCmd.Platform.PC, result.platform());
        assertNull(result.profile().getMessage().orElse(null));
        assertFalse(result.profile().isPrivate());
        assertFalse(result.profile().getUsername().isBlank());
        assertNotNull(result.profile().getGames().getQuickplayWon());
        assertFalse(result.profile().getLevel().isBlank());
        assertFalse(result.profile().getPortrait().isBlank());
        assertFalse(result.profile().getQuickplayPlaytime().isBlank());
        assertFalse(result.getQuickplay().getEliminationsPerLife().isBlank());
        assertFalse(result.getQuickplay().getPlayed().isBlank());
    }

    @Test
    public void testGetResponseFuzzy() {
        final String method = "getOverwatchProfile";
        assertThrows(CommandException.class, () -> this.invoke(method, SPECIAL_CHARS, OverwatchCmd.Platform.PC));
    }

}
