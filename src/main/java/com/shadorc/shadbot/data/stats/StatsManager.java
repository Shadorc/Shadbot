package com.shadorc.shadbot.data.stats;

import com.shadorc.shadbot.data.Data;
import com.shadorc.shadbot.data.stats.core.MapStatistic;
import com.shadorc.shadbot.data.stats.core.TableStatistic;
import com.shadorc.shadbot.data.stats.enums.CommandEnum;
import com.shadorc.shadbot.data.stats.enums.VariousEnum;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

public class StatsManager extends Data {

    public static final File STATS_DIR = new File("./stats");

    public static final MapStatistic<VariousEnum> VARIOUS_STATS = new MapStatistic<>("various.json", VariousEnum.class);
    public static final TableStatistic<CommandEnum> COMMAND_STATS = new TableStatistic<>("command.json", CommandEnum.class);

    private static StatsManager instance;

    static {
        StatsManager.instance = new StatsManager();
    }

    private StatsManager() {
        super("statistics", Duration.ofMinutes(10), Duration.ofMinutes(10));
    }

    @Override
    public void write() throws IOException {
        for (final StatisticEnum statistic : StatisticEnum.values()) {
            statistic.getStat().save();
        }
    }

    @Override
    public Object getData() {
        return null;
    }

    public static StatsManager getInstance() {
        return StatsManager.instance;
    }

}