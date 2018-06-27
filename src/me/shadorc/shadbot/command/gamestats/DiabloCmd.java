package me.shadorc.shadbot.command.gamestats;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.GAMESTATS, names = { "diablo" }, alias = "d3")
public class DiabloCmd extends AbstractCommand {

	private enum Region {
		EU, US, TW, KR;
	}

	@Override
	public void execute(Context context) {
		List<String> args = context.requireArgs(2);

		Region region = Utils.getValueOrNull(Region.class, args.get(0));
		if(region == null) {
			throw new CommandException(String.format("`%s` is not a valid Region. %s",
					args.get(0), FormatUtils.formatOptions(Region.class)));
		}

		String battletag = args.get(1);
		if(!battletag.matches("(\\p{L}*)#[0-9]*")) {
			throw new CommandException(String.format("`%s` is not a valid Battletag.", args.get(1)));
		}
		battletag = battletag.replaceAll("#", "-");

		LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			String url = String.format("https://%s.api.battle.net/d3/profile/%s/?locale=en_GB&apikey=%s",
					region, NetUtils.encode(battletag), APIKeys.get(APIKey.BLIZZARD_API_KEY));
			JSONObject playerObj = new JSONObject(NetUtils.getJSON(url));

			if(playerObj.has("code") && playerObj.getString("code").equals("NOTFOUND")) {
				loadingMsg.send(Emoji.MAGNIFYING_GLASS + " This user doesn't play Diablo 3 or doesn't exist.");
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

			context.getAuthorAvatarUrl().subscribe(avatarUrl -> {
				EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
						.setAuthor("Diablo 3 Stats", null, avatarUrl)
						.setThumbnail("http://osx.wdfiles.com/local--files/icon:d3/D3.png")
						.setDescription(String.format("Stats for **%s** (Guild: **%s**)"
								+ "%n%nParangon level: **%s** (*Normal*) / **%s** (*Hardcore*)"
								+ "%nSeason Parangon level: **%s** (*Normal*) / **%s** (*Hardcore*)",
								playerObj.getString("battleTag"), playerObj.getString("guildName"),
								playerObj.getInt("paragonLevel"), playerObj.getInt("paragonLevelSeasonHardcore"),
								playerObj.getInt("paragonLevelSeason"), playerObj.getInt("paragonLevelSeasonHardcore")))
						.addField("Heroes", FormatUtils.format(heroesMap.values().stream(), Object::toString, "\n"), true)
						.addField("Damage", FormatUtils.format(heroesMap.keySet().stream(),
								dps -> String.format("%s DPS", FormatUtils.formatNum(dps)), "\n"), true);
				loadingMsg.send(embed);
			});

		} catch (FileNotFoundException err) {
			loadingMsg.send(Emoji.MAGNIFYING_GLASS + " This user doesn't play Diablo 3 or doesn't exist.");
		} catch (JSONException | IOException err) {
			loadingMsg.send(ExceptionUtils.handleAndGet("getting Diablo 3 stats", context, err));
		}
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show player's stats for Diablo 3.")
				.addArg("region", String.format("user's region (%s)", FormatUtils.format(Region.values(), region -> region.toString().toLowerCase(), ", ")), false)
				.addArg("battletag#0000", false)
				.setExample(String.format("`%s%s eu Shadbot#1758`", context.getPrefix(), this.getName()))
				.build();
	}

}
