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
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.FRENCH, names = { "blague", "joke" })
public class JokeCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

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
			loadingMsg.send(embed);

		} catch (IOException err) {
			loadingMsg.send(ExceptionUtils.handleAndGet("getting a joke", context, err));
		}
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show a random French joke.")
				.setSource("http://www.une-blague.com")
				.build();
	}
}
