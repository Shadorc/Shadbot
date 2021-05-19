package com.locibot.locibot.command.fun;

import com.locibot.locibot.command.CmdTest;
import com.locibot.locibot.api.html.thisday.ThisDay;
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
