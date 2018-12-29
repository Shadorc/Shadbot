package me.shadorc.shadbot.data.stats;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import me.shadorc.shadbot.data.Data;
import me.shadorc.shadbot.data.stats.core.MapStatistic;
import me.shadorc.shadbot.data.stats.core.TableStatistic;
import me.shadorc.shadbot.data.stats.enums.CommandEnum;
import me.shadorc.shadbot.data.stats.enums.MoneyEnum;
import me.shadorc.shadbot.data.stats.enums.VariousEnum;

public class StatsManager extends Data {

	public static final File STATS_DIR = new File("./stats");

	public static final MapStatistic<VariousEnum> VARIOUS_STATS = new MapStatistic<>("various.json", VariousEnum.class);
	public static final TableStatistic<CommandEnum> COMMAND_STATS = new TableStatistic<>("command.json", CommandEnum.class);
	public static final TableStatistic<MoneyEnum> MONEY_STATS = new TableStatistic<>("money.json", MoneyEnum.class);

	public StatsManager() {
		super("statistics", Duration.ofMinutes(10), Duration.ofMinutes(10));
	}

	@Override
	public void write() throws IOException {
		for(final StatisticEnum statistic : StatisticEnum.values()) {
			statistic.getStat().save();
		}
	}

	@Override
	public Object getData() {
		return null;
	}

}