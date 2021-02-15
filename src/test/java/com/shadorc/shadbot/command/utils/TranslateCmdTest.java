package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.command.CmdTest;
import com.shadorc.shadbot.command.utils.translate.TranslateCmd;
import com.shadorc.shadbot.command.utils.translate.TranslateData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TranslateCmdTest extends CmdTest<TranslateCmd> {

    @Test
    public void testGetTranslation() {
        final TranslateData data1 = new TranslateData();
        data1.setSourceLang("english");
        data1.setDestLang("french");
        data1.setSourceText("Hello, how are you?");
        assertEquals("Bonjour comment vas-tu?", this.invoke("getTranslation", data1));

        final TranslateData data2 = new TranslateData();
        data2.setSourceLang("en");
        data2.setDestLang("fr");
        data2.setSourceText("Hello, how are you?");
        assertEquals("Bonjour comment vas-tu?", this.invoke("getTranslation", data2));

        final TranslateData data3 = new TranslateData();
        data3.setDestLang("fr");
        data3.setSourceText("Hello, how are you?");
        assertEquals("Bonjour comment vas-tu?", this.invoke("getTranslation", data3));

        final TranslateData data4 = new TranslateData();
        data4.setDestLang("something");
        data4.setSourceText("Hello, how are you?");
        assertThrows(IllegalArgumentException.class, () -> this.invoke("getTranslation", data4));

        final TranslateData data6 = new TranslateData();
        data6.setSourceLang("en");
        assertThrows(IllegalArgumentException.class, () -> data6.setDestLang("en"));
    }

}
