package me.shadorc.shadbot.command.utils;

import discord4j.core.object.Embed;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.wikipedia.WikipediaPage;
import me.shadorc.shadbot.api.wikipedia.WikipediaResponse;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.object.message.LoadingMessage;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
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

        final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());
        return Mono.fromCallable(() -> {
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
                    NetUtils.encode(arg));

            final WikipediaResponse wikipedia = NetUtils.get(url, WikipediaResponse.class).block();
            final Map<String, WikipediaPage> pages = wikipedia.getQuery().getPages();
            final String pageId = pages.keySet().toArray()[0].toString();
            final WikipediaPage page = pages.get(pageId);

            if ("-1".equals(pageId) || page.getExtract() == null) {
                return loadingMsg.setContent(String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No Wikipedia results found for `%s`",
                        context.getUsername(), arg));
            }

            if (page.getExtract().endsWith("may refer to:")) {
                return loadingMsg.setContent(String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) This term refers to several results, "
                        + "try with a more precise search.", context.getUsername()));
            }

            final String extract = StringUtils.abbreviate(page.getExtract(), Embed.MAX_DESCRIPTION_LENGTH);

            return loadingMsg.setEmbed(EmbedUtils.getDefaultEmbed()
                    .andThen(embed -> embed.setAuthor(String.format("Wikipedia: %s", page.getTitle()),
                            String.format("https://en.wikipedia.org/wiki/%s", page.getTitle().replace(" ", "_")),
                            context.getAvatarUrl())
                            .setThumbnail("https://upload.wikimedia.org/wikipedia/commons/thumb/7/77/Wikipedia_svg_logo.svg/1024px-Wikipedia_svg_logo.svg.png")
                            .setDescription(extract)));
        })
                .flatMap(LoadingMessage::send)
                .doOnTerminate(loadingMsg::stopTyping)
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show Wikipedia description for a search.")
                .addArg("search", false)
                .build();
    }

}
