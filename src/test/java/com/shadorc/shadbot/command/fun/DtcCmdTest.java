package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.api.json.dtc.Quote;
import com.shadorc.shadbot.command.CmdTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class DtcCmdTest extends CmdTest<DtcCmd> {

    @Test
    public void testGetRandomQuote() {
        final Quote result = this.invoke("getRandomQuote");
        assertFalse(result.getContent().isBlank());
        assertFalse(result.getId().isBlank());
    }

}
