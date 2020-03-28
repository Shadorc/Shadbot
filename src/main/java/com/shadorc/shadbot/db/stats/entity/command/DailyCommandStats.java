package com.shadorc.shadbot.db.stats.entity.command;

import com.shadorc.shadbot.db.SerializableEntity;
import com.shadorc.shadbot.db.stats.bean.command.DailyCommandStatsBean;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DailyCommandStats extends SerializableEntity<DailyCommandStatsBean> {

    public DailyCommandStats(DailyCommandStatsBean bean) {
        super(bean);
    }

    public String getId() {
        return this.getBean().getId();
    }

    /**
     * @return A {@link LocalDate} that corresponds to the day on which these statistics were collected.
     */
    public LocalDate getDate() {
        return LocalDate.ofInstant(
                Instant.ofEpochMilli(
                        TimeUnit.DAYS.toMillis(
                                Integer.parseInt(this.getBean().getId()))),
                ZoneId.systemDefault());
    }

    /**
     * @return A {@link Map} with command names as keys and number of utilization during 1 day as values.
     */
    public Map<String, Integer> getCommandStats() {
        return Collections.unmodifiableMap(this.getBean().getCommandStats());
    }

    @Override
    public String toString() {
        return "DailyCommandStats{" +
                "bean=" + this.getBean() +
                '}';
    }
}
