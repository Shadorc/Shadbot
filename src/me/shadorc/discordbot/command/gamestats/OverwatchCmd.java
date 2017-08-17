package me.shadorc.discordbot.command.gamestats;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.temporal.ChronoUnit;

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
		String region = splitArgs[1].toLowerCase();
		String battletag = splitArgs[2];

		try {
			String url = "https://playoverwatch.com/en-gb/career/" + plateform + "/" + region + "/" + battletag.replace("#", "-");
			Document doc = NetUtils.getDoc(url);

			String icon = doc.getElementsByClass("masthead-player").select("img").first().absUrl("src");
			String level = doc.getElementsByClass("masthead-player").first().getElementsByClass("u-vertical-center").text();
			String wins = doc.getElementsByClass("masthead-detail").first().text().split(" ")[0];
			String topHero = doc.getElementsByClass("progress-category").first().getElementsByClass("title").first().text();
			String topHeroTime = doc.getElementsByClass("progress-category").first().getElementsByClass("description").first().text();
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
					.appendField("Top hero (Casual matchmaking)", topHero + " (" + topHeroTime + ")", true)
					.withFooterText("Career link: " + url);
			BotUtils.sendEmbed(builder.build(), context.getChannel());
		} catch (FileNotFoundException fnf) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " Plateform, region or Battletag is invalid.", context.getChannel());
		} catch (IOException e) {
			LogUtils.error("Something went wrong while getting information from Overwatch profil.... Please, try again later.", e, context.getChannel());
		}
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
