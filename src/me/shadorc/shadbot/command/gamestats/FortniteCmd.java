package me.shadorc.shadbot.command.gamestats;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;

@RateLimited
@Command(category = CommandCategory.GAMESTATS, names = { "fortnite" })
public class FortniteCmd extends AbstractCommand {

	private final static String SOLO_STATS = "p2";
	private final static String DUO_STATS = "p10";
	private final static String SQUAD_STATS = "p9";
	private final static String SEASON = "curr_";

	private enum Platform {
		PC, XBL, PSN;
	}

	private enum Stats {
		TOP1_SOLO,
		TOP1_DUO,
		TOP1_SQUAD,
		KD_S3_SOLO,
		KD_S3_DUO,
		KD_S3_SQUAD,
		KD_LT_SOLO,
		KD_LT_DUO,
		KD_LT_SQUAD;
	}

	@Override
	public void execute(Context context) {
		List<String> args = context.requireArgs(2);

		Platform platform = Utils.getValueOrNull(Platform.class, args.get(0));
		if(platform == null) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid Platform. %s",
					args.get(0), FormatUtils.formatOptions(Platform.class)));
		}

		String epicNickname = args.get(1);

		LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			String url = String.format("https://api.fortnitetracker.com/v1/profile/%s/%s",
					platform.toString().toLowerCase(), epicNickname);

			Response response = Jsoup.connect(url)
					.method(Method.GET)
					.ignoreContentType(true)
					.ignoreHttpErrors(true)
					.header("TRN-Api-Key", APIKeys.get(APIKey.FORTNITE_API_KEY))
					.execute();

			if(response.statusCode() != 200) {
				throw new HttpStatusException("Fortnite API did not return a valid status code.", 503, url);
			}

			JSONObject mainObj = new JSONObject(response.parse().body().html());

			if("Player Not Found".equals(mainObj.optString("error"))) {
				loadingMsg.send(Emoji.MAGNIFYING_GLASS + " This user doesn't play Fortnite on this platform or doesn't exist.");
				return;
			}

			JSONObject statsObj = mainObj.getJSONObject("stats");

			Map<Stats, String> statsMap = new HashMap<>();

			JSONObject soloStatsObj = statsObj.optJSONObject(SOLO_STATS);
			statsMap.put(Stats.TOP1_SOLO, this.getTop1(soloStatsObj));
			statsMap.put(Stats.KD_LT_SOLO, this.getKD(soloStatsObj));

			JSONObject duoStatsObj = statsObj.optJSONObject(DUO_STATS);
			statsMap.put(Stats.TOP1_DUO, this.getTop1(duoStatsObj));
			statsMap.put(Stats.KD_LT_DUO, this.getKD(duoStatsObj));

			JSONObject squadStatsObj = statsObj.optJSONObject(SQUAD_STATS);
			statsMap.put(Stats.TOP1_SQUAD, this.getTop1(squadStatsObj));
			statsMap.put(Stats.KD_LT_SQUAD, this.getKD(squadStatsObj));

			JSONObject soloSeasonStatsObj = statsObj.optJSONObject(SEASON + SOLO_STATS);
			statsMap.put(Stats.KD_S3_SOLO, this.getKD(soloSeasonStatsObj));

			JSONObject duoSeasonStatsObj = statsObj.optJSONObject(SEASON + DUO_STATS);
			statsMap.put(Stats.KD_S3_DUO, this.getKD(duoSeasonStatsObj));

			JSONObject squadSeasonStatsObj = statsObj.optJSONObject(SEASON + SQUAD_STATS);
			statsMap.put(Stats.KD_S3_SQUAD, this.getKD(squadSeasonStatsObj));

			int length = 10;
			String format = "%n%-" + (length + 5) + "s %-" + length + "s %-" + length + "s %-" + length + "s";

			String description = String.format("Stats for user **%s**%n", epicNickname)
					+ "```prolog"
					+ String.format(format, " ", "Solo", "Duo", "Squad")
					+ String.format(format, "Top 1", statsMap.get(Stats.TOP1_SOLO), statsMap.get(Stats.TOP1_DUO), statsMap.get(Stats.TOP1_SQUAD))
					+ String.format(format, "K/D season", statsMap.get(Stats.KD_S3_SOLO), statsMap.get(Stats.KD_S3_DUO), statsMap.get(Stats.KD_S3_SQUAD))
					+ String.format(format, "K/D lifetime", statsMap.get(Stats.KD_LT_SOLO), statsMap.get(Stats.KD_LT_DUO), statsMap.get(Stats.KD_LT_SQUAD))
					+ "```";

			EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed("Fortnite Stats",
					String.format("https://fortnitetracker.com/profile/%s/%s", platform.toString().toLowerCase(), NetUtils.encode(epicNickname)))
					.setThumbnail("https://orig00.deviantart.net/9517/f/2017/261/9/f/fortnite___icon_by_blagoicons-dbnu8a0.png")
					.setDescription(description);
			loadingMsg.send(embed);

		} catch (JSONException | IOException err) {
			loadingMsg.send(ExceptionUtils.handleAndGet("getting Fortnite stats", context, err));
		}

	}

	private String getTop1(JSONObject statsObj) {
		if(statsObj == null) {
			return "0";
		}
		return Integer.toString(statsObj.getJSONObject("top1").getInt("valueInt"));
	}

	private String getKD(JSONObject statsObj) {
		if(statsObj == null) {
			return "0";
		}
		return Double.toString(statsObj.getJSONObject("kd").getDouble("valueDec"));
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show player's stats for Fortnite.")
				.addArg("platform", String.format("user's platform (%s)", FormatUtils.format(Platform.values(), region -> region.toString().toLowerCase(), ", ")), false)
				.addArg("epic-nickname", false)
				.setExample(String.format("`%s%s pc Shadbot`", prefix, this.getName()))
				.build();
	}

}
