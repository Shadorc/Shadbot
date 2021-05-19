package com.locibot.locibot.command.util.translate;

import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.core.i18n.I18nManager;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.object.RequestHelper;
import com.locibot.locibot.utils.ShadbotUtil;
import com.locibot.locibot.utils.StringUtil;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import org.json.JSONArray;
import org.json.JSONException;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public class TranslateCmd extends BaseCmd {

    private static final String DOC_URL = "https://cloud.google.com/translate/docs/languages";

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

    private static Consumer<EmbedCreateSpec> formatEmbed(Context context, TranslateRequest request,
                                                         TranslateResponse response) {
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(context.localize("translate.title"), null, context.getAuthorAvatar())
                        .setDescription("**%s**%n%s%n%n**%s**%n%s".formatted(
                                StringUtil.capitalize(request.isoToLang(response.sourceLang())),
                                request.getSourceText(),
                                StringUtil.capitalize(request.isoToLang(request.getDestLang())),
                                response.translatedText())));
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
