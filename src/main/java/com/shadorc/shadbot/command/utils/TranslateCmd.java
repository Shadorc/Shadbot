package com.shadorc.shadbot.command.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.exception.MissingArgumentException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.StringUtils;
import discord4j.core.spec.EmbedCreateSpec;
import org.json.JSONArray;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public class TranslateCmd extends BaseCmd {

    private static final String AUTO = "auto";
    private static final int CHARACTERS_LIMIT = 150;

    private final BiMap<String, String> langIsoMap;

    public TranslateCmd() {
        super(CommandCategory.UTILS, List.of("translate", "translation", "trans"));
        this.setDefaultRateLimiter();

        this.langIsoMap = HashBiMap.create();
        Arrays.stream(Locale.getISOLanguages())
                .forEach(iso -> this.langIsoMap.put(new Locale(iso).getDisplayLanguage(Locale.ENGLISH).toLowerCase(), iso));
        this.langIsoMap.put(AUTO, AUTO);
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final List<String> quotedWords = StringUtils.getQuotedElements(arg);
        if (quotedWords.size() != 1) {
            return Mono.error(new CommandException("The text to translate cannot be empty and must be enclosed in quotation marks."));
        }
        final String sourceText = quotedWords.get(0);
        if (sourceText.length() > CHARACTERS_LIMIT) {
            return Mono.error(new CommandException(String.format("The text to translate cannot exceed %d characters.", CHARACTERS_LIMIT)));
        }

        final List<String> langs = StringUtils.split(StringUtils.remove(arg, sourceText, "\""));
        if (langs.isEmpty()) {
            return Mono.error(new MissingArgumentException());
        }

        if (langs.size() == 1) {
            langs.add(0, AUTO);
        }

        final String langFrom = this.toISO(langs.get(0));
        final String langTo = this.toISO(langs.get(1));

        if (langTo != null && Objects.equals(langFrom, langTo)) {
            return Mono.error(new CommandException("The destination language must be different from the source one."));
        }

        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading translation...", context.getUsername()))
                .send()
                .then(Mono.fromCallable(() -> {
                    final String url = String.format("https://translate.googleapis.com/translate_a/single?"
                                    + "client=gtx"
                                    + "&ie=UTF-8"
                                    + "&oe=UTF-8"
                                    + "&sl=%s"
                                    + "&tl=%s"
                                    + "&dt=t"
                                    + "&q=%s",
                            NetUtils.encode(langFrom), NetUtils.encode(langTo), NetUtils.encode(sourceText));

                    final JSONArray result = new JSONArray(NetUtils.get(url).block());

                    if (langFrom == null || langTo == null || !(result.get(0) instanceof JSONArray)) {
                        throw new CommandException(String.format("One of the specified language isn't supported. "
                                + "Use `%shelp %s` to see a complete list of supported languages.", context.getPrefix(), this.getName()));
                    }

                    final StringBuilder translatedText = new StringBuilder();
                    final JSONArray translations = result.getJSONArray(0);
                    for (int i = 0; i < translations.length(); i++) {
                        translatedText.append(translations.getJSONArray(i).getString(0));
                    }

                    if (translatedText.toString().equalsIgnoreCase(sourceText)) {
                        throw new CommandException(String.format("The text could not been translated."
                                + "%nCheck that the specified languages are supported and that the text is in the specified language."
                                + "%nUse `%shelp %s` to see a complete list of supported languages.", context.getPrefix(), this.getName()));
                    }

                    return updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                            .andThen(embed -> embed.setAuthor("Translation", null, context.getAvatarUrl())
                                    .setDescription(String.format("**%s**%n%s%n%n**%s**%n%s",
                                            StringUtils.capitalize(this.langIsoMap.inverse().get(langFrom)), sourceText,
                                            StringUtils.capitalize(this.langIsoMap.inverse().get(langTo)), translatedText.toString()))));

                }))
                .flatMap(UpdatableMessage::send)
                .then();
    }

    private String toISO(String lang) {
        return this.langIsoMap.containsValue(lang) ? lang : this.langIsoMap.get(lang);
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Translate a text from a language to another.")
                .addArg("fromLang", "source language, by leaving it blank the language will be automatically detected", true)
                .addArg("toLang", "destination language", false)
                .addArg("\"text\"", false)
                .setExample(String.format("`%s%s en fr \"How are you ?\"`", context.getPrefix(), this.getName()))
                .addField("Documentation", "List of supported languages: https://cloud.google.com/translate/docs/languages", false)
                .build();
    }
}
