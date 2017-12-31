package me.shadorc.shadbot.command.french;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.MathUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.FRENCH, names = { "blague", "joke" })
public class JokeCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		try {
			String url = String.format("http://www.une-blague.com/blagues-courtes.html?&p=%d", MathUtils.rand(1, 5));
			Document doc = NetUtils.getDoc(url);
			Elements jokesElements = doc.getElementsByClass("texte ");

			String joke;
			do {
				Element element = jokesElements.get(MathUtils.rand(jokesElements.size()));
				joke = FormatUtils.formatArray(element.html().split("<br>"), line -> Jsoup.parse(line.toString()).text().trim(), "\n");
			} while(joke.length() > 1000);

			EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
					.withAuthorName("Blague")
					.withUrl("http://www.une-blague.com/")
					.appendDescription(joke);
			BotUtils.sendMessage(embed.build(), context.getChannel());

		} catch (IOException err) {
			ExceptionUtils.handle("getting a joke", context, err);
		}
	}

	@Override
	public EmbedObject getHelp(Context context) {
		return new HelpBuilder(this, context.getPrefix())
				.setDescription("Show a random French joke.")
				.build();
	}
}
