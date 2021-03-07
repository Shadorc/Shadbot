package com.shadorc.shadbot.command.utils.translate;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.StringUtil;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import org.json.JSONArray;
import org.json.JSONException;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.Optional;
import java.util.function.Consumer;

public class TranslateCmd extends BaseCmd {

    public TranslateCmd() {
        super(CommandCategory.UTILS, "translate", "Translate a text");
        this.addOption("source_lang",
                "Source language, if not specified, it will be automatically detected",
                false,
                ApplicationCommandOptionType.STRING);
        this.addOption("destination_lang",
                "Destination language",
                true,
                ApplicationCommandOptionType.STRING);
        this.addOption("text",
                "The text to translate",
                true,
                ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Optional<String> sourceLang = context.getOptionAsString("source_lang");
        final String destLang = context.getOptionAsString("destination_lang").orElseThrow();
        final String text = context.getOptionAsString("text").orElseThrow();

        final TranslateData data = new TranslateData();
        data.setSourceText(text);
        try {
            data.setSourceLang(sourceLang.orElse(null));
            data.setDestLang(destLang);
        } catch (final IllegalArgumentException err) {
            throw new CommandException(String.format("%s. Use `/help %s` to see a complete list of supported languages.",
                    err.getMessage(), this.getName()));
        }

        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading translation...", context.getAuthorName())
                .zipWith(TranslateCmd.getTranslation(data))
                .map(TupleUtils.function((messageId, translatedText) ->
                        context.editFollowupMessage(messageId, TranslateCmd.formatEmbed(context.getAuthorAvatarUrl(), data, translatedText))))
                .onErrorMap(IllegalArgumentException.class,
                        err -> new CommandException(String.format("%s. Use `/help %s` to see a complete list of supported languages.",
                                err.getMessage(), this.getName())));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(String avatarUrl, TranslateData data, String translatedText) {
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor("Translation", null, avatarUrl)
                        .setDescription(String.format("**%s**%n%s%n%n**%s**%n%s",
                                StringUtil.capitalize(TranslateData.isoToLang(data.getSourceLang())), data.getSourceText(),
                                StringUtil.capitalize(TranslateData.isoToLang(data.getDestLang())), translatedText)));
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
                .setExample(String.format("`/%s en fr \"How are you ?\"`", this.getName()))
                .addField("Documentation", "List of supported languages: "
                        + "https://cloud.google.com/translate/docs/languages", false)
                .build();
    }

}
