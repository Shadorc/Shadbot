package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.api.json.urbandictionary.UrbanDefinition;
import com.shadorc.shadbot.command.CmdTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UrbanCmdTest extends CmdTest<UrbanCmd> {

    @Test
    public void testGetUrbanDefinition() {
        final UrbanDefinition result = this.invoke("getUrbanDefinition", "dab");
        assertNotNull(result.getDefinition());
        assertNotNull(result.getExample());
        assertNotNull(result.getPermalink());
        assertNotNull(result.getWord());
    }

    @Test
    public void testGetUrbanDefinitionSpecial() {
        final UrbanDefinition result = this.invoke(
                "getUrbanDefinition", "&~#{([-|`_\"'\\^@)]=}°+¨^ $£¤%*µ,?;.:/!§<>+-*/");
        assertNotNull(result);
    }

}
