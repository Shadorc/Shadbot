package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.command.MissingArgumentException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.MapUtils;
import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.ShadbotUtils;
import com.shadorc.shadbot.utils.StringUtils;
import discord4j.core.spec.EmbedCreateSpec;
import org.json.JSONArray;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TranslateCmd extends BaseCmd {

    private static final String AUTO = "auto";
    private static final int CHARACTERS_LIMIT = 150;

    private final Map<String, String> langIsoMap;
    private final Map<String, String> isoLangMap;

    public TranslateCmd() {
        super(CommandCategory.UTILS, List.of("translate"));
        this.setDefaultRateLimiter();

        final Map<String, String> map = Arrays.stream(Locale.getISOLanguages())
                .collect(Collectors.toMap(
                        iso -> new Locale(iso).getDisplayLanguage(Locale.ENGLISH).toLowerCase(),
                        iso -> iso));
        map.put(AUTO, AUTO);

        this.langIsoMap = Collections.unmodifiableMap(map);
        this.isoLangMap = MapUtils.inverse(map);
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final List<String> quotedWords = StringUtils.getQuotedElements(arg);
        if (quotedWords.size() != 1) {
            return Mono.error(new CommandException(
                    "The text to translate cannot be empty and must be enclosed in quotation marks."));
        }
        final String sourceText = quotedWords.get(0);
        if (sourceText.length() > CHARACTERS_LIMIT) {
            return Mono.error(new CommandException(
                    String.format("The text to translate cannot exceed %d characters.", CHARACTERS_LIMIT)));
        }

        final List<String> languages = StringUtils.split(StringUtils.remove(arg, sourceText, "\""));
        if (languages.isEmpty()) {
            return Mono.error(new MissingArgumentException());
        }

        if (languages.size() == 1) {
            languages.add(0, AUTO);
        }

        final String langFrom = this.toISO(languages.get(0));
        final String langTo = this.toISO(languages.get(1));

        if (langTo != null && Objects.equals(langFrom, langTo)) {
            return Mono.error(new CommandException("The destination language must be different from the source one."));
        }

        final String url = String.format("https://translate.googleapis.com/translate_a/single?"
                        + "client=gtx"
                        + "&ie=UTF-8"
                        + "&oe=UTF-8"
                        + "&sl=%s"
                        + "&tl=%s"
                        + "&dt=t"
                        + "&q=%s",
                NetUtils.encode(langFrom), NetUtils.encode(langTo), NetUtils.encode(sourceText));

        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());
        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading translation...",
                context.getUsername()))
                .send()
                .then(NetUtils.get(url))
                .map(body -> {
                    if (langFrom == null || langTo == null
                            // The body is an error 400 if one of the specified language exists but is not supported
                            // by Google translator
                            || !body.startsWith("[")
                            || !(new JSONArray(body).get(0) instanceof JSONArray)) {
                        throw new CommandException(String.format("One of the specified language isn't supported. "
                                        + "Use `%shelp %s` to see a complete list of supported languages.",
                                context.getPrefix(), this.getName()));
                    }

                    final JSONArray result = new JSONArray(body);
                    final StringBuilder translatedText = new StringBuilder();
                    final JSONArray translations = result.getJSONArray(0);
                    for (int i = 0; i < translations.length(); i++) {
                        translatedText.append(translations.getJSONArray(i).getString(0));
                    }

                    if (translatedText.toString().equalsIgnoreCase(sourceText)) {
                        throw new CommandException(String.format("The text could not been translated."
                                        + "%nCheck that the specified languages are supported, that the text is in "
                                        + "the specified language and that the destination language is different from the "
                                        + "source one. %nUse `%shelp %s` to see a complete list of supported languages.",
                                context.getPrefix(), this.getName()));
                    }

                    return updatableMsg.setEmbed(ShadbotUtils.getDefaultEmbed()
                            .andThen(embed -> embed.setAuthor("Translation", null, context.getAvatarUrl())
                                    .setDescription(String.format("**%s**%n%s%n%n**%s**%n%s",
                                            StringUtils.capitalize(this.isoLangMap.get(langFrom)), sourceText,
                                            StringUtils.capitalize(this.isoLangMap.get(langTo)), translatedText))));

                })
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private String toISO(String lang) {
        return this.langIsoMap.containsValue(lang) ? lang : this.langIsoMap.get(lang);
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
}
