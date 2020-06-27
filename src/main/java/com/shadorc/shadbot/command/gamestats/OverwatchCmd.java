package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.json.gamestats.overwatch.OverwatchProfile;
import com.shadorc.shadbot.api.json.gamestats.overwatch.profile.ProfileResponse;
import com.shadorc.shadbot.api.json.gamestats.overwatch.stats.StatsResponse;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.EnumUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.ShadbotUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class OverwatchCmd extends BaseCmd {

    private static final String HOME_URL = "https://owapi.io";

    public enum Platform {
        PC("pc"),
        PSN("psn"),
        XBL("xbl"),
        NINTENDO_SWITCH("nintendo-switch");

        private final String name;

        Platform(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    public OverwatchCmd() {
        super(CommandCategory.GAMESTATS, List.of("overwatch"), "ow");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(2);

        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        final Platform platform = EnumUtils.parseEnum(Platform.class, args.get(0),
                new CommandException(String.format("`%s` is not a valid Platform. %s",
                        args.get(0), FormatUtils.options(Platform.class))));
        final String battletag = args.get(1);

        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading Overwatch stats...", context.getUsername()))
                .send()
                .then(this.getOverwatchProfile(battletag, platform))
                .map(profile -> {
                    if (profile.getProfile().isPrivate()) {
                        return updatableMsg.setContent(
                                String.format(Emoji.ACCESS_DENIED + " (**%s**) This profile is private.",
                                        context.getUsername()));
                    }
                    return updatableMsg.setEmbed(ShadbotUtils.getDefaultEmbed()
                            .andThen(embed -> embed.setAuthor("Overwatch Stats (Quickplay)",
                                    String.format("https://playoverwatch.com/en-gb/career/%s/%s",
                                            platform.getName(), profile.getProfile().getUsername()), context.getAvatarUrl())
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

    private Mono<OverwatchProfile> getOverwatchProfile(String battletag, Platform platform) {
        final String username = NetUtils.encode(battletag.replace("#", "-"));

        final Mono<ProfileResponse> getProfile =
                NetUtils.get(this.getUrl("profile", platform, username), ProfileResponse.class)
                        .map(profile -> {
                            if (profile.getMessage().map("Error: Profile not found"::equals).orElse(false)) {
                                throw new CommandException("Profile not found. The specified platform may be incorrect.");
                            }
                            return profile;
                        });
        final Mono<StatsResponse> getStats =
                NetUtils.get(this.getUrl("stats", platform, username), StatsResponse.class);

        return Mono.zip(Mono.just(platform), getProfile, getStats)
                .map(TupleUtils.function(OverwatchProfile::new))
                .filter(overwatchProfile -> overwatchProfile.getProfile().getPortrait() != null)
                .switchIfEmpty(Mono.error(new IOException("Overwatch API returned malformed JSON")));
    }

    private String getUrl(String endpoint, Platform platform, String username) {
        return String.format("%s/%s/%s/global/%s", HOME_URL, endpoint, platform.getName(), username);
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Show player's stats for Overwatch.")
                .addArg("platform", String.format("user's platform (%s)", FormatUtils.format(Platform.class, ", ")), false)
                .addArg("username", "case sensitive", false)
                .setExample(String.format("%s%s pc Shadorc#2503", context.getPrefix(), this.getName()))
                .build();
    }

}
