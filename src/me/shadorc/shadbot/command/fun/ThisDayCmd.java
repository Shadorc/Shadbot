package me.shadorc.shadbot.command.fun;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import discord4j.core.object.Embed;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.FUN, names = { "this_day", "this-day", "thisday" }, alias = "td")
public class ThisDayCmd extends AbstractCommand {

	private static final String HOME_URL = "http://www.onthisday.com/";

	@Override
	public Mono<Void> execute(Context context) {
		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			final Document doc = NetUtils.getDoc(HOME_URL);

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

			final Consumer<EmbedCreateSpec> embedConsumer = EmbedUtils.getDefaultEmbed()
					.andThen(embed -> embed.setAuthor(String.format("On This Day: %s", date), HOME_URL, context.getAvatarUrl())
							.setThumbnail("http://icons.iconarchive.com/icons/paomedia/small-n-flat/1024/calendar-icon.png")
							.setDescription(StringUtils.abbreviate(events, Embed.MAX_DESCRIPTION_LENGTH)));
			
			return loadingMsg.send(embedConsumer).then();

		} catch (final IOException err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show significant events of the day.")
				.setSource(HOME_URL)
				.build();
	}
}
