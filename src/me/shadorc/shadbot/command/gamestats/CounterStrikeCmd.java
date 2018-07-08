package me.shadorc.shadbot.command.gamestats;

import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import discord4j.core.spec.EmbedCreateSpec;
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
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.GAMESTATS, names = { "cs", "csgo" })
public class CounterStrikeCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		context.requireArg();

		LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			String arg = context.getArg().get();
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
				String url = String.format("http://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/?key=%s&vanityurl=%s",
						APIKeys.get(APIKey.STEAM_API_KEY), NetUtils.encode(arg));
				JSONObject mainObj = new JSONObject(NetUtils.getJSON(url));
				JSONObject responseObj = mainObj.getJSONObject("response");
				steamid = responseObj.optString("steamid");
			}

			String url = String.format("http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=%s&steamids=%s",
					APIKeys.get(APIKey.STEAM_API_KEY), steamid);
			JSONObject mainUserObj = new JSONObject(NetUtils.getJSON(url));

			// Search users matching the steamID
			JSONArray players = mainUserObj.getJSONObject("response").getJSONArray("players");
			if(players.length() == 0) {
				loadingMsg.send(Emoji.MAGNIFYING_GLASS + " User not found.");
				return;
			}

			JSONObject userObj = players.getJSONObject(0);

			/*
			 * CommunityVisibilityState 1: Private 2: FriendsOnly 3: Public
			 */
			if(userObj.getInt("communityvisibilitystate") != 3) {
				loadingMsg.send(Emoji.ACCESS_DENIED + " This profile is private.");
				return;
			}

			url = String.format("http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v0002/?appid=730&key=%s&steamid=%s",
					APIKeys.get(APIKey.STEAM_API_KEY), steamid);

			String body = NetUtils.getBody(url);
			if(body.contains("500 Internal Server Error")) {
				loadingMsg.send(Emoji.ACCESS_DENIED + " The game details of this profile are not public.");
				return;
			}

			JSONObject mainStatsObj = new JSONObject(body);

			if(!mainStatsObj.has("playerstats") || !mainStatsObj.getJSONObject("playerstats").has("stats")) {
				loadingMsg.send(Emoji.MAGNIFYING_GLASS + " This user doesn't play Counter-Strike: Global Offensive.");
				return;
			}

			JSONArray statsArray = mainStatsObj.getJSONObject("playerstats").getJSONArray("stats");

			EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
					.setAuthor("Counter-Strike: Global Offensive Stats",
							"http://www.icon100.com/up/2841/256/csgo.png",
							"http://steamcommunity.com/profiles/" + steamid)
					.setThumbnail(userObj.getString("avatarfull"))
					.setDescription(String.format("Stats for **%s**", userObj.getString("personaname")))
					.addField("Kills", Integer.toString(this.getValue(statsArray, "total_kills")), true)
					.addField("Deaths", Integer.toString(this.getValue(statsArray, "total_deaths")), true)
					.addField("Ratio", String.format("%.2f", (float) this.getValue(statsArray, "total_kills") / this.getValue(statsArray, "total_deaths")), true)
					.addField("Total wins", Integer.toString(this.getValue(statsArray, "total_wins")), true)
					.addField("Total MVP", Integer.toString(this.getValue(statsArray, "total_mvps")), true);
			loadingMsg.send(embed);

		} catch (IOException err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}
	}

	private int getValue(JSONArray array, String key) {
		for(int i = 0; i < array.length(); i++) {
			JSONObject obj = array.getJSONObject(i);
			if(obj.getString("name").equals(key)) {
				return obj.getInt("value");
			}
		}
		return 0;
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show player's stats for Counter-Strike: Global Offensive.")
				.addArg("steamID", "steam ID, custom ID or profile URL", false)
				.build();
	}
}
