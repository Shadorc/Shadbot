package me.shadorc.shadbot.data.stats;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.data.stats.annotation.StatsInit;
import me.shadorc.shadbot.data.stats.annotation.StatsJSON;
import me.shadorc.shadbot.utils.embed.EmbedUtils;

public class CommandStatsManager {

	public enum CommandEnum {
		COMMAND_USED,
		COMMAND_LIMITED,
		COMMAND_HELPED,
		COMMAND_MISSING_ARG,
		COMMAND_ILLEGAL_ARG;
	}

	private static final String NAME = "commands";
	private static final Map<CommandEnum, Map<String, AtomicInteger>> COMMANDS_STATS_MAP = new HashMap<>();

	@StatsInit(name = NAME)
	public static void load(JSONObject mainObj) {
		for(CommandEnum cmdEnum : CommandEnum.values()) {
			StatsManager.register(cmdEnum.toString().toLowerCase(),
					() -> EmbedUtils.getStatsEmbed(COMMANDS_STATS_MAP.get(cmdEnum), cmdEnum.toString().toLowerCase()));
		}

		for(CommandEnum cmdEnum : CommandEnum.values()) {
			JSONObject cmdStatsObj = mainObj.optJSONObject(cmdEnum.toString());
			if(cmdStatsObj != null) {
				COMMANDS_STATS_MAP.put(cmdEnum, new HashMap<>());
				for(String key : cmdStatsObj.keySet()) {
					COMMANDS_STATS_MAP.get(cmdEnum).put(key, new AtomicInteger(cmdStatsObj.getInt(key)));
				}
			}
		}

	}

	public static void log(CommandEnum key, AbstractCommand cmd) {
		COMMANDS_STATS_MAP.computeIfAbsent(key, k -> new HashMap<>());
		COMMANDS_STATS_MAP.get(key).computeIfAbsent(cmd.getName(), k -> new AtomicInteger(0)).incrementAndGet();
	}

	public static Map<String, AtomicInteger> get(CommandEnum cmdEnum) {
		return COMMANDS_STATS_MAP.get(cmdEnum);
	}

	@StatsJSON(name = NAME)
	public static JSONObject toJSON() {
		JSONObject mainObj = new JSONObject();
		for(CommandEnum cmdEnum : COMMANDS_STATS_MAP.keySet()) {
			JSONObject statsObj = new JSONObject();
			for(String key : COMMANDS_STATS_MAP.get(cmdEnum).keySet()) {
				statsObj.put(key, COMMANDS_STATS_MAP.get(cmdEnum).get(key).get());
			}
			mainObj.put(cmdEnum.toString(), statsObj);
		}
		return mainObj;
	}

}
