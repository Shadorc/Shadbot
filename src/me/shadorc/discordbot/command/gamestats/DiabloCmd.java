package me.shadorc.discordbot.command.gamestats;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.Config.APIKey;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.ExceptionUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class DiabloCmd extends AbstractCommand {

	private final DecimalFormat formatter = new DecimalFormat("#,###");

	public DiabloCmd() {
		super(CommandCategory.GAMESTATS, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "diablo");
		this.setAlias("d3");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		String[] splitArgs = StringUtils.getSplittedArg(context.getArg(), 2);
		if(splitArgs.length != 2) {
			throw new MissingArgumentException();
		}

		String region = splitArgs[0].toLowerCase();
		if(!Arrays.asList("eu", "us", "tw", "kr").contains(region)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Region is invalid. Options: eu, us, tw, kr.", context.getChannel());
			return;
		}

		String battletag = splitArgs[1].replaceAll("#", "-");
		try {
			JSONObject mainObj = new JSONObject(NetUtils.getBody("https://" + region + ".api.battle.net/d3/profile"
					+ "/" + URLEncoder.encode(battletag, "UTF-8") + "/?"
					+ "locale=en_GB&"
					+ "apikey=" + Config.get(APIKey.BLIZZARD_API_KEY)));

			if(mainObj.has("code") && mainObj.getString("code").equals("NOTFOUND")) {
				BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " This user doesn't play Diablo 3 or doesn't exist.", context.getChannel());
				return;
			}

			List<JSONObject> heroesList = new ArrayList<>();
			JSONArray heroesArray = mainObj.getJSONArray("heroes");
			for(int i = 0; i < heroesArray.length(); i++) {
				JSONObject heroObj = new JSONObject(NetUtils.getBody(
						"https://" + region + ".api.battle.net/d3/profile/" + URLEncoder.encode(battletag, "UTF-8")
								+ "/hero/" + heroesArray.getJSONObject(i).getLong("id")
								+ "?locale=en_GB"
								+ "&apikey=" + Config.get(APIKey.BLIZZARD_API_KEY)));
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
			BotUtils.sendMessage(builder.build(), context.getChannel());

		} catch (FileNotFoundException err) {
			BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " This user doesn't play Diablo 3 or doesn't exist.", context.getChannel());
		} catch (JSONException | IOException err) {
			ExceptionUtils.manageException("getting Diablo 3 stats", context, err);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show player's stats for Diablo 3.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <eu|us|tw|kr> <battletag#0000>`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

}
