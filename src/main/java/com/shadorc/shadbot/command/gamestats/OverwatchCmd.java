package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.json.gamestats.overwatch.OverwatchProfile;
import com.shadorc.shadbot.api.json.gamestats.overwatch.profile.ProfileResponse;
import com.shadorc.shadbot.api.json.gamestats.overwatch.stats.StatsResponse;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.NetUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.io.IOException;
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
        super(CommandCategory.GAMESTATS, "overwatch", "Search for Overwatch statistics");
        this.addOption("platform", "User's platform", true, ApplicationCommandOptionType.STRING,
                DiscordUtil.toOptions(Platform.class));
        this.addOption("battletag", "User's battletag, case sensitive", true,
                ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Platform platform = context.getOptionAsEnum(Platform.class, "platform").orElseThrow();
        final String battletag = context.getOptionAsString("battletag").orElseThrow();

        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading Overwatch statistics...", context.getAuthorName())
                .zipWith(this.getOverwatchProfile(battletag, platform))
                .flatMap(TupleUtils.function((messageId, profile) -> {
                    if (profile.getProfile().isPrivate()) {
                        return context.editFollowupMessage(messageId,
                                Emoji.ACCESS_DENIED + " (**%s**) This profile is private.", context.getAuthorName());
                    }
                    return context.editFollowupMessage(messageId,
                            OverwatchCmd.formatEmbed(profile, context.getAuthorAvatar(), platform));
                }));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(final OverwatchProfile profile, final String avatarUrl,
                                                         final Platform platform) {
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor("Overwatch Stats (Quickplay)",
                        "https://playoverwatch.com/en-gb/career/%s/%s"
                                .formatted(platform.getName(), profile.getProfile().getUsername()), avatarUrl)
                        .setThumbnail(profile.getProfile().getPortrait())
                        .setDescription("Stats for user **%s**".formatted(profile.getProfile().getUsername()))
                        .addField("Level", profile.getProfile().getLevel(), true)
                        .addField("Time played", profile.getProfile().getQuickplayPlaytime(), true)
                        .addField("Games won",
                                FormatUtil.number(profile.getProfile().getGames().getQuickplayWon()), true)
                        .addField("Competitive ranks", profile.getProfile().formatCompetitive(), true)
                        .addField("Top hero (Time played)", profile.getQuickplay().getPlayed(), true)
                        .addField("Top hero (Eliminations per life)",
                                profile.getQuickplay().getEliminationsPerLife(), true));
    }

    private Mono<OverwatchProfile> getOverwatchProfile(String battletag, Platform platform) {
        final String username = NetUtil.encode(battletag.replace("#", "-"));

        final String profileUrl = OverwatchCmd.buildUrl("profile", platform, username);
        final Mono<ProfileResponse> getProfile = RequestHelper.fromUrl(profileUrl)
                .to(ProfileResponse.class)
                .filter(profile -> !"Error: Profile not found".equals(profile.getMessage().orElse("")))
                .switchIfEmpty(Mono.error(
                        new CommandException("Profile not found. The specified platform may be incorrect.")));

        final String statsUrl = OverwatchCmd.buildUrl("stats", platform, username);
        final Mono<StatsResponse> getStats = RequestHelper.fromUrl(statsUrl)
                .to(StatsResponse.class);

        return Mono.zip(Mono.just(platform), getProfile, getStats)
                .map(TupleUtils.function(OverwatchProfile::new))
                .filter(overwatchProfile -> overwatchProfile.getProfile().getPortrait() != null)
                .switchIfEmpty(Mono.error(new IOException("Overwatch API returned malformed JSON")));
    }

    private static String buildUrl(String endpoint, Platform platform, String username) {
        return "%s/%s/%s/global/%s".formatted(HOME_URL, endpoint, platform.getName(), username);
    }

}
