package me.shadorc.discordbot.command.gamestats;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;

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
import me.shadorc.discordbot.utils.NetUtils;
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
			String steamids;
			if(StringUtils.isInteger(context.getArg())) {
				steamids = context.getArg();
			} else {
				Document userPage = NetUtils.getDoc("https://steamcommunity.com/id/" + URLEncoder.encode(context.getArg(), "UTF-8") + "/");

				if(!userPage.getElementsByClass("error_ctn").isEmpty()) {
					BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " This user doesn't exist.", context.getChannel());
					return;
				}

				String html = userPage.getElementsByClass("responsive_page_template_content").html();
				JSONObject userObj = new JSONObject(html.substring(html.indexOf('{'), html.indexOf('}') + 1));
				steamids = userObj.getString("steamid");
			}

			JSONObject mainStatsObj = new JSONObject(IOUtils.toString(new URL(
					"http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v0002/?"
							+ "appid=730"
							+ "&key=" + Storage.getApiKey(ApiKeys.STEAM_API_KEY)
							+ "&steamid=" + steamids), "UTF-8"));

			JSONObject mainUserObj = new JSONObject(IOUtils.toString(new URL(
					"http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?"
							+ "key=" + Storage.getApiKey(ApiKeys.STEAM_API_KEY)
							+ "&steamids=" + steamids), "UTF-8"));

			JSONArray statsArray = mainStatsObj.getJSONObject("playerstats").getJSONArray("stats");
			JSONObject userObj = mainUserObj.getJSONObject("response").getJSONArray("players").getJSONObject(0);

			EmbedBuilder builder = new EmbedBuilder()
					.setLenient(true)
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

		} catch (HttpStatusException e) {
			BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " This user doesn't play to Counter-Strike: Global Offensive or doesn't exist.", context.getChannel());
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
