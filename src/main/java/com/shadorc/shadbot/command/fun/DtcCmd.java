package com.shadorc.shadbot.command.fun;

import com.fasterxml.jackson.databind.JavaType;
import com.shadorc.shadbot.api.json.dtc.Quote;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class DtcCmd extends BaseCmd {

    private static final String HOME_URL = "https://api.danstonchat.com/0.3/view/random";
    private static final String RANDOM_QUOTE_URL = String.format("%s?key=%s&format=json",
            HOME_URL, CredentialManager.getInstance().get(Credential.DTC_API_KEY));

    public DtcCmd() {
        super(CommandCategory.FUN, List.of("dtc"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());
        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading quote...", context.getUsername()))
                .send()
                .then(this.getRandomQuote())
                .map(quote -> updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> embed.setAuthor("Quote DansTonChat",
                                String.format("https://danstonchat.com/%s.html", quote.getId()), context.getAvatarUrl())
                                .setThumbnail("https://i.imgur.com/5YvTlAA.png")
                                .setDescription(this.formatContent(quote.getContent())))))
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private Mono<Quote> getRandomQuote() {
        final JavaType valueType = Utils.MAPPER.getTypeFactory().constructCollectionType(List.class, Quote.class);
        return NetUtils
                .<List<Quote>>get(RANDOM_QUOTE_URL, valueType)
                .map(quotes -> {
                    Quote quote;
                    do {
                        quote = Utils.randValue(quotes);
                    } while (quote.getContent().length() > 1000);
                    return quote;
                });
    }

    private String formatContent(String content) {
        final String formattedContent = content.replace("*", "\\*");
        return FormatUtils.format(content.split("\n"), this::formatLine, "\n");
    }

    private String formatLine(String line) {
        // Set the user name as bold
        if (line.contains(" ")) {
            final int index = line.indexOf(' ');
            return String.format("**%s** %s", line.substring(0, index), line.substring(index + 1));
        }
        return line;
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Show a random quote from DansTonChat.com")
                .setSource("https://www.danstonchat.com/")
                .build();
    }
}
