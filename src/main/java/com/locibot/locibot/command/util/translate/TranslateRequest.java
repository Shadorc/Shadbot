package com.locibot.locibot.command.util.translate;

import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.i18n.I18nManager;
import com.locibot.locibot.utils.MapUtil;
import com.locibot.locibot.utils.NetUtil;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TranslateRequest {

    private static final String API_URL = "https://translate.googleapis.com/translate_a/single";
    private static final String AUTO = "auto";
    private static final int CHARACTERS_LIMIT = 150;

    private final Locale locale;
    private final Map<String, String> langIsoMap;
    private final Map<String, String> isoLangMap;
    private String destLang;
    private String sourceLang;
    private String sourceText;

    public TranslateRequest(final Locale locale, final String sourceLang, final String destLang,
                            final String sourceText) {
        this.locale = locale;

        this.langIsoMap = Arrays.stream(Locale.getISOLanguages())
                .collect(Collectors.toUnmodifiableMap(
                        iso -> new Locale(iso).getDisplayLanguage(locale).toLowerCase(),
                        Function.identity(),
                        (value1, value2) -> value1));
        this.isoLangMap = MapUtil.inverse(this.langIsoMap);

        this.setDestLang(destLang);
        this.setSourceLang(sourceLang);
        this.setSourceText(sourceText);
    }

    private void setSourceText(final String sourceText) {
        if (sourceText.length() > CHARACTERS_LIMIT) {
            throw new CommandException(I18nManager.localize(this.locale, "translate.exception.too.many.chars")
                    .formatted(CHARACTERS_LIMIT));
        }

        this.sourceText = sourceText;
    }

    private void setSourceLang(final String sourceLang) {
        if (sourceLang.equalsIgnoreCase(AUTO)) {
            this.sourceLang = AUTO;
        } else {
            this.sourceLang = this.langToIso(sourceLang);
        }

        if (this.sourceLang == null) {
            throw new IllegalArgumentException(I18nManager.localize(this.locale, "translate.exception.source.lang"));
        }
        if (Objects.equals(this.sourceLang, this.destLang)) {
            throw new IllegalArgumentException(I18nManager.localize(this.locale, "translate.exception.same.langs"));
        }
    }

    private void setDestLang(String destLang) {
        this.destLang = this.langToIso(destLang);

        if (this.destLang == null) {
            throw new IllegalArgumentException(I18nManager.localize(this.locale, "translate.exception.dest.lang"));
        }
        if (Objects.equals(this.sourceLang, this.destLang)) {
            throw new IllegalArgumentException(I18nManager.localize(this.locale, "translate.exception.same.langs"));
        }
    }

    public String getUrl() {
        return "%s?client=gtx&ie=UTF-8&oe=UTF-8&sl=%s&tl=%s&dt=t&q=%s"
                .formatted(API_URL, NetUtil.encode(this.sourceLang),
                        NetUtil.encode(this.destLang), NetUtil.encode(this.sourceText));
    }

    public Locale getLocale() {
        return this.locale;
    }

    public String getDestLang() {
        return this.destLang;
    }

    public String getSourceText() {
        return this.sourceText;
    }

    protected String langToIso(final String lang) {
        return this.langIsoMap.getOrDefault(lang, lang);
    }

    protected String isoToLang(final String iso) {
        return this.isoLangMap.get(iso);
    }
}
