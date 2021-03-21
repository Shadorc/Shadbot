package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.ServerAccessException;
import com.shadorc.shadbot.api.json.TokenResponse;
import com.shadorc.shadbot.api.json.gamestats.diablo.hero.HeroResponse;
import com.shadorc.shadbot.api.json.gamestats.diablo.profile.HeroId;
import com.shadorc.shadbot.api.json.gamestats.diablo.profile.ProfileResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.*;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

class DiabloCmd extends BaseCmd {

    private static final String ACCESS_TOKEN_URL = "https://us.battle.net/oauth/token?grant_type=client_credentials";

    private enum Region {
        EU, US, TW, KR
    }

    private final String clientId;
    private final String clientSecret;
    private final AtomicLong lastTokenGeneration;
    private final AtomicReference<TokenResponse> token;

    public DiabloCmd() {
        super(CommandCategory.GAMESTATS, "diablo", "Search for Diablo 3 statistics");
        this.addOption("region", "User's region", true, ApplicationCommandOptionType.STRING,
                DiscordUtil.toOptions(Region.class));
        this.addOption("battletag", "User's battletag", true, ApplicationCommandOptionType.STRING);

        this.clientId = CredentialManager.getInstance().get(Credential.BLIZZARD_CLIENT_ID);
        this.clientSecret = CredentialManager.getInstance().get(Credential.BLIZZARD_CLIENT_SECRET);
        this.lastTokenGeneration = new AtomicLong();
        this.token = new AtomicReference<>();
    }

    @Override
    public Mono<?> execute(Context context) {
        final Region region = context.getOptionAsEnum(Region.class, "region").orElseThrow();
        final String battletag = context.getOptionAsString("battletag").orElseThrow().replace("#", "-");

        return context.reply(Emoji.HOURGLASS, context.localize("diablo3.loading"))
                .then(this.requestAccessToken())
                .then(Mono.fromCallable(() -> this.buildProfileApiUrl(region, battletag)))
                .flatMap(url -> RequestHelper.fromUrl(url)
                        .to(ProfileResponse.class))
                .flatMap(profile -> {
                    if ("NOTFOUND".equals(profile.getCode().orElse(""))) {
                        return context.editReply(Emoji.MAGNIFYING_GLASS, context.localize("diablo3.user.not.found"));
                    }

                    return Flux.fromIterable(profile.getHeroIds())
                            .map(heroId -> this.buildHeroApiUrl(region, battletag, heroId))
                            .flatMap(heroUrl -> RequestHelper.fromUrl(heroUrl)
                                    .to(HeroResponse.class)
                                    .onErrorResume(ServerAccessException.isStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR),
                                            err -> Mono.empty()))
                            .filter(hero -> hero.getCode().isEmpty())
                            // Sort heroes by ascending damage
                            .sort(Comparator.comparingDouble(hero -> hero.getStats().getDamage()))
                            .collectList()
                            .flatMap(heroResponses -> {
                                if (heroResponses.isEmpty()) {
                                    return context.editReply(Emoji.MAGNIFYING_GLASS, context.localize("diablo3.no.heroes"));
                                }
                                Collections.reverse(heroResponses);
                                return context.editReply(DiabloCmd.formatEmbed(context, profile, heroResponses));
                            });
                });
    }

    private String buildProfileApiUrl(Region region, String battletag) {
        return "https://%s.api.blizzard.com/d3/profile/%s/?access_token=%s"
                .formatted(region, NetUtil.encode(battletag), this.token.get().getAccessToken());
    }

    private String buildHeroApiUrl(Region region, String battletag, HeroId heroId) {
        return "https://%s.api.blizzard.com/d3/profile/%s/hero/%d?access_token=%s"
                .formatted(region, NetUtil.encode(battletag), heroId.getId(), this.token.get().getAccessToken());
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(Context context, ProfileResponse profile,
                                                         List<HeroResponse> heroResponses) {
        final String description = context.localize("diablo3.description")
                .formatted(profile.getBattleTag(), profile.getGuildName(),
                        profile.getParagonLevel(), profile.getParagonLevelHardcore(),
                        profile.getParagonLevelSeason(), profile.getParagonLevelSeasonHardcore());

        final String heroes = FormatUtil.format(heroResponses,
                hero -> "**%s** (*%s*)".formatted(hero.getName(), hero.getClassName()), "\n");

        final String damages = FormatUtil.format(heroResponses,
                hero -> context.localize("diablo3.hero.dps").formatted(context.localize(hero.getStats().getDamage())), "\n");

        return ShadbotUtil.getDefaultEmbed(embed ->
                embed.setAuthor(context.localize("diablo3.title"), null, context.getAuthorAvatar())
                        .setThumbnail("https://i.imgur.com/QUS9QkX.png")
                        .setDescription(description)
                        .addField(context.localize("diablo3.heroes"), heroes, true)
                        .addField(context.localize("diablo3.damages"), damages, true));
    }

    private boolean isTokenExpired() {
        final TokenResponse token = this.token.get();
        if (token == null) {
            return true;
        }
        return TimeUtil.elapsed(this.lastTokenGeneration.get()) >= TimeUnit.SECONDS.toMillis(token.getExpiresIn());
    }

    private Mono<TokenResponse> requestAccessToken() {
        if (this.isTokenExpired()) {
            return RequestHelper.fromUrl(ACCESS_TOKEN_URL)
                    .setMethod(HttpMethod.POST)
                    .addHeaders(HttpHeaderNames.AUTHORIZATION, this.buildAuthorizationValue())
                    .to(TokenResponse.class)
                    .doOnNext(token -> {
                        this.token.set(token);
                        this.lastTokenGeneration.set(System.currentTimeMillis());
                        DEFAULT_LOGGER.info("Blizzard token generated: {}", token.getAccessToken());
                    });
        }
        return Mono.just(this.token.get());
    }

    private String buildAuthorizationValue() {
        return "Basic %s".formatted(Base64.getEncoder()
                .encodeToString("%s:%s".formatted(this.clientId, this.clientSecret).getBytes()));
    }

}
