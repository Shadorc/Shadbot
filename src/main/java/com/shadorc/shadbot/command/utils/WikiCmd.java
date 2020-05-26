package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.api.json.wikipedia.WikipediaPage;
import com.shadorc.shadbot.api.json.wikipedia.WikipediaQuery;
import com.shadorc.shadbot.api.json.wikipedia.WikipediaResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NetUtils;
import discord4j.core.object.Embed;
import discord4j.core.spec.EmbedCreateSpec;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class WikiCmd extends BaseCmd {

    public WikiCmd() {
        super(CommandCategory.UTILS, List.of("wiki", "wikipedia"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading Wikipedia...", context.getUsername()))
                .send()
                .then(this.getWikipediaPage(arg))
                .map(page -> {
                    if (page.getExtract().endsWith("may refer to:")) {
                        return updatableMsg.setContent(
                                String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) This term refers to several results, "
                                        + "try with a more precise search.", context.getUsername()));
                    }

                    final String extract = StringUtils.abbreviate(page.getExtract(), Embed.MAX_DESCRIPTION_LENGTH);

                    return updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                            .andThen(embed -> embed.setAuthor(String.format("Wikipedia: %s", page.getTitle()),
                                    String.format("https://en.wikipedia.org/wiki/%s", page.getEncodedTitle()),
                                    context.getAvatarUrl())
                                    .setThumbnail("https://i.imgur.com/7X7Cvhf.png")
                                    .setDescription(extract)));
                })
                .switchIfEmpty(Mono.fromCallable(() -> updatableMsg.setContent(
                        String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No Wikipedia results found for `%s`",
                                context.getUsername(), arg))))
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private Mono<WikipediaPage> getWikipediaPage(String search) {
        // Wiki api doc https://en.wikipedia.org/w/api.php?action=help&modules=query%2Bextracts
        final String url = String.format("https://en.wikipedia.org/w/api.php?"
                        + "format=json"
                        + "&action=query"
                        + "&titles=%s"
                        + "&redirects=true"
                        + "&prop=extracts"
                        + "&explaintext=true"
                        + "&exintro=true"
                        + "&exsentences=5",
                NetUtils.encode(search));

        return NetUtils.get(url, WikipediaResponse.class)
                .map(WikipediaResponse::getQuery)
                .map(WikipediaQuery::getPages)
                .flatMapIterable(Map::entrySet)
                .next()
                .filter(entry -> !"-1".equals(entry.getKey()) && entry.getValue().getExtract() != null)
                .map(Map.Entry::getValue);
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Show Wikipedia description for a search.")
                .addArg("search", false)
                .setSource("https://www.wikipedia.org/")
                .build();
    }

}
