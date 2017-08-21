package me.shadorc.discordbot.command.gamestats;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.ApiKeys;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.util.EmbedBuilder;

public class DiabloCmd extends AbstractCommand {

	private final DecimalFormat formatter = new DecimalFormat("#,###");

	public DiabloCmd() {
		super(Role.USER, "diablo", "d3");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
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
				BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " This user doesn't play to Diablo 3 or doesn't exist.", context.getChannel());
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

			EmbedBuilder builder = new EmbedBuilder()
					.setLenient(true)
					.withAuthorName("Diablo 3 Stats")
					.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
					.withThumbnail("http://osx.wdfiles.com/local--files/icon:d3/D3.png")
					.withColor(Config.BOT_COLOR)
					.appendDescription("Stats for **" + mainObj.getString("battleTag") + "** (Guild: **" + mainObj.getString("guildName") + "**)")
					.appendField("__Parangon level__",
							"**Normal:** " + mainObj.getInt("paragonLevel")
									+ "\n**Hardcore:** " + mainObj.getInt("paragonLevelHardcore"), true)
					.appendField("__Season Parangon level__",
							"**Normal:** " + mainObj.getInt("paragonLevelSeason")
									+ "\n**Hardcore:** " + mainObj.getInt("paragonLevelSeasonHardcore"), true)
					.appendField("__Heroes__",
							heroesList.stream().map(
									heroObj -> "**" + heroObj.getString("name") + "** "
											+ "(*" + StringUtils.capitalize(heroObj.getString("class").replace("-", " ")) + "*)")
									.collect(Collectors.joining("\n")), true)
					.appendField("__Damage__",
							heroesList.stream().map(
									heroObj -> formatter.format(heroObj.getJSONObject("stats").getDouble("damage")) + " DPS")
									.collect(Collectors.joining("\n")), true);
			BotUtils.sendEmbed(builder.build(), context.getChannel());

		} catch (FileNotFoundException e) {
			BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " This user doesn't play to Diablo 3 or doesn't exist.", context.getChannel());
		} catch (IOException e) {
			LogUtils.error("Something went wrong while getting Diablo 3 stats.... Please, try again later.", e, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show user stats for Diablo 3.**")
				.appendField("Usage", context.getPrefix() + "diablo <eu|us|tw|kr> <battletag#0000>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
