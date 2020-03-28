package com.shadorc.shadbot.db.stats.entity.command;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DailyCommandStats {

    private final LocalDate date;
    private final Map<String, Integer> commands;

    public DailyCommandStats(int timestamps, Map<String, Integer> commands) {
        this.date = LocalDate.ofInstant(Instant.ofEpochMilli(TimeUnit.DAYS.toMillis(timestamps)), ZoneId.systemDefault());
        this.commands = commands;
    }

    public LocalDate getDate() {
        return this.date;
    }

    /**
     * @return A {@link Map} with command names as keys and number of utilization during 1 day as values.
     */
    public Map<String, Integer> getCommandStats() {
        return Collections.unmodifiableMap(this.commands);
    }

}
