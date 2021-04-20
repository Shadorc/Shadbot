package com.shadorc.shadbot.command.util;

import com.shadorc.shadbot.api.json.wikipedia.WikipediaPage;
import com.shadorc.shadbot.command.CmdTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WikipediaCmdTest extends CmdTest<WikipediaCmd> {

    @Test
    public void testGetWikipediaPage() {
        final WikipediaPage result = this.invoke("getWikipediaPage", "21 guns");
        assertNotNull(result.extract());
        assertFalse(result.extract().isBlank());
        assertFalse(result.title().isBlank());
        assertFalse(result.getEncodedTitle().isBlank());
    }

    @Test
    public void testGetWikipediaPageFuzzy() {
        final WikipediaPage result = this.invoke("getWikipediaPage", SPECIAL_CHARS);
        assertNull(result);
    }

}
