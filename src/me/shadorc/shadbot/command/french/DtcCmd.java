package me.shadorc.shadbot.command.french;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.fasterxml.jackson.databind.JavaType;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.dtc.Quote;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.credential.Credential;
import me.shadorc.shadbot.data.credential.Credentials;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.FRENCH, names = { "dtc" })
public class DtcCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			final URL url = new URL(String.format("https://api.danstonchat.com/0.3/view/random?key=%s&format=json",
					Credentials.get(Credential.DTC_API_KEY)));

			final JavaType valueType = Utils.MAPPER.getTypeFactory().constructCollectionType(List.class, Quote.class);
			final List<Quote> quotes = Utils.MAPPER.readValue(url, valueType);

			Quote quote;
			do {
				quote = Utils.randValue(quotes);
			} while(quote.getContent().length() > 1000);

			final String content = quote.getContent().replace("*", "\\*");
			final String id = quote.getId();

			return context.getAvatarUrl()
					.flatMap(avatarUrl -> loadingMsg.send(EmbedUtils.getDefaultEmbed()
							.setAuthor("Quote DansTonChat",
									String.format("https://danstonchat.com/%s.html", id),
									avatarUrl)
							.setThumbnail("https://danstonchat.com/themes/danstonchat/images/logo2.png")
							.setDescription(FormatUtils.format(content.split("\n"), this::format, "\n"))))
					.then();

		} catch (final IOException err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}
	}

	private String format(String line) {
		// Set the user name as bold
		if(line.contains(" ")) {
			final int index = line.indexOf(' ');
			return String.format("**%s** %s", line.substring(0, index), line.substring(index + 1));
		}
		return line;
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show a random quote from DansTonChat.com")
				.setSource("https://www.danstonchat.com/")
				.build();
	}
}
