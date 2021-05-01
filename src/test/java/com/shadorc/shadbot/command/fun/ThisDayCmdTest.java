package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.api.html.thisday.ThisDay;
import com.shadorc.shadbot.command.CmdTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ThisDayCmdTest extends CmdTest<ThisDayCmd> {

    @Test
    public void testGetThisDay() {
        final ThisDay result = this.invoke("getThisDay");
        assertFalse(result.getDate().isBlank());
        assertFalse(result.getEvents().isBlank());
    }

}
