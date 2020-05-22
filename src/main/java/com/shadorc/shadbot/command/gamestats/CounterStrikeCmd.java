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
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.*;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;
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

    public CounterStrikeCmd() {
        super(CommandCategory.GAMESTATS, List.of("cs", "csgo"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());
        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading CS:GO stats...", context.getUsername()))
                .send()
                .thenReturn(this.getIdentificator(arg))
                .flatMap(this::getSteamId)
                .flatMap(this::getPlayerSummary)
                .flatMap(player -> this.getStats(context, updatableMsg, player))
                .switchIfEmpty(Mono.defer(() -> Mono.just(updatableMsg.setContent(
                        String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) Steam player not found.", context.getUsername())))))
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    /**
     * @return The identificator, either directly provided or extracted from an URL.
     */
    private String getIdentificator(String arg) {
        // The user provided an URL that can contains a pseudo or an ID
        if (arg.contains("/")) {
            final List<String> splittedUrl = StringUtils.split(arg, "/");
            if (splittedUrl.isEmpty()) {
                throw new CommandException("Invalid steam ID.");
            }
            return splittedUrl.get(splittedUrl.size() - 1);
        } else {
            return arg;
        }
    }

    /**
     * @return The identificator converted as an ID or empty if not found.
     */
    private Mono<String> getSteamId(String identificator) {
        // The user directly provided the ID
        if (NumberUtils.isPositiveLong(identificator)) {
            return Mono.just(identificator);
        }
        // The user provided a pseudo
        else {
            final String url = String.format("%s?key=%s&vanityurl=%s",
                    RESOLVE_VANITY_URL, CredentialManager.getInstance().get(Credential.STEAM_API_KEY), NetUtils.encode(identificator));
            return NetUtils.get(url, ResolveVanityUrlResponse.class)
                    .map(ResolveVanityUrlResponse::getResponse)
                    .map(Response::getSteamId)
                    .flatMap(Mono::justOrEmpty);
        }
    }

    /**
     * @return The {@link PlayerSummary} corresponding to the provided steam ID.
     */
    private Mono<PlayerSummary> getPlayerSummary(String steamId) {
        final String url = String.format("%s?key=%s&steamids=%s",
                PLAYER_SUMMARIES_URL, CredentialManager.getInstance().get(Credential.STEAM_API_KEY), steamId);
        return NetUtils.get(url, PlayerSummariesResponse.class)
                .map(PlayerSummariesResponse::getResponse)
                // Users matching the steamId
                .map(PlayerSummaries::getPlayers)
                .flatMapMany(Flux::fromIterable)
                .next();
    }

    private Mono<UpdatableMessage> getStats(Context context, UpdatableMessage updatableMsg, PlayerSummary player) {
        if (player.getCommunityVisibilityState() != PlayerSummary.CommunityVisibilityState.PUBLIC) {
            return Mono.just(updatableMsg.setContent(
                    String.format(Emoji.ACCESS_DENIED + " (**%s**) This profile is private, more info here: <%s>",
                            context.getUsername(), PRIVACY_HELP_URL)));
        }

        final String userStatsUrl = String.format("%s?appid=730&key=%s&steamid=%s",
                USER_STATS_FOR_GAME_URL, CredentialManager.getInstance().get(Credential.STEAM_API_KEY), player.getSteamId());

        return NetUtils.get(userStatsUrl)
                .flatMap(body -> {
                    if (body.contains("500 Internal Server Error")) {
                        return Mono.just(updatableMsg.setContent(
                                String.format(Emoji.ACCESS_DENIED + " (**%s**) The game details of this " +
                                                "profile are not public, more info here: <%s>",
                                        context.getUsername(), PRIVACY_HELP_URL)));
                    }

                    return Mono.fromCallable(() -> Utils.MAPPER.readValue(body, UserStatsForGameResponse.class))
                            .map(userStats -> {
                                if (userStats.getPlayerStats().flatMap(PlayerStats::getStats).isEmpty()) {
                                    return updatableMsg.setContent(
                                            String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) This user " +
                                                            "doesn't play Counter-Strike: Global Offensive.",
                                                    context.getUsername()));
                                }

                                final Map<String, Integer> statsMap = userStats.getPlayerStats()
                                        .flatMap(PlayerStats::getStats)
                                        .orElseThrow()
                                        .stream()
                                        .collect(Collectors.toMap(Stats::getName, Stats::getValue));

                                final int kills = statsMap.get("total_kills");
                                final int deaths = statsMap.get("total_deaths");
                                final float ratio = (float) kills / deaths;
                                final int wins = statsMap.get("total_wins");
                                final int mvps = statsMap.get("total_mvps");

                                return updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                                        .andThen(embed -> embed.setAuthor("Counter-Strike: Global Offensive Stats",
                                                String.format("http://steamcommunity.com/profiles/%s", player.getSteamId()),
                                                context.getAvatarUrl())
                                                .setThumbnail(player.getAvatarFull())
                                                .setDescription(String.format("Stats for **%s**", player.getPersonaName()))
                                                .addField("Kills", FormatUtils.number(kills), true)
                                                .addField("Deaths", FormatUtils.number(deaths), true)
                                                .addField("Ratio", String.format("%.2f", ratio), true)
                                                .addField("Total wins", FormatUtils.number(wins), true)
                                                .addField("Total MVP", FormatUtils.number(mvps), true)));
                            });
                });
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Show player's stats for Counter-Strike: Global Offensive.")
                .addArg("steamID", "steam ID, custom ID or profile URL", false)
                .setExample(String.format("%s%s shadorc", context.getPrefix(), this.getName()))
                .build();
    }
}
