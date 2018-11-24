package me.shadorc.shadbot.command.image;

import java.io.IOException;
import java.net.URL;

import org.apache.http.HttpStatus;
import org.jsoup.HttpStatusException;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.api.image.giphy.GiphyResponse;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.apikey.APIKey;
import me.shadorc.shadbot.data.apikey.APIKeys;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.IMAGE, names = { "gif" })
public class GifCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			final URL url = new URL(String.format("https://api.giphy.com/v1/gifs/random?api_key=%s&tag=%s",
					APIKeys.get(APIKey.GIPHY_API_KEY), NetUtils.encode(context.getArg().orElse(""))));

			final GiphyResponse giphy = Utils.MAPPER.readValue(url, GiphyResponse.class);

			if(giphy.getGifs() == null) {
				throw new HttpStatusException("Giphy did not return valid JSON.", HttpStatus.SC_SERVICE_UNAVAILABLE, url.toString());
			}

			if(giphy.getGifs().isEmpty()) {
				return loadingMsg.send(String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No gifs were found for the search `%s`",
						context.getUsername(), context.getArg().orElse("random search")))
						.then();
			}

			final EmbedCreateSpec embed = new EmbedCreateSpec()
					.setColor(Config.BOT_COLOR)
					.setImage(giphy.getGifs().get(0).getImageUrl());
			return loadingMsg.send(embed).then();

		} catch (IOException err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show a random gif")
				.addArg("tag", "the tag to search", true)
				.setSource("https://www.giphy.com/")
				.build();
	}

}
