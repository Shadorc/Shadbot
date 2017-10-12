package me.shadorc.discordbot.command.fun;

import java.io.IOException;
import java.time.temporal.ChronoUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class ThisDayCmd extends AbstractCommand {

	private static final String HOME_URL = "http://www.onthisday.com/";
	private final RateLimiter rateLimiter;

	public ThisDayCmd() {
		super(CommandCategory.FUN, Role.USER, "this_day", "this-day", "thisday");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		try {
			Document doc = NetUtils.getDoc(HOME_URL);
			Elements eventsEl = doc.getElementsByClass("event-list event-list--with-advert").first().getElementsByClass("event-list__item");
			StringBuilder strBuilder = new StringBuilder();
			for(Element eventEl : eventsEl) {
				strBuilder.append(Jsoup.parse(eventEl.html().replaceAll("<b>|</b>", "**")).text() + "\n\n");
			}

			if(strBuilder.length() > EmbedBuilder.DESCRIPTION_CONTENT_LIMIT) {
				strBuilder.setLength(EmbedBuilder.DESCRIPTION_CONTENT_LIMIT - 3);
				strBuilder.append("...");
			}

			EmbedBuilder embed = Utils.getDefaultEmbed()
					.withAuthorName("On This Day")
					.withUrl(HOME_URL)
					.withThumbnail("http://icons.iconarchive.com/icons/paomedia/small-n-flat/1024/calendar-icon.png")
					.appendDescription(strBuilder.toString());

			BotUtils.sendMessage(embed.build(), context.getChannel());

		} catch (IOException err) {
			LogUtils.error("Something went wrong while getting events... Please, try again later.", err, context);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show significant events of the day.**");
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
