package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.command.CmdTest;
import discord4j.common.util.Snowflake;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ChatCmdTest extends CmdTest<ChatCmd> {

    @Test
    public void testGetResponse() {
        final String result = this.invoke("getResponse", Snowflake.of(1234L), "Hello World!");
        assertNotNull(result);
    }

    @Test
    public void testGetResponseSpecial() {
        final String result = this.invoke("getResponse", Snowflake.of(1234L), "&~#{([-|`_\"'\\^@)]=}°+¨^ $£¤%*µ,?;.:/!§<>+-*/");
        assertNotNull(result);
    }

}
