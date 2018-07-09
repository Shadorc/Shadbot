package me.shadorc.shadbot.command.gamestats;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.steam.player.PlayerSummariesResponse;
import me.shadorc.shadbot.api.steam.player.PlayerSummary;
import me.shadorc.shadbot.api.steam.resolver.ResolveVanityUrlResponse;
import me.shadorc.shadbot.api.steam.stats.Stats;
import me.shadorc.shadbot.api.steam.stats.UserStatsForGameResponse;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.GAMESTATS, names = { "cs", "csgo" })
public class CounterStrikeCmd extends AbstractCommand {

	private static final String PRIVACY_HELP_URL = "https://support.steampowered.com/kb_article.php?ref=4113-YUDH-6401";

	@Override
	public Mono<Void> execute(Context context) {
		String arg = context.requireArg();

		LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			String steamid = null;

			// The user provided an URL that can contains a pseudo or an ID
			if(arg.contains("/")) {
				List<String> splittedURl = StringUtils.split(arg, "/");
				arg = splittedURl.get(splittedURl.size() - 1);
			}

			// The user directly provided the ID
			if(NumberUtils.isPositiveLong(arg)) {
				steamid = arg;
			}
			// The user provided a pseudo
			else {
				final URL url = new URL(String.format("http://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/?key=%s&vanityurl=%s",
						APIKeys.get(APIKey.STEAM_API_KEY), NetUtils.encode(arg)));
				ResolveVanityUrlResponse response = Utils.MAPPER.readValue(url, ResolveVanityUrlResponse.class);
				steamid = response.getResponse().getSteamId();
			}

			URL url = new URL(String.format("http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=%s&steamids=%s",
					APIKeys.get(APIKey.STEAM_API_KEY), steamid));

			PlayerSummariesResponse playerSummary = Utils.MAPPER.readValue(url, PlayerSummariesResponse.class);

			// Search users matching the steamId
			List<PlayerSummary> players = playerSummary.getResponse().getPlayers();
			if(players.isEmpty()) {
				return loadingMsg.send(Emoji.MAGNIFYING_GLASS + " User not found.").then();
			}

			final PlayerSummary player = players.get(0);
			if(player.getCommunityVisibilityState() != 3) {
				return loadingMsg.send(Emoji.ACCESS_DENIED + " This profile is private, more info here: " + PRIVACY_HELP_URL).then();
			}

			url = new URL(String.format("http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v0002/?appid=730&key=%s&steamid=%s",
					APIKeys.get(APIKey.STEAM_API_KEY), steamid));

			final String body = NetUtils.getBody(url.toString());
			if(body.contains("500 Internal Server Error")) {
				return loadingMsg.send(Emoji.ACCESS_DENIED + " The game details of this profile are not public, more info here: " + PRIVACY_HELP_URL)
						.then();
			}

			UserStatsForGameResponse userStats = Utils.MAPPER.readValue(url, UserStatsForGameResponse.class);

			if(userStats.getPlayerStats() == null || userStats.getPlayerStats().getStats() == null) {
				return loadingMsg.send(Emoji.MAGNIFYING_GLASS + " This user doesn't play Counter-Strike: Global Offensive.").then();
			}

			final List<Stats> stats = userStats.getPlayerStats().getStats();

			final Map<String, Integer> statsMap = new HashMap<>();
			stats.forEach(stat -> statsMap.put(stat.getName(), stat.getValue()));

			EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
					.setAuthor("Counter-Strike: Global Offensive Stats",
							"http://www.icon100.com/up/2841/256/csgo.png",
							"http://steamcommunity.com/profiles/" + steamid)
					.setThumbnail(player.getAvatarFull())
					.setDescription(String.format("Stats for **%s**", player.getPersonaName()))
					.addField("Kills", statsMap.get("total_kills").toString(), true)
					.addField("Deaths", statsMap.get("total_deaths").toString(), true)
					.addField("Ratio", String.format("%.2f", (float) statsMap.get("total_kills") / statsMap.get("total_deaths")), true)
					.addField("Total wins", statsMap.get("total_wins").toString(), true)
					.addField("Total MVP", statsMap.get("total_mvps").toString(), true);

			return loadingMsg.send(embed).then();

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
