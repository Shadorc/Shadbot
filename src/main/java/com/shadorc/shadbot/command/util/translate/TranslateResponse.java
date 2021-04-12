package com.shadorc.shadbot.command.util.translate;

public class TranslateResponse {

    private final String translatedText;
    private final String sourceLang;

    public TranslateResponse(final String translatedText, final String sourceLang) {
        this.translatedText = translatedText;
        this.sourceLang = sourceLang;
    }

    public String getTranslatedText() {
        return this.translatedText;
    }

    public String getSourceLang() {
        return this.sourceLang;
    }
}
