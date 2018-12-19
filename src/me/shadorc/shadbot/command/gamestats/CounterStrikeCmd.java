package me.shadorc.shadbot.command.gamestats;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.gamestats.steam.player.PlayerSummariesResponse;
import me.shadorc.shadbot.api.gamestats.steam.player.PlayerSummary;
import me.shadorc.shadbot.api.gamestats.steam.resolver.ResolveVanityUrlResponse;
import me.shadorc.shadbot.api.gamestats.steam.stats.Stats;
import me.shadorc.shadbot.api.gamestats.steam.stats.UserStatsForGameResponse;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.credential.Credential;
import me.shadorc.shadbot.data.credential.Credentials;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.GAMESTATS, names = { "cs", "csgo" })
public class CounterStrikeCmd extends AbstractCommand {

	private static final String PRIVACY_HELP_URL = "https://support.steampowered.com/kb_article.php?ref=4113-YUDH-6401";

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			String identificator = arg;

			// The user provided an URL that can contains a pseudo or an ID
			if(arg.contains("/")) {
				final List<String> splittedURl = StringUtils.split(arg, "/");
				identificator = splittedURl.get(splittedURl.size() - 1);
			}

			String steamId;
			// The user directly provided the ID
			if(NumberUtils.isPositiveLong(identificator)) {
				steamId = identificator;
			}
			// The user provided a pseudo
			else {
				final URL resolveVanityUrl = new URL(String.format("http://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/?key=%s&vanityurl=%s",
						Credentials.get(Credential.STEAM_API_KEY), NetUtils.encode(identificator)));
				final ResolveVanityUrlResponse response = Utils.MAPPER.readValue(resolveVanityUrl, ResolveVanityUrlResponse.class);
				steamId = response.getResponse().getSteamId();
			}

			final URL playerSummariesUrl = new URL(String.format("http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=%s&steamids=%s",
					Credentials.get(Credential.STEAM_API_KEY), steamId));

			final PlayerSummariesResponse playerSummary = Utils.MAPPER.readValue(playerSummariesUrl, PlayerSummariesResponse.class);

			// Search users matching the steamId
			final List<PlayerSummary> players = playerSummary.getResponse().getPlayers();
			if(players.isEmpty()) {
				return loadingMsg.send(
						String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) Steam player not found.", context.getUsername()))
						.then();
			}

			final PlayerSummary player = players.get(0);
			if(player.getCommunityVisibilityState() != 3) {
				return loadingMsg.send(Emoji.ACCESS_DENIED + " This profile is private, more info here: " + PRIVACY_HELP_URL).then();
			}

			final URL userStatsUrl = new URL(String.format("http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v0002/?appid=730&key=%s&steamid=%s",
					Credentials.get(Credential.STEAM_API_KEY), steamId));

			final String body = NetUtils.getBody(userStatsUrl.toString());
			if(body.contains("500 Internal Server Error")) {
				return loadingMsg.send(Emoji.ACCESS_DENIED + " The game details of this profile are not public, more info here: " + PRIVACY_HELP_URL)
						.then();
			}

			final UserStatsForGameResponse userStats = Utils.MAPPER.readValue(userStatsUrl, UserStatsForGameResponse.class);

			if(userStats.getPlayerStats() == null || userStats.getPlayerStats().getStats() == null) {
				return loadingMsg.send(Emoji.MAGNIFYING_GLASS + " This user doesn't play Counter-Strike: Global Offensive.").then();
			}

			final List<Stats> stats = userStats.getPlayerStats().getStats();

			final Map<String, Integer> statsMap = new HashMap<>();
			stats.forEach(stat -> statsMap.put(stat.getName(), stat.getValue()));

			return context.getAvatarUrl()
					.map(avatarUrl -> EmbedUtils.getDefaultEmbed()
							.setAuthor("Counter-Strike: Global Offensive Stats",
									"http://steamcommunity.com/profiles/" + steamId,
									avatarUrl)
							.setThumbnail(player.getAvatarFull())
							.setDescription(String.format("Stats for **%s**", player.getPersonaName()))
							.addField("Kills", statsMap.get("total_kills").toString(), true)
							.addField("Deaths", statsMap.get("total_deaths").toString(), true)
							.addField("Total wins", statsMap.get("total_wins").toString(), true)
							.addField("Total MVP", statsMap.get("total_mvps").toString(), true)
							.addField("Ratio", String.format("%.2f", (float) statsMap.get("total_kills") / statsMap.get("total_deaths")), false))
					.flatMap(loadingMsg::send)
					.then();

		} catch (IOException err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}

	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show player's stats for Counter-Strike: Global Offensive.")
				.addArg("steamID", "steam ID, custom ID or profile URL", false)
				.build();
	}
}
