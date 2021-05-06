package com.shadorc.shadbot.command.util;

import com.shadorc.shadbot.api.json.wikipedia.WikipediaPage;
import com.shadorc.shadbot.command.CmdTest;
import com.shadorc.shadbot.data.Config;
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
