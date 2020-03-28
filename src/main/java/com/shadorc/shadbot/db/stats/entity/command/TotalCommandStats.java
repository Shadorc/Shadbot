package com.shadorc.shadbot.db.stats.entity.command;

import com.shadorc.shadbot.db.SerializableEntity;
import com.shadorc.shadbot.db.stats.bean.command.TotalCommandStatsBean;

import java.util.List;
import java.util.stream.Collectors;

public class TotalCommandStats extends SerializableEntity<TotalCommandStatsBean> {

    public TotalCommandStats(TotalCommandStatsBean bean) {
        super(bean);
    }

    public List<DailyCommandStats> getDailyCommandStats() {
        return this.getBean().getCommandStats()
                .entrySet()
                .stream()
                .map(entry -> new DailyCommandStats(entry.getKey(), entry.getValue()))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public String toString() {
        return "TotalCommandStats{" +
                "bean=" + this.getBean() +
                '}';
    }
}
