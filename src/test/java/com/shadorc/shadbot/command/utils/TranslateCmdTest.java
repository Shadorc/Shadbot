package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.command.CmdTest;
import com.shadorc.shadbot.command.utils.translate.TranslateCmd;
import com.shadorc.shadbot.command.utils.translate.TranslateData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TranslateCmdTest extends CmdTest<TranslateCmd> {

    @Test
    public void testGetTranslationLangFullName() {
        final TranslateData data = new TranslateData();
        data.setSourceLang("english");
        data.setDestLang("french");
        data.setSourceText("Hello, how are you?");
        assertEquals("Bonjour comment vas-tu?", this.invoke("getTranslation", data));
    }

    @Test
    public void testGetTranslationLangShortName() {
        final TranslateData data = new TranslateData();
        data.setSourceLang("en");
        data.setDestLang("fr");
        data.setSourceText("Hello, how are you?");
        assertEquals("Bonjour comment vas-tu?", this.invoke("getTranslation", data));
    }

    @Test
    public void testGetTranslationFuzzy() {
        final TranslateData data = new TranslateData();
        data.setSourceLang("en");
        data.setDestLang("fr");
        data.setSourceText(SPECIAL_CHARS);
        assertEquals("& ~ # {([- | `_\" '\\ ^ @)] =} ° + ¨ ^ $ £ ¤% * µ,?;.: /! § <> + - * /",
                this.invoke("getTranslation", data));
    }

    @Test
    public void testGetTranslationOnlyDest() {
        final TranslateData data = new TranslateData();
        data.setDestLang("fr");
        data.setSourceText("Hello, how are you?");
        assertEquals("Bonjour comment vas-tu?", this.invoke("getTranslation", data));
    }

    @Test
    public void testGetTranslationWrongDest() {
        final TranslateData data = new TranslateData();
        data.setDestLang("something");
        data.setSourceText("Hello, how are you?");
        assertThrows(IllegalArgumentException.class, () -> this.invoke("getTranslation", data));
    }

    @Test
    public void testGetTranslationNoText() {
        final TranslateData data = new TranslateData();
        data.setSourceLang("en");
        assertThrows(IllegalArgumentException.class, () -> data.setDestLang("en"));
    }

}
