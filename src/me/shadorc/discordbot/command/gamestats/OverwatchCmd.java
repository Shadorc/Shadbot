package me.shadorc.discordbot.command.gamestats;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Log;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.HtmlUtils;
import sx.blah.discord.util.EmbedBuilder;

public class OverwatchCmd extends Command {

	private final RateLimiter rateLimiter;

	public OverwatchCmd() {
		super(false, "overwatch", "ow");
		this.rateLimiter = new RateLimiter(2, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(context.getArg() == null) {
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
			String html = HtmlUtils.getHTML(url);

			String icon = HtmlUtils.parseTextHTML(html, "<div class=\"masthead-player\"><img src=\"", "<div class=\"masthead-player\"><img src=\"", "\" class=\"player-portrait\">");
			String level = HtmlUtils.parseTextHTML(html, "class=\"player-level\">", "<div class=\"u-vertical-center\">", "</div>");
			String wins = HtmlUtils.parseTextHTML(html, "<p class=\"masthead-detail h4\"><span>", "<p class=\"masthead-detail h4\"><span>", " games won</span></p>");
			String topHero = HtmlUtils.parseTextHTML(html, "<div class=\"title\">", "<div class=\"title\">", "</div>");
			String topHeroTime = HtmlUtils.parseTextHTML(html, "<div class=\"description\">", "<div class=\"description\">", "</div>");
			String timePlayed = HtmlUtils.parseTextHTML(html, "<td>Time Played</td>", "<td>Time Played</td><td>", "</td>");
			String rank = HtmlUtils.parseTextHTML(html, "<div class=\"u-align-center h6\">", "<div class=\"u-align-center h6\">", "</div>");

			EmbedBuilder builder = new EmbedBuilder()
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
					.withFooterText("Career link: " + url.toString());
			BotUtils.sendEmbed(builder.build(), context.getChannel());
		} catch (FileNotFoundException fnf) {
			BotUtils.sendMessage(Emoji.WARNING + " Plateform, region or Battletag is invalid.", context.getChannel());
		} catch (IOException e) {
			Log.error("An error occured while getting information from Overwatch profil, please try again later.", e, context.getChannel());
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
