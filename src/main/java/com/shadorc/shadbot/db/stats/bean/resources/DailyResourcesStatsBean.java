package com.shadorc.shadbot.db.stats.bean.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.db.Bean;

import java.util.List;

public class DailyResourcesStatsBean implements Bean {

    @JsonProperty("system_resources")
    private List<ResourcesStatsBean> systemResourcesStats;

    public List<ResourcesStatsBean> getSystemResourcesStats() {
        return this.systemResourcesStats;
    }

    @Override
    public String toString() {
        return "DailySystemResourcesStatsBean{" +
                "systemResourcesStats=" + this.systemResourcesStats +
                '}';
    }
}
