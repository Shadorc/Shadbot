package me.shadorc.shadbot.command.fun;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.FUN, names = { "this_day", "this-day", "thisday" }, alias = "td")
public class ThisDayCmd extends AbstractCommand {

	private static final String HOME_URL = "http://www.onthisday.com/";

	@Override
	public void execute(Context context) {
		try {
			Document doc = NetUtils.getDoc(HOME_URL);

			String date = doc.getElementsByClass("date-large").first().attr("datetime");

			Elements eventsEl = doc.getElementsByClass("event-list event-list--with-advert").first().getElementsByClass("event-list__item");
			StringBuilder strBuilder = new StringBuilder();
			for(Element eventEl : eventsEl) {
				strBuilder.append(Jsoup.parse(eventEl.html().replaceAll("<b>|</b>", "**")).text() + "\n\n");
			}

			String result = StringUtils.truncate(strBuilder.toString(), EmbedBuilder.DESCRIPTION_CONTENT_LIMIT);
			EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
					.withAuthorName(String.format("On This Day (%s)", date))
					.withUrl(HOME_URL)
					.withThumbnail("http://icons.iconarchive.com/icons/paomedia/small-n-flat/1024/calendar-icon.png")
					.appendDescription(result);

			BotUtils.sendMessage(embed.build(), context.getChannel());

		} catch (IOException err) {
			ExceptionUtils.handle("getting events", context, err);
		}
	}

	@Override
	public EmbedObject getHelp(Context context) {
		return new HelpBuilder(this, context.getPrefix())
				.setDescription("Show significant events of the day.")
				.build();
	}
}
