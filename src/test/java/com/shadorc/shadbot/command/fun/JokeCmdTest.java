package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.command.CmdTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class JokeCmdTest extends CmdTest<JokeCmd> {

    @Test
    public void testGetRandomJoke() {
        final String result = this.invoke("getRandomJoke");
        assertFalse(result.isBlank());
    }
}
