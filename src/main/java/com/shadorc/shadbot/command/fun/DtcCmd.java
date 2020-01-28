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

    public DtcCmd() {
        super(CommandCategory.FUN, List.of("dtc"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        final String url = String.format("%s?key=%s&format=json",
                HOME_URL, CredentialManager.getInstance().get(Credential.DTC_API_KEY));

        final JavaType valueType = Utils.MAPPER.getTypeFactory().constructCollectionType(List.class, Quote.class);
        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading quote...", context.getUsername()))
                .send()
                .<List<Quote>>then(NetUtils.get(url, valueType))
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
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
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
        return HelpBuilder.create(this, context)
                .setDescription("Show a random quote from DansTonChat.com")
                .setSource("https://www.danstonchat.com/")
                .build();
    }
}
