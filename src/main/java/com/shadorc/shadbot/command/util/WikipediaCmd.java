package com.shadorc.shadbot.command.util;

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

import java.util.Locale;
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

        return context.createFollowupMessage(Emoji.HOURGLASS, context.localize("wikipedia.loading"))
                .then(WikipediaCmd.getWikipediaPage(context.getLocale(), word))
                .flatMap(page -> {
                    if (page.extract().orElseThrow().endsWith("may refer to:")) {
                        return context.editFollowupMessage(Emoji.MAGNIFYING_GLASS,
                                context.localize("wikipedia.several.results"));
                    }

                    return context.editFollowupMessage(WikipediaCmd.formatEmbed(context, page));
                })
                .switchIfEmpty(context.editFollowupMessage(Emoji.MAGNIFYING_GLASS, context.localize("wikipedia.not.found")
                        .formatted(word)));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(Context context, WikipediaPage page) {
        final String extract = StringUtil.abbreviate(page.extract().orElseThrow(), Embed.MAX_DESCRIPTION_LENGTH);
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(context.localize("wikipedia.title").formatted(page.title()),
                        "https://%s.wikipedia.org/wiki/%s"
                                .formatted(context.getLocale().getLanguage(),
                                        page.getEncodedTitle()), context.getAuthorAvatar())
                        .setThumbnail("https://i.imgur.com/7X7Cvhf.png")
                        .setDescription(extract));
    }

    private static Mono<WikipediaPage> getWikipediaPage(Locale locale, String search) {
        // Wiki api doc https://en.wikipedia.org/w/api.php?action=help&modules=query%2Bextracts
        final String url = "https://%s.wikipedia.org/w/api.php?".formatted(locale.getLanguage())
                + "format=json"
                + "&action=query"
                + "&titles=%s".formatted(NetUtil.encode(search))
                + "&redirects=true"
                + "&prop=extracts"
                + "&explaintext=true"
                + "&exintro=true"
                + "&exsentences=5";

        return RequestHelper.fromUrl(url)
                .to(WikipediaResponse.class)
                .map(WikipediaResponse::query)
                .map(WikipediaQuery::pages)
                .flatMapIterable(Map::entrySet)
                .next()
                .filter(entry -> !"-1".equals(entry.getKey()) && entry.getValue().extract().isPresent())
                .map(Map.Entry::getValue);
    }

}
