package me.shadorc.shadbot.command.image;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.TokenResponse;
import me.shadorc.shadbot.api.image.deviantart.Content;
import me.shadorc.shadbot.api.image.deviantart.DeviantArtResponse;
import me.shadorc.shadbot.api.image.deviantart.Image;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.credential.Credential;
import me.shadorc.shadbot.data.credential.Credentials;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.object.message.LoadingMessage;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ImageCmd extends BaseCmd {

    private final AtomicLong lastTokenGeneration;
    private TokenResponse token;

    public ImageCmd() {
        super(CommandCategory.IMAGE, List.of("image"));
        this.setDefaultRateLimiter();

        this.lastTokenGeneration = new AtomicLong(0);
        this.token = null;
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());
        return Mono.fromCallable(() -> {
            final Image image = this.getRandomPopularImage(NetUtils.encode(arg));
            if (image == null) {
                return loadingMsg.setContent(String.format(
                        Emoji.MAGNIFYING_GLASS + " (**%s**) No images were found for the search `%s`",
                        context.getUsername(), arg));
            }

            return loadingMsg.setEmbed(EmbedUtils.getDefaultEmbed()
                    .andThen(embed -> embed.setAuthor(String.format("DeviantArt: %s", arg), image.getUrl(), context.getAvatarUrl())
                            .setThumbnail("http://www.pngall.com/wp-content/uploads/2016/04/Deviantart-Logo-Transparent.png")
                            .addField("Title", image.getTitle(), false)
                            .addField("Author", image.getAuthor().getUsername(), false)
                            .addField("Category", image.getCategoryPath(), false)
                            .setImage(image.getContent().map(Content::getSource).get())));
        })
                .flatMap(LoadingMessage::send)
                .doOnTerminate(loadingMsg::stopTyping)
                .then();
    }

    private Image getRandomPopularImage(String encodedSearch) throws IOException {
        if (this.isTokenExpired()) {
            this.generateAccessToken();
        }

        final String url = String.format("https://www.deviantart.com/api/v1/oauth2/browse/popular?"
                        + "q=%s"
                        + "&timerange=alltime"
                        + "&limit=25" // The pagination limit (min: 1 max: 50)
                        + "&offset=%d" // The pagination offset (min: 0 max: 50000)
                        + "&access_token=%s",
                encodedSearch, ThreadLocalRandom.current().nextInt(150), this.token.getAccessToken());

        final DeviantArtResponse deviantArt = Utils.MAPPER.readValue(NetUtils.getJSON(url), DeviantArtResponse.class);
        final List<Image> images = deviantArt.getResults().stream()
                .filter(image -> image.getContent().isPresent())
                .collect(Collectors.toList());

        return images.isEmpty() ? null : Utils.randValue(images);
    }

    private boolean isTokenExpired() {
        return this.token == null
                || TimeUtils.getMillisUntil(this.lastTokenGeneration.get()) >= TimeUnit.SECONDS.toMillis(this.token.getExpiresIn());
    }

    private void generateAccessToken() throws IOException {
        final String url = String.format("https://www.deviantart.com/oauth2/token?client_id=%s&client_secret=%s&grant_type=client_credentials",
                Credentials.get(Credential.DEVIANTART_CLIENT_ID),
                Credentials.get(Credential.DEVIANTART_API_SECRET));
        this.token = Utils.MAPPER.readValue(NetUtils.getJSON(url), TokenResponse.class);
        this.lastTokenGeneration.set(System.currentTimeMillis());
        LogUtils.info("DeviantArt token generated: %s", this.token.getAccessToken());
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Search for a random image on DeviantArt.")
                .addArg("search", false)
                .setSource("https://www.deviantart.com/")
                .build();
    }
}
