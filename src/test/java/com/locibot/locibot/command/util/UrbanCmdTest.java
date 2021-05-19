package com.locibot.locibot.command.util;

import com.locibot.locibot.command.CmdTest;
import com.locibot.locibot.api.json.urbandictionary.UrbanDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class UrbanCmdTest extends CmdTest<UrbanCmd> {

    @Test
    public void testGetUrbanDefinition() {
        final UrbanDefinition result = this.invoke("getUrbanDefinition", "dab");
        assertFalse(result.getDefinition().isBlank());
        assertFalse(result.getExample().isBlank());
        assertFalse(result.permalink().isBlank());
        assertFalse(result.word().isBlank());
    }

    @Test
    public void testGetUrbanDefinitionFuzzy() {
        final UrbanDefinition result = this.invoke("getUrbanDefinition", SPECIAL_CHARS);
        assertFalse(result.getDefinition().isBlank());
        assertFalse(result.getExample().isBlank());
        assertFalse(result.permalink().isBlank());
        assertFalse(result.word().isBlank());
    }

}
