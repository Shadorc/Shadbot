package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.json.gamestats.fortnite.FortniteResponse;
import com.shadorc.shadbot.api.json.gamestats.fortnite.Stats;
import com.shadorc.shadbot.core.cache.MultiValueCache;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.command.GroupCmd;
import com.shadorc.shadbot.core.command.SubCmd;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.NetUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

import java.io.IOException;

public class FortniteCmd extends SubCmd {

    private enum Platform {
        PC, XBL, PSN
    }

    private static final String PLAYER_NOT_FOUND = "Player Not Found";

    private final MultiValueCache<String, FortniteResponse> cachedValues;
    private final String apiKey;

    public FortniteCmd(final GroupCmd groupCmd) {
        super(groupCmd, CommandCategory.GAMESTATS, "fortnite", "Search for Fortnite statistics");
        this.addOption(option -> option.name("platform")
                .description("User's platform")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue())
                .choices(DiscordUtil.toOptions(Platform.class)));
        this.addOption(option -> option.name("username")
                .description("Epic nickname")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue()));

        this.cachedValues = MultiValueCache.Builder.<String, FortniteResponse>create()
                .withTtl(Config.CACHE_TTL)
                .build();
        this.apiKey = CredentialManager.get(Credential.FORTNITE_API_KEY);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Platform platform = context.getOptionAsEnum(Platform.class, "platform").orElseThrow();
        final String username = context.getOptionAsString("username").orElseThrow();

        final String encodedUsername = NetUtil.encode(username.replace(" ", "%20"));
        final String url = FortniteCmd.buildApiUrl(platform, encodedUsername);

        return context.createFollowupMessage(Emoji.HOURGLASS, context.localize("fortnite.loading"))
                .then(this.cachedValues.getOrCache(url, RequestHelper.fromUrl(url)
                        .addHeaders("TRN-Api-Key", this.apiKey)
                        .to(FortniteResponse.class)))
                .flatMap(fortnite -> {
                    if (PLAYER_NOT_FOUND.equals(fortnite.error().orElse(""))) {
                        throw Exceptions.propagate(new IOException(PLAYER_NOT_FOUND));
                    }

                    final String profileUrl = FortniteCmd.buildProfileUrl(platform, encodedUsername);
                    final String description = FortniteCmd.formatDescription(context, fortnite.stats(), username);
                    return context.editFollowupMessage(FortniteCmd.formatEmbed(context, profileUrl, description));
                })
                .onErrorResume(FortniteCmd::isNotFound,
                        err -> context.editFollowupMessage(Emoji.MAGNIFYING_GLASS, context.localize("fortnite.user.not.found")));
    }

    private static boolean isNotFound(Throwable err) {
        return err.getMessage().equals(PLAYER_NOT_FOUND) || err.getMessage().contains("wrong header");
    }

    private static String buildApiUrl(Platform platform, final String encodedUsername) {
        return "https://api.fortnitetracker.com/v1/profile/%s/%s"
                .formatted(platform.name().toLowerCase(), encodedUsername);
    }

    private static String buildProfileUrl(Platform platform, String encodedUsername) {
        return "https://fortnitetracker.com/profile/%s/%s"
                .formatted(platform.name().toLowerCase(), encodedUsername);
    }

    private static EmbedCreateSpec formatEmbed(Context context, String profileUrl, String description) {
        return ShadbotUtil.createEmbedBuilder()
                .author(context.localize("fortnite.title"), profileUrl, context.getAuthorAvatar())
                .thumbnail("https://i.imgur.com/8NrvS8e.png")
                .description(description)
                .build();
    }

    private static String formatDescription(Context context, Stats stats, String username) {
        final int length = 8;
        final String format = "%n%-" + (length + 5) + "s %-" + length + "s %-" + length + "s %-" + (length + 3) + "s";
        return context.localize("fortnite.description").formatted(username) +
                "```prolog" +
                format.formatted(" ", context.localize("fortnite.solo"),
                        context.localize("fortnite.duo"), context.localize("fortnite.squad")) +
                format.formatted(context.localize("fortnite.top"),
                        stats.getSoloStats().getTop1(),
                        stats.getDuoStats().getTop1(),
                        stats.getSquadStats().getTop1()) +
                format.formatted(context.localize("fortnite.ratio.season"),
                        stats.getSeasonSoloStats().getRatio(),
                        stats.getSeasonDuoStats().getRatio(),
                        stats.getSeasonSquadStats().getRatio()) +
                format.formatted(context.localize("fortnite.ratio.lifetime"),
                        stats.getSoloStats().getRatio(),
                        stats.getDuoStats().getRatio(),
                        stats.getSquadStats().getRatio()) +
                "```";
    }

}
