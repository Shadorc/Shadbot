/*
package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.command.CmdTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TranslateCmdTest extends CmdTest<TranslateCmd> {

    @Test
    public void testGetTranslation() {
        final TranslateCmd.TranslateData data1 = new TranslateCmd.TranslateData();
        data1.setLanguages(List.of("english", "french"));
        data1.setSourceText("Hello, how are you?");
        assertEquals("Salut comment allez-vous?", this.invoke("getTranslation", data1));

        final TranslateCmd.TranslateData data2 = new TranslateCmd.TranslateData();
        data2.setLanguages(List.of("en", "fr"));
        data2.setSourceText("Hello, how are you?");
        assertEquals("Salut comment allez-vous?", this.invoke("getTranslation", data2));

        final TranslateCmd.TranslateData data3 = new TranslateCmd.TranslateData();
        data3.setLanguages(List.of("fr"));
        data3.setSourceText("Hello, how are you?");
        assertEquals("Salut comment allez-vous?", this.invoke("getTranslation", data3));

        final TranslateCmd.TranslateData data4 = new TranslateCmd.TranslateData();
        data4.setLanguages(List.of("something"));
        data4.setSourceText("Hello, how are you?");
        assertThrows(IllegalArgumentException.class, () -> this.invoke("getTranslation", data4));

        final TranslateCmd.TranslateData data6 = new TranslateCmd.TranslateData();
        assertThrows(IllegalArgumentException.class, () -> data6.setLanguages(List.of("en", "en")));
    }

}
*/
