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
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.LoadingMessage;

@RateLimited
@Command(category = CommandCategory.FUN, names = { "this_day", "this-day", "thisday" }, alias = "td")
public class ThisDayCmd extends AbstractCommand {

	private static final String HOME_URL = "http://www.onthisday.com/";

	@Override
	public void execute(Context context) {
		LoadingMessage loadingMsg = new LoadingMessage("Loading information...", context.getChannel());
		loadingMsg.send();

		try {
			Document doc = NetUtils.getDoc(HOME_URL);

			String date = doc.getElementsByClass("date-large")
					.first()
					.attr("datetime");

			Elements eventsElmt = doc.getElementsByClass("event-list event-list--with-advert")
					.first()
					.getElementsByClass("event-list__item");

			String events = eventsElmt.stream()
					.map(Element::html)
					.map(html -> html.replaceAll("<b>|</b>", "**"))
					.map(Jsoup::parse)
					.map(Document::text)
					.collect(Collectors.joining("\n\n"));

			EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed(String.format("On This Day (%s)", date), HOME_URL)
					.setThumbnail("http://icons.iconarchive.com/icons/paomedia/small-n-flat/1024/calendar-icon.png")
					.setDescription(StringUtils.truncate(events, EmbedCreateSpec.DESCRIPTION_CONTENT_LIMIT));

			loadingMsg.edit(embed);

		} catch (IOException err) {
			loadingMsg.delete();
			ExceptionUtils.handle("getting events", context, err);
		}
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show significant events of the day.")
				.setSource(HOME_URL)
				.build();
	}
}
