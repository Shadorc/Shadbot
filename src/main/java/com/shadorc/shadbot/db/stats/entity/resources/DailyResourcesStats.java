package com.shadorc.shadbot.db.stats.entity.resources;

import com.shadorc.shadbot.db.SerializableEntity;
import com.shadorc.shadbot.db.stats.bean.resources.DailyResourcesStatsBean;

import java.util.List;
import java.util.stream.Collectors;

public class DailyResourcesStats extends SerializableEntity<DailyResourcesStatsBean> {

    public DailyResourcesStats(DailyResourcesStatsBean bean) {
        super(bean);
    }

    public List<ResourcesStats> getResourcesUsage() {
        return this.getBean().getSystemResourcesStats().stream()
                .map(ResourcesStats::new)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public String toString() {
        return "DailyResourcesStats{" +
                "bean=" + this.getBean() +
                '}';
    }

}
