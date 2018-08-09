package me.shadorc.shadbot.data.stats;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.shadorc.shadbot.data.annotation.DataInit;
import me.shadorc.shadbot.data.annotation.DataSave;
import me.shadorc.shadbot.data.stats.core.SimpleStat;
import me.shadorc.shadbot.data.stats.core.TableStat;
import me.shadorc.shadbot.data.stats.enums.CommandEnum;
import me.shadorc.shadbot.data.stats.enums.DatabaseEnum;
import me.shadorc.shadbot.data.stats.enums.MoneyEnum;
import me.shadorc.shadbot.data.stats.enums.VariousEnum;
import me.shadorc.shadbot.utils.Utils;

public class StatsManager {

	public enum SimpleStatEnum {
		DATABASE,
		VARIOUS
	}

	public enum TableStatEnum {
		COMMANDS,
		MONEY
	}

	private static final String FILE_NAME = "stats.json";
	private static final File FILE = new File(FILE_NAME);

	private static Map<SimpleStatEnum, SimpleStat<?>> simpleStatsMap = new ConcurrentHashMap<>();
	private static Map<TableStatEnum, TableStat<?>> tableStatsMap = new ConcurrentHashMap<>();

	@DataInit
	public static void init() throws IOException {
		simpleStatsMap = Utils.MAPPER.readValue(FILE, 
				Utils.MAPPER.getTypeFactory().constructMapType(
						ConcurrentHashMap.class, SimpleStatEnum.class, SimpleStat.class));
		simpleStatsMap.putIfAbsent(SimpleStatEnum.DATABASE, new SimpleStat<DatabaseEnum>());
		simpleStatsMap.putIfAbsent(SimpleStatEnum.VARIOUS, new SimpleStat<VariousEnum>());

		tableStatsMap = Utils.MAPPER.readValue(FILE,
				Utils.MAPPER.getTypeFactory().constructMapType(
						ConcurrentHashMap.class, TableStatEnum.class, TableStat.class));
		tableStatsMap.putIfAbsent(TableStatEnum.COMMANDS, new TableStat<CommandEnum>());
		tableStatsMap.putIfAbsent(TableStatEnum.MONEY, new TableStat<MoneyEnum>());
	}

	@DataSave(filePath = FILE_NAME, initialDelay = 10, period = 10, unit = ChronoUnit.MINUTES)
	public static void save() throws IOException {
		try (FileWriter writer = new FileWriter(FILE)) {
			writer.write(Utils.MAPPER.writeValueAsString(simpleStatsMap));
			writer.write(Utils.MAPPER.writeValueAsString(tableStatsMap));
		}
	}

	public static SimpleStat<?> get(SimpleStatEnum stat) {
		return simpleStatsMap.get(stat);
	}

	public static TableStat<?> get(TableStatEnum stat) {
		return tableStatsMap.get(stat);
	}

}