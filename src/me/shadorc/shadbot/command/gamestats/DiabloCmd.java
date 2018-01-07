package me.shadorc.shadbot.command.gamestats;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
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
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.GAMESTATS, names = { "diablo" }, alias = "d3")
public class DiabloCmd extends AbstractCommand {

	private final DecimalFormat formatter = new DecimalFormat("#,###");

	private enum Region {
		EU, US, TW, KR;
	}

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		List<String> splitArgs = StringUtils.split(context.getArg(), 2);
		if(splitArgs.size() != 2) {
			throw new MissingArgumentException();
		}

		Region region = Utils.getValueOrNull(Region.class, splitArgs.get(0));
		if(region == null) {
			throw new IllegalCmdArgumentException("Region is invalid. Options: "
					+ FormatUtils.format(Region.values(), Object::toString, ", "));
		}

		String battletag = splitArgs.get(1);
		if(!battletag.matches("(\\p{L}*)#[0-9]*")) {
			throw new IllegalCmdArgumentException("Invalid Battletag.");
		}
		battletag = battletag.replaceAll("#", "-");

		try {
			String url = String.format("https://%s.api.battle.net/d3/profile/%s/?locale=en_GB&apikey=%s",
					region, NetUtils.encode(battletag), APIKeys.get(APIKey.BLIZZARD_API_KEY));
			JSONObject playerObj = new JSONObject(NetUtils.getBody(url));

			if(playerObj.has("code") && playerObj.getString("code").equals("NOTFOUND")) {
				BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " This user doesn't play Diablo 3 or doesn't exist.", context.getChannel());
				return;
			}

			List<JSONObject> heroesList = new ArrayList<>();
			JSONArray heroesArray = playerObj.getJSONArray("heroes");
			for(int i = 0; i < heroesArray.length(); i++) {
				url = String.format("https://%s.api.battle.net/d3/profile/%s/hero/%d?locale=en_GB&apikey=%s",
						region, NetUtils.encode(battletag), heroesArray.getJSONObject(i).getLong("id"), APIKeys.get(APIKey.BLIZZARD_API_KEY));
				heroesList.add(new JSONObject(NetUtils.getBody(url)));
			}

			EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
					.setLenient(true)
					.withAuthorName("Diablo 3 Stats")
					.withThumbnail("http://osx.wdfiles.com/local--files/icon:d3/D3.png")
					.appendDescription(String.format("Stats for **%s** (Guild: **%s**)",
							playerObj.getString("battleTag"), playerObj.getString("guildName")))
					.appendField("__Parangon level__",
							String.format("**Normal:** %d%n**Hardcore:** %d",
									playerObj.getInt("paragonLevel"), playerObj.getInt("paragonLevelHardcore")), true)
					.appendField("__Season Parangon level__",
							String.format("**Normal:** %d%n**Hardcore:** %d",
									playerObj.getInt("paragonLevelSeason"), playerObj.getInt("paragonLevelSeasonHardcore")), true)
					.appendField("__Heroes__",
							FormatUtils.format(heroesList,
									heroObj -> String.format("**%s** (*%s*)",
											heroObj.getString("name"), StringUtils.capitalize(heroObj.getString("class").replace("-", " "))),
									"\n"), true)
					.appendField("__Damage__",
							FormatUtils.format(heroesList,
									heroObj -> String.format("%d DPS",
											formatter.format(heroObj.getJSONObject("stats").getDouble("damage"))),
									"\n"), true);
			BotUtils.sendMessage(embed.build(), context.getChannel());

		} catch (FileNotFoundException err) {
			BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " This user doesn't play Diablo 3 or doesn't exist.", context.getChannel());
		} catch (JSONException | IOException err) {
			ExceptionUtils.handle("getting Diablo 3 stats", context, err);
		}
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show player's stats for Diablo 3.")
				.addArg("region", String.format("user's region (%s)", FormatUtils.format(Region.values(), region -> region.toString().toLowerCase(), ", ")), false)
				.addArg("battletag#0000", false)
				.setExample(String.format("`%s%s eu Shadbot#1758`", prefix, this.getName()))
				.build();
	}

}
