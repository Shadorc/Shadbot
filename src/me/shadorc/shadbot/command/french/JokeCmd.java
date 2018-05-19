package me.shadorc.shadbot.command.french;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;

@RateLimited
@Command(category = CommandCategory.FRENCH, names = { "blague", "joke" })
public class JokeCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		LoadingMessage loadingMsg = new LoadingMessage("Loading joke...", context.getChannel());
		loadingMsg.send();

		try {
			String url = String.format("http://www.une-blague.com/blagues-courtes.html?&p=%d", ThreadLocalRandom.current().nextInt(1, 6));
			Document doc = NetUtils.getDoc(url);

			List<String> jokes = doc.getElementsByClass("texte ").stream()
					.map(Element::html)
					.filter(elmt -> elmt.length() < 1000)
					.collect(Collectors.toList());

			String jokeHtml = jokes.get(ThreadLocalRandom.current().nextInt(jokes.size()));
			String joke = FormatUtils.format(jokeHtml.split("<br>"), line -> Jsoup.parse(line).text().trim(), "\n");

			EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed("Blague", "http://www.une-blague.com/")
					.setDescription(joke);
			loadingMsg.edit(embed);

		} catch (IOException err) {
			loadingMsg.delete();
			ExceptionUtils.handle("getting a joke", context, err);
		}
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show a random French joke.")
				.setSource("http://www.une-blague.com")
				.build();
	}
}
