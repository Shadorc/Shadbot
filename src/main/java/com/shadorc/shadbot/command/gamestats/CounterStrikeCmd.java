package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.gamestats.steam.player.PlayerSummariesResponse;
import com.shadorc.shadbot.api.gamestats.steam.player.PlayerSummary;
import com.shadorc.shadbot.api.gamestats.steam.resolver.ResolveVanityUrlResponse;
import com.shadorc.shadbot.api.gamestats.steam.stats.PlayerStats;
import com.shadorc.shadbot.api.gamestats.steam.stats.Stats;
import com.shadorc.shadbot.api.gamestats.steam.stats.UserStatsForGameResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.Credentials;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import com.shadorc.shadbot.utils.StringUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CounterStrikeCmd extends BaseCmd {

    private static final String PRIVACY_HELP_URL = "https://support.steampowered.com/kb_article.php?ref=4113-YUDH-6401";

    public CounterStrikeCmd() {
        super(CommandCategory.GAMESTATS, List.of("cs", "csgo"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());
        return Mono.fromCallable(() -> {
            String identificator = arg;

            // The user provided an URL that can contains a pseudo or an ID
            if (arg.contains("/")) {
                final List<String> splittedURl = StringUtils.split(arg, "/");
                identificator = splittedURl.get(splittedURl.size() - 1);
            }

            String steamId;
            // The user directly provided the ID
            if (NumberUtils.isPositiveLong(identificator)) {
                steamId = identificator;
            }
            // The user provided a pseudo
            else {
                final String resolveVanityUrl = String.format("http://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/?key=%s&vanityurl=%s",
                        Credentials.get(Credential.STEAM_API_KEY), NetUtils.encode(identificator));
                final ResolveVanityUrlResponse response = NetUtils.get(resolveVanityUrl, ResolveVanityUrlResponse.class).block();
                steamId = response.getResponse().getSteamId();
            }

            final String playerSummariesUrl = String.format("http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=%s&steamids=%s",
                    Credentials.get(Credential.STEAM_API_KEY), steamId);

            final PlayerSummariesResponse playerSummary = NetUtils.get(playerSummariesUrl, PlayerSummariesResponse.class).block();

            // Search users matching the steamId
            final List<PlayerSummary> players = playerSummary.getResponse().getPlayers();
            if (players.isEmpty()) {
                return updatableMsg.setContent(
                        String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) Steam player not found.",
                                context.getUsername()));
            }

            final PlayerSummary player = players.get(0);
            if (player.getCommunityVisibilityState() != 3) {
                return updatableMsg.setContent(
                        String.format(Emoji.ACCESS_DENIED + " (**%s**) This profile is private, more info here: <%s>",
                                context.getUsername(), PRIVACY_HELP_URL));
            }

            final String userStatsUrl = String.format("http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v0002/?appid=730&key=%s&steamid=%s",
                    Credentials.get(Credential.STEAM_API_KEY), steamId);

            final String body = NetUtils.get(userStatsUrl).block();
            if (body.contains("500 Internal Server Error")) {
                return updatableMsg.setContent(
                        String.format(Emoji.ACCESS_DENIED + " (**%s**) The game details of this profile are not public, more info here: <%s>",
                                context.getUsername(), PRIVACY_HELP_URL));
            }

            final UserStatsForGameResponse userStats = NetUtils.get(userStatsUrl, UserStatsForGameResponse.class).block();

            if (userStats.getPlayerStats().flatMap(PlayerStats::getStats).isEmpty()) {
                return updatableMsg.setContent(
                        String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) This user doesn't play Counter-Strike: Global Offensive.",
                                context.getUsername()));
            }

            final List<Stats> stats = userStats.getPlayerStats().flatMap(PlayerStats::getStats).get();

            final Map<String, Integer> statsMap = new HashMap<>();
            stats.forEach(stat -> statsMap.put(stat.getName(), stat.getValue()));

            return updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                    .andThen(embed -> embed.setAuthor("Counter-Strike: Global Offensive Stats",
                            "http://steamcommunity.com/profiles/" + steamId,
                            context.getAvatarUrl())
                            .setThumbnail(player.getAvatarFull())
                            .setDescription(String.format("Stats for **%s**", player.getPersonaName()))
                            .addField("Kills", statsMap.get("total_kills").toString(), true)
                            .addField("Deaths", statsMap.get("total_deaths").toString(), true)
                            .addField("Total wins", statsMap.get("total_wins").toString(), true)
                            .addField("Total MVP", statsMap.get("total_mvps").toString(), true)
                            .addField("Ratio", String.format("%.2f", (float) statsMap.get("total_kills") / statsMap.get("total_deaths")), false)));
        })
                .flatMap(UpdatableMessage::send)
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show player's stats for Counter-Strike: Global Offensive.")
                .addArg("steamID", "steam ID, custom ID or profile URL", false)
                .build();
    }
}
