package com.shadorc.shadbot.command.util;

import com.shadorc.shadbot.api.json.wikipedia.WikipediaPage;
import com.shadorc.shadbot.api.json.wikipedia.WikipediaQuery;
import com.shadorc.shadbot.api.json.wikipedia.WikipediaResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.i18n.I18nContext;
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

        return context.reply(Emoji.HOURGLASS, context.localize("wikipedia.loading"))
                .then(WikipediaCmd.getWikipediaPage(context, word))
                .flatMap(page -> {
                    if (page.getExtract().endsWith("may refer to:")) {
                        return context.editReply(Emoji.MAGNIFYING_GLASS,
                                context.localize("wikipedia.several.results"));
                    }

                    return context.editReply(WikipediaCmd.formatEmbed(context, page));
                })
                .switchIfEmpty(context.editReply(Emoji.MAGNIFYING_GLASS, context.localize("wikipedia.not.found")
                        .formatted(word)));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(Context context, WikipediaPage page) {
        final String extract = StringUtil.abbreviate(page.getExtract(), Embed.MAX_DESCRIPTION_LENGTH);
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(context.localize("wikipedia.title").formatted(page.getTitle()),
                        "https://%s.wikipedia.org/wiki/%s"
                                .formatted(context.getLocale().getLanguage(),
                                        page.getEncodedTitle()), context.getAuthorAvatar())
                        .setThumbnail("https://i.imgur.com/7X7Cvhf.png")
                        .setDescription(extract));
    }

    private static Mono<WikipediaPage> getWikipediaPage(I18nContext context, String search) {
        // Wiki api doc https://en.wikipedia.org/w/api.php?action=help&modules=query%2Bextracts
        final String url = String.format("https://%s.wikipedia.org/w/api.php?"
                        + "format=json"
                        + "&action=query"
                        + "&titles=%s"
                        + "&redirects=true"
                        + "&prop=extracts"
                        + "&explaintext=true"
                        + "&exintro=true"
                        + "&exsentences=5",
                context.getLocale().getLanguage(), NetUtil.encode(search));

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
