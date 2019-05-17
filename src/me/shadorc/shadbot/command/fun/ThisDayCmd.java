package me.shadorc.shadbot.command.fun;

import discord4j.core.object.Embed;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.object.message.LoadingMessage;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
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
        return Mono.fromCallable(() -> {
            final Document doc = NetUtils.getDocument(HOME_URL);

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

            return loadingMsg.setEmbed(EmbedUtils.getDefaultEmbed()
                    .andThen(embed -> embed.setAuthor(String.format("On This Day: %s", date), HOME_URL, context.getAvatarUrl())
                            .setThumbnail("http://icons.iconarchive.com/icons/paomedia/small-n-flat/1024/calendar-icon.png")
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
