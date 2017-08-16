package me.shadorc.discordbot.command.french;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.List;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.HtmlUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.StringUtils;
import sx.blah.discord.util.EmbedBuilder;

public class JokeCmd extends AbstractCommand {

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

		boolean success = false;

		try {
			String htmlPage = HtmlUtils.getHTML("https://www.blague-drole.net/blagues-" + MathUtils.rand(1, 25) + ".html?tri=top");
			List<String> jokesList = HtmlUtils.getAllSubstring(htmlPage, " \"description\": \"", "</script>");

			String joke;
			do {
				joke = jokesList.get(MathUtils.rand(jokesList.size()));
				joke = joke.substring(0, joke.lastIndexOf('"')).replace("&amp;", "&").replace("\n\n", "\n").trim();
				joke = StringUtils.convertHtmlToUTF8(joke);
			} while(joke.length() > 1800);

			BotUtils.sendMessage("```" + joke + "```", context.getChannel());

		} catch (IOException ignored) {
			success = false;
		}

		if(!success) {
			try {
				String htmlPage = HtmlUtils.getHTML("http://www.une-blague.com/blagues-courtes.html?page=2&cat=16&p=" + MathUtils.rand(1, 5) + "&call=1");
				List<String> jokesList = HtmlUtils.getAllSubstring(htmlPage, "class=\"texte \">", "</h4>");

				String joke;
				do {
					joke = jokesList.get(MathUtils.rand(jokesList.size()));
					joke = joke.replace("<br>", "\n").replace("<br />", "\n").trim();
				} while(joke.length() > 1800);

				BotUtils.sendMessage("```" + joke + "```", context.getChannel());
			} catch (IOException e) {
				LogUtils.error("An error occured while getting a joke. :(", e, context.getChannel());
			}
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
