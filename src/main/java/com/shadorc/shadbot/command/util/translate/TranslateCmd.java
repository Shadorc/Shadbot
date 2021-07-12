package com.shadorc.shadbot.command.util.translate;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.Cmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.i18n.I18nManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.StringUtil;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import org.json.JSONArray;
import org.json.JSONException;
import reactor.core.publisher.Mono;

public class TranslateCmd extends Cmd {

    private static final String DOC_URL = "https://cloud.google.com/translate/docs/languages";

    public TranslateCmd() {
        super(CommandCategory.UTILS, "translate", "Translate a text");
        this.addOption(option -> option.name("source_lang")
                .description("Source language, 'auto' to automatically detect")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue()));
        this.addOption(option -> option.name("destination_lang")
                .description("Destination language")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue()));
        this.addOption(option -> option.name("text")
                .description("The text to translate")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        final String sourceLang = context.getOptionAsString("source_lang").orElseThrow();
        final String destLang = context.getOptionAsString("destination_lang").orElseThrow();
        final String text = context.getOptionAsString("text").orElseThrow();

        return Mono.fromCallable(() -> new TranslateRequest(context.getLocale(), sourceLang, destLang, text))
                .onErrorMap(IllegalArgumentException.class,
                        err -> new CommandException(context.localize("translate.exception.doc")
                                .formatted(err.getMessage(), DOC_URL)))
                .flatMap(request -> context.createFollowupMessage(Emoji.HOURGLASS, context.localize("translate.loading"))
                        .then(TranslateCmd.getTranslation(request))
                        .flatMap(response -> context.editFollowupMessage(
                                TranslateCmd.formatEmbed(context, request, response)))
                        .onErrorMap(IllegalArgumentException.class,
                                err -> new CommandException(context.localize("translate.exception.doc")
                                        .formatted(err.getMessage(), DOC_URL))));
    }

    private static EmbedCreateSpec formatEmbed(Context context, TranslateRequest request,
                                               TranslateResponse response) {
        return ShadbotUtil.createEmbedBuilder()
                .author(context.localize("translate.title"), null, context.getAuthorAvatar())
                .description("**%s**%n%s%n%n**%s**%n%s".formatted(
                        StringUtil.capitalize(request.isoToLang(response.sourceLang())),
                        request.getSourceText(),
                        StringUtil.capitalize(request.isoToLang(request.getDestLang())),
                        response.translatedText()))
                .build();
    }

    private static Mono<TranslateResponse> getTranslation(TranslateRequest data) {
        return RequestHelper.request(data.getUrl())
                .map(body -> {
                    // The body is an error 400 if one of the specified language
                    // exists but is not supported by Google translator
                    if (!TranslateCmd.isValidBody(body)) {
                        throw new IllegalArgumentException(I18nManager.localize(data.getLocale(),
                                "translate.unsupported.language"));
                    }

                    final JSONArray array = new JSONArray(body);
                    final JSONArray translations = array.getJSONArray(0);
                    final StringBuilder translatedText = new StringBuilder();
                    for (int i = 0; i < translations.length(); i++) {
                        translatedText.append(translations.getJSONArray(i).getString(0));
                    }

                    if (translatedText.toString().equalsIgnoreCase(data.getSourceText())) {
                        throw new IllegalArgumentException(I18nManager.localize(data.getLocale(),
                                "translate.exception"));
                    }

                    final String sourceLang = array.getString(2);
                    return new TranslateResponse(translatedText.toString(), sourceLang);
                });
    }

    private static boolean isValidBody(String body) {
        try {
            return new JSONArray(body).get(0) instanceof JSONArray;
        } catch (final JSONException err) {
            return false;
        }
    }

}
