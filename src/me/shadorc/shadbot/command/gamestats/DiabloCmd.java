package me.shadorc.shadbot.command.gamestats;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

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
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;

@RateLimited
@Command(category = CommandCategory.GAMESTATS, names = { "diablo" }, alias = "d3")
public class DiabloCmd extends AbstractCommand {

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
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid Region. %s",
					splitArgs.get(0), FormatUtils.formatOptions(Region.class)));
		}

		String battletag = splitArgs.get(1);
		if(!battletag.matches("(\\p{L}*)#[0-9]*")) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid Battletag.", splitArgs.get(1)));
		}
		battletag = battletag.replaceAll("#", "-");

		LoadingMessage loadingMsg = new LoadingMessage("Loading Diablo 3 stats...", context.getChannel());
		loadingMsg.send();

		try {
			String url = String.format("https://%s.api.battle.net/d3/profile/%s/?locale=en_GB&apikey=%s",
					region, NetUtils.encode(battletag), APIKeys.get(APIKey.BLIZZARD_API_KEY));
			JSONObject playerObj = new JSONObject(NetUtils.getJSON(url));

			if(playerObj.has("code") && playerObj.getString("code").equals("NOTFOUND")) {
				loadingMsg.edit(Emoji.MAGNIFYING_GLASS + " This user doesn't play Diablo 3 or doesn't exist.");
				return;
			}

			TreeMap<Double, String> heroesMap = new TreeMap<>(Collections.reverseOrder());
			JSONArray heroesArray = playerObj.getJSONArray("heroes");
			for(int i = 0; i < heroesArray.length(); i++) {
				JSONObject heroObj = heroesArray.getJSONObject(i);

				String name = heroObj.getString("name");
				String heroClass = StringUtils.capitalize(heroObj.getString("class").replace("-", " "));

				url = String.format("https://%s.api.battle.net/d3/profile/%s/hero/%d?locale=en_GB&apikey=%s",
						region, NetUtils.encode(battletag), heroObj.getLong("id"), APIKeys.get(APIKey.BLIZZARD_API_KEY));
				JSONObject statsHeroObj = new JSONObject(NetUtils.getJSON(url));

				Double dps = statsHeroObj.has("code") ? Double.NaN : statsHeroObj.getJSONObject("stats").getDouble("damage");
				heroesMap.put(dps, String.format("**%s** (*%s*)", name, heroClass));
			}

			EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
					.setLenient(true)
					.withAuthorName("Diablo 3 Stats")
					.withThumbnail("http://osx.wdfiles.com/local--files/icon:d3/D3.png")
					.appendDescription(String.format("Stats for **%s** (Guild: **%s**)"
							+ "%n%nParangon level: **%s** (*Normal*) / **%s** (*Hardcore*)"
							+ "%nSeason Parangon level: **%s** (*Normal*) / **%s** (*Hardcore*)",
							playerObj.getString("battleTag"), playerObj.getString("guildName"),
							playerObj.getInt("paragonLevel"), playerObj.getInt("paragonLevelSeasonHardcore"),
							playerObj.getInt("paragonLevelSeason"), playerObj.getInt("paragonLevelSeasonHardcore")))
					.addField("Heroes", FormatUtils.format(heroesMap.values().stream(), Object::toString, "\n"), true)
					.addField("Damage", FormatUtils.format(heroesMap.keySet().stream(),
							dps -> String.format("%s DPS", FormatUtils.formatNum(dps)), "\n"), true);
			loadingMsg.edit(embed.build());

		} catch (FileNotFoundException err) {
			loadingMsg.delete();
			BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " This user doesn't play Diablo 3 or doesn't exist.", context.getChannel());
		} catch (JSONException | IOException err) {
			loadingMsg.delete();
			Utils.handle("getting Diablo 3 stats", context, err);
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
