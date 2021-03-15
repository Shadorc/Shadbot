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

import java.util.function.Consumer;

public class TranslateCmd extends BaseCmd {

    public TranslateCmd() {
        super(CommandCategory.UTILS, "translate", "Translate a text");
        this.addOption("source_lang", "Source language, 'auto' to automatically detect",
                true, ApplicationCommandOptionType.STRING);
        this.addOption("destination_lang", "Destination language", true,
                ApplicationCommandOptionType.STRING);
        this.addOption("text", "The text to translate", true, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        final String sourceLang = context.getOptionAsString("source_lang").orElseThrow();
        final String destLang = context.getOptionAsString("destination_lang").orElseThrow();
        final String text = context.getOptionAsString("text").orElseThrow();

        final TranslateData data = new TranslateData();
        data.setSourceText(text);
        try {
            data.setSourceLang(sourceLang);
            data.setDestLang(destLang);
        } catch (final IllegalArgumentException err) {
            return Mono.error(new CommandException("%s. Use `/help %s` to see a complete list of supported languages."
                    .formatted(err.getMessage(), this.getName())));
        }

        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading translation...", context.getAuthorName())
                .zipWith(TranslateCmd.getTranslation(data))
                .flatMap(TupleUtils.function((messageId, translatedText) ->
                        context.editReply(messageId,
                                TranslateCmd.formatEmbed(data, context.getAuthorAvatar(), translatedText))))
                .onErrorMap(IllegalArgumentException.class,
                        err -> new CommandException("%s. Use `/help %s` to see a complete list of supported languages."
                                .formatted(err.getMessage(), this.getName())));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(TranslateData data, String avatarUrl, String translatedText) {
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor("Translation", null, avatarUrl)
                        .setDescription("**%s**%n%s%n%n**%s**%n%s".formatted(
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
                .setExample("`/%s en fr \"How are you ?\"`".formatted(this.getName()))
                .addField("Documentation", "List of supported languages: " +
                        "https://cloud.google.com/translate/docs/languages", false)
                .build();
    }

}
