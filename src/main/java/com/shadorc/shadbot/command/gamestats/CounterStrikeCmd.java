package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.json.gamestats.steam.player.PlayerSummaries;
import com.shadorc.shadbot.api.json.gamestats.steam.player.PlayerSummariesResponse;
import com.shadorc.shadbot.api.json.gamestats.steam.player.PlayerSummary;
import com.shadorc.shadbot.api.json.gamestats.steam.resolver.ResolveVanityUrlResponse;
import com.shadorc.shadbot.api.json.gamestats.steam.resolver.Response;
import com.shadorc.shadbot.api.json.gamestats.steam.stats.PlayerStats;
import com.shadorc.shadbot.api.json.gamestats.steam.stats.Stats;
import com.shadorc.shadbot.api.json.gamestats.steam.stats.UserStatsForGameResponse;
import com.shadorc.shadbot.command.CommandException;
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
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CounterStrikeCmd extends BaseCmd {

    private static final String PRIVACY_HELP_URL = "https://support.steampowered.com/kb_article.php?ref=4113-YUDH-6401";
    private static final String PLAYER_SUMMARIES_URL = "http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/";
    private static final String USER_STATS_FOR_GAME_URL = "http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v0002/";
    private static final String RESOLVE_VANITY_URL = "http://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/";

    private final String apiKey;

    public CounterStrikeCmd() {
        super(CommandCategory.GAMESTATS, "csgo", "Search for Counter-Strike: Global Offensive statistics");
        this.addOption("steamid", "Steam ID, custom ID or profile URL", true,
                ApplicationCommandOptionType.STRING);

        this.apiKey = CredentialManager.getInstance().get(Credential.STEAM_API_KEY);
    }

    @Override
    public Mono<?> execute(Context context) {
        final String steamId = context.getOptionAsString("steamid").orElseThrow();
        final String identificator = CounterStrikeCmd.getIdentificator(steamId);

        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading CS:GO statistics...", context.getAuthorName())
                .flatMap(messageId -> this.getSteamId(identificator)
                        .flatMap(this::getPlayerSummary)
                        .flatMap(player -> {
                            if (player.getCommunityVisibilityState() != PlayerSummary.CommunityVisibilityState.PUBLIC) {
                                return context.editFollowupMessage(messageId, Emoji.ACCESS_DENIED +
                                                " (**%s**) This profile is private, more info [here](<%s>).",
                                        context.getAuthorName(), PRIVACY_HELP_URL);
                            }

                            final String userStatsUrl = "%s?appid=730&key=%s&steamid=%s"
                                    .formatted(USER_STATS_FOR_GAME_URL, this.apiKey, player.getSteamId());

                            return RequestHelper.request(userStatsUrl)
                                    .flatMap(body -> {
                                        if (body.contains("500 Internal Server Error")) {
                                            return context.editFollowupMessage(messageId, Emoji.ACCESS_DENIED +
                                                            " (**%s**) The game details of this profile are not " +
                                                            "public, more info [here](<%s>).",
                                                    context.getAuthorName(), PRIVACY_HELP_URL);
                                        }

                                        return Mono.fromCallable(() -> NetUtil.MAPPER.readValue(body, UserStatsForGameResponse.class))
                                                .map(userStats -> userStats.getPlayerStats()
                                                        .flatMap(PlayerStats::getStats))
                                                .flatMap(Mono::justOrEmpty)
                                                .flatMap(stats -> context.editFollowupMessage(messageId,
                                                        CounterStrikeCmd.formatEmbed(
                                                                context.getAuthorAvatarUrl(), player, stats)))
                                                .switchIfEmpty(context.editFollowupMessage(messageId, Emoji.MAGNIFYING_GLASS +
                                                                " (**%s**) This user doesn't play Counter-Strike: Global Offensive.",
                                                        context.getAuthorName()));
                                    });
                        })
                        .switchIfEmpty(context.editFollowupMessage(messageId, Emoji.MAGNIFYING_GLASS +
                                        " (**%s**) Steam player not found.",
                                context.getAuthorName())));
    }

    private static String getIdentificator(String arg) {
        // The user provided an URL that can contains a pseudo or an ID
        if (arg.contains("/")) {
            final List<String> splittedUrl = StringUtil.split(arg, "/");
            if (splittedUrl.isEmpty()) {
                throw new CommandException("Invalid steam ID.");
            }
            return splittedUrl.get(splittedUrl.size() - 1);
        } else {
            return arg;
        }
    }

    private Mono<String> getSteamId(String identificator) {
        // The user directly provided the ID
        if (NumberUtil.isPositiveLong(identificator)) {
            return Mono.just(identificator);
        }
        // The user provided a pseudo
        else {
            final String url = "%s?key=%s&vanityurl=%s"
                    .formatted(RESOLVE_VANITY_URL, this.apiKey, NetUtil.encode(identificator));
            return RequestHelper.fromUrl(url)
                    .to(ResolveVanityUrlResponse.class)
                    .map(ResolveVanityUrlResponse::getResponse)
                    .map(Response::getSteamId)
                    .flatMap(Mono::justOrEmpty);
        }
    }

    private Mono<PlayerSummary> getPlayerSummary(String steamId) {
        final String url = "%s?key=%s&steamids=%s".formatted(PLAYER_SUMMARIES_URL, this.apiKey, steamId);
        return RequestHelper.fromUrl(url)
                .to(PlayerSummariesResponse.class)
                .map(PlayerSummariesResponse::getResponse)
                // Users matching the steamId
                .flatMapIterable(PlayerSummaries::getPlayers)
                .next();
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(final String avatarUrl, final PlayerSummary player,
                                                         final List<Stats> stats) {
        final Map<String, Integer> statsMap = stats.stream()
                .collect(Collectors.toMap(Stats::getName, Stats::getValue));

        final int kills = statsMap.get("total_kills");
        final int deaths = statsMap.get("total_deaths");
        final float ratio = (float) kills / deaths;

        final int wins = statsMap.get("total_wins");
        final int roundsPlayed = statsMap.get("total_rounds_played");
        final float win = (float) wins / roundsPlayed * 100;

        final int mvps = statsMap.get("total_mvps");
        final float timePlayed = statsMap.get("total_time_played") / 3600f;

        final int shotsHit = statsMap.get("total_shots_hit");
        final int shotsFired = statsMap.get("total_shots_fired");
        final float accuracy = (float) shotsHit / shotsFired * 100;

        final int killsHeadshot = statsMap.get("total_kills_headshot");
        final float headshot = (float) killsHeadshot / kills * 100;

        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor("Counter-Strike: Global Offensive Stats",
                        "http://steamcommunity.com/profiles/%s".formatted(player.getSteamId()), avatarUrl)
                        .setThumbnail(player.getAvatarFull())
                        .setDescription("Stats for **%s**".formatted(player.getPersonaName()))
                        .addField("Kills", FormatUtil.number(kills), true)
                        .addField("Time played", "%.1fh".formatted(timePlayed), true)
                        .addField("MVPs", FormatUtil.number(mvps), true)
                        .addField("Win", "%.1f%%".formatted(win), true)
                        .addField("Accuracy", "%.1f%%".formatted(accuracy), true)
                        .addField("Headshot", "%.1f%%".formatted(headshot), true)
                        .addField("K/D Ratio", "%.2f".formatted(ratio), true));
    }

}
