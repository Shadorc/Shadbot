package me.shadorc.discordbot.command.gamestats;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.Config.APIKey;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class CounterStrikeCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public CounterStrikeCmd() {
		super(Role.USER, "cs", "csgo");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		try {
			String steamid = null;
			if(context.getArg().contains("/")) {
				String[] splittedUrl = context.getArg().split("/");
				steamid = splittedUrl[splittedUrl.length - 1].trim();
			} else if(StringUtils.isPositiveLong(context.getArg())) {
				steamid = context.getArg().trim();
			} else {
				JSONObject mainObj = new JSONObject(NetUtils.getBody("http://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/?"
						+ "key=" + Config.get(APIKey.STEAM_API_KEY)
						+ "&vanityurl=" + URLEncoder.encode(context.getArg(), "UTF-8")));
				JSONObject responseObj = mainObj.getJSONObject("response");
				if(responseObj.has("steamid")) {
					steamid = responseObj.getString("steamid");
				}
			}

			JSONObject mainUserObj = new JSONObject(NetUtils.getBody("http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?"
					+ "key=" + Config.get(APIKey.STEAM_API_KEY)
					+ "&steamids=" + steamid));

			JSONArray players = mainUserObj.getJSONObject("response").getJSONArray("players");

			if(players.length() == 0) {
				BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " User not found.", context.getChannel());
				return;
			}

			JSONObject userObj = mainUserObj.getJSONObject("response").getJSONArray("players").getJSONObject(0);

			/*
			 * CommunityVisibilityState
			 * 1: Private
			 * 2: FriendsOnly
			 * 3: Public
			 */
			if(userObj.getInt("communityvisibilitystate") != 3) {
				BotUtils.sendMessage(Emoji.ACCESS_DENIED + " This profile is private.", context.getChannel());
				return;
			}

			JSONObject mainStatsObj = new JSONObject(NetUtils.getBody("http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v0002/?"
					+ "appid=730"
					+ "&key=" + Config.get(APIKey.STEAM_API_KEY)
					+ "&steamid=" + steamid));

			JSONArray statsArray = mainStatsObj.getJSONObject("playerstats").getJSONArray("stats");

			EmbedBuilder builder = new EmbedBuilder()
					.setLenient(true)
					.withAuthorName("Counter-Strike: Global Offensive Stats")
					.withAuthorIcon("http://www.icon100.com/up/2841/256/csgo.png")
					.withUrl("http://steamcommunity.com/profiles/" + steamid)
					.withThumbnail(userObj.getString("avatarfull"))
					.withColor(Config.BOT_COLOR)
					.appendDescription("Stats for **" + userObj.getString("personaname") + "**")
					.appendField("Kills", Integer.toString(this.getValue(statsArray, "total_kills")), true)
					.appendField("Deaths", Integer.toString(this.getValue(statsArray, "total_deaths")), true)
					.appendField("Ratio", String.format("%.2f", (float) this.getValue(statsArray, "total_kills") / this.getValue(statsArray, "total_deaths")), true)
					.appendField("Total wins", Integer.toString(this.getValue(statsArray, "total_wins")), true)
					.appendField("Total MVP", Integer.toString(this.getValue(statsArray, "total_mvps")), true);
			BotUtils.sendEmbed(builder.build(), context.getChannel());

		} catch (JSONException | IOException err) {
			if(err.getMessage().contains("400")) {
				BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " This user doesn't play Counter-Strike: Global Offensive or doesn't exist.", context.getChannel());
			} else {
				LogUtils.error("Something went wrong while getting Counter-Strike: Global Offensive stats.... Please, try again later.", err, context);
			}
		}
	}

	private int getValue(JSONArray array, String key) {
		for(int i = 0; i < array.length(); i++) {
			JSONObject obj = array.getJSONObject(i);
			if(obj.getString("name").equals(key)) {
				return obj.getInt("value");
			}
		}
		return -1;
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show player's stats for Counter-Strike: Global Offensive.**")
				.appendField("Usage", context.getPrefix() + "cs <steamID>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
