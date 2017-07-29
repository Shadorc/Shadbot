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
import me.shadorc.discordbot.utility.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class CounterStrikeCmd extends Command {

	public CounterStrikeCmd() {
		super(false, "cs", "csgo");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			throw new IllegalArgumentException();
		}

		try {
			String steamids;
			if(!Utils.isInteger(context.getArg())) {
				steamids = NetUtils.parseHTML(new URL("https://steamcommunity.com/id/" + context.getArg() + "/"), "\"steamid\":\"", "\"steamid\":\"", "\",\"");
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

			JSONArray statsArray = new JSONObject(NetUtils.getHTML(statsUrl)).getJSONObject("playerstats").getJSONArray("stats");
			JSONObject userObj = new JSONObject(NetUtils.getHTML(userUrl)).getJSONObject("response").getJSONArray("players").getJSONObject(0);

			EmbedBuilder builder = new EmbedBuilder()
					.withAuthorName("Statistiques Counter-Strike: Global Offensive")
					.withAuthorIcon("http://www.icon100.com/up/2841/256/csgo.png")
					.withThumbnail(userObj.getString("avatarfull"))
					.withColor(new Color(170, 196, 222))
					.withDesc("Statistiques pour **" + userObj.getString("personaname") + "**")
					.appendField("Tués", Integer.toString(this.getValue(statsArray, "total_kills")), true)
					.appendField("Morts",  Integer.toString(this.getValue(statsArray, "total_deaths")), true)
					.appendField("Ratio", String.format("%.2f", (float) this.getValue(statsArray, "total_kills")/this.getValue(statsArray, "total_deaths")), true)
					.appendField("Nombre de victoires",  Integer.toString(this.getValue(statsArray, "total_wins")), true)
					.appendField("Nombre de fois meilleur joueur",  Integer.toString(this.getValue(statsArray, "total_mvps")), true)
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

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Affiche les statistiques d'un utilisateur pour le jeu Counter-Strike: Global Offensive.**")
				.appendField("Utilisation", "/cs <steamID>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
