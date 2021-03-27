package com.shadorc.shadbot.command.util;

import com.shadorc.shadbot.api.json.urbandictionary.UrbanDefinition;
import com.shadorc.shadbot.command.CmdTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class UrbanCmdTest extends CmdTest<UrbanCmd> {

    @Test
    public void testGetUrbanDefinition() {
        final UrbanDefinition result = this.invoke("getUrbanDefinition", "dab");
        assertFalse(result.getDefinition().isBlank());
        assertFalse(result.getExample().isBlank());
        assertFalse(result.getPermalink().isBlank());
        assertFalse(result.getWord().isBlank());
    }

    @Test
    public void testGetUrbanDefinitionFuzzy() {
        final UrbanDefinition result = this.invoke("getUrbanDefinition", SPECIAL_CHARS);
        assertFalse(result.getDefinition().isBlank());
        assertFalse(result.getExample().isBlank());
        assertFalse(result.getPermalink().isBlank());
        assertFalse(result.getWord().isBlank());
    }

}
