package me.shadorc.discordbot.command.french;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class JokeCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public JokeCmd() {
		super(Role.USER, "blague", "joke");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		try {
			Document doc = NetUtils.getDoc("http://www.une-blague.com/blagues-courtes.html?&p=" + MathUtils.rand(1, 5));
			Elements jokesElements = doc.getElementsByClass("texte ");
			String joke;
			do {
				Element element = jokesElements.get(MathUtils.rand(jokesElements.size()));
				joke = StringUtils.formatList(Arrays.asList(element.html().split("<br>")), line -> Jsoup.parse(line).text().trim(), "\n");
			} while(joke.length() > 1000);

			EmbedBuilder embed = Utils.getDefaultEmbed()
					.withAuthorName("Blague")
					.withUrl("http://www.une-blague.com/")
					.appendDescription(joke);
			BotUtils.sendEmbed(embed.build(), context.getChannel());

		} catch (IOException err) {
			LogUtils.error("Something went wrong while getting a joke... Please, try again later.", err, context);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show a random french joke.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
