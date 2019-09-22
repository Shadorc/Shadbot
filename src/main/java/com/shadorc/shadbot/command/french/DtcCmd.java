package com.shadorc.shadbot.command.french;

import com.fasterxml.jackson.databind.JavaType;
import com.shadorc.shadbot.api.dtc.Quote;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.Credentials;
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

    public DtcCmd() {
        super(CommandCategory.FRENCH, List.of("dtc"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        final String url = String.format("https://api.danstonchat.com/0.3/view/random?key=%s&format=json",
                Credentials.get(Credential.DTC_API_KEY));

        final JavaType valueType = Utils.MAPPER.getTypeFactory().constructCollectionType(List.class, Quote.class);
        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading quote...", context.getUsername()))
                .send()
                .then(NetUtils.get(url, valueType))
                .cast((Class<List<Quote>>) (Object) List.class)
                .map(quotes -> {
                    Quote quote;
                    do {
                        quote = Utils.randValue(quotes);
                    } while (quote.getContent().length() > 1000);

                    final String content = quote.getContent().replace("*", "\\*");
                    final String id = quote.getId();

                    return updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                            .andThen(embed -> embed.setAuthor("Quote DansTonChat",
                                    String.format("https://danstonchat.com/%s.html", id),
                                    context.getAvatarUrl())
                                    .setThumbnail("https://i.imgur.com/5YvTlAA.png")
                                    .setDescription(FormatUtils.format(content.split("\n"), DtcCmd::format, "\n"))));
                })
                .flatMap(UpdatableMessage::send)
                .then();
    }

    private static String format(String line) {
        // Set the user name as bold
        if (line.contains(" ")) {
            final int index = line.indexOf(' ');
            return String.format("**%s** %s", line.substring(0, index), line.substring(index + 1));
        }
        return line;
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show a random quote from DansTonChat.com")
                .setSource("https://www.danstonchat.com/")
                .build();
    }
}
