package com.shadorc.shadbot.db.stats.bean.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.db.Bean;

import java.util.Collections;
import java.util.List;

public class DailyResourceStatsBean implements Bean {

    @JsonProperty("system_resources")
    private List<ResourcesStatsBean> systemResourceStats;

    public List<ResourcesStatsBean> getSystemResourceStats() {
        return Collections.unmodifiableList(this.systemResourceStats);
    }

    @Override
    public String toString() {
        return "DailyResourceStatsBean{" +
                "systemResourceStats=" + this.systemResourceStats +
                '}';
    }
}
