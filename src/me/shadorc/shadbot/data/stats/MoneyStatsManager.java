package me.shadorc.shadbot.data.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.json.JSONObject;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.data.lotto.LottoManager;
import me.shadorc.shadbot.data.stats.CommandStatsManager.CommandEnum;
import me.shadorc.shadbot.data.stats.annotation.StatsInit;
import me.shadorc.shadbot.data.stats.annotation.StatsJSON;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Pair;
import sx.blah.discord.util.EmbedBuilder;

public class MoneyStatsManager {

	public enum MoneyEnum {
		MONEY_GAINED,
		MONEY_LOST
	}

	private static final String NAME = "money";
	private static final Map<MoneyEnum, Map<String, AtomicLong>> MONEY_STATS_MAP = new HashMap<>();

	@StatsInit(name = NAME)
	public static void load(JSONObject mainObj) {
		for(MoneyEnum moneyEnum : MoneyEnum.values()) {
			StatsManager.register(moneyEnum.toString().toLowerCase(),
					() -> EmbedUtils.getStatsEmbed(MONEY_STATS_MAP.get(moneyEnum), moneyEnum.toString().toLowerCase()));
		}
		StatsManager.register("average", () -> MoneyStatsManager.getAverageEmbed());

		for(MoneyEnum moneyEnum : MoneyEnum.values()) {
			JSONObject moneyStatsObj = mainObj.optJSONObject(moneyEnum.toString());
			if(moneyStatsObj != null) {
				MONEY_STATS_MAP.put(moneyEnum, new HashMap<>());
				for(String key : moneyStatsObj.keySet()) {
					MONEY_STATS_MAP.get(moneyEnum).put(key, new AtomicLong(moneyStatsObj.getLong(key)));
				}
			}
		}
	}

	public static void log(MoneyEnum key, String cmd, int coins) {
		if(MoneyEnum.MONEY_LOST.equals(key)) {
			LottoManager.addToPool(coins);
		}

		MONEY_STATS_MAP.computeIfAbsent(key, k -> new HashMap<>());
		MONEY_STATS_MAP.get(key).computeIfAbsent(cmd, k -> new AtomicLong(0)).addAndGet(coins);
	}

	public static void log(MoneyEnum key, AbstractCommand cmd, int coins) {
		MoneyStatsManager.log(key, cmd.getName(), coins);
	}

	public static EmbedBuilder getAverageEmbed() {
		Map<String, AtomicLong> moneyGained = MONEY_STATS_MAP.getOrDefault(MoneyEnum.MONEY_GAINED, Collections.emptyMap());
		Map<String, AtomicLong> moneyLost = MONEY_STATS_MAP.getOrDefault(MoneyEnum.MONEY_LOST, Collections.emptyMap());

		EmbedBuilder embed = EmbedUtils.getDefaultEmbed().withAuthorName("Stats: average");

		List<String> gamesName = new ArrayList<>();
		gamesName.addAll(moneyGained.keySet());
		gamesName.addAll(moneyLost.keySet());
		gamesName = gamesName.stream().distinct().collect(Collectors.toList());

		if(gamesName.isEmpty()) {
			return embed.withDescription("No statistics yet.");
		}

		Map<String, AtomicInteger> commandsUsed = CommandStatsManager.get(CommandEnum.COMMAND_USED);

		Map<String, Pair<Float, Long>> averageMap = new HashMap<>();
		for(String gameName : gamesName) {
			long gains = moneyGained.getOrDefault(gameName, new AtomicLong(0)).get();
			long losses = moneyLost.getOrDefault(gameName, new AtomicLong(0)).get();
			long usages = commandsUsed.get(gameName).get();
			float average = ((float) gains - losses) / usages;
			averageMap.put(gameName, new Pair<Float, Long>(average, usages));
		}

		Comparator<Entry<String, Pair<Float, Long>>> comparator = (v1, v2) -> v1.getValue().getSecond().compareTo(v2.getValue().getSecond());
		Map<String, Pair<Float, Long>> sortedMap = Utils.sortByValue(averageMap, comparator.reversed());

		return embed.appendField("Name", FormatUtils.format(sortedMap.keySet().stream(), Object::toString, "\n"), true)
				.appendField("Average", FormatUtils.format(sortedMap.values().stream().map(Pair::getFirst), num -> FormatUtils.formatNum(num.intValue()), "\n"), true)
				.appendField("Count", FormatUtils.format(sortedMap.values().stream().map(Pair::getSecond), num -> FormatUtils.formatNum(num), "\n"), true);
	}

	@StatsJSON(name = NAME)
	public static JSONObject toJSON() {
		JSONObject mainObj = new JSONObject();
		for(MoneyEnum moneyEnum : MONEY_STATS_MAP.keySet()) {
			JSONObject statsObj = new JSONObject();
			for(String key : MONEY_STATS_MAP.get(moneyEnum).keySet()) {
				statsObj.put(key, MONEY_STATS_MAP.get(moneyEnum).get(key).get());
			}
			mainObj.put(moneyEnum.toString(), statsObj);
		}
		return mainObj;
	}

}
