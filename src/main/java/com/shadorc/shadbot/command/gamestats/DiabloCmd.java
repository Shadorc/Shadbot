package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.ServerAccessException;
import com.shadorc.shadbot.api.json.TokenResponse;
import com.shadorc.shadbot.api.json.gamestats.diablo.hero.HeroResponse;
import com.shadorc.shadbot.api.json.gamestats.diablo.profile.ProfileResponse;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.*;
import discord4j.core.spec.EmbedCreateSpec;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class DiabloCmd extends BaseCmd {

    private static final String ACCESS_TOKEN_URL = String.format(
            "https://us.battle.net/oauth/token?grant_type=client_credentials&client_id=%s&client_secret=%s",
            CredentialManager.getInstance().get(Credential.BLIZZARD_CLIENT_ID),
            CredentialManager.getInstance().get(Credential.BLIZZARD_CLIENT_SECRET));

    private enum Region {
        EU, US, TW, KR;
    }

    private final AtomicLong lastTokenGeneration;
    private TokenResponse token;

    public DiabloCmd() {
        super(CommandCategory.GAMESTATS, List.of("diablo"), "d3");
        this.setDefaultRateLimiter();

        this.lastTokenGeneration = new AtomicLong();
        this.token = null;
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(2);

        final Region region = EnumUtils.parseEnum(Region.class, args.get(0),
                new CommandException(String.format("`%s` is not a valid Region. %s",
                        args.get(0), FormatUtils.options(Region.class))));

        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        final String battletag = args.get(1).replace("#", "-");

        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading Diablo III stats...", context.getUsername()))
                .send()
                .then(this.requestAccessToken())
                .then(Mono.fromCallable(() -> String.format("https://%s.api.blizzard.com/d3/profile/%s/?access_token=%s",
                        region.toString().toLowerCase(), NetUtils.encode(battletag), this.token.getAccessToken())))
                .flatMap(url -> NetUtils.get(url, ProfileResponse.class))
                .flatMap(profile -> {
                    if (profile.getCode().map("NOTFOUND"::equals).orElse(false)) {
                        return Mono.just(updatableMsg.setContent(String.format(
                                Emoji.MAGNIFYING_GLASS + " (**%s**) This user doesn't play Diablo 3 or doesn't exist.",
                                context.getUsername())));
                    }

                    return Flux.fromIterable(profile.getHeroIds())
                            .map(heroId -> String.format("https://%s.api.blizzard.com/d3/profile/%s/hero/%d?access_token=%s",
                                    region, NetUtils.encode(battletag), heroId.getId(), this.token.getAccessToken()))
                            .flatMap(heroUrl -> NetUtils.get(heroUrl, HeroResponse.class)
                                    .onErrorResume(ServerAccessException.isStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR),
                                            err -> Mono.empty()))
                            .filter(hero -> hero.getCode().isEmpty())
                            // Sort heroes by ascending damage
                            .sort(Comparator.comparingDouble(hero -> hero.getStats().getDamage()))
                            .collectList()
                            .map(heroResponses -> {
                                Collections.reverse(heroResponses);
                                return updatableMsg.setEmbed(this.getEmbed(context.getAvatarUrl(), profile, heroResponses));
                            });
                })
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private Consumer<EmbedCreateSpec> getEmbed(String avatarUrl, ProfileResponse profile, List<HeroResponse> heroResponses) {
        final String description = String.format("Stats for **%s** (Guild: **%s**)"
                        + "%n%nParangon level: **%s** (*Normal*) / **%s** (*Hardcore*)"
                        + "%nSeason Parangon level: **%s** (*Normal*) / **%s** (*Hardcore*)",
                profile.getBattleTag(), profile.getGuildName(),
                profile.getParagonLevel(), profile.getParagonLevelHardcore(),
                profile.getParagonLevelSeason(), profile.getParagonLevelSeasonHardcore());

        final String heroes = FormatUtils.format(heroResponses,
                hero -> String.format("**%s** (*%s*)", hero.getName(), hero.getClassName()), "\n");

        final String damages = FormatUtils.format(heroResponses,
                hero -> String.format("%s DPS", FormatUtils.number(hero.getStats().getDamage())), "\n");

        return ShadbotUtils.getDefaultEmbed()
                .andThen(embed -> embed.setAuthor("Diablo 3 Stats", null, avatarUrl)
                        .setThumbnail("https://i.imgur.com/QUS9QkX.png")
                        .setDescription(description)
                        .addField("Heroes", heroes, true)
                        .addField("Damage", damages, true));
    }

    private boolean isTokenExpired() {
        return this.token == null
                || TimeUtils.getMillisUntil(this.lastTokenGeneration.get()) >= TimeUnit.SECONDS.toMillis(this.token.getExpiresIn());
    }

    /**
     * Requests to update the Blizzard token, if expired.
     *
     * @return A {@link Mono} that completes once the token has been successfully updated, if expired.
     */
    private Mono<Void> requestAccessToken() {
        final Mono<TokenResponse> requestAccessToken = NetUtils.get(ACCESS_TOKEN_URL, TokenResponse.class)
                .doOnNext(token -> {
                    this.token = token;
                    this.lastTokenGeneration.set(System.currentTimeMillis());
                    DEFAULT_LOGGER.info("Blizzard token generated: {}", this.token.getAccessToken());
                });

        return Mono.justOrEmpty(this.token)
                .filter(token -> !this.isTokenExpired())
                .switchIfEmpty(requestAccessToken)
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Show player's stats for Diablo 3.")
                .addArg("region", String.format("user's region (%s)", FormatUtils.format(Region.class, ", ")), false)
                .addArg("battletag#0000", "case sensitive", false)
                .setExample(String.format("`%s%s eu Shadorc#2503`", context.getPrefix(), this.getName()))
                .build();
    }

}
