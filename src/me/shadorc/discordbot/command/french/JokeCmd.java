package me.shadorc.discordbot.command.french;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.ExceptionUtils;
import me.shadorc.discordbot.utils.FormatUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class JokeCmd extends AbstractCommand {

	public JokeCmd() {
		super(CommandCategory.FRENCH, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "blague", "joke");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		try {
			Document doc = NetUtils.getDoc("http://www.une-blague.com/blagues-courtes.html?&p=" + MathUtils.rand(1, 5));
			Elements jokesElements = doc.getElementsByClass("texte ");
			String joke;
			do {
				Element element = jokesElements.get(MathUtils.rand(jokesElements.size()));
				joke = FormatUtils.formatArray(element.html().split("<br>"), line -> Jsoup.parse((String) line).text().trim(), "\n");
			} while(joke.length() > 1000);

			EmbedBuilder embed = Utils.getDefaultEmbed()
					.withAuthorName("Blague")
					.withUrl("http://www.une-blague.com/")
					.appendDescription(joke);
			BotUtils.sendMessage(embed.build(), context.getChannel());

		} catch (IOException err) {
			ExceptionUtils.manageException("getting a joke", context, err);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show a random French joke.**");
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
