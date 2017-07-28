package me.shadorc.discordbot.command.info;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.ApiKeys;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.NetUtils;
import sx.blah.discord.util.EmbedBuilder;

public class CounterStrikeCmd extends Command {

	public CounterStrikeCmd() {
		super(false, "cs", "csgo");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			BotUtils.sendMessage(Emoji.WARNING + " Veuillez indiquer le Steam ID de l'utilisateur.", context.getChannel());
			return;
		}

		try {
			URL statsUrl = new URL("http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v0002/?"
					+ "appid=730"
					+ "&key=" + Storage.getApiKey(ApiKeys.STEAM_API_KEY)
					+ "&steamid=" + context.getArg());

			URL userUrl = new URL("http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?"
					+ "key=" + Storage.getApiKey(ApiKeys.STEAM_API_KEY)
					+ "&steamids=" + context.getArg());

			JSONArray statsArray = new JSONObject(NetUtils.getHTML(statsUrl)).getJSONObject("playerstats").getJSONArray("stats");
			JSONObject userObj = new JSONObject(NetUtils.getHTML(userUrl)).getJSONObject("response").getJSONArray("players").getJSONObject(0);

			EmbedBuilder builder = new EmbedBuilder()
					.withAuthorName("Statistiques Counter-Strike: Global Offensive")
					.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
					.withThumbnail(userObj.getString("avatarfull"))
					.withColor(new Color(170, 196, 222))
					.withDesc("Statistiques pour **" + userObj.getString("personaname") + "**")
					.appendField("Tués", Integer.toString(this.getValue(statsArray, "total_kills")), true)
					.appendField("Morts",  Integer.toString(this.getValue(statsArray, "total_deaths")), true)
					.appendField("Ratio", String.format("%.1f", (float) this.getValue(statsArray, "total_kills")/this.getValue(statsArray, "total_deaths")), true)
					.appendField("Nombre de victoires",  Integer.toString(this.getValue(statsArray, "total_wins")), true)
					.appendField("Nombre de fois meilleur joueur",  Integer.toString(this.getValue(statsArray, "total_mvps")), true)
					.withFooterIcon("https://upload.wikimedia.org/wikipedia/commons/thumb/8/83/Steam_icon_logo.svg/1024px-Steam_icon_logo.svg.png")
					.withFooterText("Profile Steam : http://steamcommunity.com/profiles/" + context.getArg() + "/");
			BotUtils.sendEmbed(builder.build(), context.getChannel());

		} catch (IOException e) {
			BotUtils.sendMessage(Emoji.WARNING + " Steam ID invalide.", context.getChannel());
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

}
