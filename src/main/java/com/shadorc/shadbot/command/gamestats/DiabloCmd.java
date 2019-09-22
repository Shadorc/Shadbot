package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.TokenResponse;
import com.shadorc.shadbot.api.gamestats.diablo.hero.HeroResponse;
import com.shadorc.shadbot.api.gamestats.diablo.profile.HeroId;
import com.shadorc.shadbot.api.gamestats.diablo.profile.ProfileResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.Credentials;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.*;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class DiabloCmd extends BaseCmd {

    private final static String ACCESS_TOKEN_URL = String.format("https://us.battle.net/oauth/token?grant_type=client_credentials&client_id=%s&client_secret=%s",
            Credentials.get(Credential.BLIZZARD_CLIENT_ID), Credentials.get(Credential.BLIZZARD_CLIENT_SECRET));

    private enum Region {
        EU, US, TW, KR;
    }

    private final AtomicLong lastTokenGeneration;
    private TokenResponse token;

    public DiabloCmd() {
        super(CommandCategory.GAMESTATS, List.of("diablo"), "d3");
        this.setDefaultRateLimiter();

        this.lastTokenGeneration = new AtomicLong(0);
        this.token = null;
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(2);

        final Region region = Utils.parseEnum(Region.class, args.get(0),
                new CommandException(String.format("`%s` is not a valid Region. %s",
                        args.get(0), FormatUtils.options(Region.class))));

        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        final String battletag = args.get(1).replaceAll("#", "-");

        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading Diablo III stats...", context.getUsername()))
                .send()
                .then(this.getAccessToken())
                .then(Mono.just(String.format("https://%s.api.blizzard.com/d3/profile/%s/?access_token=%s",
                        region.toString().toLowerCase(), NetUtils.encode(battletag), this.token.getAccessToken())))
                .flatMap(url -> NetUtils.get(url, ProfileResponse.class))
                // TODO: Remove once every block operations are removed
                .publishOn(Schedulers.elastic())
                .map(profile -> {
                    if (profile.getCode().map("NOTFOUND"::equals).orElse(false)) {
                        return updatableMsg.setContent(String.format(
                                Emoji.MAGNIFYING_GLASS + " (**%s**) This user doesn't play Diablo 3 or doesn't exist.",
                                context.getUsername()));
                    }

                    final List<HeroResponse> heroResponses = new ArrayList<>();
                    for (final HeroId heroId : profile.getHeroIds()) {
                        final String heroUrl = String.format("https://%s.api.blizzard.com/d3/profile/%s/hero/%d?access_token=%s",
                                region, NetUtils.encode(battletag), heroId.getId(), this.token.getAccessToken());

                        final HeroResponse hero = NetUtils.get(heroUrl, HeroResponse.class).block();
                        if (hero.getCode().isEmpty()) {
                            heroResponses.add(hero);
                        }
                    }

                    // Sort heroes by ascending damage
                    heroResponses.sort(Comparator.comparingDouble(hero -> hero.getStats().getDamage()));
                    Collections.reverse(heroResponses);

                    return updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                            .andThen(embed -> embed.setAuthor("Diablo 3 Stats", null, context.getAvatarUrl())
                                    .setThumbnail("https://i.imgur.com/QUS9QkX.png")
                                    .setDescription(String.format("Stats for **%s** (Guild: **%s**)"
                                                    + "%n%nParangon level: **%s** (*Normal*) / **%s** (*Hardcore*)"
                                                    + "%nSeason Parangon level: **%s** (*Normal*) / **%s** (*Hardcore*)",
                                            profile.getBattleTag(), profile.getGuildName(),
                                            profile.getParagonLevel(), profile.getParagonLevelHardcore(),
                                            profile.getParagonLevelSeason(), profile.getParagonLevelSeasonHardcore()))
                                    .addField("Heroes", FormatUtils.format(heroResponses,
                                            hero -> String.format("**%s** (*%s*)", hero.getName(), hero.getClassName()), "\n"), true)
                                    .addField("Damage", FormatUtils.format(heroResponses,
                                            hero -> String.format("%s DPS", FormatUtils.number(hero.getStats().getDamage())), "\n"), true)));
                })
                .flatMap(UpdatableMessage::send)
                .then();
    }

    private boolean isTokenExpired() {
        return this.token == null
                || TimeUtils.getMillisUntil(this.lastTokenGeneration.get()) >= TimeUnit.SECONDS.toMillis(this.token.getExpiresIn());
    }

    private Mono<TokenResponse> getAccessToken() {
        final Mono<TokenResponse> getAccessToken = NetUtils.get(ACCESS_TOKEN_URL, TokenResponse.class)
                .doOnNext(token -> {
                    this.token = token;
                    this.lastTokenGeneration.set(System.currentTimeMillis());
                    LogUtils.info("Blizzard token generated: %s", this.token.getAccessToken());
                });

        return Mono.justOrEmpty(this.token)
                .filter(token -> !this.isTokenExpired())
                .switchIfEmpty(getAccessToken);
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show player's stats for Diablo 3.")
                .addArg("region", String.format("user's region (%s)", FormatUtils.format(Region.class, ", ")), false)
                .addArg("battletag#0000", false)
                .setExample(String.format("`%s%s eu Shadorc#2503`", context.getPrefix(), this.getName()))
                .build();
    }

}
