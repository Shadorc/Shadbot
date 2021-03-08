package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.TokenResponse;
import com.shadorc.shadbot.api.json.image.deviantart.Content;
import com.shadorc.shadbot.api.json.image.deviantart.DeviantArtResponse;
import com.shadorc.shadbot.api.json.image.deviantart.Image;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.NetUtil;
import com.shadorc.shadbot.utils.RandUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.TimeUtil;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class DeviantartCmd extends BaseCmd {

    private static final String OAUTH_URL = "https://www.deviantart.com/oauth2/token";
    private static final String BROWSE_POPULAR_URL = "https://www.deviantart.com/api/v1/oauth2/browse/popular";

    private final AtomicLong lastTokenGeneration;
    private final AtomicReference<TokenResponse> token;

    public DeviantartCmd() {
        super(CommandCategory.IMAGE, "deviantart", "Search random image from DeviantArt");
        this.addOption("query", "Search for an image", true, ApplicationCommandOptionType.STRING);

        this.lastTokenGeneration = new AtomicLong();
        this.token = new AtomicReference<>();
    }

    @Override
    public Mono<?> execute(final Context context) {
        final String query = context.getOptionAsString("query").orElseThrow();

        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading image...", context.getAuthorName())
                .flatMap(messageId -> this.getPopularImage(query)
                        .flatMap(image -> context.editFollowupMessage(messageId,
                                DeviantartCmd.formatEmbed(context.getAuthorAvatarUrl(), query, image)))
                        .switchIfEmpty(context.editFollowupMessage(messageId,
                                Emoji.MAGNIFYING_GLASS + " (**%s**) No images found matching query `%s`",
                                context.getAuthorName(), query)));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(final String avatarUrl, final String query, final Image image) {
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor("DeviantArt: %s".formatted(query), image.getUrl(), avatarUrl)
                        .setThumbnail("https://i.imgur.com/gT4hHUB.png")
                        .addField("Title", image.getTitle(), false)
                        .addField("Author", image.getAuthor().getUsername(), false)
                        .addField("Category", image.getCategoryPath(), false)
                        .setImage(image.getContent().map(Content::getSource).orElseThrow()));
    }

    private Mono<Image> getPopularImage(final String query) {
        return this.requestAccessToken()
                .map(token -> String.format("%s?"
                                + "q=%s"
                                + "&timerange=alltime"
                                + "&limit=25" // The pagination limit (min: 1 max: 50)
                                + "&offset=%d" // The pagination offset (min: 0 max: 50000)
                                + "&access_token=%s",
                        BROWSE_POPULAR_URL, NetUtil.encode(query), ThreadLocalRandom.current().nextInt(150),
                        token.getAccessToken()))
                .flatMap(url -> RequestHelper.fromUrl(url)
                        .to(DeviantArtResponse.class))
                .flatMapIterable(DeviantArtResponse::getResults)
                .filter(image -> image.getContent().isPresent())
                .collectList()
                .map(list -> Optional.ofNullable(RandUtil.randValue(list)))
                .flatMap(Mono::justOrEmpty);
    }

    private Mono<TokenResponse> requestAccessToken() {
        if (this.isTokenExpired()) {
            final String url = String.format("%s?client_id=%s" +
                            "&client_secret=%s" +
                            "&grant_type=client_credentials",
                    OAUTH_URL, CredentialManager.getInstance().get(Credential.DEVIANTART_CLIENT_ID),
                    CredentialManager.getInstance().get(Credential.DEVIANTART_API_SECRET));
            return RequestHelper.fromUrl(url)
                    .to(TokenResponse.class)
                    .doOnNext(token -> {
                        this.token.set(token);
                        this.lastTokenGeneration.set(System.currentTimeMillis());
                        DEFAULT_LOGGER.info("DeviantArt token generated: {}", token.getAccessToken());
                    });
        }
        return Mono.just(this.token.get());
    }

    private boolean isTokenExpired() {
        if (this.token.get() == null) {
            return true;
        }
        final long elapsedMs = TimeUtil.getMillisUntil(this.lastTokenGeneration.get());
        final long expiresIn = TimeUnit.SECONDS.toMillis(this.token.get().getExpiresIn());
        return elapsedMs >= expiresIn;
    }

}