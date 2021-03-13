package com.shadorc.shadbot.command.utils.translate;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.utils.MapUtil;
import com.shadorc.shadbot.utils.NetUtil;
import reactor.util.annotation.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TranslateData {

    private static final String API_URL = "https://translate.googleapis.com/translate_a/single";
    private static final String AUTO = "auto";
    private static final int CHARACTERS_LIMIT = 150;

    private static final Supplier<IllegalArgumentException> EQUAL_LANGS_EXCEPTION = () ->
            new IllegalArgumentException("The destination language must be different from the source one");
    private static final Map<String, String> LANG_ISO_MAP = Arrays.stream(Locale.getISOLanguages())
            .collect(Collectors.toUnmodifiableMap(
                    iso -> new Locale(iso).getDisplayLanguage(Locale.ENGLISH).toLowerCase(),
                    Function.identity(),
                    (value1, value2) -> value1));
    private static final Map<String, String> ISO_LANG_MAP = MapUtil.inverse(LANG_ISO_MAP);

    private String destLang;
    private String sourceLang;
    private String sourceText;

    public TranslateData() {
        this.sourceLang = AUTO;
    }

    public void setSourceText(final String sourceText) {
        if (sourceText.length() > CHARACTERS_LIMIT) {
            throw new CommandException("The text to translate cannot exceed %d characters.".formatted(CHARACTERS_LIMIT));
        }

        this.sourceText = sourceText;
    }

    public void setSourceLang(final String sourceLang) {
        if (sourceLang.equalsIgnoreCase(AUTO)) {
            this.sourceLang = AUTO;
        } else {
            this.sourceLang = TranslateData.langToIso(sourceLang);
        }

        if (this.sourceLang == null) {
            throw new IllegalArgumentException("The source language isn't supported");
        }
        if (Objects.equals(this.sourceLang, this.destLang)) {
            throw EQUAL_LANGS_EXCEPTION.get();
        }
    }

    public void setDestLang(final String destLang) {
        this.destLang = TranslateData.langToIso(destLang);

        if (this.destLang == null) {
            throw new IllegalArgumentException("The destination language isn't supported");
        }
        if (Objects.equals(this.sourceLang, this.destLang)) {
            throw EQUAL_LANGS_EXCEPTION.get();
        }
    }

    public String getUrl() {
        return "%s?client=gtx&ie=UTF-8&oe=UTF-8&sl=%s&tl=%s&dt=t&q=%s"
                .formatted(API_URL, NetUtil.encode(this.sourceLang),
                        NetUtil.encode(this.destLang), NetUtil.encode(this.sourceText));
    }

    public String getDestLang() {
        return this.destLang;
    }

    public String getSourceLang() {
        return this.sourceLang;
    }

    public String getSourceText() {
        return this.sourceText;
    }

    protected static String langToIso(final String lang) {
        return LANG_ISO_MAP.getOrDefault(lang, lang);
    }

    protected static String isoToLang(final String iso) {
        return ISO_LANG_MAP.get(iso);
    }
}
