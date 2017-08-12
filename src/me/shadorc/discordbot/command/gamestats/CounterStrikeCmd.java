package me.shadorc.discordbot.command.gamestats;

import java.io.IOException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.ApiKeys;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.HtmlUtils;
import me.shadorc.discordbot.utils.StringUtils;
import sx.blah.discord.util.EmbedBuilder;

public class CounterStrikeCmd extends Command {

	public CounterStrikeCmd() {
		super(false, "cs", "csgo");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(context.getArg() == null) {
			throw new MissingArgumentException();
		}

		try {
			String steamids;
			if(!StringUtils.isInteger(context.getArg())) {
				steamids = HtmlUtils.parseHTML(new URL("https://steamcommunity.com/id/" + context.getArg() + "/"), "\"steamid\":\"", "\"steamid\":\"", "\",\"");
			} else {
				steamids = context.getArg();
			}

			URL statsUrl = new URL("http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v0002/?"
					+ "appid=730"
					+ "&key=" + Storage.getApiKey(ApiKeys.STEAM_API_KEY)
					+ "&steamid=" + steamids);

			URL userUrl = new URL("http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?"
					+ "key=" + Storage.getApiKey(ApiKeys.STEAM_API_KEY)
					+ "&steamids=" + steamids);

			JSONArray statsArray = new JSONObject(HtmlUtils.getHTML(statsUrl)).getJSONObject("playerstats").getJSONArray("stats");
			JSONObject userObj = new JSONObject(HtmlUtils.getHTML(userUrl)).getJSONObject("response").getJSONArray("players").getJSONObject(0);

			EmbedBuilder builder = new EmbedBuilder()
					.withAuthorName("Statistiques Counter-Strike: Global Offensive")
					.withAuthorIcon("http://www.icon100.com/up/2841/256/csgo.png")
					.withThumbnail(userObj.getString("avatarfull"))
					.withColor(Config.BOT_COLOR)
					.withDesc("Stats for **" + userObj.getString("personaname") + "**")
					.appendField("Kills", Integer.toString(this.getValue(statsArray, "total_kills")), true)
					.appendField("Deaths", Integer.toString(this.getValue(statsArray, "total_deaths")), true)
					.appendField("Ratio", String.format("%.2f", (float) this.getValue(statsArray, "total_kills") / this.getValue(statsArray, "total_deaths")), true)
					.appendField("Total wins", Integer.toString(this.getValue(statsArray, "total_wins")), true)
					.appendField("Total MVP", Integer.toString(this.getValue(statsArray, "total_mvps")), true)
					.withFooterText("Steam Profile: http://steamcommunity.com/profiles/" + context.getArg() + "/");
			BotUtils.sendEmbed(builder.build(), context.getChannel());

		} catch (IOException e) {
			BotUtils.sendMessage(Emoji.WARNING + " Steam ID is invalid.", context.getChannel());
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
				.withAuthorName("Help for /" + this.getNames()[0])
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show stats of a player for Counter-Strike: Global Offensive.**")
				.appendField("Usage", "/cs <steamID>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
