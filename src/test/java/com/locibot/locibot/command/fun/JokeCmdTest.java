package com.locibot.locibot.command.fun;

import com.locibot.locibot.command.CmdTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class JokeCmdTest extends CmdTest<JokeCmd> {

    @Test
    public void testGetRandomJoke() {
        final String result = this.invoke("getRandomJoke");
        assertFalse(result.isBlank());
    }
}
