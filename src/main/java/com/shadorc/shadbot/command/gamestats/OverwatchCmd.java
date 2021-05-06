package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.json.gamestats.overwatch.OverwatchProfile;
import com.shadorc.shadbot.api.json.gamestats.overwatch.profile.Competitive;
import com.shadorc.shadbot.api.json.gamestats.overwatch.profile.ProfileResponse;
import com.shadorc.shadbot.api.json.gamestats.overwatch.stats.StatsResponse;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.cache.MultiValueCache;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.i18n.I18nManager;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.NetUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.io.IOException;
import java.util.Locale;
import java.util.function.Consumer;

public class OverwatchCmd extends BaseCmd {

    private static final String HOME_URL = "https://owapi.io";
    private static final String PROFILE_API_URL = "%s/profile".formatted(HOME_URL);
    private static final String STATS_API_URL = "%s/stats".formatted(HOME_URL);

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

    private final MultiValueCache<String, OverwatchProfile> cachedValues;

    public OverwatchCmd() {
        super(CommandCategory.GAMESTATS, "overwatch", "Search for Overwatch statistics");
        this.addOption("platform", "User's platform", true, ApplicationCommandOptionType.STRING,
                DiscordUtil.toOptions(Platform.class));
        this.addOption("battletag", "User's battletag, case sensitive", true,
                ApplicationCommandOptionType.STRING);

        this.cachedValues = MultiValueCache.Builder.<String, OverwatchProfile>create()
                .withTtl(Config.CACHE_TTL)
                .build();
    }

    @Override
    public Mono<?> execute(Context context) {
        final Platform platform = context.getOptionAsEnum(Platform.class, "platform").orElseThrow();
        final String battletag = context.getOptionAsString("battletag").orElseThrow();

        return context.createFollowupMessage(Emoji.HOURGLASS, context.localize("overwatch.loading"))
                .then(this.getOverwatchProfile(context.getLocale(), battletag, platform))
                .flatMap(profile -> {
                    if (profile.profile().isPrivate()) {
                        return context.editFollowupMessage(Emoji.ACCESS_DENIED, context.localize("overwatch.private"));
                    }
                    return context.editFollowupMessage(OverwatchCmd.formatEmbed(context, profile, platform));
                });
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(Context context, OverwatchProfile overwatchProfile, Platform platform) {
        final ProfileResponse profile = overwatchProfile.profile();
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(context.localize("overwatch.title"),
                        "https://playoverwatch.com/en-gb/career/%s/%s"
                                .formatted(platform.getName(), profile.username()),
                        context.getAuthorAvatar())
                        .setThumbnail(profile.portrait())
                        .setDescription(context.localize("overwatch.description")
                                .formatted(profile.username()))
                        .addField(context.localize("overwatch.level"),
                                Integer.toString(profile.level()), true)
                        .addField(context.localize("overwatch.playtime"),
                                profile.getQuickplayPlaytime(), true)
                        .addField(context.localize("overwatch.games.won"),
                                context.localize(profile.games().getQuickplayWon()), true)
                        .addField(context.localize("overwatch.ranks"),
                                OverwatchCmd.formatCompetitive(context, profile.competitive()), true)
                        .addField(context.localize("overwatch.heroes.played"),
                                overwatchProfile.getQuickplay().getPlayed(), true)
                        .addField(context.localize("overwatch.heroes.ratio"),
                                overwatchProfile.getQuickplay().getEliminationsPerLife(), true));
    }

    private static String formatCompetitive(Context context, Competitive competitive) {
        final StringBuilder strBuilder = new StringBuilder();
        competitive.damage().rank()
                .ifPresent(rank -> strBuilder.append(context.localize("overwatch.competitive.damage")
                        .formatted(context.localize(rank))));
        competitive.tank().rank()
                .ifPresent(rank -> strBuilder.append(context.localize("overwatch.competitive.tank")
                        .formatted(context.localize(rank))));
        competitive.support().rank()
                .ifPresent(rank -> strBuilder.append(context.localize("overwatch.competitive.support")
                        .formatted(context.localize(rank))));
        return strBuilder.isEmpty() ? context.localize("overwatch.not.ranked") : strBuilder.toString();
    }

    private Mono<OverwatchProfile> getOverwatchProfile(Locale locale, String battletag, Platform platform) {
        final String username = NetUtil.encode(battletag.replace("#", "-"));

        final String profileUrl = OverwatchCmd.buildUrl(PROFILE_API_URL, platform, username);
        final Mono<ProfileResponse> getProfile = RequestHelper.fromUrl(profileUrl)
                .to(ProfileResponse.class)
                .filter(profile -> !"Error: Profile not found".equals(profile.message().orElse("")))
                .switchIfEmpty(Mono.error(new CommandException(I18nManager.localize(locale, "overwatch.not.found"))));

        final String statsUrl = OverwatchCmd.buildUrl(STATS_API_URL, platform, username);
        final Mono<StatsResponse> getStats = RequestHelper.fromUrl(statsUrl)
                .to(StatsResponse.class);

        return this.cachedValues
                .getOrCache(profileUrl, Mono.zip(Mono.just(platform), getProfile, getStats)
                        .map(TupleUtils.function(OverwatchProfile::new)))
                .filter(overwatchProfile -> overwatchProfile.profile().portrait() != null)
                .switchIfEmpty(Mono.error(new IOException("Overwatch API returned malformed JSON")));
    }

    private static String buildUrl(String url, Platform platform, String username) {
        return "%s/%s/global/%s".formatted(url, platform.getName(), username);
    }

}
