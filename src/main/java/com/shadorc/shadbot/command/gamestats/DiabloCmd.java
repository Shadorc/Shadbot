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
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class DiabloCmd extends BaseCmd {

    private static final String ACCESS_TOKEN_URL = String.format(
            "https://us.battle.net/oauth/token?grant_type=client_credentials&client_id=%s&client_secret=%s",
            CredentialManager.getInstance().get(Credential.BLIZZARD_CLIENT_ID),
            CredentialManager.getInstance().get(Credential.BLIZZARD_CLIENT_SECRET));

    private enum Region {
        EU, US, TW, KR
    }

    private final AtomicLong lastTokenGeneration;
    private final AtomicReference<TokenResponse> token;

    public DiabloCmd() {
        super(CommandCategory.GAMESTATS, "diablo", "Search for Diablo 3 statistics");
        this.addOption("region", "User's region", true, ApplicationCommandOptionType.STRING,
                DiscordUtil.toOptions(Region.class));
        this.addOption("battletag", "User's battletag", true, ApplicationCommandOptionType.STRING);

        this.lastTokenGeneration = new AtomicLong();
        this.token = new AtomicReference<>();
    }

    @Override
    public Mono<?> execute(Context context) {
        final Region region = context.getOptionAsEnum(Region.class, "region").orElseThrow();
        final String battletag = context.getOptionAsString("battletag").orElseThrow().replace("#", "-");

        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading Diablo 3 statistics...", context.getAuthorName())
                .flatMap(messageId -> this.requestAccessToken()
                        .then(RequestHelper.fromUrl(this.buildProfileApiUrl(region, battletag))
                                .to(ProfileResponse.class))
                        .flatMap(profile -> {
                            if (profile.getCode().orElse("").equals("NOTFOUND")) {
                                return context.editFollowupMessage(messageId,
                                        Emoji.MAGNIFYING_GLASS + " (**%s**) This user doesn't play Diablo 3 or doesn't exist.",
                                        context.getAuthorName());
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
                                            return context.editFollowupMessage(messageId,
                                                    Emoji.MAGNIFYING_GLASS + " (**%s**) This user doesn't have " +
                                                            "any heroes or they are not found.",
                                                    context.getAuthorName());
                                        }
                                        Collections.reverse(heroResponses);
                                        return context.editFollowupMessage(messageId,
                                                DiabloCmd.formatEmbed(context.getAuthorAvatarUrl(), profile, heroResponses));
                                    });
                        }));
    }

    private String buildProfileApiUrl(final Region region, final String battletag) {
        return "https://%s.api.blizzard.com/d3/profile/%s/?access_token=%s"
                .formatted(region.name().toLowerCase(), NetUtil.encode(battletag), this.token.get().getAccessToken());
    }

    private String buildHeroApiUrl(final Region region, final String battletag, final HeroId heroId) {
        return "https://%s.api.blizzard.com/d3/profile/%s/hero/%d?access_token=%s"
                .formatted(region, NetUtil.encode(battletag), heroId.getId(), this.token.get().getAccessToken());
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(final String avatarUrl, final ProfileResponse profile,
                                                         final List<HeroResponse> heroResponses) {
        final String description = String.format("Stats for **%s** (Guild: **%s**)"
                        + "%n%nParangon level: **%s** (*Normal*) / **%s** (*Hardcore*)"
                        + "%nSeason Parangon level: **%s** (*Normal*) / **%s** (*Hardcore*)",
                profile.getBattleTag(), profile.getGuildName(),
                profile.getParagonLevel(), profile.getParagonLevelHardcore(),
                profile.getParagonLevelSeason(), profile.getParagonLevelSeasonHardcore());

        final String heroes = FormatUtil.format(heroResponses,
                hero -> "**%s** (*%s*)".formatted(hero.getName(), hero.getClassName()), "\n");

        final String damages = FormatUtil.format(heroResponses,
                hero -> "%s DPS".formatted(FormatUtil.number(hero.getStats().getDamage())), "\n");

        return ShadbotUtil.getDefaultEmbed(embed ->
                embed.setAuthor("Diablo 3 Stats", null, avatarUrl)
                        .setThumbnail("https://i.imgur.com/QUS9QkX.png")
                        .setDescription(description)
                        .addField("Heroes", heroes, true)
                        .addField("Damage", damages, true));
    }

    private boolean isTokenExpired() {
        final TokenResponse token = this.token.get();
        if (token == null) {
            return true;
        }
        return TimeUtil.getMillisUntil(this.lastTokenGeneration.get()) >= TimeUnit.SECONDS.toMillis(token.getExpiresIn());
    }

    private Mono<TokenResponse> requestAccessToken() {
        if (this.isTokenExpired()) {
            return RequestHelper.fromUrl(ACCESS_TOKEN_URL)
                    .to(TokenResponse.class)
                    .doOnNext(token -> {
                        this.token.set(token);
                        this.lastTokenGeneration.set(System.currentTimeMillis());
                        DEFAULT_LOGGER.info("Blizzard token generated: {}", token.getAccessToken());
                    });
        }
        return Mono.just(this.token.get());
    }

}
