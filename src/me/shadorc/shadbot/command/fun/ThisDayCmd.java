package me.shadorc.shadbot.command.fun;

import java.io.IOException;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.LoadingMessage;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

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

			String date = doc.getElementsByClass("date-large").first().attr("datetime");

			Elements eventsElmt = doc.getElementsByClass("event-list event-list--with-advert").first().getElementsByClass("event-list__item");

			String events = eventsElmt.stream()
					.map(elmt -> Jsoup.parse(elmt.html().replaceAll("<b>|</b>", "**")).text())
					.collect(Collectors.joining("\n\n"));

			EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
					.withAuthorName(String.format("On This Day (%s)", date))
					.withUrl(HOME_URL)
					.withThumbnail("http://icons.iconarchive.com/icons/paomedia/small-n-flat/1024/calendar-icon.png")
					.appendDescription(StringUtils.truncate(events, EmbedBuilder.DESCRIPTION_CONTENT_LIMIT));

			loadingMsg.edit(embed.build());

		} catch (IOException err) {
			Utils.handle("getting events", context, err);
		}
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show significant events of the day.")
				.setSource(HOME_URL)
				.build();
	}
}
