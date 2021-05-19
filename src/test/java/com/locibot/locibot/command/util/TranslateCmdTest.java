package com.locibot.locibot.command.util;

import com.locibot.locibot.command.CmdTest;
import com.locibot.locibot.command.util.translate.TranslateCmd;
import com.locibot.locibot.command.util.translate.TranslateRequest;
import com.locibot.locibot.command.util.translate.TranslateResponse;
import com.locibot.locibot.data.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TranslateCmdTest extends CmdTest<TranslateCmd> {

    @Test
    public void testGetTranslationLangFullName() {
        final TranslateRequest data = new TranslateRequest(Config.DEFAULT_LOCALE,
                "english", "french", "Hello, how are you?");
        final TranslateResponse response = this.invoke("getTranslation", data);
        assertEquals("en", response.sourceLang());
        assertEquals("Bonjour comment vas-tu?", response.translatedText());
    }

    @Test
    public void testGetTranslationLangShortName() {
        final TranslateRequest data = new TranslateRequest(Config.DEFAULT_LOCALE,
                "en", "fr", "Hello, how are you?");
        final TranslateResponse response = this.invoke("getTranslation", data);
        assertEquals("en", response.sourceLang());
        assertEquals("Bonjour comment vas-tu?", response.translatedText());
    }

    @Test
    public void testGetTranslationFuzzy() {
        final TranslateRequest data = new TranslateRequest(Config.DEFAULT_LOCALE,
                "en", "fr", SPECIAL_CHARS);
        final TranslateResponse response = this.invoke("getTranslation", data);
        assertEquals("en", response.sourceLang());
        assertEquals("& ~ # {([- | `_\" '\\ ^ @)] =} ° + ¨ ^ $ £ ¤% * µ,?;.: /! § <> + - * /",
                response.translatedText());
    }

    @Test
    public void testGetTranslationOnlyDest() {
        final TranslateRequest data = new TranslateRequest(Config.DEFAULT_LOCALE,
                "auto", "fr", "Hello, how are you?");
        final TranslateResponse response = this.invoke("getTranslation", data);
        assertEquals("en", response.sourceLang());
        assertEquals("Bonjour comment vas-tu?", response.translatedText());
    }

    @Test
    public void testGetTranslationWrongDest() {
        final TranslateRequest data = new TranslateRequest(Config.DEFAULT_LOCALE,
                "auto", "something", "Hello, how are you?");
        assertThrows(IllegalArgumentException.class, () -> this.invoke("getTranslation", data));
    }

    @Test
    public void testGetTranslationNoText() {
        assertThrows(IllegalArgumentException.class, () -> new TranslateRequest(Config.DEFAULT_LOCALE,
                "en", "en", "Hello world"));
    }

}
