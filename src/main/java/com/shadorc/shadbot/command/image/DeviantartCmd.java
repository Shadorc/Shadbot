package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.TokenResponse;
import com.shadorc.shadbot.api.json.image.deviantart.Content;
import com.shadorc.shadbot.api.json.image.deviantart.DeviantArtResponse;
import com.shadorc.shadbot.api.json.image.deviantart.Image;
import com.shadorc.shadbot.core.cache.SingleValueCache;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.command.GroupCmd;
import com.shadorc.shadbot.core.command.SubCmd;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.NetUtil;
import com.shadorc.shadbot.utils.RandUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.spec.legacy.LegacyEmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class DeviantartCmd extends SubCmd {

    private static final String OAUTH_URL = "https://www.deviantart.com/oauth2/token";
    private static final String BROWSE_POPULAR_URL = "https://www.deviantart.com/api/v1/oauth2/browse/popular";

    private final String clientId;
    private final String apiSecret;
    private final SingleValueCache<TokenResponse> token;

    public DeviantartCmd(final GroupCmd groupCmd) {
        super(groupCmd, CommandCategory.IMAGE, "deviantart", "Search random image from DeviantArt");
        this.addOption(option -> option.name("query")
                .description("Search for an image")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue()));

        this.clientId = CredentialManager.get(Credential.DEVIANTART_CLIENT_ID);
        this.apiSecret = CredentialManager.get(Credential.DEVIANTART_API_SECRET);
        this.token = SingleValueCache.Builder.create(this.requestAccessToken())
                .withTtlForValue(TokenResponse::getExpiresIn)
                .build();
    }

    @Override
    public Mono<?> execute(Context context) {
        final String query = context.getOptionAsString("query").orElseThrow();

        return context.createFollowupMessage(Emoji.HOURGLASS, context.localize("deviantart.loading"))
                .then(this.token)
                .map(TokenResponse::accessToken)
                .flatMap(accessToken -> this.getPopularImage(accessToken, query))
                .flatMap(image -> context.editFollowupMessage(DeviantartCmd.formatEmbed(context, query, image)))
                .switchIfEmpty(context.editFollowupMessage(Emoji.MAGNIFYING_GLASS,
                        context.localize("deviantart.not.found").formatted(query)));
    }

    private Mono<Image> getPopularImage(String accessToken, String query) {
        return Mono.fromCallable(() ->
                "%s?".formatted(BROWSE_POPULAR_URL)
                        + "q=%s".formatted(NetUtil.encode(query))
                        + "&timerange=alltime"
                        + "&limit=25" // The pagination limit (min: 1 max: 50)
                        // The pagination offset (min: 0 max: 50000)
                        + "&offset=%d".formatted(ThreadLocalRandom.current().nextInt(150))
                        + "&access_token=%s".formatted(accessToken))
                .flatMap(url -> RequestHelper.fromUrl(url)
                        .to(DeviantArtResponse.class))
                .flatMapIterable(DeviantArtResponse::results)
                .filter(image -> image.content().isPresent())
                .collectList()
                .map(list -> Optional.ofNullable(RandUtil.randValue(list)))
                .flatMap(Mono::justOrEmpty);
    }

    private static Consumer<LegacyEmbedCreateSpec> formatEmbed(Context context, String query, Image image) {
        return ShadbotUtil.getDefaultLegacyEmbed(
                embed -> embed.setAuthor("DeviantArt: %s".formatted(query), image.url(), context.getAuthorAvatar())
                        .setThumbnail("https://i.imgur.com/gT4hHUB.png")
                        .addField(context.localize("deviantart.title"), image.title(), false)
                        .addField(context.localize("deviantart.author"), image.author().username(), false)
                        .addField(context.localize("deviantart.category"), image.categoryPath(), false)
                        .setImage(image.content().map(Content::source).orElseThrow()));
    }

    private Mono<TokenResponse> requestAccessToken() {
        return Mono.fromCallable(() ->
                "%s?".formatted(OAUTH_URL)
                        + "client_id=%s".formatted(this.clientId)
                        + "&client_secret=%s".formatted(this.apiSecret)
                        + "&grant_type=client_credentials")
                .flatMap(url -> RequestHelper.fromUrl(url)
                        .to(TokenResponse.class))
                .doOnNext(token -> DEFAULT_LOGGER.info("DeviantArt token generated {}, expires in {}s",
                        token.accessToken(), token.getExpiresIn().toSeconds()));
    }

}
