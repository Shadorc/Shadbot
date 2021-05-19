package com.locibot.locibot.command.fun;

import com.locibot.locibot.command.CmdTest;
import discord4j.common.util.Snowflake;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ChatCmdTest extends CmdTest<ChatCmd> {

    @Test
    public void testGetResponse() {
        final String result = this.invoke("getResponse", Snowflake.of(1234L), "Hello World!");
        assertFalse(result.isBlank());
    }

    @Test
    public void testGetResponseFuzzy() {
        final String result = this.invoke("getResponse", Snowflake.of(1234L), SPECIAL_CHARS);
        assertFalse(result.isBlank());
    }

}
