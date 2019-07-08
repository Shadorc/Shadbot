package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.LoadingMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NetUtils;
import discord4j.core.object.Embed;
import discord4j.core.spec.EmbedCreateSpec;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ThisDayCmd extends BaseCmd {

    private static final String HOME_URL = "https://www.onthisday.com/";

    public ThisDayCmd() {
        super(CommandCategory.FUN, List.of("this_day", "this-day", "thisday"), "td");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

        return NetUtils.get(HOME_URL)
                .map(Jsoup::parse)
                .map(doc -> {
                    final String date = doc.getElementsByClass("date-large")
                            .first()
                            .attr("datetime");

                    final Elements eventsElmt = doc.getElementsByClass("event-list event-list--with-advert")
                            .first()
                            .getElementsByClass("event-list__item");

                    final String events = eventsElmt.stream()
                            .map(Element::html)
                            .map(html -> html.replaceAll("<b>|</b>", "**"))
                            .map(Jsoup::parse)
                            .map(Document::text)
                            .collect(Collectors.joining("\n\n"));

                    return loadingMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                            .andThen(embed -> embed.setAuthor(String.format("On This Day: %s", date), HOME_URL, context.getAvatarUrl())
                                    .setThumbnail("https://i.imgur.com/FdfyJDD.png")
                                    .setDescription(StringUtils.abbreviate(events, Embed.MAX_DESCRIPTION_LENGTH))));
                })
                .flatMap(LoadingMessage::send)
                .doOnTerminate(loadingMsg::stopTyping)
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show significant events of the day.")
                .setSource(HOME_URL)
                .build();
    }
}
