package me.shadorc.shadbot.command.french;

import java.io.IOException;
import java.util.List;
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

	@Override
	public Mono<Void> execute(Context context) {
		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		return context.getAvatarUrl()
				.flatMap(avatarUrl -> {
					try {
						final String url = String.format("https://www.humour.com/blagues/");

						final List<String> jokes = NetUtils.getDoc(url).getElementsByClass("result-blague").select("p").stream()
								.map(Element::html)
								.filter(elmt -> elmt.length() < 1000)
								.collect(Collectors.toList());

						final String joke = FormatUtils.format(Utils.randValue(jokes).split("<br>"),
								line -> Jsoup.parse(line).text().trim(), "\n");

						return loadingMsg.send(EmbedUtils.getDefaultEmbed()
								.setAuthor("Blague", "https://www.humour.com/blagues/", avatarUrl)
								.setDescription(joke));

					} catch (final IOException err) {
						loadingMsg.stopTyping();
						throw Exceptions.propagate(err);
					}
				})
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show a random French joke.")
				.setSource("https://www.humour.com/blagues/")
				.build();
	}
}
