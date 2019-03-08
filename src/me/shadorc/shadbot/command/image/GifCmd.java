package me.shadorc.shadbot.command.image;

import java.util.List;
import java.util.function.Consumer;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.image.giphy.GiphyResponse;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.credential.Credential;
import me.shadorc.shadbot.data.credential.Credentials;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.publisher.Mono;

public class GifCmd extends BaseCmd {

	public GifCmd() {
		super(CommandCategory.IMAGE, List.of("gif"));
		this.setDefaultRateLimiter();
	}

	@Override
	public Mono<Void> execute(Context context) {
		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());
		return Mono.fromCallable(() -> {
			final String url = String.format("https://api.giphy.com/v1/gifs/random?api_key=%s&tag=%s",
					Credentials.get(Credential.GIPHY_API_KEY), NetUtils.encode(context.getArg().orElse("")));

			final GiphyResponse giphy = Utils.MAPPER.readValue(NetUtils.getJSON(url), GiphyResponse.class);
			if(giphy.getGifs().isEmpty()) {
				return loadingMsg.setContent(String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No gifs were found for the search `%s`",
						context.getUsername(), context.getArg().orElse("random search")));
			}

			return loadingMsg.setEmbed(EmbedUtils.getDefaultEmbed()
					.andThen(embed -> embed.setImage(giphy.getGifs().get(0).getImageUrl())));
		})
				.flatMap(LoadingMessage::send)
				.doOnTerminate(loadingMsg::stopTyping)
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show a random gif")
				.addArg("tag", "the tag to search", true)
				.setSource("https://www.giphy.com/")
				.build();
	}

}
