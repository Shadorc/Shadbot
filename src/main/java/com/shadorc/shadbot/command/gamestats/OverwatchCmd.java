package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.json.gamestats.overwatch.OverwatchProfile;
import com.shadorc.shadbot.api.json.gamestats.overwatch.profile.ProfileResponse;
import com.shadorc.shadbot.api.json.gamestats.overwatch.stats.StatsResponse;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
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
import reactor.function.TupleUtils;
import reactor.util.annotation.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class OverwatchCmd extends BaseCmd {

    private static final String HOME_URL = "https://owapi.io";

    public enum Platform {
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

        final Platform platform;
        final String username;
        if (args.size() == 2) {
            platform = Utils.parseEnum(Platform.class, args.get(0),
                    new CommandException(String.format("`%s` is not a valid Platform. %s",
                            args.get(0), FormatUtils.options(Platform.class))));
            username = args.get(1);
        } else {
            platform = null;
            username = args.get(0);
        }

        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading Overwatch stats...", context.getUsername()))
                .send()
                .then(this.getOverwatchProfile(username, platform))
                .map(profile -> {
                    if (profile.getProfile().isPrivate()) {
                        return updatableMsg.setContent(
                                String.format(Emoji.ACCESS_DENIED + " (**%s**) This profile is private.",
                                        context.getUsername()));
                    }
                    return updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                            .andThen(embed -> embed.setAuthor("Overwatch Stats (Quickplay)",
                                    String.format("https://playoverwatch.com/en-gb/career/%s/%s",
                                            platform.toString().toLowerCase(), profile.getProfile().getUsername()), context.getAvatarUrl())
                                    .setThumbnail(profile.getProfile().getPortrait())
                                    .setDescription(String.format("Stats for user **%s**", profile.getProfile().getUsername()))
                                    .addField("Level", profile.getProfile().getLevel(), true)
                                    .addField("Time played", profile.getProfile().getQuickplayPlaytime(), true)
                                    .addField("Games won", FormatUtils.number(profile.getProfile().getGames().getQuickplayWon()), true)
                                    .addField("Competitive ranks", profile.getProfile().formatCompetitive(), true)
                                    .addField("Top hero (Time played)", profile.getQuickplay().getPlayed(), true)
                                    .addField("Top hero (Eliminations per life)",
                                            profile.getQuickplay().getEliminationsPerLife(), true)));
                })
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    /**
     * Automatically detects the platform by iterating over all of them if {@code platform} is null.
     */
    private Mono<OverwatchProfile> getOverwatchProfile(String battletag, @Nullable Platform platform) {
        final String username = battletag.replace("#", "-");
        final List<Platform> platforms = platform == null ? List.of(Platform.values()) : List.of(platform);

        return Flux.fromIterable(platforms)
                .flatMap(platformItr -> {
                    final Mono<ProfileResponse> getProfile = NetUtils.get(
                            this.getUrl("profile", platformItr, username), ProfileResponse.class)
                            .map(profile -> {
                                if (profile.getMessage().map("Error: Profile not found"::equals).orElse(false)) {
                                    throw new CommandException("Profile not found.");
                                }
                                return profile;
                            });
                    final Mono<StatsResponse> getStats = NetUtils.get(
                            this.getUrl("stats", platformItr, username), StatsResponse.class);
                    return Mono.zip(Mono.just(platformItr), getProfile, getStats);
                })
                .onErrorResume(err -> Mono.empty())
                .next()
                .map(TupleUtils.function(OverwatchProfile::new))
                .switchIfEmpty(Mono.error(new CommandException(String.format("Platform not found. Try again specifying it. %s",
                        FormatUtils.options(Platform.class)))));
    }

    private String getUrl(String endpoint, Platform platform, String username) {
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
