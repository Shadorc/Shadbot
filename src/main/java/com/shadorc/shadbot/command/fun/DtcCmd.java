package com.shadorc.shadbot.command.fun;

import com.fasterxml.jackson.databind.JavaType;
import com.shadorc.shadbot.api.json.dtc.Quote;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.NetUtil;
import com.shadorc.shadbot.utils.RandUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.List;
import java.util.function.Consumer;

public class DtcCmd extends BaseCmd {

    private static final String HOME_URL = "https://api.danstonchat.com/0.3/view/random";
    private static final String RANDOM_QUOTE_URL = String.format("%s?key=%s&format=json",
            HOME_URL, CredentialManager.getInstance().get(Credential.DTC_API_KEY));

    public DtcCmd() {
        super(CommandCategory.FUN, "dtc", "Show a random quote from DansTonChat.com");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading quote...", context.getAuthorName())
                .zipWith(DtcCmd.getRandomQuote())
                .flatMap(TupleUtils.function((messageId, quote) ->
                        context.editFollowupMessage(messageId, DtcCmd.formatEmbed(quote, context.getAuthorAvatarUrl()))));
    }

    private static Mono<Quote> getRandomQuote() {
        final JavaType valueType = NetUtil.MAPPER.getTypeFactory().constructCollectionType(List.class, Quote.class);
        return RequestHelper.fromUrl(RANDOM_QUOTE_URL)
                .<List<Quote>>to(valueType)
                .map(quotes -> {
                    Quote quote;
                    do {
                        quote = RandUtil.randValue(quotes);
                    } while (quote.getContent().length() > 1000);
                    return quote;
                });
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(final Quote quote, final String avatarUrl) {
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor("Quote DansTonChat",
                        String.format("https://danstonchat.com/%s.html", quote.getId()), avatarUrl)
                        .setThumbnail("https://i.imgur.com/5YvTlAA.png")
                        .setDescription(DtcCmd.formatContent(quote.getContent())));
    }

    private static String formatContent(String content) {
        final String formattedContent = content.replace("*", "\\*");
        return FormatUtil.format(formattedContent.split("\n"), DtcCmd::formatLine, "\n");
    }

    private static String formatLine(String line) {
        // Set the user name as bold
        if (line.contains(" ")) {
            final int index = line.indexOf(' ');
            return String.format("**%s** %s", line.substring(0, index), line.substring(index + 1));
        }
        return line;
    }

}
