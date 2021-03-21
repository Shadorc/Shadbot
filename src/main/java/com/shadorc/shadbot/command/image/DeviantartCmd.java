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

class DeviantartCmd extends BaseCmd {

    private static final String OAUTH_URL = "https://www.deviantart.com/oauth2/token";
    private static final String BROWSE_POPULAR_URL = "https://www.deviantart.com/api/v1/oauth2/browse/popular";

    private final String clientId;
    private final String apiSecret;
    private final AtomicLong lastTokenGeneration;
    private final AtomicReference<TokenResponse> token;

    public DeviantartCmd() {
        super(CommandCategory.IMAGE, "deviantart", "Search random image from DeviantArt");
        this.addOption("query", "Search for an image", true, ApplicationCommandOptionType.STRING);

        this.clientId = CredentialManager.getInstance().get(Credential.DEVIANTART_CLIENT_ID);
        this.apiSecret = CredentialManager.getInstance().get(Credential.DEVIANTART_API_SECRET);
        this.lastTokenGeneration = new AtomicLong();
        this.token = new AtomicReference<>();
    }

    @Override
    public Mono<?> execute(Context context) {
        final String query = context.getOptionAsString("query").orElseThrow();

        return context.reply(Emoji.HOURGLASS, context.localize("deviantart.loading"))
                .then(this.getPopularImage(query))
                .flatMap(image -> context.editReply(DeviantartCmd.formatEmbed(context, query, image)))
                .switchIfEmpty(context.editReply(Emoji.MAGNIFYING_GLASS,
                        context.localize("deviantart.not.found").formatted(query)));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(Context context, String query, Image image) {
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor("DeviantArt: %s".formatted(query), image.getUrl(), context.getAuthorAvatar())
                        .setThumbnail("https://i.imgur.com/gT4hHUB.png")
                        .addField(context.localize("deviantart.title"), image.getTitle(), false)
                        .addField(context.localize("deviantart.author"), image.getAuthor().getUsername(), false)
                        .addField(context.localize("deviantart.category"), image.getCategoryPath(), false)
                        .setImage(image.getContent().map(Content::getSource).orElseThrow()));
    }

    private Mono<Image> getPopularImage(final String query) {
        return this.requestAccessToken()
                .map(token -> "%s?".formatted(BROWSE_POPULAR_URL)
                        + "q=%s".formatted(NetUtil.encode(query))
                        + "&timerange=alltime"
                        + "&limit=25" // The pagination limit (min: 1 max: 50)
                        // The pagination offset (min: 0 max: 50000)
                        + "&offset=%d".formatted(ThreadLocalRandom.current().nextInt(150))
                        + "&access_token=%s".formatted(token.getAccessToken()))
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
            final String url = "%s?".formatted(OAUTH_URL)
                    + "client_id=%s".formatted(this.clientId)
                    + "&client_secret=%s".formatted(this.apiSecret)
                    + "&grant_type=client_credentials";
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
        final long elapsedMs = TimeUtil.elapsed(this.lastTokenGeneration.get());
        final long expiresIn = TimeUnit.SECONDS.toMillis(this.token.get().getExpiresIn());
        return elapsedMs >= expiresIn;
    }

}
