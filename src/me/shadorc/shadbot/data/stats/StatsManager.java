package me.shadorc.shadbot.data.stats;

import java.io.File;
import java.io.IOException;
import java.time.temporal.ChronoUnit;

import me.shadorc.shadbot.data.annotation.DataInit;
import me.shadorc.shadbot.data.annotation.DataSave;
import me.shadorc.shadbot.data.stats.core.MapStat;
import me.shadorc.shadbot.data.stats.core.TableStat;
import me.shadorc.shadbot.data.stats.enums.CommandEnum;
import me.shadorc.shadbot.data.stats.enums.MoneyEnum;
import me.shadorc.shadbot.data.stats.enums.VariousEnum;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

public class StatsManager {

	public static final File STATS_DIR = new File("./stats");

	public static MapStat<VariousEnum> VARIOUS_STATS;
	public static TableStat<CommandEnum> COMMAND_STATS;
	public static TableStat<MoneyEnum> MONEY_STATS;

	@DataInit
	public static void init() throws IOException {
		if(!STATS_DIR.exists() && !STATS_DIR.mkdir()) {
			LogUtils.error("The statistics directory could not be created.");
			return;
		}

		VARIOUS_STATS = new MapStat<>(new File("various.json"));
		COMMAND_STATS = new TableStat<>(new File("command.json"));
		MONEY_STATS = new TableStat<>(new File("money.json"));
	}

	@DataSave(initialDelay = 10, period = 10, unit = ChronoUnit.MINUTES)
	public static void save() throws IOException {
		VARIOUS_STATS.save();
		COMMAND_STATS.save();
		MONEY_STATS.save();
	}

}