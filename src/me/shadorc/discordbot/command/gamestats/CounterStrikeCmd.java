package me.shadorc.discordbot.command.gamestats;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.ApiKeys;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.StringUtils;
import sx.blah.discord.util.EmbedBuilder;

public class CounterStrikeCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public CounterStrikeCmd() {
		super(Role.USER, "cs", "csgo");
		this.rateLimiter = new RateLimiter(2, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		if(rateLimiter.isLimited(context.getGuild(), context.getAuthor())) {
			if(!rateLimiter.isWarned(context.getGuild(), context.getAuthor())) {
				rateLimiter.warn("Take it easy, don't spam :)", context);
			}
			return;
		}

		try {
			String steamid = null;
			if(context.getArg().contains("/")) {
				String[] splittedUrl = context.getArg().split("/");
				steamid = splittedUrl[splittedUrl.length - 1].trim();
			} else if(StringUtils.isLong(context.getArg())) {
				steamid = context.getArg().trim();
			} else {
				JSONObject mainObj = new JSONObject(IOUtils.toString(new URL("http://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/?"
						+ "key=" + Storage.getApiKey(ApiKeys.STEAM_API_KEY)
						+ "&vanityurl=" + URLEncoder.encode(context.getArg(), "UTF-8")), "UTF-8"));
				JSONObject responseObj = mainObj.getJSONObject("response");
				if(responseObj.has("steamid")) {
					steamid = responseObj.getString("steamid");
				}
			}

			JSONObject mainStatsObj = new JSONObject(IOUtils.toString(new URL(
					"http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v0002/?"
							+ "appid=730"
							+ "&key=" + Storage.getApiKey(ApiKeys.STEAM_API_KEY)
							+ "&steamid=" + steamid), "UTF-8"));

			JSONObject mainUserObj = new JSONObject(IOUtils.toString(new URL(
					"http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?"
							+ "key=" + Storage.getApiKey(ApiKeys.STEAM_API_KEY)
							+ "&steamids=" + steamid), "UTF-8"));

			JSONArray statsArray = mainStatsObj.getJSONObject("playerstats").getJSONArray("stats");
			JSONObject userObj = mainUserObj.getJSONObject("response").getJSONArray("players").getJSONObject(0);

			EmbedBuilder builder = new EmbedBuilder()
					.setLenient(true)
					.withAuthorName("Counter-Strike: Global Offensive Stats")
					.withAuthorIcon("http://www.icon100.com/up/2841/256/csgo.png")
					.withThumbnail(userObj.getString("avatarfull"))
					.withColor(Config.BOT_COLOR)
					.withDesc("Stats for **" + userObj.getString("personaname") + "**")
					.appendField("Kills", Integer.toString(this.getValue(statsArray, "total_kills")), true)
					.appendField("Deaths", Integer.toString(this.getValue(statsArray, "total_deaths")), true)
					.appendField("Ratio", String.format("%.2f", (float) this.getValue(statsArray, "total_kills") / this.getValue(statsArray, "total_deaths")), true)
					.appendField("Total wins", Integer.toString(this.getValue(statsArray, "total_wins")), true)
					.appendField("Total MVP", Integer.toString(this.getValue(statsArray, "total_mvps")), true)
					.withFooterText("Steam Profile: http://steamcommunity.com/profiles/" + steamid);
			BotUtils.sendEmbed(builder.build(), context.getChannel());
		} catch (IOException e) {
			if(e.getMessage().contains("400")) {
				BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " This user doesn't play to Counter-Strike: Global Offensive or doesn't exist.", context.getChannel());
			} else {
				LogUtils.error("Something went wrong while getting Counter-Strike: Global Offensive stats.... Please, try again later.", e, context.getChannel());
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
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show stats of a player for Counter-Strike: Global Offensive.**")
				.appendField("Usage", context.getPrefix() + "cs <steamID>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
