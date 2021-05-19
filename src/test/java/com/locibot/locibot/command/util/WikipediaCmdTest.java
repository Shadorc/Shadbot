package com.locibot.locibot.command.util;

import com.locibot.locibot.command.CmdTest;
import com.locibot.locibot.api.json.wikipedia.WikipediaPage;
import com.locibot.locibot.data.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WikipediaCmdTest extends CmdTest<WikipediaCmd> {

    @Test
    public void testGetWikipediaPage() {
        final WikipediaPage result = this.invoke("getWikipediaPage", Config.DEFAULT_LOCALE, "21 guns");
        assertTrue(result.extract().isPresent());
        assertFalse(result.extract().orElseThrow().isBlank());
        assertFalse(result.title().isBlank());
        assertFalse(result.getEncodedTitle().isBlank());
    }

    @Test
    public void testGetWikipediaPageFuzzy() {
        final WikipediaPage result = this.invoke("getWikipediaPage", Config.DEFAULT_LOCALE, SPECIAL_CHARS);
        assertNull(result);
    }

}
