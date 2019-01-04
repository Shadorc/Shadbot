package me.shadorc.shadbot.command.image;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.TokenResponse;
import me.shadorc.shadbot.api.image.deviantart.DeviantArtResponse;
import me.shadorc.shadbot.api.image.deviantart.Image;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.credential.Credential;
import me.shadorc.shadbot.data.credential.Credentials;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.IMAGE, names = { "image" })
public class ImageCmd extends AbstractCommand {

	private TokenResponse token;
	private long lastTokenGeneration;

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			final Image image = this.getRandomPopularImage(NetUtils.encode(arg));
			if(image == null) {
				return loadingMsg.send(String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No images were found for the search `%s`",
						context.getUsername(), arg))
						.then();
			}

			return context.getAvatarUrl()
					.map(avatarUrl -> EmbedUtils.getDefaultEmbed()
							.setAuthor(String.format("DeviantArt: %s", arg), image.getUrl(), avatarUrl)
							.setThumbnail("http://www.pngall.com/wp-content/uploads/2016/04/Deviantart-Logo-Transparent.png")
							.addField("Title", image.getTitle(), false)
							.addField("Author", image.getAuthor().getUsername(), false)
							.addField("Category", image.getCategoryPath(), false)
							.setImage(image.getContent().getSource()))
					.flatMap(loadingMsg::send)
					.then();

		} catch (final IOException err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}
	}

	private Image getRandomPopularImage(String encodedSearch) throws IOException {
		if(this.isTokenExpired()) {
			this.generateAccessToken();
		}

		final URL url = new URL(String.format("https://www.deviantart.com/api/v1/oauth2/browse/popular?"
				+ "q=%s"
				+ "&timerange=alltime"
				+ "&limit=25" // The pagination limit (min: 1 max: 50)
				+ "&offset=%d" // The pagination offset (min: 0 max: 50000)
				+ "&access_token=%s",
				encodedSearch, ThreadLocalRandom.current().nextInt(150), this.token.getAccessToken()));

		final DeviantArtResponse deviantArt = Utils.MAPPER.readValue(url, DeviantArtResponse.class);
		return deviantArt.getResults().isEmpty() ? null : Utils.randValue(deviantArt.getResults());
	}

	private boolean isTokenExpired() {
		return this.token == null
				|| TimeUtils.getMillisUntil(this.lastTokenGeneration) >= TimeUnit.SECONDS.toMillis(this.token.getExpiresIn());
	}

	private void generateAccessToken() throws IOException {
		synchronized (this) {
			if(this.isTokenExpired()) {
				final URL url = new URL(String.format("https://www.deviantart.com/oauth2/token?client_id=%s&client_secret=%s&grant_type=client_credentials",
						Credentials.get(Credential.DEVIANTART_CLIENT_ID),
						Credentials.get(Credential.DEVIANTART_API_SECRET)));
				this.token = Utils.MAPPER.readValue(url, TokenResponse.class);
				this.lastTokenGeneration = System.currentTimeMillis();
				LogUtils.info("DeviantArt token generated: %s", this.token.getAccessToken());
			}
		}
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Search for a random image on DeviantArt.")
				.addArg("search", false)
				.setSource("https://www.deviantart.com/")
				.build();
	}
}
