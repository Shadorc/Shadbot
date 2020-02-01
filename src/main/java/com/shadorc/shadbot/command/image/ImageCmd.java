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
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.*;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class ImageCmd extends BaseCmd {

    private static final String OAUTH_URL = "https://www.deviantart.com/oauth2/token";
    private static final String BROWSE_POPULAR_URL = "https://www.deviantart.com/api/v1/oauth2/browse/popular";

    private final AtomicLong lastTokenGeneration;
    private TokenResponse token;

    public ImageCmd() {
        super(CommandCategory.IMAGE, List.of("image"));
        this.setDefaultRateLimiter();

        this.lastTokenGeneration = new AtomicLong();
        this.token = null;
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());
        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading image...", context.getUsername()))
                .send()
                .thenMany(this.getPopularImages(NetUtils.encode(arg)))
                .collectList()
                .map(images -> {
                    if (images.isEmpty()) {
                        return updatableMsg.setContent(String.format(
                                Emoji.MAGNIFYING_GLASS + " (**%s**) No images were found for the search `%s`",
                                context.getUsername(), arg));
                    }
                    final Image image = Utils.randValue(images);
                    return updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                            .andThen(embed -> embed.setAuthor(
                                    String.format("DeviantArt: %s", arg), image.getUrl(), context.getAvatarUrl())
                                    .setThumbnail("https://i.imgur.com/gT4hHUB.png")
                                    .addField("Title", image.getTitle(), false)
                                    .addField("Author", image.getAuthor().getUsername(), false)
                                    .addField("Category", image.getCategoryPath(), false)
                                    .setImage(image.getContent().map(Content::getSource).get())));
                })
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private Flux<Image> getPopularImages(String encodedSearch) {
        return this.requestAccessToken()
                .then(Mono.defer(() -> Mono.just(String.format("%s?"
                                + "q=%s"
                                + "&timerange=alltime"
                                + "&limit=25" // The pagination limit (min: 1 max: 50)
                                + "&offset=%d" // The pagination offset (min: 0 max: 50000)
                                + "&access_token=%s",
                        BROWSE_POPULAR_URL, encodedSearch, ThreadLocalRandom.current().nextInt(150),
                        this.token.getAccessToken()))))
                .flatMap(url -> NetUtils.get(url, DeviantArtResponse.class))
                .map(DeviantArtResponse::getResults)
                .flatMapMany(Flux::fromIterable)
                .filter(image -> image.getContent().isPresent());
    }

    private Mono<Void> requestAccessToken() {
        return Mono.just(String.format("%s?client_id=%s&client_secret=%s&grant_type=client_credentials",
                OAUTH_URL, CredentialManager.getInstance().get(Credential.DEVIANTART_CLIENT_ID),
                CredentialManager.getInstance().get(Credential.DEVIANTART_API_SECRET)))
                .filter(url -> this.isTokenExpired())
                .flatMap(url -> NetUtils.get(url, TokenResponse.class))
                .doOnNext(token -> {
                    this.token = token;
                    this.lastTokenGeneration.set(System.currentTimeMillis());
                    LogUtils.info("DeviantArt token generated: %s", this.token.getAccessToken());
                })
                .then();
    }

    private boolean isTokenExpired() {
        return this.token == null
                || TimeUtils.getMillisUntil(this.lastTokenGeneration.get()) >= TimeUnit.SECONDS.toMillis(this.token.getExpiresIn());
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Search for a random image on DeviantArt.")
                .addArg("search", false)
                .setSource("https://www.deviantart.com/")
                .build();
    }
}
