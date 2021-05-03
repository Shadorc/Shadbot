package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.ServerAccessException;
import com.shadorc.shadbot.api.json.TokenResponse;
import com.shadorc.shadbot.api.json.gamestats.diablo.hero.HeroResponse;
import com.shadorc.shadbot.api.json.gamestats.diablo.profile.HeroId;
import com.shadorc.shadbot.api.json.gamestats.diablo.profile.ProfileResponse;
import com.shadorc.shadbot.core.cache.MultiValueCache;
import com.shadorc.shadbot.core.cache.SingleValueCache;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.NetUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class DiabloCmd extends BaseCmd {

    private static final String ACCESS_TOKEN_URL = "https://us.battle.net/oauth/token?grant_type=client_credentials";

    private enum Region {
        EU, US, TW, KR
    }

    private final String clientId;
    private final String clientSecret;
    private final SingleValueCache<TokenResponse> token;
    private final MultiValueCache<String, ProfileResponse> profileCache;
    private final MultiValueCache<String, HeroResponse> heroCache;

    public DiabloCmd() {
        super(CommandCategory.GAMESTATS, "diablo", "Search for Diablo 3 statistics");
        this.addOption(option -> option.name("region")
                .description("User's region")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue())
                .choices(DiscordUtil.toOptions(Region.class)));
        this.addOption(option -> option.name("battletag")
                .description("User's battletag")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue()));

        this.clientId = CredentialManager.get(Credential.BLIZZARD_CLIENT_ID);
        this.clientSecret = CredentialManager.get(Credential.BLIZZARD_CLIENT_SECRET);
        this.token = SingleValueCache.Builder.create(this.requestAccessToken())
                .withTtlForValue(TokenResponse::getExpiresIn)
                .build();
        this.profileCache = MultiValueCache.Builder.<String, ProfileResponse>create().withTtl(Config.CACHE_TTL).build();
        this.heroCache = MultiValueCache.Builder.<String, HeroResponse>create().withTtl(Config.CACHE_TTL).build();
    }

    @Override
    public Mono<?> execute(Context context) {
        final Region region = context.getOptionAsEnum(Region.class, "region").orElseThrow();
        final String battletag = context.getOptionAsString("battletag").orElseThrow().replace("#", "-");

        return context.createFollowupMessage(Emoji.HOURGLASS, context.localize("diablo3.loading"))
                .then(this.token)
                .map(TokenResponse::accessToken)
                .flatMap(token -> {
                    final String profileUrl = DiabloCmd.buildProfileApiUrl(token, region, battletag);
                    return this.profileCache.getOrCache(profileUrl, RequestHelper.fromUrl(profileUrl)
                            .to(ProfileResponse.class))
                            .flatMap(profile -> {
                                if ("NOTFOUND".equals(profile.code().orElse(""))) {
                                    return context.editFollowupMessage(Emoji.MAGNIFYING_GLASS,
                                            context.localize("diablo3.user.not.found"));
                                }

                                return Flux.fromIterable(profile.heroIds())
                                        .map(heroId -> DiabloCmd.buildHeroApiUrl(token, region, battletag, heroId))
                                        .flatMap(heroUrl -> this.heroCache.getOrCache(heroUrl, RequestHelper.fromUrl(heroUrl)
                                                .to(HeroResponse.class)
                                                .onErrorResume(ServerAccessException.isStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR),
                                                        err -> Mono.empty())))
                                        .filter(hero -> hero.code().isEmpty())
                                        // Sort heroes by ascending damage
                                        .sort(Comparator.<HeroResponse>comparingDouble(hero -> hero.stats().damage())
                                                .reversed())
                                        .collectList()
                                        .flatMap(heroResponses -> {
                                            if (heroResponses.isEmpty()) {
                                                return context.editFollowupMessage(Emoji.MAGNIFYING_GLASS,
                                                        context.localize("diablo3.no.heroes"));
                                            }
                                            return context.editFollowupMessage(DiabloCmd.formatEmbed(context, profile, heroResponses));
                                        });
                            });
                });
    }

    private static String buildProfileApiUrl(String accessToken, Region region, String battletag) {
        return "https://%s.api.blizzard.com/d3/profile/%s/?access_token=%s"
                .formatted(region, NetUtil.encode(battletag), accessToken);
    }

    private static String buildHeroApiUrl(String accessToken, Region region, String battletag, HeroId heroId) {
        return "https://%s.api.blizzard.com/d3/profile/%s/hero/%d?access_token=%s"
                .formatted(region, NetUtil.encode(battletag), heroId.id(), accessToken);
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(Context context, ProfileResponse profile,
                                                         List<HeroResponse> heroResponses) {
        final String description = context.localize("diablo3.description")
                .formatted(profile.battleTag(), profile.guildName(),
                        profile.paragonLevel(), profile.paragonLevelHardcore(),
                        profile.paragonLevelSeason(), profile.paragonLevelSeasonHardcore());

        final String heroes = FormatUtil.format(heroResponses,
                hero -> "**%s** (*%s*)".formatted(hero.name(), hero.getClassName()), "\n");

        final String damages = FormatUtil.format(heroResponses,
                hero -> context.localize("diablo3.hero.dps").formatted(context.localize(hero.stats().damage())), "\n");

        return ShadbotUtil.getDefaultEmbed(embed ->
                embed.setAuthor(context.localize("diablo3.title"), null, context.getAuthorAvatar())
                        .setThumbnail("https://i.imgur.com/QUS9QkX.png")
                        .setDescription(description)
                        .addField(context.localize("diablo3.heroes"), heroes, true)
                        .addField(context.localize("diablo3.damages"), damages, true));
    }

    private Mono<TokenResponse> requestAccessToken() {
        return RequestHelper.fromUrl(ACCESS_TOKEN_URL)
                .setMethod(HttpMethod.POST)
                .addHeaders(HttpHeaderNames.AUTHORIZATION, this.buildAuthorizationValue())
                .to(TokenResponse.class)
                .doOnNext(token -> DEFAULT_LOGGER.info("Blizzard token generated {}, expires in {}s",
                        token.accessToken(), token.getExpiresIn().toSeconds()));
    }

    private String buildAuthorizationValue() {
        return "Basic %s".formatted(Base64.getEncoder()
                .encodeToString("%s:%s".formatted(this.clientId, this.clientSecret).getBytes()));
    }

}
