package me.shadorc.shadbot.command.french;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.FRENCH, names = { "blague", "joke" })
public class JokeCmd extends AbstractCommand {

	private static final String HOME_URL = "https://www.humour.com/blagues/";

	@Override
	public Mono<Void> execute(Context context) {
		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			final List<String> jokes = NetUtils.getDoc(HOME_URL)
					.getElementsByClass("gag__content")
					.stream()
					.map(Element::html)
					.filter(elmt -> elmt.length() < 1000)
					.collect(Collectors.toList());

			final String joke = FormatUtils.format(Utils.randValue(jokes).split("<br>"),
					line -> Jsoup.parse(line).text().trim(), "\n");

			final Consumer<EmbedCreateSpec> embedConsumer = EmbedUtils.getDefaultEmbed()
					.andThen(embed -> embed.setAuthor("Blague", "https://www.humour.com/blagues/", context.getAvatarUrl())
							.setDescription(joke));

			return loadingMsg.send(embedConsumer).then();

		} catch (final Exception err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show a random French joke.")
				.setSource("https://www.humour.com/blagues/")
				.build();
	}
}
