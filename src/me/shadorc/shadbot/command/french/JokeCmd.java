package me.shadorc.shadbot.command.french;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.object.message.LoadingMessage;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class JokeCmd extends BaseCmd {

	private static final String HOME_URL = "https://www.humour.com/blagues/";

	public JokeCmd() {
		super(CommandCategory.FRENCH, List.of("blague", "joke"));
		this.setDefaultRateLimiter();
	}

	@Override
	public Mono<Void> execute(Context context) {
		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		return Mono.fromCallable(() -> {
			final List<String> jokes = NetUtils.getDoc(HOME_URL)
					.getElementsByClass("gag__content")
					.stream()
					.map(Element::html)
					.filter(elmt -> elmt.length() < 1000)
					.collect(Collectors.toList());

			final String joke = FormatUtils.format(Utils.randValue(jokes).split("<br>"),
					line -> Jsoup.parse(line).text().trim(), "\n");

			return loadingMsg.setEmbed(EmbedUtils.getDefaultEmbed()
					.andThen(embed -> embed.setAuthor("Blague", "https://www.humour.com/blagues/", context.getAvatarUrl())
							.setDescription(joke)));
		})
				.flatMap(LoadingMessage::send)
				.doOnTerminate(loadingMsg::stopTyping)
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show a random French joke.")
				.setSource("https://www.humour.com/blagues/")
				.build();
	}
}
