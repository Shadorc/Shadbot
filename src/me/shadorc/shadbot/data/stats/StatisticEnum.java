package me.shadorc.shadbot.data.stats;

import me.shadorc.shadbot.data.stats.core.Statistic;

public enum StatisticEnum {
    VARIOUS(StatsManager.VARIOUS_STATS),
    COMMAND(StatsManager.COMMAND_STATS);

    private final Statistic<?> stat;

    StatisticEnum(Statistic<?> stat) {
        this.stat = stat;
    }

    public Statistic<?> getStat() {
        return this.stat;
    }

}
