package me.shadorc.discordbot.command.french;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.StringUtils;
import sx.blah.discord.util.EmbedBuilder;

public class JokeCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public JokeCmd() {
		super(Role.USER, "blague", "joke");
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
			String joke = this.getJoke(
					"https://www.blague-drole.net/blagues-" + MathUtils.rand(1, 25) + ".html?tri=top",
					"text-justify texte");
			BotUtils.sendMessage("```" + joke + "```", context.getChannel());

		} catch (IOException err) {
			try {
				String joke = this.getJoke(
						"http://www.une-blague.com/blagues-courtes.html?page=2&cat=16&p=" + MathUtils.rand(1, 5) + "&call=1",
						"texte ");
				BotUtils.sendMessage("```" + joke + "```", context.getChannel());

			} catch (IOException e) {
				LogUtils.error("Something went wrong while getting a joke... Please, try again later.", e, context.getChannel());
			}
		}
	}

	private String getJoke(String url, String className) throws IOException {
		Document doc = NetUtils.getDoc(url);
		Elements jokesElements = doc.getElementsByClass(className);
		String joke;
		do {
			Element element = jokesElements.get(MathUtils.rand(jokesElements.size()));
			joke = StringUtils.formatList(Arrays.asList(element.html().split("<p>|<br>")), line -> line.trim(), "\n");
			joke = StringEscapeUtils.unescapeHtml4(joke.replace("\n\n", "\n").replaceAll("\\<.*?>", ""));
		} while(joke.length() > 1000);
		return joke;
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show a random french joke.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
