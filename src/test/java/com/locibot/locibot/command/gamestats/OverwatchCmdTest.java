package com.locibot.locibot.command.gamestats;

import com.locibot.locibot.command.CmdTest;
import com.locibot.locibot.api.json.gamestats.overwatch.OverwatchProfile;
import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.data.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OverwatchCmdTest extends CmdTest<OverwatchCmd> {

    @Test
    public void testGetResponse() {
        final String method = "getOverwatchProfile";
        final OverwatchProfile result = this.invoke(method,
                Config.DEFAULT_LOCALE, "Shadorc#2503", OverwatchCmd.Platform.PC);
        assertEquals(OverwatchCmd.Platform.PC, result.platform());
        assertNull(result.profile().message().orElse(null));
        assertFalse(result.profile().isPrivate());
        assertFalse(result.profile().username().isBlank());
        assertNotEquals(0, result.profile().games().getQuickplayWon());
        assertNotEquals(0, result.profile().level());
        assertFalse(result.profile().portrait().isBlank());
        assertFalse(result.profile().getQuickplayPlaytime().isBlank());
        assertFalse(result.getQuickplay().getEliminationsPerLife().isBlank());
        assertFalse(result.getQuickplay().getPlayed().isBlank());
    }

    @Test
    public void testGetResponseFuzzy() {
        final String method = "getOverwatchProfile";
        assertThrows(CommandException.class, () -> this.invoke(method,
                Config.DEFAULT_LOCALE, SPECIAL_CHARS, OverwatchCmd.Platform.PC));
    }

}
