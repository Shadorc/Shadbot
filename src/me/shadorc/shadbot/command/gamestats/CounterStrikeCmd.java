package me.shadorc.shadbot.command.gamestats;

import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.CastUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.LoadingMessage;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.GAMESTATS, names = { "cs", "csgo" })
public class CounterStrikeCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		LoadingMessage loadingMsg = new LoadingMessage("Loading CS:GO stats...", context.getChannel());
		loadingMsg.send();

		try {
			String arg = context.getArg();
			String steamid = null;

			// The user provided an URL that can contains a pseudo or an ID
			if(arg.contains("/")) {
				List<String> splittedURl = StringUtils.split(arg, "/");
				arg = splittedURl.get(splittedURl.size() - 1);
			}

			// The user directly provided the ID
			if(CastUtils.isPositiveLong(arg)) {
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
				loadingMsg.edit(Emoji.MAGNIFYING_GLASS + " User not found.");
				return;
			}

			JSONObject userObj = players.getJSONObject(0);

			/*
			 * CommunityVisibilityState
			 * 1: Private
			 * 2: FriendsOnly
			 * 3: Public
			 */
			if(userObj.getInt("communityvisibilitystate") != 3) {
				loadingMsg.edit(Emoji.ACCESS_DENIED + " This profile is private.");
				return;
			}

			url = String.format("http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v0002/?appid=730&key=%s&steamid=%s",
					APIKeys.get(APIKey.STEAM_API_KEY), steamid);
			JSONObject mainStatsObj = new JSONObject(NetUtils.getJSON(url));

			if(!mainStatsObj.has("playerstats") || !mainStatsObj.getJSONObject("playerstats").has("stats")) {
				loadingMsg.edit(Emoji.MAGNIFYING_GLASS + " This user doesn't play Counter-Strike: Global Offensive.");
				return;
			}

			JSONArray statsArray = mainStatsObj.getJSONObject("playerstats").getJSONArray("stats");

			EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
					.setLenient(true)
					.withAuthorName("Counter-Strike: Global Offensive Stats")
					.withAuthorIcon("http://www.icon100.com/up/2841/256/csgo.png")
					.withAuthorUrl("http://steamcommunity.com/profiles/" + steamid)
					.withThumbnail(userObj.getString("avatarfull"))
					.appendDescription(String.format("Stats for **%s**", userObj.getString("personaname")))
					.appendField("Kills", Integer.toString(this.getValue(statsArray, "total_kills")), true)
					.appendField("Deaths", Integer.toString(this.getValue(statsArray, "total_deaths")), true)
					.appendField("Ratio", String.format("%.2f", (float) this.getValue(statsArray, "total_kills") / this.getValue(statsArray, "total_deaths")), true)
					.appendField("Total wins", Integer.toString(this.getValue(statsArray, "total_wins")), true)
					.appendField("Total MVP", Integer.toString(this.getValue(statsArray, "total_mvps")), true);
			loadingMsg.edit(embed.build());

		} catch (JSONException | IOException err) {
			loadingMsg.delete();
			Utils.handle("getting Counter-Strike: Global Offensive stats", context, err);
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
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show player's stats for Counter-Strike: Global Offensive.")
				.addArg("steamID", "steam ID, custom ID or profile URL", false)
				.build();
	}
}
