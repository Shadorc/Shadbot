package com.shadorc.shadbot.db.stats.entity.resources;

import com.shadorc.shadbot.db.SerializableEntity;
import com.shadorc.shadbot.db.stats.bean.resources.DailyResourceStatsBean;

import java.util.List;
import java.util.stream.Collectors;

public class DailyResourceStats extends SerializableEntity<DailyResourceStatsBean> {

    public DailyResourceStats(DailyResourceStatsBean bean) {
        super(bean);
    }

    public List<ResourceStats> getResourcesUsage() {
        return this.getBean().getSystemResourceStats().stream()
                .map(ResourceStats::new)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public String toString() {
        return "DailyResourceStats{" +
                "bean=" + this.getBean() +
                '}';
    }

}
