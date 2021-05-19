package com.locibot.locibot.command.image;

import com.locibot.locibot.command.CmdTest;
import com.locibot.locibot.api.html.suicidegirl.SuicideGirl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class SuicideGirlsCmdTest extends CmdTest<SuicideGirlsCmd> {

    @Test
    public void testGetRandomSuicideGirl() {
        final SuicideGirl result = this.invoke("getRandomSuicideGirl");
        assertFalse(result.getName().isBlank());
        assertFalse(result.getImageUrl().isBlank());
        assertFalse(result.getUrl().isBlank());
    }
}
