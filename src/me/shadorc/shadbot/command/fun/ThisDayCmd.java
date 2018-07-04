package me.shadorc.shadbot.command.fun;

import java.io.IOException;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.FUN, names = { "this_day", "this-day", "thisday" }, alias = "td")
public class ThisDayCmd extends AbstractCommand {

	private static final String HOME_URL = "http://www.onthisday.com/";

	@Override
	public Mono<Void> execute(Context context) {
		LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			Document doc = NetUtils.getDoc(HOME_URL);

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

			return context.getAuthorAvatarUrl()
					.map(avatarUrl -> {
						return EmbedUtils.getDefaultEmbed()
								.setAuthor(String.format("On This Day (%s)", date), HOME_URL, avatarUrl)
								.setThumbnail("http://icons.iconarchive.com/icons/paomedia/small-n-flat/1024/calendar-icon.png")
								.setDescription(StringUtils.truncate(events, DiscordUtils.DESCRIPTION_CONTENT_LIMIT));
					})
					.map(loadingMsg::send)
					.then();

		} catch (IOException err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show significant events of the day.")
				.setSource(HOME_URL)
				.build();
	}
}
