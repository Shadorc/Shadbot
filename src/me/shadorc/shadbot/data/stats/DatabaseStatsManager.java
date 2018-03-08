package me.shadorc.shadbot.data.stats;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;

import me.shadorc.shadbot.data.stats.annotation.StatsInit;
import me.shadorc.shadbot.data.stats.annotation.StatsJSON;
import me.shadorc.shadbot.utils.embed.EmbedUtils;

public class DatabaseStatsManager {

	public enum DatabaseEnum {
		GUILD_LOADED,
		USER_LOADED,
		GUILD_SAVED,
		USER_SAVED
	}

	private static final String NAME = "database";
	private static final Map<DatabaseEnum, AtomicInteger> DATABASE_STATS_MAP = new HashMap<>();

	@StatsInit(name = NAME)
	public static void load(JSONObject mainObj) {
		StatsManager.register(NAME, () -> EmbedUtils.getStatsEmbed(DATABASE_STATS_MAP, NAME));

		for(DatabaseEnum databaseEnum : DatabaseEnum.values()) {
			DATABASE_STATS_MAP.put(databaseEnum, new AtomicInteger(mainObj.optInt(databaseEnum.toString())));
		}

	}

	public static void log(DatabaseEnum key) {
		DATABASE_STATS_MAP.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
	}

	@StatsJSON(name = NAME)
	public static JSONObject toJSON() {
		JSONObject mainObj = new JSONObject();
		for(DatabaseEnum databaseEnum : DatabaseEnum.values()) {
			mainObj.put(databaseEnum.toString(), DATABASE_STATS_MAP.get(databaseEnum));
		}
		return mainObj;
	}

}
