package me.shadorc.shadbot.data.stats;

import java.io.File;
import java.io.IOException;
import java.time.temporal.ChronoUnit;

import me.shadorc.shadbot.data.annotation.DataInit;
import me.shadorc.shadbot.data.annotation.DataSave;
import me.shadorc.shadbot.data.stats.core.MapStatistic;
import me.shadorc.shadbot.data.stats.core.Statistic;
import me.shadorc.shadbot.data.stats.core.TableStatistic;
import me.shadorc.shadbot.data.stats.enums.CommandEnum;
import me.shadorc.shadbot.data.stats.enums.MoneyEnum;
import me.shadorc.shadbot.data.stats.enums.VariousEnum;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

public class StatsManager {

	public static final File STATS_DIR = new File("./stats");

	public static MapStatistic<VariousEnum> VARIOUS_STATS = new MapStatistic<>("various.json", VariousEnum.class);
	public static TableStatistic<CommandEnum> COMMAND_STATS = new TableStatistic<>("command.json", CommandEnum.class);
	public static TableStatistic<MoneyEnum> MONEY_STATS = new TableStatistic<>("money.json", MoneyEnum.class);

	public enum StatisticEnum {
		VARIOUS(VARIOUS_STATS),
		COMMAND(COMMAND_STATS),
		MONEY(MONEY_STATS);

		private final Statistic<?> stat;

		StatisticEnum(Statistic<?> stat) {
			this.stat = stat;
		}

		public Statistic<?> getStat() {
			return stat;
		}

	}

	@DataInit
	public static void init() throws IOException {
		if(!STATS_DIR.exists() && !STATS_DIR.mkdir()) {
			LogUtils.error("The statistics directory could not be created.");
			return;
		}
	}

	@DataSave(initialDelay = 10, period = 10, unit = ChronoUnit.MINUTES)
	public static void save() throws IOException {
		for(StatisticEnum statistic : StatisticEnum.values()) {
			statistic.getStat().save();
		}
	}

}