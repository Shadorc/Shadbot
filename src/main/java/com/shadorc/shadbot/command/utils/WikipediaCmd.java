package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.api.json.wikipedia.WikipediaPage;
import com.shadorc.shadbot.api.json.wikipedia.WikipediaQuery;
import com.shadorc.shadbot.api.json.wikipedia.WikipediaResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.NetUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.StringUtil;
import discord4j.core.object.Embed;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Consumer;

public class WikipediaCmd extends BaseCmd {

    public WikipediaCmd() {
        super(CommandCategory.UTILS, "wikipedia", "Search for Wikipedia article");
        this.addOption("word", "The word to search", true, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        final String word = context.getOptionAsString("word").orElseThrow();

        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading Wikipedia article...", context.getAuthorName())
                .flatMap(messageId -> WikipediaCmd.getWikipediaPage(word)
                        .flatMap(page -> {
                            if (page.getExtract().endsWith("may refer to:")) {
                                return context.editFollowupMessage(messageId,
                                        Emoji.MAGNIFYING_GLASS + " (**%s**) This term refers to several results, "
                                                + "try to refine your search.", context.getAuthorName());
                            }

                            return context.editFollowupMessage(messageId,
                                    WikipediaCmd.formatEmbed(page, context.getAuthorAvatar()));
                        })
                        .switchIfEmpty(context.editFollowupMessage(messageId,
                                Emoji.MAGNIFYING_GLASS + " (**%s**) No Wikipedia article matching word `%s`",
                                context.getAuthorName(), word)));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(final WikipediaPage page, final String avatarUrl) {
        final String extract = StringUtil.abbreviate(page.getExtract(), Embed.MAX_DESCRIPTION_LENGTH);
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor("Wikipedia: %s".formatted(page.getTitle()),
                        "https://en.wikipedia.org/wiki/%s".formatted(page.getEncodedTitle()), avatarUrl)
                        .setThumbnail("https://i.imgur.com/7X7Cvhf.png")
                        .setDescription(extract));
    }

    private static Mono<WikipediaPage> getWikipediaPage(String search) {
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
                NetUtil.encode(search));

        return RequestHelper.fromUrl(url)
                .to(WikipediaResponse.class)
                .map(WikipediaResponse::getQuery)
                .map(WikipediaQuery::getPages)
                .flatMapIterable(Map::entrySet)
                .next()
                .filter(entry -> !"-1".equals(entry.getKey()) && entry.getValue().getExtract() != null)
                .map(Map.Entry::getValue);
    }

}
