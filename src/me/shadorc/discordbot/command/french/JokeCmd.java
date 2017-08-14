package me.shadorc.discordbot.command.french;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.StringUtils;
import sx.blah.discord.util.EmbedBuilder;

public class JokeCmd extends Command {

	private final RateLimiter rateLimiter;

	public JokeCmd() {
		super(false, "blague", "joke");
		this.rateLimiter = new RateLimiter(2, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isLimited(context.getGuild(), context.getAuthor())) {
			if(!rateLimiter.isWarned(context.getGuild(), context.getAuthor())) {
				rateLimiter.warn("Take it easy, don't spam :)", context);
			}
			return;
		}

		try {
			String htmlPage = HtmlUtils.getHTML("https://www.blague-drole.net/blagues-" + MathUtils.rand(1, 25) + ".html?tri=top");
			List<String> jokesList = HtmlUtils.getAllSubstring(htmlPage, " \"description\": \"", "</script>");
			String joke = jokesList.get(MathUtils.rand(jokesList.size()));
			joke = joke.substring(0, joke.lastIndexOf("\"")).replace("&amp;", "&").trim();
			BotUtils.sendMessage("```" + StringUtils.convertHtmlToUTF8(joke).replace("\n\n", "\n") + "```", context.getChannel());
		} catch (SocketTimeoutException e) {
			BotUtils.sendMessage(Emoji.HOURGLASS + " Jokes website is busy right now, please try again later.", context.getChannel());
			Log.warn("SocketTimeoutException while getting a joke (" + e.getMessage() + ").");
		} catch (IOException e) {
			Log.error("An error occured while getting a joke.", e, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show a random joke from blague-drole.net.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
