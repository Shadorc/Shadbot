package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.api.json.wikipedia.WikipediaPage;
import com.shadorc.shadbot.command.CmdTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class WikipediaCmdTest extends CmdTest<WikipediaCmd> {

    @Test
    public void testGetWikipediaPage() {
        final WikipediaPage result = this.invoke("getWikipediaPage", "21 guns");
        assertNotNull(result.getExtract());
        assertNotNull(result.getTitle());
    }

    @Test
    public void testGetWikipediaPageSpecial() {
        final WikipediaPage result = this.invoke(
                "getWikipediaPage", "&~#{([-|`_\"'\\^@)]=}°+¨^ $£¤%*µ,?;.:/!§<>+-*/");
        assertNull(result);
    }

}
