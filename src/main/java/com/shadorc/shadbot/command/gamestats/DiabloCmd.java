package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.json.TokenResponse;
import com.shadorc.shadbot.api.json.gamestats.diablo.hero.HeroResponse;
import com.shadorc.shadbot.api.json.gamestats.diablo.profile.ProfileResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.*;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class DiabloCmd extends BaseCmd {

    private static final String ACCESS_TOKEN_URL = String.format("https://us.battle.net/oauth/token?grant_type=client_credentials&client_id=%s&client_secret=%s",
            CredentialManager.getInstance().get(Credential.BLIZZARD_CLIENT_ID), CredentialManager.getInstance().get(Credential.BLIZZARD_CLIENT_SECRET));

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

        final Region region = Utils.parseEnum(Region.class, args.get(0),
                new CommandException(String.format("`%s` is not a valid Region. %s",
                        args.get(0), FormatUtils.options(Region.class))));

        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        final String battletag = args.get(1).replace("#", "-");

        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading Diablo III stats...", context.getUsername()))
                .send()
                .then(this.getAccessToken())
                .map(ignored -> String.format("https://%s.api.blizzard.com/d3/profile/%s/?access_token=%s",
                        region.toString().toLowerCase(), NetUtils.encode(battletag), this.token.getAccessToken()))
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
                            .flatMap(heroUrl -> NetUtils.get(heroUrl, HeroResponse.class))
                            .filter(hero -> hero.getCode().isEmpty())
                            // Sort heroes by ascending damage
                            .sort(Comparator.comparingDouble(hero -> hero.getStats().getDamage()))
                            .collectList()
                            .map(heroResponses -> {
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
                            });
                })
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
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
        return HelpBuilder.create(this, context)
                .setDescription("Show player's stats for Diablo 3.")
                .addArg("region", String.format("user's region (%s)", FormatUtils.format(Region.class, ", ")), false)
                .addArg("battletag#0000", false)
                .setExample(String.format("`%s%s eu Shadorc#2503`", context.getPrefix(), this.getName()))
                .build();
    }

}
