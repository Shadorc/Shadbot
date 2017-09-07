package me.shadorc.discordbot.command.gamestats;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.ApiKeys;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class DiabloCmd extends AbstractCommand {

	private final DecimalFormat formatter = new DecimalFormat("#,###");
	private final RateLimiter rateLimiter;

	public DiabloCmd() {
		super(Role.USER, "diablo", "d3");
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

		String[] splitArgs = context.getArg().split(" ", 2);
		if(splitArgs.length != 2) {
			throw new MissingArgumentException();
		}

		String region = splitArgs[0].toLowerCase();
		if(!Arrays.asList("eu", "us", "tw", "kr").contains(region)) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " Region is invalid. Options: eu, us, tw, kr.", context.getChannel());
			return;
		}

		String battletag = splitArgs[1].replaceAll("#", "-");
		try {
			JSONObject mainObj = new JSONObject(IOUtils.toString(new URL("https://" + region + ".api.battle.net/d3/profile"
					+ "/" + URLEncoder.encode(battletag, "UTF-8") + "/?"
					+ "locale=en_GB&"
					+ "apikey=" + Storage.getApiKey(ApiKeys.BLIZZARD_API_KEY)), "UTF-8"));

			if(mainObj.has("code") && mainObj.getString("code").equals("NOTFOUND")) {
				BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " This user doesn't play Diablo 3 or doesn't exist.", context.getChannel());
				return;
			}

			List<JSONObject> heroesList = new ArrayList<>();
			JSONArray heroesArray = mainObj.getJSONArray("heroes");
			for(int i = 0; i < heroesArray.length(); i++) {
				JSONObject heroObj = new JSONObject(IOUtils.toString(new URL(
						"https://" + region + ".api.battle.net/d3/profile/" + URLEncoder.encode(battletag, "UTF-8")
								+ "/hero/" + heroesArray.getJSONObject(i).getLong("id")
								+ "?locale=en_GB"
								+ "&apikey=" + Storage.getApiKey(ApiKeys.BLIZZARD_API_KEY)), "UTF-8"));
				heroesList.add(heroObj);
			}

			EmbedBuilder builder = Utils.getDefaultEmbed()
					.setLenient(true)
					.withAuthorName("Diablo 3 Stats")
					.withThumbnail("http://osx.wdfiles.com/local--files/icon:d3/D3.png")
					.appendDescription("Stats for **" + mainObj.getString("battleTag") + "** (Guild: **" + mainObj.getString("guildName") + "**)")
					.appendField("__Parangon level__",
							"**Normal:** " + mainObj.getInt("paragonLevel")
									+ "\n**Hardcore:** " + mainObj.getInt("paragonLevelHardcore"), true)
					.appendField("__Season Parangon level__",
							"**Normal:** " + mainObj.getInt("paragonLevelSeason")
									+ "\n**Hardcore:** " + mainObj.getInt("paragonLevelSeasonHardcore"), true)
					.appendField("__Heroes__",
							StringUtils.formatList(heroesList,
									heroObj -> "**" + heroObj.getString("name") + "** "
											+ "(*" + StringUtils.capitalize(heroObj.getString("class").replace("-", " ")) + "*)", "\n"), true)
					.appendField("__Damage__",
							StringUtils.formatList(heroesList,
									heroObj -> formatter.format(heroObj.getJSONObject("stats").getDouble("damage")) + " DPS", "\n"), true);
			BotUtils.sendEmbed(builder.build(), context.getChannel());

		} catch (FileNotFoundException err) {
			BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " This user doesn't play Diablo 3 or doesn't exist.", context.getChannel());
		} catch (JSONException | IOException err) {
			LogUtils.error("Something went wrong while getting Diablo 3 stats.... Please, try again later.", err, context);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show user stats for Diablo 3.**")
				.appendField("Usage", context.getPrefix() + "diablo <eu|us|tw|kr> <battletag#0000>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
