/*
package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.command.MissingArgumentException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.MapUtils;
import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.ShadbotUtils;
import com.shadorc.shadbot.utils.StringUtils;
import discord4j.core.spec.EmbedCreateSpec;
import org.json.JSONArray;
import org.json.JSONException;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TranslateCmd extends BaseCmd {

    public TranslateCmd() {
        super(CommandCategory.UTILS, List.of("translate"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final List<String> quotedWords = StringUtils.getQuotedElements(arg);
        if (quotedWords.size() != 1) {
            return Mono.error(new CommandException(
                    "The text to translate cannot be empty and must be enclosed in quotation marks."));
        }

        final TranslateData data = new TranslateData();

        final String sourceText = quotedWords.get(0);
        data.setSourceText(sourceText);

        final List<String> languages = StringUtils.split(StringUtils.remove(arg, sourceText, "\""));
        try {
            data.setLanguages(languages);
        } catch (final IllegalArgumentException err) {
            throw new CommandException(String.format("%s. Use `%shelp %s` to see a complete list of supported languages.",
                    err.getMessage(), context.getPrefix(), this.getName()));
        }

        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());
        return updatableMsg.setContent(
                String.format(Emoji.HOURGLASS + " (**%s**) Loading translation...", context.getUsername()))
                .send()
                .then(TranslateCmd.getTranslation(data))
                .map(translatedText -> updatableMsg.setEmbed(ShadbotUtils.getDefaultEmbed()
                        .andThen(embed -> embed.setAuthor("Translation", null, context.getAvatarUrl())
                                .setDescription(String.format("**%s**%n%s%n%n**%s**%n%s",
                                        StringUtils.capitalize(TranslateData.isoToLang(data.getLangFrom())), sourceText,
                                        StringUtils.capitalize(TranslateData.isoToLang(data.getLangTo())), translatedText)))))
                .onErrorMap(IllegalArgumentException.class,
                        err -> new CommandException(String.format("%s. Use `%shelp %s` to see a complete list of supported languages.",
                                err.getMessage(), context.getPrefix(), this.getName())))
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private static Mono<String> getTranslation(TranslateData data) {
        return RequestHelper.request(data.getUrl())
                .map(body -> {
                    // The body is an error 400 if one of the specified language
                    // exists but is not supported by Google translator
                    if (!TranslateCmd.isValidBody(body)) {
                        throw new IllegalArgumentException("One of the specified language isn't supported");
                    }

                    final JSONArray translations = new JSONArray(body).getJSONArray(0);
                    final StringBuilder translatedText = new StringBuilder();
                    for (int i = 0; i < translations.length(); i++) {
                        translatedText.append(translations.getJSONArray(i).getString(0));
                    }

                    if (translatedText.toString().equalsIgnoreCase(data.getSourceText())) {
                        throw new IllegalArgumentException("The text could not been translated."
                                + "%nCheck that the specified languages are supported, that the text is in "
                                + "the specified language and that the destination language is different from the "
                                + "source one");
                    }

                    return translatedText.toString();
                });
    }

    private static boolean isValidBody(final String body) {
        try {
            return new JSONArray(body).get(0) instanceof JSONArray;
        } catch (final JSONException err) {
            return false;
        }
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Translate a text from a language to another.")
                .addArg("fromLang", "source language, by leaving it blank the language will "
                        + "be automatically detected", true)
                .addArg("toLang", "destination language", false)
                .addArg("\"text\"", false)
                .setExample(String.format("`%s%s en fr \"How are you ?\"`", context.getPrefix(), this.getName()))
                .addField("Documentation", "List of supported languages: "
                        + "https://cloud.google.com/translate/docs/languages", false)
                .build();
    }

    public static class TranslateData {

        private static final String API_URL = "https://translate.googleapis.com/translate_a/single";
        private static final int CHARACTERS_LIMIT = 150;
        private static final String AUTO = "auto";

        private static final Map<String, String> LANG_ISO_MAP = Arrays.stream(Locale.getISOLanguages())
                .collect(Collectors.toUnmodifiableMap(
                        iso -> new Locale(iso).getDisplayLanguage(Locale.ENGLISH).toLowerCase(),
                        iso -> iso,
                        (value1, value2) -> value1));
        private static final Map<String, String> ISO_LANG_MAP = MapUtils.inverse(LANG_ISO_MAP);

        private String langTo;
        private String langFrom;
        private String sourceText;

        public TranslateData setSourceText(final String sourceText) {
            if (sourceText.length() > CHARACTERS_LIMIT) {
                throw new CommandException(
                        String.format("The text to translate cannot exceed %d characters.", CHARACTERS_LIMIT));
            }

            this.sourceText = sourceText;
            return this;
        }

        public TranslateData setLanguages(final List<String> languages) {
            if (languages.isEmpty()) {
                throw new MissingArgumentException();
            }

            if (languages.size() == 1) {
                this.langFrom = AUTO;
                this.langTo = TranslateData.langToIso(languages.get(0));
            } else {
                this.langFrom = TranslateData.langToIso(languages.get(0));
                this.langTo = TranslateData.langToIso(languages.get(1));
            }

            if (this.langFrom == null || this.langTo == null) {
                throw new IllegalArgumentException("One of the specified language isn't supported");
            }

            if (Objects.equals(this.langFrom, this.langTo)) {
                throw new IllegalArgumentException("The destination language must be different from the source one.");
            }

            return this;
        }

        public String getUrl() {
            return String.format("%s?client=gtx&ie=UTF-8&oe=UTF-8&sl=%s&tl=%s&dt=t&q=%s",
                    API_URL, NetUtils.encode(this.langFrom), NetUtils.encode(this.langTo), NetUtils.encode(this.sourceText));
        }

        public String getLangTo() {
            return this.langTo;
        }

        public String getLangFrom() {
            return this.langFrom;
        }

        public String getSourceText() {
            return this.sourceText;
        }

        private static String langToIso(final String lang) {
            return LANG_ISO_MAP.getOrDefault(lang, lang);
        }

        private static String isoToLang(final String iso) {
            return ISO_LANG_MAP.get(iso);
        }
    }
}
*/
