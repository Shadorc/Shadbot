package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.api.json.wikipedia.WikipediaPage;
import com.shadorc.shadbot.command.CmdTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

public class WikipediaCmdTest extends CmdTest<WikipediaCmd> {

    @Test
    public void testGetWikipediaPage() {
        final WikipediaPage result = this.invoke("getWikipediaPage", "21 guns");
        assertFalse(result.getExtract().isBlank());
        assertFalse(result.getTitle().isBlank());
        assertFalse(result.getEncodedTitle().isBlank());
    }

    @Test
    public void testGetWikipediaPageSpecialChars() {
        final WikipediaPage result = this.invoke("getWikipediaPage", SPECIAL_CHARS);
        assertNull(result);
    }

}
