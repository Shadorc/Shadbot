package me.shadorc.shadbot.data.stats;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;

import me.shadorc.shadbot.data.stats.annotation.StatsInit;
import me.shadorc.shadbot.data.stats.annotation.StatsJSON;
import me.shadorc.shadbot.utils.embed.EmbedUtils;

public class VariousStatsManager {

	public enum VariousEnum {
		MUSICS_LOADED,
		COMMANDS_EXECUTED,
		MESSAGES_RECEIVED,
		PRIVATE_MESSAGES_RECEIVED,
		MESSAGES_SENT,
		EMBEDS_SENT
	}

	private static final String NAME = "various";
	private static final Map<VariousEnum, AtomicInteger> VARIOUS_STATS_MAP = new HashMap<>();

	@StatsInit(name = NAME)
	public static void load(JSONObject mainObj) {
		StatsManager.register(NAME, () -> EmbedUtils.getStatsEmbed(VARIOUS_STATS_MAP, NAME));

		for(VariousEnum variousEnum : VariousEnum.values()) {
			VARIOUS_STATS_MAP.put(variousEnum, new AtomicInteger(mainObj.optInt(variousEnum.toString())));
		}

	}

	public static void log(VariousEnum key) {
		VARIOUS_STATS_MAP.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
	}

	@StatsJSON(name = NAME)
	public static JSONObject toJSON() {
		JSONObject mainObj = new JSONObject();
		for(VariousEnum variousEnum : VariousEnum.values()) {
			mainObj.put(variousEnum.toString(), VARIOUS_STATS_MAP.get(variousEnum));
		}
		return mainObj;
	}
}
