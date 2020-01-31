package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.json.gamestats.overwatch.profile.ProfileResponse;
import com.shadorc.shadbot.api.json.gamestats.overwatch.stats.Quickplay;
import com.shadorc.shadbot.api.json.gamestats.overwatch.stats.StatsResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

import java.util.List;
import java.util.function.Consumer;

public class OverwatchCmd extends BaseCmd {

    private static final String HOME_URL = "https://owapi.io";

    private enum Platform {
        PC("pc"),
        PSN("psn"),
        XBL("xbl"),
        SWITCH("nintendo-switch");

        private final String value;

        Platform(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    public OverwatchCmd() {
        super(CommandCategory.GAMESTATS, List.of("overwatch"), "ow");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(1, 2);

        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        final Mono<Tuple3<Platform, ProfileResponse, StatsResponse>> getResponse =
                args.size() == 1 ? OverwatchCmd.getResponse(args.get(0)) : OverwatchCmd.getResponse(args.get(0), args.get(1));

        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading Overwatch stats...", context.getUsername()))
                .send()
                .then(getResponse)
                .map(response -> {
                    final Platform platform = response.getT1();
                    final ProfileResponse profile = response.getT2();
                    final Quickplay topHeroes = response.getT3().getStats().getTopHeroes().getQuickplay();

                    if (profile.isPrivate()) {
                        return updatableMsg.setContent(
                                String.format(Emoji.ACCESS_DENIED + " (**%s**) This profile is private.",
                                        context.getUsername()));
                    }

                    return updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                            .andThen(embed -> embed.setAuthor("Overwatch Stats (Quickplay)",
                                    String.format("https://playoverwatch.com/en-gb/career/%s/%s",
                                            platform.toString().toLowerCase(), profile.getUsername()), context.getAvatarUrl())
                                    .setThumbnail(profile.getPortrait())
                                    .setDescription(String.format("Stats for user **%s**", profile.getUsername()))
                                    .addField("Level", profile.getLevel(), true)
                                    .addField("Time played", profile.getQuickplayPlaytime(), true)
                                    .addField("Games won", FormatUtils.number(profile.getGames().getQuickplayWon()), true)
                                    .addField("Competitive ranks", profile.formatCompetitive(), true)
                                    .addField("Top hero (Time played)", topHeroes.getPlayed(), true)
                                    .addField("Top hero (Eliminations per life)", topHeroes.getEliminationsPerLife(), true)));
                })
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private static Mono<Tuple3<Platform, ProfileResponse, StatsResponse>> getResponse(String battletag) {
        return Flux.fromArray(Platform.values())
                .flatMap(platform -> OverwatchCmd.getResponse(platform.toString(), battletag))
                .onErrorResume(err -> Mono.empty())
                .next()
                .switchIfEmpty(Mono.error(new CommandException(String.format("Platform not found. Try again specifying it. %s",
                        FormatUtils.options(Platform.class)))));
    }

    private static Mono<Tuple3<Platform, ProfileResponse, StatsResponse>> getResponse(String platformStr, String battletag) {
        final String username = battletag.replace("#", "-");
        final Platform platform = Utils.parseEnum(Platform.class, platformStr,
                new CommandException(String.format("`%s` is not a valid Platform. %s",
                        platformStr, FormatUtils.options(Platform.class))));

        final Mono<ProfileResponse> getProfile = NetUtils.get(OverwatchCmd.getUrl("profile", platform, username), ProfileResponse.class)
                .map(profile -> {
                    if (profile.getMessage().map("Error: Profile not found"::equals).orElse(false)) {
                        throw new CommandException("Profile not found.");
                    }
                    return profile;
                });
        final Mono<StatsResponse> getStats = NetUtils.get(OverwatchCmd.getUrl("stats", platform, username), StatsResponse.class);
        return Mono.zip(Mono.just(platform), getProfile, getStats);
    }

    private static String getUrl(String endpoint, Platform platform, String username) {
        return String.format("%s/%s/%s/global/%s", HOME_URL, endpoint, platform.toString().toLowerCase(), username);
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Show player's stats for Overwatch.")
                .addArg("platform", String.format("user's platform (%s)", FormatUtils.format(Platform.class, ", ")), true)
                .addArg("username", "case sensitive", false)
                .addField("Info", "**platform** is automatically detected if nothing is specified.", false)
                .setExample(String.format("%s%s pc Shadorc#2503", context.getPrefix(), this.getName()))
                .build();
    }

}
