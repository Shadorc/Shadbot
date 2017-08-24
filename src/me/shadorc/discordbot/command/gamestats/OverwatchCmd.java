package me.shadorc.discordbot.command.gamestats;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.NetUtils;
import sx.blah.discord.util.EmbedBuilder;

public class OverwatchCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public OverwatchCmd() {
		super(Role.USER, "overwatch", "ow");
		this.rateLimiter = new RateLimiter(2, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		String[] splitArgs = context.getArg().split(" ", 3);
		if(splitArgs.length != 3) {
			throw new MissingArgumentException();
		}

		if(rateLimiter.isLimited(context.getGuild(), context.getAuthor())) {
			if(!rateLimiter.isWarned(context.getGuild(), context.getAuthor())) {
				rateLimiter.warn("Take it easy, don't spam :)", context);
			}
			return;
		}

		String plateform = splitArgs[0].toLowerCase();
		if(!Arrays.asList("pc", "psn", "xbl").contains(plateform)) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " Plateform is invalid. Options: pc, psn, xbl.", context.getChannel());
			return;
		}

		String region = splitArgs[1].toLowerCase();
		if(!Arrays.asList("eu", "us", "cn", "kr").contains(region)) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " Region is invalid. Options: eu, us, cn, kr.", context.getChannel());
			return;
		}

		String battletag = splitArgs[2];

		try {
			String url = "https://playoverwatch.com/en-gb/career"
					+ "/" + plateform
					+ "/" + region
					+ "/" + URLEncoder.encode(battletag.replace("#", "-"), "UTF-8");
			Document doc = NetUtils.getDoc(url);

			String icon = doc.getElementsByClass("masthead-player").select("img").first().absUrl("src");
			String level = doc.getElementsByClass("masthead-player").first().getElementsByClass("u-vertical-center").text();
			String wins = doc.getElementsByClass("masthead-detail").first().text().split(" ")[0];
			String topTimePlayed = this.getTopThreeHeroes(doc.getElementsByClass("progress-category").get(0));
			String topEliminationsPerKill = this.getTopThreeHeroes(doc.getElementsByClass("progress-category").get(3));
			String timePlayed = doc.getElementsByClass("column xs-12 md-6 xl-4").get(6).select("td").get(1).text();
			String rank = null;

			Element rankElem = doc.getElementsByClass("u-align-center h6").first();
			if(rankElem != null) {
				rank = rankElem.text();
			}

			EmbedBuilder builder = new EmbedBuilder()
					.setLenient(true)
					.withAuthorName("Overwatch Stats")
					.withAuthorIcon("http://vignette4.wikia.nocookie.net/overwatch/images/b/bd/Overwatch_line_art_logo_symbol-only.png")
					.withUrl(url)
					.withThumbnail(icon)
					.withColor(Config.BOT_COLOR)
					.withDesc("Stats for user **" + battletag + "**.")
					.appendField("Level", level, true)
					.appendField("Competitive rank", rank, true)
					.appendField("Wins", wins, true)
					.appendField("Game time", timePlayed, true)
					.appendField("Top hero (Time played)", topTimePlayed, true)
					.appendField("Top hero (Eliminations per life)", topEliminationsPerKill, true);
			BotUtils.sendEmbed(builder.build(), context.getChannel());
		} catch (HttpStatusException e) {
			BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " This user doesn't play to Overwatch or doesn't exist.", context.getChannel());
		} catch (IOException e) {
			LogUtils.error("Something went wrong while getting information from Overwatch profil.... Please, try again later.", e, context.getChannel());
		}
	}

	private String getTopThreeHeroes(Element element) {
		StringBuilder strBuilder = new StringBuilder();
		for(int i = 0; i < Math.min(element.getElementsByClass("bar-text").size(), 3); i++) {
			String hero = element.getElementsByClass("title").get(i).text();
			String desc = element.getElementsByClass("description").get(i).text();
			strBuilder.append("**" + (i + 1) + "**. " + hero + " (" + desc + ")\n");
		}
		return strBuilder.toString();
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show user stats for Overwatch.**")
				.appendField("Usage", context.getPrefix() + "overwatch <pc|psn|xbl> <eu|us|cn|kr> <battletag#0000>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
